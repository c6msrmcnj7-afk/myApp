import SwiftUI
import SharedRecipesSwift

/// Root screen: filter chips, the recipe list and the loading/error states.
struct RecipeListView: View {

    @Environment(RecipesStore.self) private var store
    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        @Bindable var store = store

        NavigationStack {
            Group {
                if store.isLoading && store.recipes.isEmpty {
                    LoadingView(message: "Loading recipes…")
                } else if let failure = store.failure, store.recipes.isEmpty {
                    ErrorStateView(failure: failure) {
                        Task { await store.reload() }
                    }
                } else if store.recipes.isEmpty {
                    EmptyStateView(searchText: store.searchText, category: store.category.title)
                } else {
                    list
                }
            }
            .navigationTitle("Recipes")
            .searchable(text: $store.searchText, prompt: "Search recipes")
            .toolbar { toolbarContent }
            .refreshable { await store.reload() }
            .safeAreaInset(edge: .top, spacing: 0) { filterBar }
        }
        .task {
            await store.loadFiltersIfNeeded()
            await store.reload()
        }
    }

    // MARK: List

    private var list: some View {
        List {
            Section {
                ForEach(store.recipes) { recipe in
                    NavigationLink(value: recipe) {
                        RecipeRowView(recipe: recipe, isFavorite: favorites.contains(recipe.id))
                    }
                    .task { await store.loadMoreIfNeeded(currentItem: recipe) }
                }
            } header: {
                HStack {
                    Text("\(store.total) recipes")
                    Spacer()
                    Text(store.category.title)
                }
                .font(.caption)
            }

            if store.isLoadingMore {
                HStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
                .listRowSeparator(.hidden)
            }

            if let message = inlineFailureMessage {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
        }
        .listStyle(.plain)
        .navigationDestination(for: RecipeItem.self) { recipe in
            RecipeDetailView(recipe: recipe)
        }
    }

    /// A failure that happened while the list already had content.
    private var inlineFailureMessage: String? {
        guard let failure = store.failure, !store.recipes.isEmpty else { return nil }
        return "Could not load more: \(failure.message)"
    }

    // MARK: Filter bar

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(title: "All", isSelected: store.category == .all) {
                    Task { await store.select(category: .all) }
                }

                ForEach(store.mealTypes, id: \.self) { meal in
                    chip(title: meal, isSelected: store.category == .mealType(meal)) {
                        Task { await store.select(category: .mealType(meal)) }
                    }
                }

                ForEach(store.tags, id: \.self) { tag in
                    chip(title: tag, isSelected: store.category == .tag(tag)) {
                        Task { await store.select(category: .tag(tag)) }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(.bar)
    }

    private func chip(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(isSelected ? .semibold : .regular))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(isSelected ? Color.accentColor.opacity(0.2) : Color.secondary.opacity(0.12))
                )
                .overlay(
                    Capsule().stroke(isSelected ? Color.accentColor : .clear, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .foregroundStyle(isSelected ? Color.accentColor : Color.primary)
    }

    // MARK: Toolbar

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        @Bindable var store = store

        ToolbarItem(placement: .topBarLeading) {
            if favorites.count > 0 {
                Label("\(favorites.count)", systemImage: "heart.fill")
                    .font(.footnote)
                    .foregroundStyle(.pink)
            }
        }

        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker("Sort by", selection: $store.selectedSort) {
                    ForEach(RecipesStore.SortOrder.allCases) { order in
                        Text(order.title).tag(order)
                    }
                }
            } label: {
                Label("Sort", systemImage: "arrow.up.arrow.down")
            }
        }
    }
}

// MARK: - Row

struct RecipeRowView: View {
    let recipe: RecipeItem
    let isFavorite: Bool

    var body: some View {
        HStack(spacing: 12) {
            RecipeThumbnail(recipe: recipe, size: 64)

            VStack(alignment: .leading, spacing: 4) {
                Text(recipe.name)
                    .font(.headline)
                    .lineLimit(2)

                HStack(spacing: 6) {
                    Label(recipe.ratingText, systemImage: "star.fill")
                        .foregroundStyle(.orange)
                    Text("(\(recipe.reviewCount))")
                        .foregroundStyle(.secondary)
                    Text("•")
                        .foregroundStyle(.secondary)
                    Text(recipe.cuisine.isEmpty ? "Unknown cuisine" : recipe.cuisine)
                        .foregroundStyle(.secondary)
                }
                .font(.caption)

                HStack(spacing: 6) {
                    Text(recipe.difficulty.isEmpty ? "—" : recipe.difficulty)
                    Text("•")
                    Label(recipe.totalTimeText, systemImage: "clock")
                    Text("•")
                    Text("\(recipe.caloriesPerServing) kcal")
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)

            if isFavorite {
                Image(systemName: "heart.fill")
                    .foregroundStyle(.pink)
                    .font(.caption)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Thumbnail

struct RecipeThumbnail: View {
    let recipe: RecipeItem
    var size: CGFloat

    var body: some View {
        AsyncImage(url: recipe.imageURL) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            case .failure:
                placeholder
            case .empty:
                ZStack {
                    placeholder
                    ProgressView()
                }
            @unknown default:
                placeholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var placeholder: some View {
        ZStack {
            Color.secondary.opacity(0.15)
            Text(recipe.cuisineInitial)
                .font(.title2.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - States

struct LoadingView: View {
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
            Text(message).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorStateView: View {
    let failure: RecipesStore.LoadFailure
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("Could not load recipes", systemImage: "wifi.exclamationmark")
        } description: {
            VStack(spacing: 6) {
                Text(failure.message)
                Text("kind: \(failure.kind)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        } actions: {
            Button("Try again", action: retry)
                .buttonStyle(.borderedProminent)
        }
    }
}

struct EmptyStateView: View {
    let searchText: String
    let category: String

    var body: some View {
        ContentUnavailableView.search(text: searchText.isEmpty ? category : searchText)
    }
}

#Preview {
    RecipeListView()
        .environment(RecipesStore(repository: .live()))
        .environment(FavoritesStore())
}
