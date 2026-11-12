package com.cssms.service;

import com.cssms.config.AppProperties;
import com.cssms.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores uploaded files under a server-controlled name; the client file name is never used as a path. */
@Slf4j
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg");

    private final Path root;

    public FileStorageService(AppProperties properties) {
        this.root = Path.of(properties.storage().uploadDir()).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        String contentType = ALLOWED.get(extension);
        if (contentType == null) {
            throw new BadRequestException("Only PDF, PNG and JPG files are allowed");
        }
        try {
            Path directory = root.resolve(folder).normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + "." + extension);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(original, root.relativize(target).toString().replace('\\', '/'), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store file", ex);
        }
    }

    public Resource load(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root) || !Files.isReadable(path)) {
            throw new BadRequestException("Attachment is not available");
        }
        return new PathResource(path);
    }

    public void delete(String relativePath) {
        if (relativePath == null) {
            return;
        }
        try {
            Path path = root.resolve(relativePath).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            log.warn("Could not delete file {}: {}", relativePath, ex.getMessage());
        }
    }

    private static String sanitize(String name) {
        String base = name == null ? "attachment" : name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
        return base.isBlank() ? "attachment" : base;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalName, String relativePath, String contentType) {
    }
}
