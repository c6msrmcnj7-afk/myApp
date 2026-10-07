import XCTest

/// End-to-end smoke test: launches the app, waits for recipes fetched through
/// the Swift package, opens the first recipe and checks the detail screen.
///
/// It needs network access, because the data comes from the live DummyJSON API.
final class RecipeFlowUITests: XCTestCase {

    override func setUp() {
        continueAfterFailure = false
    }

    func testListLoadsAndOpensDetail() throws {
        let app = XCUIApplication()
        app.launch()

        // The list header shows the total reported by the API.
        let header = app.staticTexts.matching(
            NSPredicate(format: "label ENDSWITH %@", "recipes")
        ).firstMatch
        XCTAssertTrue(header.waitForExistence(timeout: 60), "The recipe list never loaded")

        let firstCell = app.cells.firstMatch
        XCTAssertTrue(firstCell.waitForExistence(timeout: 30), "No recipe rows were rendered")

        // Capture the list for the test report.
        attach(app, name: "recipe-list")

        // Tapping the row's own label is more reliable than tapping the cell
        // container, which sits underneath the search field's hit area.
        let recipeName = app.staticTexts["Aloo Keema"]
        XCTAssertTrue(recipeName.waitForExistence(timeout: 30), "The expected first recipe is missing")
        recipeName.tap()

        // The detail screen is built from the library's Recipe model: its
        // section titles and stat cards must all be present.
        let ingredients = app.staticTexts["Ingredients"]
        XCTAssertTrue(ingredients.waitForExistence(timeout: 30), "The detail screen did not appear")

        XCTAssertTrue(app.staticTexts["Instructions"].exists, "Instructions section is missing")
        XCTAssertTrue(app.staticTexts["Servings"].exists, "Stat cards are missing")
        XCTAssertTrue(app.staticTexts["Prep"].exists, "Prep time is missing")
        XCTAssertTrue(app.staticTexts["Total"].exists, "Total time is missing")

        attach(app, name: "recipe-detail")
    }

    /// Opens the search field, types a query and checks the list reacts.
    func testSearchFiltersTheList() throws {
        let app = XCUIApplication()
        app.launch()

        let firstCell = app.cells.firstMatch
        XCTAssertTrue(firstCell.waitForExistence(timeout: 60), "The recipe list never loaded")

        let searchField = app.searchFields.firstMatch
        XCTAssertTrue(searchField.waitForExistence(timeout: 10), "Search field is missing")
        searchField.tap()
        searchField.typeText("pizza")

        // Give the debounce plus the request time to finish.
        let pizzaCell = app.cells.containing(
            NSPredicate(format: "label CONTAINS[c] %@", "Pizza")
        ).firstMatch
        XCTAssertTrue(pizzaCell.waitForExistence(timeout: 30), "Search results did not arrive")

        attach(app, name: "recipe-search")
    }

    private func attach(_ app: XCUIApplication, name: String) {
        let screenshot = app.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
