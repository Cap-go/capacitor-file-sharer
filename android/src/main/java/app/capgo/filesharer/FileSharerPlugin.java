package app.capgo.filesharer;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@CapacitorPlugin(name = "FileSharer")
public class FileSharerPlugin extends Plugin {

    private static final String CAP_FILE_SHARER_TEMP = "capfilesharer";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String ERR_PARAM_NO_FILENAME = "ERR_PARAM_NO_FILENAME";
    private static final String ERR_PARAM_NO_DATA = "ERR_PARAM_NO_DATA";
    private static final String ERR_PARAM_DATA_INVALID = "ERR_PARAM_DATA_INVALID";
    private static final String ERR_LOCAL_FILE_NOT_FOUND = "ERR_LOCAL_FILE_NOT_FOUND";
    private static final String ERR_FILE_CACHING_FAILED = "ERR_FILE_CACHING_FAILED";
    private static final String ERR_FILE_SAVE_FAILED = "ERR_FILE_SAVE_FAILED";
    private static final String ERR_ACTIVITY_NOT_FOUND = "ERR_ACTIVITY_NOT_FOUND";
    private static final int URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private final FileSharer implementation = new FileSharer();

    @PluginMethod
    public void share(PluginCall call) {
        final String filename;
        final File cachedFile;
        final String contentType;

        try {
            filename = requireFilename(call);
            cachedFile = new File(getCacheDir(), filename);

            try (Source source = openSource(call); OutputStream outputStream = new FileOutputStream(cachedFile)) {
                contentType = source.contentType;
                copy(source.inputStream, outputStream);
            }
        } catch (FileSharerException exception) {
            reject(call, exception.code, exception);
            return;
        } catch (IllegalArgumentException exception) {
            reject(call, ERR_PARAM_DATA_INVALID, exception);
            return;
        } catch (IOException exception) {
            reject(call, ERR_FILE_CACHING_FAILED, exception);
            return;
        }

        Uri contentUri = FileSharerProvider.getUriForFile(getContext(), getProviderAuthority(), cachedFile);
        Intent sendIntent = buildShareIntent(call, contentUri, contentType, filename);
        Intent chooser = Intent.createChooser(sendIntent, chooserTitle(call, filename));
        chooser.addFlags(URI_GRANT_FLAGS);
        grantUriPermissions(sendIntent, contentUri);

        try {
            getActivity().startActivity(chooser);
            call.resolve();
        } catch (ActivityNotFoundException exception) {
            reject(call, ERR_ACTIVITY_NOT_FOUND, exception);
        }
    }

    @PluginMethod
    public void save(PluginCall call) {
        final String filename;
        final Uri uri;

        try {
            filename = requireFilename(call);

            try (Source source = openSource(call)) {
                uri = saveToPublicCollection(call, filename, source);
            }
        } catch (FileSharerException exception) {
            reject(call, exception.code, exception);
            return;
        } catch (IllegalArgumentException exception) {
            reject(call, ERR_PARAM_DATA_INVALID, exception);
            return;
        } catch (IOException exception) {
            reject(call, ERR_FILE_SAVE_FAILED, exception);
            return;
        }

        JSObject result = new JSObject();
        result.put("uri", uri.toString());
        call.resolve(result);
    }

    @PluginMethod
    public void getPluginVersion(PluginCall call) {
        JSObject result = new JSObject();
        result.put("version", implementation.getPluginVersion());
        call.resolve(result);
    }

    private Intent buildShareIntent(PluginCall call, Uri contentUri, String contentType, String filename) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setTypeAndNormalize(contentType);
        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        sendIntent.putExtra(Intent.EXTRA_TITLE, firstNonEmpty(call.getString("title"), filename));
        sendIntent.setClipData(ClipData.newUri(getContext().getContentResolver(), filename, contentUri));
        sendIntent.addFlags(URI_GRANT_FLAGS);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        String subject = firstNonEmpty(call.getString("subject"), call.getString("title"));
        if (subject != null) {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }

        String text = trimToNull(call.getString("text"));
        if (text != null) {
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        }

        return sendIntent;
    }

    private Source openSource(PluginCall call) throws FileSharerException, IOException {
        String requestedContentType = trimToNull(call.getString("contentType"));
        String base64Data = trimToNull(call.getString("base64Data"));

        if (base64Data != null) {
            String contentType = firstNonEmpty(requestedContentType, implementation.contentTypeFromDataUrl(base64Data));
            return new Source(implementation.base64InputStream(base64Data), firstNonEmpty(contentType, DEFAULT_CONTENT_TYPE));
        }

        String path = trimToNull(call.getString("path"));
        if (path == null) {
            throw new FileSharerException(ERR_PARAM_NO_DATA);
        }

        Uri uri = Uri.parse(implementation.normalizePath(path));
        if (uri.getScheme() == null) {
            uri = Uri.fromFile(new File(uri.toString()));
        }

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new FileSharerException(ERR_LOCAL_FILE_NOT_FOUND);
            }

            String contentType = firstNonEmpty(requestedContentType, getContext().getContentResolver().getType(uri));
            return new Source(inputStream, firstNonEmpty(contentType, contentTypeFromFilename(uri.getLastPathSegment())));
        }

        File file = fileFromUri(uri);
        if (!file.exists() || !file.isFile()) {
            throw new FileSharerException(ERR_LOCAL_FILE_NOT_FOUND);
        }

        String contentType = firstNonEmpty(requestedContentType, contentTypeFromFilename(file.getName()));
        return new Source(new FileInputStream(file), firstNonEmpty(contentType, DEFAULT_CONTENT_TYPE));
    }

    private Uri saveToPublicCollection(PluginCall call, String filename, Source source) throws IOException, FileSharerException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveWithMediaStore(call, filename, source);
        }

        return saveLegacy(call, filename, source);
    }

    private Uri saveWithMediaStore(PluginCall call, String filename, Source source) throws IOException, FileSharerException {
        ContentResolver resolver = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, source.contentType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(call, source.contentType));
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collectionUri = collectionUri(call, source.contentType);
        Uri itemUri = resolver.insert(collectionUri, values);
        if (itemUri == null) {
            throw new FileSharerException(ERR_FILE_SAVE_FAILED);
        }

        try (OutputStream outputStream = resolver.openOutputStream(itemUri)) {
            if (outputStream == null) {
                throw new FileSharerException(ERR_FILE_SAVE_FAILED);
            }
            copy(source.inputStream, outputStream);
        } catch (IOException | FileSharerException exception) {
            resolver.delete(itemUri, null, null);
            throw exception;
        }

        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(itemUri, values, null, null);
        return itemUri;
    }

    @SuppressWarnings("deprecation")
    private Uri saveLegacy(PluginCall call, String filename, Source source) throws IOException {
        File directory = Environment.getExternalStoragePublicDirectory(legacyDirectory(call, source.contentType));
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create save directory");
        }

        File outputFile = new File(directory, filename);
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            copy(source.inputStream, outputStream);
        }
        return Uri.fromFile(outputFile);
    }

    private Uri collectionUri(PluginCall call, String contentType) {
        String saveDirectory = androidOption(call, "saveDirectory");
        String normalizedDirectory = saveDirectory == null ? "" : saveDirectory.toLowerCase(Locale.US);

        if ("pictures".equals(normalizedDirectory) || contentType.startsWith("image/")) {
            return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        if ("movies".equals(normalizedDirectory) || contentType.startsWith("video/")) {
            return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        if ("music".equals(normalizedDirectory) || contentType.startsWith("audio/")) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        return MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }

    private String relativePath(PluginCall call, String contentType) {
        String customRelativePath = trimToNull(androidOption(call, "relativePath"));
        if (customRelativePath != null) {
            return sanitizeRelativePath(customRelativePath);
        }

        return legacyDirectory(call, contentType);
    }

    private String legacyDirectory(PluginCall call, String contentType) {
        String saveDirectory = androidOption(call, "saveDirectory");
        String normalizedDirectory = saveDirectory == null ? "" : saveDirectory.toLowerCase(Locale.US);

        if ("pictures".equals(normalizedDirectory) || contentType.startsWith("image/")) {
            return Environment.DIRECTORY_PICTURES;
        }
        if ("movies".equals(normalizedDirectory) || contentType.startsWith("video/")) {
            return Environment.DIRECTORY_MOVIES;
        }
        if ("music".equals(normalizedDirectory) || contentType.startsWith("audio/")) {
            return Environment.DIRECTORY_MUSIC;
        }
        if ("documents".equals(normalizedDirectory)) {
            return Environment.DIRECTORY_DOCUMENTS;
        }
        return Environment.DIRECTORY_DOWNLOADS;
    }

    private File getCacheDir() throws FileSharerException {
        File cacheDir = new File(getContext().getCacheDir(), CAP_FILE_SHARER_TEMP);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new FileSharerException(ERR_FILE_CACHING_FAILED);
        }

        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File cachedFile : cachedFiles) {
                if (cachedFile.isFile()) {
                    cachedFile.delete();
                }
            }
        }

        return cacheDir;
    }

    private String requireFilename(PluginCall call) throws FileSharerException {
        String filename = implementation.safeFilename(call.getString("filename"));
        if (filename == null) {
            throw new FileSharerException(ERR_PARAM_NO_FILENAME);
        }
        return filename;
    }

    private File fileFromUri(Uri uri) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return new File(Objects.requireNonNull(uri.getPath()));
        }
        return new File(uri.toString());
    }

    private String contentTypeFromFilename(String filename) {
        String safeFilename = filename == null ? "" : filename;
        String extension = MimeTypeMap.getFileExtensionFromUrl(safeFilename);
        if (extension == null || extension.isEmpty()) {
            int dotIndex = safeFilename.lastIndexOf('.');
            extension = dotIndex > -1 ? safeFilename.substring(dotIndex + 1) : "";
        }

        String contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
        return firstNonEmpty(contentType, DEFAULT_CONTENT_TYPE);
    }

    private void grantUriPermissions(Intent sendIntent, Uri contentUri) {
        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfos) {
            getContext().grantUriPermission(resolveInfo.activityInfo.packageName, contentUri, URI_GRANT_FLAGS);
        }
    }

    private String chooserTitle(PluginCall call, String filename) {
        return firstNonEmpty(androidOption(call, "chooserTitle"), call.getString("title"), filename);
    }

    private String androidOption(PluginCall call, String key) {
        JSObject androidOptions = call.getObject("android", new JSObject());
        return trimToNull(androidOptions.getString(key));
    }

    private String sanitizeRelativePath(String relativePath) {
        StringBuilder sanitized = new StringBuilder();
        for (String segment : relativePath.replace('\\', '/').split("/")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty() || ".".equals(trimmed) || "..".equals(trimmed)) {
                continue;
            }
            if (sanitized.length() > 0) {
                sanitized.append('/');
            }
            sanitized.append(trimmed);
        }
        return sanitized.length() == 0 ? Environment.DIRECTORY_DOWNLOADS : sanitized.toString();
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024 * 32];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
    }

    private String getProviderAuthority() {
        return getContext().getPackageName() + ".filesharer.fileprovider";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private void reject(PluginCall call, String code, Exception exception) {
        call.reject(code, code, exception);
    }

    private static class Source implements AutoCloseable {

        private final InputStream inputStream;
        private final String contentType;

        private Source(InputStream inputStream, String contentType) {
            this.inputStream = inputStream;
            this.contentType = contentType;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    private static class FileSharerException extends Exception {

        private final String code;

        private FileSharerException(String code) {
            super(code);
            this.code = code;
        }
    }
}
