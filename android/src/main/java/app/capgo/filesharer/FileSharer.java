package app.capgo.filesharer;

import android.util.Base64;
import android.util.Base64InputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class FileSharer {

    public String getPluginVersion() {
        return "8.0.0";
    }

    public String safeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        String trimmed = filename.trim().replace('\\', '/');
        int lastSlash = trimmed.lastIndexOf('/');
        String safeName = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        return safeName.isEmpty() ? null : safeName;
    }

    public String normalizeBase64(String base64Data) {
        String trimmed = base64Data.trim();
        int commaIndex = trimmed.indexOf(',');

        if (commaIndex > -1 && trimmed.substring(0, commaIndex).toLowerCase().contains("base64")) {
            trimmed = trimmed.substring(commaIndex + 1);
        }

        return trimmed.replaceAll("\\s", "");
    }

    public InputStream base64InputStream(String base64Data) {
        String payload = normalizeBase64(base64Data);
        return new Base64InputStream(new ByteArrayInputStream(payload.getBytes(StandardCharsets.US_ASCII)), Base64.DEFAULT);
    }

    public String contentTypeFromDataUrl(String base64Data) {
        String trimmed = base64Data.trim();
        int commaIndex = trimmed.indexOf(',');

        if (commaIndex > -1) {
            String header = trimmed.substring(0, commaIndex).toLowerCase();
            if (header.startsWith("data:") && header.contains(";base64")) {
                return trimmed.substring(5, header.indexOf(";base64"));
            }
        }

        return null;
    }

    public String normalizePath(String path) {
        String trimmed = path.trim();
        int markerIndex = trimmed.indexOf("_capacitor_file_");

        if (markerIndex > -1) {
            String rawPath = trimmed.substring(markerIndex + "_capacitor_file_".length());
            return URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        }

        return trimmed;
    }
}
