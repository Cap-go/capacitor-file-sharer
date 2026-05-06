package app.capgo.filesharer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FileSharerTest {

    @Test
    public void safeFilenameUsesLastPathComponent() {
        FileSharer fileSharer = new FileSharer();

        assertEquals("report.pdf", fileSharer.safeFilename("../exports/report.pdf"));
    }

    @Test
    public void safeFilenameRejectsBlankNames() {
        FileSharer fileSharer = new FileSharer();

        assertNull(fileSharer.safeFilename("   "));
    }

    @Test
    public void normalizeBase64RemovesDataUrlPrefix() {
        FileSharer fileSharer = new FileSharer();

        assertEquals("SGVsbG8=", fileSharer.normalizeBase64("data:text/plain;base64,SGVsbG8="));
    }
}
