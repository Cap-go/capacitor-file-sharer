import Foundation

@objc public class FileSharer: NSObject {
    @objc public func getPluginVersion() -> String {
        return "8.0.0"
    }

    public func safeFilename(_ filename: String) -> String? {
        let trimmed = filename.trimmingCharacters(in: .whitespacesAndNewlines)
        let lastPathComponent = URL(fileURLWithPath: trimmed).lastPathComponent
        return lastPathComponent.isEmpty ? nil : lastPathComponent
    }

    public func temporaryFileUrl(filename: String) -> URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent(filename)
    }

    public func decodedBase64Data(from base64Data: String) -> Data? {
        let trimmed = base64Data.trimmingCharacters(in: .whitespacesAndNewlines)
        let payload: String

        if let commaIndex = trimmed.firstIndex(of: ","),
           trimmed[..<commaIndex].lowercased().contains("base64") {
            payload = String(trimmed[trimmed.index(after: commaIndex)...])
        } else {
            payload = trimmed
        }

        return Data(base64Encoded: payload, options: .ignoreUnknownCharacters)
    }

    public func fileUrl(from path: String) -> URL? {
        let trimmed = path.trimmingCharacters(in: .whitespacesAndNewlines)

        if let capacitorRange = trimmed.range(of: "_capacitor_file_") {
            let rawPath = String(trimmed[capacitorRange.upperBound...])
            let decodedPath = rawPath.removingPercentEncoding ?? rawPath
            return URL(fileURLWithPath: decodedPath)
        }

        if let url = URL(string: trimmed), url.isFileURL {
            return url
        }

        if trimmed.hasPrefix("/") {
            return URL(fileURLWithPath: trimmed)
        }

        return nil
    }
}
