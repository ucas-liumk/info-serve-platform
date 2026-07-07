package org.dromara.portal.resources.support;

import org.dromara.file.api.domain.RemoteFile;
import org.dromara.portal.resources.domain.InfoResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPreviewConverterTest {

    @TempDir
    Path tempDir;

    private DocumentPreviewConverter converter() {
        DocumentPreviewConverter c = new DocumentPreviewConverter();
        ReflectionTestUtils.setField(c, "previewCacheDir", tempDir.resolve("cache").toString());
        ReflectionTestUtils.setField(c, "sofficeCommand", "soffice");
        ReflectionTestUtils.setField(c, "sofficeTimeoutSeconds", 30L);
        return c;
    }

    private InfoResource resource() {
        InfoResource r = new InfoResource();
        r.setResourceId(42L);
        r.setFileSize(1024L);
        r.setUpdateTime(new Date(1720000000000L));
        return r;
    }

    private RemoteFile remoteFile() {
        RemoteFile f = new RemoteFile();
        f.setOssId(77L);
        return f;
    }

    @Test
    void cache_file_name_is_deterministic_and_dir_created() throws Exception {
        Path p1 = converter().cacheFileFor(resource(), remoteFile());
        Path p2 = converter().cacheFileFor(resource(), remoteFile());
        assertEquals(p1, p2);
        assertEquals("42-77-1024-1720000000000.pdf", p1.getFileName().toString());
        assertTrue(Files.isDirectory(p1.getParent()));
    }

    @Test
    void isReady_false_for_missing_true_for_nonempty() throws Exception {
        DocumentPreviewConverter c = converter();
        Path p = c.cacheFileFor(resource(), remoteFile());
        assertFalse(c.isReady(p));
        Files.write(p, new byte[]{1});
        assertTrue(c.isReady(p));
    }
}
