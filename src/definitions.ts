/**
 * Android public collection used by save().
 */
export type AndroidSaveDirectory = 'downloads' | 'pictures' | 'movies' | 'music' | 'documents';

/**
 * Android-specific behavior for file sharing and saving.
 */
export interface AndroidFileSharerOptions {
  /**
   * Title shown at the top of the Android chooser.
   */
  chooserTitle?: string;

  /**
   * Public collection used by save(). Defaults from contentType.
   */
  saveDirectory?: AndroidSaveDirectory;

  /**
   * Optional relative folder inside the selected public collection on Android 10+.
   */
  relativePath?: string;
}

/**
 * Options used to share a file.
 */
export interface ShareFileOptions {
  /**
   * File name presented to the receiving app. Include the extension.
   */
  filename: string;

  /**
   * Base64 encoded file data. Data URL prefixes are accepted.
   */
  base64Data?: string;

  /**
   * Local file path, file:// URL, content:// URL, or Capacitor _capacitor_file_ URL.
   */
  path?: string;

  /**
   * MIME type of the file. Defaults to application/octet-stream when omitted.
   */
  contentType?: string;

  /**
   * Optional text or caption shared with the file.
   */
  text?: string;

  /**
   * Optional title for the share sheet or shared item.
   */
  title?: string;

  /**
   * Optional subject used by mail and compatible share targets.
   */
  subject?: string;

  /**
   * Android-specific options.
   */
  android?: AndroidFileSharerOptions;
}

/**
 * Options used to save a file locally.
 */
export type SaveFileOptions = ShareFileOptions;

/**
 * Result returned by save().
 */
export interface SaveFileResult {
  /**
   * Native URI of the saved file when the platform provides one.
   */
  uri?: string;
}

/**
 * Plugin version payload.
 */
export interface PluginVersionResult {
  /**
   * Version identifier returned by the platform implementation.
   */
  version: string;
}

/**
 * Capacitor File Sharer plugin.
 */
export interface FileSharerPlugin {
  /**
   * Share a file using the native share sheet on Android and iOS.
   * On web, this downloads the file because browsers do not expose a consistent native file share target.
   */
  share(options: ShareFileOptions): Promise<void>;

  /**
   * Save a file locally.
   * On Android this writes to MediaStore/Downloads. On web this downloads the file.
   * On iOS this opens the share sheet so the user can choose Save to Files or another target.
   */
  save(options: SaveFileOptions): Promise<SaveFileResult>;

  /**
   * Returns the platform implementation version marker.
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
