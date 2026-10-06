This is a Kotlin Multiplatform project targeting Android, iOS, Web.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/sharedUI](./sharedUI/src) is for the code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./sharedUI/src/commonMain/kotlin) is for the code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./sharedUI/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./sharedUI/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/webApp](./webApp) contains a React web application. It uses the Kotlin/JS library produced
  by the [sharedLogic](./sharedLogic) module.

* [/recipes](./recipes) is a **standalone, publishable** Kotlin Multiplatform library that talks to the
  [DummyJSON recipes API](https://dummyjson.com/docs/recipes). It has no Compose or app dependency, so it can be
  consumed on its own from Android, iOS, JVM and JS. See [Publishing the recipes library](#publishing-the-recipes-library).

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Web app:
  1. Install [Node.js](https://nodejs.org/en/download) (which includes `npm`)
  2. Build and run the web application:
     ```shell
     npm run build:shared
     npm install
     npm run start
     ```
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest`
- Web tests: `./gradlew :sharedLogic:jsTest`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

### Requirements

- JDK 17 or newer (`JAVA_HOME` must point at it; Android Studio's bundled JBR works)
- Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`)
- A full **Xcode** installation for anything that links an Apple binary
  (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`). Compiling the iOS Kotlin sources does not need Xcode,
  but `linkReleaseFramework*` and the XCFramework do.

---

# recipes

A Kotlin Multiplatform client for the [DummyJSON recipes API](https://dummyjson.com/docs/recipes), distributed from
this repository:

- **Android / JVM / KMP** → Maven artifacts on GitHub Packages
- **iOS** → a `SharedRecipes.xcframework` published as a GitHub release asset and exposed through a Swift Package

Targets: `android`, `jvm`, `js`, `iosArm64`, `iosSimulatorArm64`.

## API at a glance

```kotlin
val repository = RecipesRepository.create()

when (val result = repository.getRecipes(RecipesQuery(limit = 10))) {
    is RecipesResult.Success -> result.value.items.forEach { println(it.name) }
    is RecipesResult.Failure -> println("${result.failure.kind}: ${result.failure.message}")
}
```

| Call | Endpoint |
| --- | --- |
| `getRecipes(query)` | `GET /recipes` (`limit`, `skip`, `sortBy`, `order`, `select`) |
| `getRecipe(id)` | `GET /recipes/{id}` |
| `searchRecipes(q, page)` | `GET /recipes/search?q=` |
| `getRecipesByTag(tag, page)` | `GET /recipes/tag/{tag}` |
| `getRecipesByMealType(meal, page)` | `GET /recipes/meal-type/{meal}` |
| `getTags()` | `GET /recipes/tags` |
| `getAllRecipes(pageSize)` | paging loop over `GET /recipes` |
| `getRecipesByDifficulty / ByCuisine` | `GET /recipes`, filtered client side |
| `addRecipe(NewRecipe)` | `POST /recipes/add` |
| `updateRecipe(id, RecipeUpdate)` / `patchRecipe` | `PUT` / `PATCH /recipes/{id}` |
| `deleteRecipe(id)` | `DELETE /recipes/{id}` |

Every call returns `RecipesResult` — see [recipes/README.md](./recipes/README.md) for the full error taxonomy,
retry behaviour and query DSL.

## Publishing the recipes library

Everything is automated by [`.github/workflows/release.yml`](./.github/workflows/release.yml). Push a tag and the
workflow publishes all three channels:

```shell
git tag v1.0.0
git push origin v1.0.0
```

It runs `:recipes:publishAllPublicationsToGitHubPackagesRepository`, builds
`SharedRecipes.xcframework.zip` with [`scripts/build-xcframework.sh`](./scripts/build-xcframework.sh), attaches it to
the GitHub release, and commits a matching `Package.swift` to the `spm` branch.

### One-time repository setup

1. Push this project to GitHub (the repository is not a git repository yet: `git init && git add . && git commit`).
2. **Settings ▸ Actions ▸ General ▸ Workflow permissions** → *Read and write permissions* (the release workflow creates
   releases and pushes the `spm` branch).
3. The workflow uses the built-in `GITHUB_TOKEN`; no extra secret is required. If you publish manually instead, use a
   personal access token with `write:packages`:
   ```shell
   GITHUB_TOKEN=<pat> GITHUB_ACTOR=<user> GITHUB_REPOSITORY=<owner>/<repo> \
     ./gradlew :recipes:publishAllPublicationsToGitHubPackagesRepository \
       -Precipes.group=com.github.<owner> -Precipes.version=1.0.0
   ```
4. The group id must start with `com.github.<owner>` for GitHub Packages to accept the publication. The workflow derives
   it automatically from `github.repository_owner`.

## Consuming on Android / KMP (GitHub Packages)

GitHub Packages requires authentication even for public artifacts, so add a repository with credentials to the
consumer's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/<owner>/<repo>")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        google()
        mavenCentral()
    }
}
```

Then depend on the library. A **plain Android app** (`com.android.application`) resolves the Android variant directly:

```kotlin
dependencies {
    implementation("com.github.<owner>:recipes:1.0.0")
}
```

A **Kotlin Multiplatform** consumer declares it in `commonMain` and Gradle picks the right variant per target:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.<owner>:recipes:1.0.0")
        }
    }
}
```

The token only needs `read:packages`. For CI, expose it as `GITHUB_TOKEN`.

## Consuming on iOS (Swift Package Manager)

Add the package in Xcode with **File ▸ Add Package Dependencies…** and the repository URL with branch `spm`:

```
https://github.com/<owner>/<repo>.git   branch: spm
```

Or in a `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/<owner>/<repo>.git", branch: "spm")
],
targets: [
    .target(name: "MyApp", dependencies: [
        .product(name: "SharedRecipesSwift", package: "SharedRecipes")
    ])
]
```

The package exposes two products:

| Product | Use |
| --- | --- |
| `SharedRecipesSwift` | Swift `async`/`await` wrappers around the Kotlin API (recommended) |
| `SharedRecipes` | the raw Kotlin framework with completion handlers |

```swift
import SharedRecipesSwift
import SharedRecipes

@MainActor
func loadRecipes() async {
    let repository = RecipesRepository.live()
    do {
        let page = try await repository.recipes(limit: 20)
        page.items.forEach { print($0.name) }
    } catch RecipesError.api(let failure) {
        print("\(failure.kind): \(failure.localizedDescriptionText)")
    } catch {
        print(error)
    }
    try? await repository.closeRepository()
}
```

Notes:

- `SharedRecipesSwift` is generated once per release onto the `spm` branch, so pin the branch or a commit.
- Kotlin default arguments are not exported to Swift: use the generated overloads (`RecipesRepository.live()`,
  `RecipesConfig.default()`, the zero/`query:` overloads) rather than relying on defaults.
- The XCFramework contains `ios-arm64` and `ios-arm64-simulator` slices, so it runs on Apple silicon Macs and devices;
  Intel simulators (`iosX64`) are not included.
- Minimum deployment target is iOS 15.
- If you already use the [KMP Xcode plugin](./iosApp), you do not need SPM in that project — the plugin builds
  `:recipes` as a regular framework dependency.

## Verifying the distribution locally

Both channels can be exercised without GitHub:

```shell
# 1. Publish to recipes/build/local-maven (no credentials, no network)
./gradlew :recipes:publishAllPublicationsToLocalTestRepository \
  -Precipes.group=com.github.testowner -Precipes.version=1.0.0

# 2. Generate a throwaway KMP consumer, resolve the artifacts and compile
#    it for JVM, Android and iOS
scripts/verify-consumer.sh 1.0.0

# 3. Build the XCFramework and render a ready-to-publish Package.swift
KONAN_DATA_DIR=~/.konan scripts/build-xcframework.sh 1.0.0 <owner>/<repo>
#   -> build/spm/SharedRecipes.xcframework
#   -> build/spm/SharedRecipes.xcframework.zip (+ .sha256)
#   -> build/spm/Package.swift
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
