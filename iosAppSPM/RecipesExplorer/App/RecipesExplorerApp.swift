import SwiftUI

@main
struct RecipesExplorerApp: App {
    /// One repository for the whole app; it owns an HTTP client, so it is
    /// created once and released when the app goes away.
    @State private var store = RecipesStore(repository: .live())
    @State private var favorites = FavoritesStore()

    var body: some Scene {
        WindowGroup {
            RecipeListView()
                .environment(store)
                .environment(favorites)
        }
    }
}
