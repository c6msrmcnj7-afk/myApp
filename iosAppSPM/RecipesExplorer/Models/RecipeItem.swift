import Foundation
import SharedRecipes
import SharedRecipesSwift

/// Lightweight value wrapper over the Kotlin `Recipe` model.
///
/// Kotlin/Native models are reference types and the SwiftUI diffing machinery
/// works best with `Identifiable` + `Hashable` values, so the store maps the
/// framework models into this struct once per fetch.
struct RecipeItem: Identifiable, Hashable {
    let id: Int
    let name: String
    let imageURL: URL?
    let cuisine: String
    let difficulty: String
    let tags: [String]
    let mealTypes: [String]
    let rating: Double
    let reviewCount: Int
    let servings: Int
    let prepTimeMinutes: Int
    let cookTimeMinutes: Int
    let caloriesPerServing: Int
    let ingredients: [String]
    let instructions: [String]

    /// Total time in minutes needed to bring the recipe to the table.
    var totalTimeMinutes: Int { prepTimeMinutes + cookTimeMinutes }

    /// `"35 min"`, used in list rows.
    var totalTimeText: String { "\(totalTimeMinutes) min" }

    /// One decimal rating, e.g. `"4.6"`.
    var ratingText: String { String(format: "%.1f", rating) }

    /// First letter of the cuisine, for the placeholder thumbnail.
    var cuisineInitial: String { cuisine.first.map(String.init) ?? "?" }
}

extension RecipeItem {
    /// Maps the Kotlin model into the value type used by the views.
    init(_ recipe: Recipe) {
        self.init(
            id: Int(recipe.id),
            name: recipe.name,
            imageURL: URL(string: recipe.image),
            cuisine: recipe.cuisine,
            difficulty: recipe.difficulty,
            tags: recipe.tags,
            mealTypes: recipe.mealType,
            rating: recipe.rating,
            reviewCount: Int(recipe.reviewCount),
            servings: Int(recipe.servings),
            prepTimeMinutes: Int(recipe.prepTimeMinutes),
            cookTimeMinutes: Int(recipe.cookTimeMinutes),
            caloriesPerServing: Int(recipe.caloriesPerServing),
            ingredients: recipe.ingredients,
            instructions: recipe.instructions
        )
    }
}

/// A page of ``RecipeItem`` values plus the pagination metadata.
struct RecipeListPage: Hashable {
    let items: [RecipeItem]
    let total: Int
    let skip: Int
    let limit: Int
    let hasMore: Bool

    /// Whether the dataset behind this page was empty.
    var isEmpty: Bool { items.isEmpty }
}

extension RecipeListPage {
    init(kotlin page: RecipesPage) {
        // `Page.items` arrives as `[Any]` after Objective-C generic erasure.
        self.init(
            items: page.items.compactMap { $0 as? Recipe }.map(RecipeItem.init),
            total: Int(page.total),
            skip: Int(page.skip),
            limit: Int(page.limit),
            hasMore: page.hasMore
        )
    }
}
