import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

/**
 * Coordinates used for both the Maven publications (GitHub Packages) and the
 * iOS/Swift Package Manager artifact naming.
 *
 * Override on the command line or in CI:
 *   ./gradlew :recipes:publishAllPublicationsToGitHubPackagesRepository \
 *     -Precipes.group=com.github.<owner> -Precipes.version=1.2.0
 */
val libraryGroup: String = providers.gradleProperty("recipes.group")
    .orElse(providers.environmentVariable("RECIPES_GROUP"))
    .getOrElse("com.example.myapplication")

val libraryVersion: String = providers.gradleProperty("recipes.version")
    .orElse(providers.environmentVariable("RECIPES_VERSION"))
    .getOrElse("1.0.0-SNAPSHOT")

/** iOS framework/SPM module name: `import SharedRecipes` in Swift. */
val frameworkBaseName = "SharedRecipes"

group = libraryGroup
version = libraryVersion

kotlin {
    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        // Keep consumers free of opt-in boilerplate for the small set of
        // experimental annotations used by this module.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ------------------------------------------------------------------
    // Targets
    // ------------------------------------------------------------------
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        outputModuleName = "sharedRecipes"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }

    android {
        namespace = "com.example.myapplication.recipes"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    // Unified Apple framework used for both Xcode integration and SPM.
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = frameworkBaseName
            isStatic = true
            binaryOption("bundleId", "com.example.myapplication.recipes")
        }
    }

    // ------------------------------------------------------------------
    // Source sets
    // ------------------------------------------------------------------
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

// ----------------------------------------------------------------------
// Publishing
// ----------------------------------------------------------------------
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                providers.gradleProperty("recipes.github.repository")
                    .orElse(providers.environmentVariable("GITHUB_REPOSITORY"))
                    .getOrElse("OWNER/REPO")
                    .let { "https://maven.pkg.github.com/$it" }
            )
            credentials {
                username = providers.gradleProperty("recipes.github.actor")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                    ?: "github-actions"
                password = providers.gradleProperty("recipes.github.token")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
                    ?: ""
            }
        }
    }
}

// ----------------------------------------------------------------------
// Local publishing / consumption verification
// ----------------------------------------------------------------------
// `./gradlew :recipes:publishAllPublicationsToLocalTestRepository` writes the
// complete publication (Gradle module metadata, POM, jars, klibs) to
// `recipes/build/local-maven`, which is what `scripts/verify-consumer.sh`
// resolves against. It needs no credentials and never touches the network.
publishing {
    repositories {
        maven {
            name = "LocalTest"
            url = uri(layout.buildDirectory.dir("local-maven"))
        }
    }
}
