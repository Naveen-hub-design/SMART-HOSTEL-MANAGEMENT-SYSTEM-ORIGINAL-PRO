package com.hostel.service;

import com.hostel.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Safe unit tests for upload hardening. No Spring context, no database.
 * Run with: mvn test -Dtest=FileUploadServiceTest
 * (The full suite is NOT run here: WardenBlockAssignmentTest targets the
 * real dev database and must not execute in this environment.)
 */
class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        ReflectionTestUtils.setField(
                fileUploadService, "uploadDir", tempDir.toString());
        fileUploadService.init();
    }

    private static byte[] imageBytes(String format) throws IOException {
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out),
                "JDK must support " + format);
        return out.toByteArray();
    }

    private static final byte[] HTML_BYTES =
            "<html><script>alert(1)</script></html>".getBytes();
    private static final byte[] SVG_BYTES =
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                    .getBytes();
    private static final byte[] JS_BYTES =
            "alert('xss')".getBytes();
    private static final byte[] TEXT_BYTES =
            "plain text, not an image".getBytes();

    @Test
    void acceptsValidJpeg() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", imageBytes("jpg"));

        String url = fileUploadService.uploadFile(file);

        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".jpg"));
        assertTrue(Files.exists(
                tempDir.resolve(url.substring("/uploads/".length()))));
    }

    @Test
    void acceptsValidPng() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", imageBytes("png"));

        String url = fileUploadService.uploadFile(file);

        assertTrue(url.endsWith(".png"));
    }

    @Test
    void acceptsValidGif() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.gif", "image/gif", imageBytes("gif"));

        String url = fileUploadService.uploadFile(file);

        assertTrue(url.endsWith(".gif"));
    }

    @Test
    void acceptsUppercaseImageExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "PHOTO.JPG", "image/jpeg", imageBytes("jpg"));

        String url = fileUploadService.uploadFile(file);

        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void storedNameIsUuidNotOriginal() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "myphoto.jpg", "image/jpeg", imageBytes("jpg"));

        String url = fileUploadService.uploadFile(file);

        assertFalse(url.contains("myphoto"));
    }

    @Test
    void rejectsHtml() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.html", "text/html", HTML_BYTES);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsSvg() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.svg", "image/svg+xml", SVG_BYTES);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsJs() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.js", "application/javascript", JS_BYTES);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsUppercaseDangerousExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "EVIL.HTML", "text/html", HTML_BYTES);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsFakeImageBytesWithImageExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.jpg", "image/jpeg", HTML_BYTES);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsMismatchedMimeAndExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/jpeg", imageBytes("png"));

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsMissingMimeType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", null, imageBytes("jpg"));

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(BadRequestException.class,
                () -> fileUploadService.uploadFile(file));
    }
}
