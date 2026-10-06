# recipes

Kotlin Multiplatform client for the
[DummyJSON recipes API](https://dummyjson.com/docs/recipes).

Pure data/domain layer: no Compose, no UI, no Android-only or iOS-only
dependency in the public API. Targets Android, JVM, iOS (device + simulator)
and Kotlin/JS.

* API reference: see the KDoc on `RecipesRepository`.
* Publishing and consumption: see the [root README](../README.md).

## Layout

```
src/commonMain/kotlin/com/example/myapplication/recipes/
├── RecipesApi.kt              endpoint paths and API constants
├── RecipesConfig.kt           base URL, timeout, logging, RetryPolicy
├── RecipesFailure.kt          sealed error hierarchy
├── RecipesRepository.kt       public entry point
├── RecipesResult.kt           Success/Failure envelope
├── model/                     Recipe, Page, RecipesQuery, NewRecipe, …
└── internal/                  Ktor client, DTOs, mappers, retry engine
```

## Usage

```kotlin
val repository = RecipesRepository.create() // or create("https://dummyjson.com")

when (val result = repository.getRecipes(RecipesQuery(limit = 10, skip = 0))) {
    is RecipesResult.Success -> result.value.items.forEach { println(it.name) }
    is RecipesResult.Failure -> println("${result.failure.kind}: ${result.failure.message}")
}
```

Search, filter and sort:

```kotlin
repository.searchRecipes("pizza")
repository.getRecipesByTag("Pakistani")
repository.getRecipesByMealType("snack")
repository.getRecipes(RecipesQuery.build {
    limit = 20
    sortByDescending(RecipeSortField.Rating)
    select = listOf(RecipeSelectField.Name, RecipeSelectField.Image)
})
```

Writes are simulated by DummyJSON: nothing is persisted server side.

```kotlin
repository.addRecipe(NewRecipe(name = "Tasty Pizza", servings = 2))
repository.updateRecipe(1, RecipeUpdate(name = "Tasty Pizza"))  // PUT
repository.patchRecipe(1, RecipeUpdate(cuisine = "Italian"))    // PATCH
repository.deleteRecipe(1)
```

Always release the client:

```kotlin
repository.close()
```

## Errors

Every call returns `RecipesResult`, so no raw exception escapes. Inspect
`failure.kind` (`RecipesFailureKind`) or match on the subclass:

| Failure | `kind` | Meaning |
| --- | --- | --- |
| `RecipesNetworkFailure` | `network` | Transport failure, DNS, TLS |
| `RecipesTimeoutFailure` | `timeout` | Exceeded `RecipesConfig.timeoutMillis` |
| `RecipesHttpFailure` | `http` | 4xx/5xx, carries `statusCode` and body |
| `RecipesSerializationFailure` | `serialization` | Body did not match the schema |
| `RecipesInvalidRequestFailure` | `invalid_request` | Bad argument, no request made |
| `RecipesUnexpectedFailure` | `unexpected` | Anything else |

Network failures, timeouts, HTTP 429 and HTTP 5xx are retried according to
`RecipesConfig.retryPolicy` (three attempts with exponential backoff and jitter
by default). `RetryPolicy.None` disables retrying.

## Tests

```shell
./gradlew :recipes:jvmTest            # 32 tests
./gradlew :recipes:testAndroid        # Android host tests
./gradlew :recipes:iosSimulatorArm64Test
```

Tests run against a Ktor `MockEngine`, so they need no network access.
