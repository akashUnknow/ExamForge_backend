package com.examforge.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores files on local disk under a configurable base directory. This is
 * the only FileStorageService implementation shipped so far - suitable
 * for local development and single-instance deployments, not for a
 * multi-instance production deployment (each instance would have its own
 * disk). An S3-compatible implementation is the natural next step and can
 * be added as a second bean behind the same interface, selected by
 * profile/config, without touching any caller.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path basePath;

    public LocalFileStorageService(@Value("${examforge.storage.local-path:./storage}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(String originalFilename, String contentType, InputStream content, long sizeBytes) throws IOException {
        Files.createDirectories(basePath);

        String extension = extractExtension(originalFilename);
        String storageKey = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path target = resolveWithinBase(storageKey);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(storageKey, Files.size(target));
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path target = resolveWithinBase(storageKey);
        if (!Files.exists(target)) {
            throw new IOException("No file found for storage key: " + storageKey);
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinBase(storageKey));
        } catch (IOException e) {
            // Best-effort: an already-missing file on delete isn't a caller-facing error.
        }
    }

    /** Resolves a storage key to a path within basePath, rejecting any attempt to escape it (e.g. "../../etc/passwd"). */
    private Path resolveWithinBase(String storageKey) throws IOException {
        Path resolved = basePath.resolve(storageKey).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IOException("Invalid storage key");
        }
        return resolved;
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 && dotIndex < filename.length() - 1 ? filename.substring(dotIndex + 1) : "";
    }
}
