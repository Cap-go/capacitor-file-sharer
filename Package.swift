// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorFileSharer",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorFileSharer",
            targets: ["FileSharerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "FileSharerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/FileSharerPlugin"),
        .testTarget(
            name: "FileSharerPluginTests",
            dependencies: ["FileSharerPlugin"],
            path: "ios/Tests/FileSharerPluginTests")
    ]
)
