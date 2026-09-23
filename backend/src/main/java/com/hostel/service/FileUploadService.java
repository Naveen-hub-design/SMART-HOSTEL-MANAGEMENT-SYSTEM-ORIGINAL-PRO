package com.hostel.service;

import com.hostel.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    /**
     * Only raster image formats decodable by the JDK built-in readers are
     * accepted. Vector/XML-based formats (SVG) and active content
     * (HTML/JS) are rejected because uploads are served publicly under
     * /uploads/** and must never be interpretable as scripts.
     */
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif"
    );

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory created at: {}", uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", uploadPath, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                    .toLowerCase();
        }

        String expectedContentType = ALLOWED_IMAGE_TYPES.get(extension);
        if (expectedContentType == null) {
            throw new BadRequestException(
                    "Only JPG, PNG and GIF images are allowed");
        }

        String declaredContentType = file.getContentType();
        if (declaredContentType == null ||
                !declaredContentType.equalsIgnoreCase(expectedContentType)) {
            throw new BadRequestException(
                    "File content type does not match its image type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Could not read uploaded file", e);
            throw new BadRequestException("Uploaded file could not be read");
        }

        // Verify the bytes genuinely decode as the claimed raster image type.
        // This rejects files like HTML/JS renamed to .jpg outright.
        try {
            BufferedImage image =
                    ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BadRequestException(
                        "Uploaded file is not a valid image");
            }
        } catch (IOException e) {
            log.error("Could not decode uploaded image", e);
            throw new BadRequestException(
                    "Uploaded file is not a valid image");
        }

        String filename = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetLocation = uploadPath.resolve(filename);
            Files.copy(new ByteArrayInputStream(bytes), targetLocation,
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded successfully: {}", filename);
        } catch (IOException e) {
            log.error("Could not upload file: {}", filename, e);
            throw new RuntimeException("Could not upload file", e);
        }

        return "/uploads/" + filename;
    }
}
