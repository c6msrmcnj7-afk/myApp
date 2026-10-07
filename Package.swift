// swift-tools-version:5.9
//
// LOCAL DEVELOPMENT manifest.
//
// It points the binary target at the XCFramework that
// `scripts/build-xcframework.sh` produces, so an Xcode project can consume this
// checkout directly with `.package(path: "…")`.
//
// The version published for consumers is rendered from
// `spm/Package.swift.template` onto the `spm` branch by the release workflow;
// there the binary target is a download URL plus checksum instead of a local
// path. Keep the two manifests in sync when the target layout changes.
//
import PackageDescription

let package = Package(
    name: "SharedRecipes",
    platforms: [
        // The XCFramework is built for arm64 device and arm64 simulator.
        .iOS(.v15),
    ],
    products: [
        // Idiomatic Swift async/await API.
        .library(name: "SharedRecipesSwift", targets: ["SharedRecipesSwift"]),
        // Raw Kotlin framework, completion-handler based API.
        .library(name: "SharedRecipes", targets: ["SharedRecipes"]),
    ],
    targets: [
        .binaryTarget(
            name: "SharedRecipes",
            path: "build/spm/SharedRecipes.xcframework"
        ),
        .target(
            name: "SharedRecipesSwift",
            dependencies: ["SharedRecipes"],
            path: "spm/Sources/SharedRecipesSwift"
        ),
    ]
)
