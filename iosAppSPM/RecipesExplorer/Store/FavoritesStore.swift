import Foundation
import Observation

/// Favourites kept in `UserDefaults`, independent of the recipes library.
@MainActor
@Observable
final class FavoritesStore {

    private static let storageKey = "favorite.recipe.ids"

    private(set) var ids: Set<Int> = []

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let stored = defaults.array(forKey: Self.storageKey) as? [Int] ?? []
        ids = Set(stored)
    }

    /// `true` when the recipe is a favourite.
    func contains(_ id: Int) -> Bool { ids.contains(id) }

    /// Adds or removes the recipe and persists the change.
    func toggle(_ id: Int) {
        if ids.contains(id) {
            ids.remove(id)
        } else {
            ids.insert(id)
        }
        defaults.set(Array(ids), forKey: Self.storageKey)
    }

    /// Total number of favourites, shown in the toolbar.
    var count: Int { ids.count }
}
