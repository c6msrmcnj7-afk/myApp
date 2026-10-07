import Foundation
import Observation
import SharedRecipes
import SharedRecipesSwift

/// Screen level state for the recipe browser.
///
/// The store owns the Kotlin `RecipesRepository` and is the only place that
/// talks to the library. Views read the published properties and call the
/// intent methods (`reload`, `loadMoreIfNeeded`, `select`, `searchText`).
@MainActor
@Observable
final class RecipesStore {

    // MARK: Types

    /// Sort orders offered in the toolbar menu.
    enum SortOrder: String, CaseIterable, Identifiable {
        case name
        case rating
        case calories
        case prepTime

        var id: String { rawValue }

        var title: String {
            switch self {
            case .name: return "Name"
            case .rating: return "Rating"
            case .calories: return "Calories"
            case .prepTime: return "Prep time"
            }
        }

        var sortField: String {
            switch self {
            case .name: return RecipeSortField.shared.Name
            case .rating: return RecipeSortField.shared.Rating
            case .calories: return RecipeSortField.shared.CaloriesPerServing
            case .prepTime: return RecipeSortField.shared.PrepTimeMinutes
            }
        }

        var direction: String {
            switch self {
            case .name, .prepTime: return RecipeSortOrder.shared.Ascending
            case .rating, .calories: return RecipeSortOrder.shared.Descending
            }
        }
    }

    /// The category currently applied to `GET /recipes`.
    enum Category: Hashable {
        case all
        case tag(String)
        case mealType(String)

        var title: String {
            switch self {
            case .all: return "All recipes"
            case .tag(let value): return "Tag: \(value)"
            case .mealType(let value): return "Meal: \(value)"
            }
        }
    }

    /// A load that failed, kept so the UI can offer a retry.
    struct LoadFailure: Equatable {
        let kind: String
        let message: String

        var isTransient: Bool {
            let kinds = RecipesFailureKind.shared
            return kind == kinds.Network || kind == kinds.Timeout
        }
    }

    // MARK: State

    private(set) var recipes: [RecipeItem] = []
    private(set) var isLoading = false
    private(set) var isLoadingMore = false
    private(set) var failure: LoadFailure?
    private(set) var total: Int = 0
    private(set) var hasMore = false
    private(set) var category: Category = .all
    private(set) var tags: [String] = []
    private(set) var mealTypes: [String] = []

    /// Tags and meal types offered as filter chips; loaded once on first use.
    var selectedSort: SortOrder = .name {
        didSet {
            guard selectedSort != oldValue else { return }
            scheduleReload()
        }
    }

    /// Bound to `.searchable`; a short debounce keeps typing cheap.
    var searchText: String = "" {
        didSet {
            guard searchText != oldValue else { return }
            scheduleReload(delay: .milliseconds(350))
        }
    }

    // MARK: Private

    private let repository: RecipesRepository
    private let pageSize: Int32 = 12
    private var reloadTask: Task<Void, Never>?

    /// Ceiling for the paging scan that backs the meal type list.
    private let mealTypeScanLimit: Int32 = 200

    init(repository: RecipesRepository) {
        self.repository = repository
    }

    // MARK: Loading

    /// Loads the first page for the current category, search text and sort.
    func reload() async {
        reloadTask?.cancel()
        isLoading = true
        failure = nil
        defer { isLoading = false }

        do {
            let page = try await fetchPage(skip: 0)
            recipes = page.items
            total = page.total
            hasMore = page.hasMore
        } catch is CancellationError {
            return
        } catch {
            recipes = []
            total = 0
            hasMore = false
            failure = Self.describe(error)
        }
    }

    /// Appends the next page. Safe to call repeatedly, for example from
    /// `.task` on the last visible row.
    func loadMoreIfNeeded(currentItem: RecipeItem) async {
        guard hasMore,
              !isLoading,
              !isLoadingMore,
              currentItem.id == recipes.last?.id
        else { return }

        isLoadingMore = true
        defer { isLoadingMore = false }

        do {
            let page = try await fetchPage(skip: Int32(recipes.count))
            // Guard against a duplicate append if the list changed meanwhile.
            let known = Set(recipes.map(\.id))
            recipes.append(contentsOf: page.items.filter { !known.contains($0.id) })
            hasMore = page.hasMore
        } catch is CancellationError {
            return
        } catch {
            failure = Self.describe(error)
        }
    }

    /// Applies a category filter and reloads.
    func select(category newCategory: Category) async {
        category = newCategory
        await reload()
    }

    /// Loads the filter chips once, so the filter bar has content.
    func loadFiltersIfNeeded() async {
        guard tags.isEmpty, mealTypes.isEmpty else { return }

        do {
            let loadedTags = try await repository.tags()
            tags = Array(loadedTags.prefix(24))
        } catch {
            // The filter bar is optional; a failure here must not block the list.
            tags = []
        }

        do {
            mealTypes = try await repository.mealTypes(maxRecipes: mealTypeScanLimit)
        } catch {
            mealTypes = []
        }
    }

    // MARK: Internals

    private func fetchPage(skip: Int32) async throws -> RecipeListPage {
        let query = RecipesQuery.page(
            limit: pageSize,
            skip: skip,
            sortBy: selectedSort.sortField,
            order: selectedSort.direction
        )
        let text = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        let page: RecipesPage
        if !text.isEmpty {
            page = try await repository.search(text, query: query)
        } else {
            switch category {
            case .all:
                page = try await repository.recipes(query: query)
            case .tag(let tag):
                page = try await repository.recipes(tag: tag, query: query)
            case .mealType(let meal):
                page = try await repository.recipes(mealType: meal, query: query)
            }
        }
        return RecipeListPage(kotlin: page)
    }

    /// Debounces a reload so a burst of keystrokes fires a single request.
    private func scheduleReload(delay: Duration = .zero) {
        reloadTask?.cancel()
        reloadTask = Task { [weak self] in
            if delay > .zero {
                try? await Task.sleep(for: delay)
            }
            guard !Task.isCancelled else { return }
            await self?.reload()
        }
    }

    private static func describe(_ error: Error) -> LoadFailure {
        if let recipesError = error as? RecipesError {
            return LoadFailure(kind: recipesError.kind, message: recipesError.localizedDescription)
        }
        return LoadFailure(
            kind: RecipesFailureKind.shared.Unexpected,
            message: error.localizedDescription
        )
    }
}
