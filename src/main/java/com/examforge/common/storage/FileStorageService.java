package com.examforge.common.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction over "where uploaded files actually live," per the spec's
 * explicit instruction not to store large files in Postgres.
 *
 * Only a local-disk implementation ({@link LocalFileStorageService}) is
 * provided so far - it's what's actually running and testable in this
 * environment. The interface is deliberately shaped so an S3-compatible
 * implementation can be swapped in later (using the STORAGE_* env vars
 * already defined since Phase 1) without any caller of this interface
 * needing to change: store()/load()/delete() say nothing about the
 * underlying mechanism.
 */
public interface FileStorageService {

    /**
     * Stores the given content and returns a storage key that can later be
     * used to load or delete it. The key is an opaque string as far as
     * callers are concerned - never assume it's a filesystem path.
     */
    StoredFile store(String originalFilename, String contentType, InputStream content, long sizeBytes) throws IOException;

    Resource load(String storageKey) throws IOException;

    void delete(String storageKey);

    record StoredFile(String storageKey, long sizeBytes) {
    }
}
