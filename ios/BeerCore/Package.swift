// swift-tools-version: 5.9
import PackageDescription

// The beer-o'clock logic for iPhone and Apple Watch: the Swift twin of the
// Kotlin `core` module. Places and breweries come from data/spots.json.
let package = Package(
    name: "BeerCore",
    defaultLocalization: "en",
    platforms: [.iOS(.v17), .watchOS(.v10), .macOS(.v14)],
    products: [.library(name: "BeerCore", targets: ["BeerCore"])],
    targets: [
        .target(name: "BeerCore", resources: [.copy("Resources/spots.json")]),
        .testTarget(name: "BeerCoreTests", dependencies: ["BeerCore"]),
    ]
)
