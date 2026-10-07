import SwiftUI

/// Detail screen: hero image, key facts, ingredients and instructions.
struct RecipeDetailView: View {

    let recipe: RecipeItem

    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                hero
                headline
                statsGrid
                if !recipe.tags.isEmpty { tagRow }
                if !recipe.ingredients.isEmpty { ingredientsSection }
                if !recipe.instructions.isEmpty { instructionsSection }
            }
            .padding(.bottom, 32)
        }
        .ignoresSafeArea(edges: .top)
        .navigationTitle(recipe.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    favorites.toggle(recipe.id)
                } label: {
                    Image(systemName: favorites.contains(recipe.id) ? "heart.fill" : "heart")
                }
                .tint(.pink)
                .accessibilityLabel(favorites.contains(recipe.id) ? "Remove from favourites" : "Add to favourites")
            }
        }
    }

    // MARK: Sections

    private var hero: some View {
        AsyncImage(url: recipe.imageURL) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                ZStack {
                    LinearGradient(
                        colors: [.accentColor.opacity(0.35), .accentColor.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    Image(systemName: "fork.knife")
                        .font(.system(size: 56))
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
        }
        .frame(height: 240)
        .frame(maxWidth: .infinity)
        .clipped()
    }

    private var headline: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(recipe.name)
                .font(.title2.weight(.bold))

            HStack(spacing: 8) {
                Label(recipe.ratingText, systemImage: "star.fill")
                    .foregroundStyle(.orange)
                Text("\(recipe.reviewCount) reviews")
                    .foregroundStyle(.secondary)
            }
            .font(.subheadline)

            HStack(spacing: 8) {
                if !recipe.cuisine.isEmpty {
                    Text(recipe.cuisine)
                }
                if !recipe.difficulty.isEmpty {
                    Text("•")
                    Text(recipe.difficulty)
                }
                if !recipe.mealTypes.isEmpty {
                    Text("•")
                    Text(recipe.mealTypes.joined(separator: ", "))
                }
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
        }
        .padding(.horizontal)
    }

    private var statsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            StatCard(title: "Prep", value: "\(recipe.prepTimeMinutes) min", systemImage: "timer")
            StatCard(title: "Cook", value: "\(recipe.cookTimeMinutes) min", systemImage: "flame")
            StatCard(title: "Total", value: recipe.totalTimeText, systemImage: "clock")
            StatCard(title: "Servings", value: "\(recipe.servings)", systemImage: "person.2")
            StatCard(title: "Calories", value: "\(recipe.caloriesPerServing) kcal", systemImage: "bolt")
            StatCard(title: "Difficulty", value: recipe.difficulty.isEmpty ? "—" : recipe.difficulty, systemImage: "chart.bar")
        }
        .padding(.horizontal)
    }

    private var tagRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(recipe.tags, id: \.self) { tag in
                    Text(tag)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(Color.secondary.opacity(0.15)))
                }
            }
            .padding(.horizontal)
        }
    }

    private var ingredientsSection: some View {
        SectionCard(title: "Ingredients", systemImage: "cart", count: recipe.ingredients.count) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(recipe.ingredients.enumerated()), id: \.offset) { _, ingredient in
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Image(systemName: "circle.fill")
                            .font(.system(size: 5))
                            .foregroundStyle(.secondary)
                        Text(ingredient)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }

    private var instructionsSection: some View {
        SectionCard(title: "Instructions", systemImage: "list.number", count: recipe.instructions.count) {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(recipe.instructions.enumerated()), id: \.offset) { index, step in
                    HStack(alignment: .top, spacing: 10) {
                        Text("\(index + 1)")
                            .font(.caption.weight(.bold))
                            .frame(width: 22, height: 22)
                            .background(Circle().fill(Color.accentColor.opacity(0.18)))
                        Text(step)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }
}

// MARK: - Building blocks

private struct StatCard: View {
    let title: String
    let value: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(.tint)
                .frame(width: 22)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.subheadline.weight(.semibold))
            }
            Spacer(minLength: 0)
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color.secondary.opacity(0.1)))
    }
}

private struct SectionCard<Content: View>: View {
    let title: String
    let systemImage: String
    let count: Int
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(title, systemImage: systemImage)
                    .font(.headline)
                Spacer()
                Text("\(count)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            content
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.secondary.opacity(0.08)))
        .padding(.horizontal)
    }
}

#Preview {
    NavigationStack {
        RecipeDetailView(
            recipe: RecipeItem(
                id: 1,
                name: "Classic Margherita Pizza",
                imageURL: URL(string: "https://cdn.dummyjson.com/recipe-images/1.webp"),
                cuisine: "Italian",
                difficulty: "Easy",
                tags: ["Pizza", "Italian"],
                mealTypes: ["Dinner"],
                rating: 4.6,
                reviewCount: 3,
                servings: 4,
                prepTimeMinutes: 20,
                cookTimeMinutes: 15,
                caloriesPerServing: 300,
                ingredients: ["Pizza dough", "Tomato sauce", "Fresh mozzarella cheese"],
                instructions: ["Preheat the oven to 475°F.", "Bake for 12-15 minutes."]
            )
        )
    }
    .environment(FavoritesStore())
}
