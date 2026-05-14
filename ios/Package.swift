// swift-tools-version:5.9
// NOTE: This Package.swift is for code validation only.
// For actual iOS builds, create an Xcode project targeting iOS 16+
// with the Mektep/ directory as the source root.

import PackageDescription

let package = Package(
    name: "Mektep",
    platforms: [.iOS(.v16)],
    targets: [
        .executableTarget(
            name: "Mektep",
            path: "Mektep"
        )
    ]
)
