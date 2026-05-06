import XCTest
@testable import FileSharerPlugin

class FileSharerTests: XCTestCase {
    func testSafeFilenameUsesLastPathComponent() {
        let implementation = FileSharer()

        XCTAssertEqual("report.pdf", implementation.safeFilename("../exports/report.pdf"))
    }

    func testDecodesDataUrlBase64() {
        let implementation = FileSharer()
        let data = implementation.decodedBase64Data(from: "data:text/plain;base64,SGVsbG8=")

        XCTAssertEqual(String(data: data ?? Data(), encoding: .utf8), "Hello")
    }

    func testGetPluginVersion() {
        let implementation = FileSharer()
        let result = implementation.getPluginVersion()

        XCTAssertEqual("8.0.0", result)
    }
}
