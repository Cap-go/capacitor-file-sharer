# @capgo/capacitor-file-sharer

<a href="https://capgo.app/"><img src="https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png" alt="Capgo - Instant updates for Capacitor" /></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_file_sharer">Get instant updates for your app with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_file_sharer">Missing a feature? We will build the plugin for you</a></h2>
</div>

Capacitor plugin for sharing and saving files on Android, iOS, and Web.

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.*.*         | v8.*.*                  | Yes        |
| v7.*.*         | v7.*.*                  | On demand  |
| v6.*.*         | v6.*.*                  | On demand  |

The plugin major version follows the Capacitor major version. New work targets Capacitor 8 first.

## Install

```bash
bun add @capgo/capacitor-file-sharer
bunx cap sync
```

## Usage

```typescript
import { FileSharer } from '@capgo/capacitor-file-sharer';

await FileSharer.share({
  filename: 'report.pdf',
  contentType: 'application/pdf',
  base64Data: reportBase64,
  title: 'Quarterly report',
  text: 'Attached report',
});
```

Share from a local file path or Capacitor file URL:

```typescript
await FileSharer.share({
  filename: 'movie.mp4',
  contentType: 'video/mp4',
  path: fileUri,
});
```

Save directly on Android or download on Web:

```typescript
const result = await FileSharer.save({
  filename: 'backup.zip',
  contentType: 'application/zip',
  base64Data: zipBase64,
  android: {
    saveDirectory: 'downloads',
    relativePath: 'Download/My App',
  },
});

console.log(result.uri);
```

## Integration Notes

### Android

- Sharing uses a `FileProvider`, `ClipData`, and URI grants so Android's chooser can read previews and thumbnails.
- `share()` resolves after the Android chooser opens. This avoids retaining large base64 payloads in activity state.
- `save()` writes to MediaStore on Android 10+ and to the matching public directory on older Android versions.
- Android 9 and below require `WRITE_EXTERNAL_STORAGE` for public saves; the plugin manifest includes it with `maxSdkVersion=28`.
- Android save directories: `downloads`, `pictures`, `movies`, `music`, and `documents`.

### iOS

- `share()` supports `base64Data` and direct local `path` sharing.
- `save()` opens the native share sheet so the user can choose Save to Files or another destination.
- Swift Package Manager and CocoaPods are both supported.

### Web

- `share()` and `save()` download the file in the browser.
- Base64 conversion is chunked to avoid large-array allocation failures in Chromium.

## Error Codes

- `ERR_PARAM_NO_FILENAME`: `filename` is missing or blank.
- `ERR_PARAM_NO_DATA`: neither `base64Data` nor `path` was provided.
- `ERR_PARAM_DATA_INVALID`: base64 input could not be decoded.
- `ERR_LOCAL_FILE_NOT_FOUND`: the provided local path or content URI could not be opened.
- `ERR_FILE_CACHING_FAILED`: the native temporary file could not be written.
- `ERR_FILE_SAVE_FAILED`: Android could not save the file to public storage.
- `ERR_ACTIVITY_NOT_FOUND`: Android could not open a share target.
- `USER_CANCELLED`: iOS share sheet was dismissed without completing.

## API

<docgen-index>

* [`share(...)`](#share)
* [`save(...)`](#save)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor File Sharer plugin.

### share(...)

```typescript
share(options: ShareFileOptions) => Promise<void>
```

Share a file using the native share sheet on Android and iOS.
On web, this downloads the file because browsers do not expose a consistent native file share target.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#sharefileoptions">ShareFileOptions</a></code> |

--------------------


### save(...)

```typescript
save(options: SaveFileOptions) => Promise<SaveFileResult>
```

Save a file locally.
On Android this writes to MediaStore/Downloads. On web this downloads the file.
On iOS this opens the share sheet so the user can choose Save to Files or another target.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#sharefileoptions">ShareFileOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#savefileresult">SaveFileResult</a>&gt;</code>

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the platform implementation version marker.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

--------------------


### Interfaces


#### ShareFileOptions

Options used to share a file.

| Prop              | Type                                                                          | Description                                                                      |
| ----------------- | ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| **`filename`**    | <code>string</code>                                                           | File name presented to the receiving app. Include the extension.                 |
| **`base64Data`**  | <code>string</code>                                                           | Base64 encoded file data. Data URL prefixes are accepted.                        |
| **`path`**        | <code>string</code>                                                           | Local file path, file:// URL, content:// URL, or Capacitor _capacitor_file_ URL. |
| **`contentType`** | <code>string</code>                                                           | MIME type of the file. Defaults to application/octet-stream when omitted.        |
| **`text`**        | <code>string</code>                                                           | Optional text or caption shared with the file.                                   |
| **`title`**       | <code>string</code>                                                           | Optional title for the share sheet or shared item.                               |
| **`subject`**     | <code>string</code>                                                           | Optional subject used by mail and compatible share targets.                      |
| **`android`**     | <code><a href="#androidfileshareroptions">AndroidFileSharerOptions</a></code> | Android-specific options.                                                        |


#### AndroidFileSharerOptions

Android-specific behavior for file sharing and saving.

| Prop                | Type                                                                  | Description                                                                    |
| ------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **`chooserTitle`**  | <code>string</code>                                                   | Title shown at the top of the Android chooser.                                 |
| **`saveDirectory`** | <code><a href="#androidsavedirectory">AndroidSaveDirectory</a></code> | Public collection used by save(). Defaults from contentType.                   |
| **`relativePath`**  | <code>string</code>                                                   | Optional relative folder inside the selected public collection on Android 10+. |


#### SaveFileResult

Result returned by save().

| Prop      | Type                | Description                                                  |
| --------- | ------------------- | ------------------------------------------------------------ |
| **`uri`** | <code>string</code> | Native URI of the saved file when the platform provides one. |


#### PluginVersionResult

Plugin version payload.

| Prop          | Type                | Description                                                 |
| ------------- | ------------------- | ----------------------------------------------------------- |
| **`version`** | <code>string</code> | Version identifier returned by the platform implementation. |


### Type Aliases


#### AndroidSaveDirectory

Android public collection used by save().

<code>'downloads' | 'pictures' | 'movies' | 'music' | 'documents'</code>


#### SaveFileOptions

Options used to save a file locally.

<code><a href="#sharefileoptions">ShareFileOptions</a></code>

</docgen-api>

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MPL-2.0
