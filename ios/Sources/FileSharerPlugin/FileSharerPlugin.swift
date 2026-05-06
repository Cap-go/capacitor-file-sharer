import Foundation
import Capacitor
import UIKit

@objc(FileSharerPlugin)
public class FileSharerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "FileSharerPlugin"
    public let jsName = "FileSharer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "share", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "save", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = FileSharer()

    @objc func share(_ call: CAPPluginCall) {
        presentShareSheet(call, resolveWithUri: false)
    }

    @objc func save(_ call: CAPPluginCall) {
        presentShareSheet(call, resolveWithUri: true)
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": implementation.getPluginVersion()
        ])
    }

    private func presentShareSheet(_ call: CAPPluginCall, resolveWithUri: Bool) {
        let fileUrl: URL

        do {
            fileUrl = try prepareFile(call)
        } catch let error as FileSharerError {
            call.reject(error.rawValue)
            return
        } catch {
            call.reject(FileSharerError.fileCachingFailed.rawValue, error.localizedDescription)
            return
        }

        DispatchQueue.main.async {
            guard let viewController = self.bridge?.viewController else {
                call.reject(FileSharerError.noViewController.rawValue)
                return
            }

            var activityItems: [Any] = [fileUrl]
            if let text = call.getString("text"), !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                activityItems.append(text)
            }

            let activityViewController = UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
            if let subject = call.getString("subject") ?? call.getString("title") {
                activityViewController.setValue(subject, forKey: "subject")
            }

            if let sourceView = viewController.view {
                activityViewController.popoverPresentationController?.sourceView = sourceView
                activityViewController.popoverPresentationController?.sourceRect = CGRect(
                    x: sourceView.bounds.midX,
                    y: sourceView.bounds.maxY,
                    width: 0,
                    height: 0
                )
            }

            activityViewController.completionWithItemsHandler = { _, completed, _, error in
                if let error = error {
                    call.reject(FileSharerError.shareFailed.rawValue, error.localizedDescription)
                    return
                }

                if completed {
                    if resolveWithUri {
                        call.resolve(["uri": fileUrl.absoluteString])
                    } else {
                        call.resolve()
                    }
                } else {
                    call.reject(FileSharerError.userCancelled.rawValue)
                }
            }

            viewController.present(activityViewController, animated: true)
        }
    }

    private func prepareFile(_ call: CAPPluginCall) throws -> URL {
        guard let requestedFilename = call.getString("filename"),
              let filename = implementation.safeFilename(requestedFilename) else {
            throw FileSharerError.noFilename
        }

        let temporaryUrl = implementation.temporaryFileUrl(filename: filename)
        try? FileManager.default.removeItem(at: temporaryUrl)

        if let base64Data = call.getString("base64Data"), !base64Data.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            guard let data = implementation.decodedBase64Data(from: base64Data) else {
                throw FileSharerError.invalidData
            }

            try data.write(to: temporaryUrl, options: .atomic)
            return temporaryUrl
        }

        if let path = call.getString("path"), !path.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            guard let sourceUrl = implementation.fileUrl(from: path) else {
                throw FileSharerError.fileNotFound
            }

            let standardizedSourceUrl = sourceUrl.standardizedFileURL
            if standardizedSourceUrl == temporaryUrl.standardizedFileURL {
                return standardizedSourceUrl
            }

            guard FileManager.default.fileExists(atPath: standardizedSourceUrl.path) else {
                throw FileSharerError.fileNotFound
            }

            try FileManager.default.copyItem(at: standardizedSourceUrl, to: temporaryUrl)
            return temporaryUrl
        }

        throw FileSharerError.noData
    }
}

private enum FileSharerError: String, Error {
    case noFilename = "ERR_PARAM_NO_FILENAME"
    case noData = "ERR_PARAM_NO_DATA"
    case invalidData = "ERR_PARAM_DATA_INVALID"
    case fileCachingFailed = "ERR_FILE_CACHING_FAILED"
    case fileNotFound = "ERR_LOCAL_FILE_NOT_FOUND"
    case noViewController = "ERR_NO_VIEW_CONTROLLER"
    case shareFailed = "ERR_SHARE_FAILED"
    case userCancelled = "USER_CANCELLED"
}
