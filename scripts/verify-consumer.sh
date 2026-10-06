#!/usr/bin/env bash
#
# Verifies that the published Maven artifacts can actually be consumed.
#
# It generates a throwaway Kotlin Multiplatform consumer in build/consumer-check,
# resolves `com.github.<owner>:recipes:<version>` from the locally published
# repository, and compiles it for Android, JVM and iOS.
#
# Usage:
#   scripts/verify-consumer.sh [version]
#
# Prerequisites: the artifacts must already be published, for example with
#   ./gradlew :recipes:publishAllPublicationsToLocalTestRepository \
#     -Precipes.group=com.github.testowner -Precipes.version=1.0.0
#
set -euo pipefail

VERSION="${1:-1.0.0}"
GROUP="com.github.testowner"
ARTIFACT="recipes"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

PUBLISHED_REPO="$REPO_ROOT/recipes/build/local-maven"
CONSUMER_DIR="$REPO_ROOT/build/consumer-check"

if [[ ! -d "$PUBLISHED_REPO" ]]; then
  echo "error: $PUBLISHED_REPO not found. Publish the artifacts first." >&2
  exit 1
fi

echo "==> Generating consumer in $CONSUMER_DIR"
rm -rf "$CONSUMER_DIR"
mkdir -p "$CONSUMER_DIR/src/commonMain/kotlin/consumer"
mkdir -p "$CONSUMER_DIR/src/androidMain/kotlin/consumer"
mkdir -p "$CONSUMER_DIR/src/iosMain/kotlin/consumer"

cat > "$CONSUMER_DIR/settings.gradle.kts" <<EOF
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // The locally published recipes library.
        maven { url = uri("$PUBLISHED_REPO") }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "consumer-check"
EOF

cat > "$CONSUMER_DIR/build.gradle.kts" <<'EOF'
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.4.20"
    id("com.android.kotlin.multiplatform.library") version "9.1.1"
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "consumer.check"
        compileSdk = 37
        minSdk = 28
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.github.testowner:recipes:1.0.0")
        }
    }
}
EOF

# The version/group are interpolated above on purpose: this is a generated project.
sed -i '' "s|com.github.testowner:recipes:1.0.0|$GROUP:$ARTIFACT:$VERSION|" "$CONSUMER_DIR/build.gradle.kts"

cat > "$CONSUMER_DIR/src/commonMain/kotlin/consumer/Consumer.kt" <<'EOF'
package consumer

import com.example.myapplication.recipes.RecipesConfig
import com.example.myapplication.recipes.RecipesRepository
import com.example.myapplication.recipes.RecipesResult
import com.example.myapplication.recipes.model.Recipe
import com.example.myapplication.recipes.model.RecipesQuery

/**
 * Compiles against the published library only: proves the public API, its
 * model types and its transitive dependencies are reachable from a consumer.
 */
suspend fun loadTopRated(): List<Recipe> {
    val repository = RecipesRepository.create(RecipesConfig.forBaseUrl("https://dummyjson.com"))
    return when (val result = repository.getRecipes(RecipesQuery(limit = 5, sortBy = "rating"))) {
        is RecipesResult.Success -> result.value.items
        is RecipesResult.Failure -> emptyList()
    }
}

/** Exercises the query DSL and the error type exported by the library. */
fun describeQuery(): String {
    val query = RecipesQuery.build {
        limit = 10
        sortByDescending("rating")
    }
    return query.toQueryParameters().entries.joinToString { "${it.key}=${it.value}" }
}

/** Reads pagination metadata from the library's Page type. */
fun describePage(page: com.example.myapplication.recipes.model.Page<Recipe>): Int = page.pageCount
EOF

cat > "$CONSUMER_DIR/src/androidMain/kotlin/consumer/AndroidConsumer.kt" <<'EOF'
package consumer

/** Android specific consumer code, proving the AAR is on the classpath. */
fun androidSmokeTest(): String = describeQuery()
EOF

cat > "$CONSUMER_DIR/src/iosMain/kotlin/consumer/IosConsumer.kt" <<'EOF'
package consumer

/** Apple target consumer code, proving the klib is on the classpath. */
fun iosSmokeTest(): String = describeQuery()
EOF

cat > "$CONSUMER_DIR/gradle.properties" <<'EOF'
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
kotlin.code.style=official
android.useAndroidX=true
EOF

# The generated consumer needs its own Android SDK pointer.
ANDROID_SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$ANDROID_SDK_DIR" && -f "$REPO_ROOT/local.properties" ]]; then
  ANDROID_SDK_DIR="$(grep -E '^sdk\.dir=' "$REPO_ROOT/local.properties" | cut -d= -f2-)"
fi
if [[ -n "$ANDROID_SDK_DIR" ]]; then
  echo "sdk.dir=$ANDROID_SDK_DIR" > "$CONSUMER_DIR/local.properties"
else
  echo "warning: no Android SDK found; the Android compile step may fail" >&2
fi

cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar" "$CONSUMER_DIR/" 2>/dev/null || true
mkdir -p "$CONSUMER_DIR/gradle/wrapper"
cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar" "$CONSUMER_DIR/gradle/wrapper/"
cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.properties" "$CONSUMER_DIR/gradle/wrapper/"
cp "$REPO_ROOT/gradlew" "$CONSUMER_DIR/"
cp "$REPO_ROOT/gradlew.bat" "$CONSUMER_DIR/"

echo "==> Resolving and compiling"
cd "$CONSUMER_DIR"
./gradlew compileKotlinJvm compileAndroidMain compileKotlinIosArm64 \
  --console=plain

echo "==> Consumer check passed"
