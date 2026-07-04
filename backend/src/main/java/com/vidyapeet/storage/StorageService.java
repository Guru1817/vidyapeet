package com.vidyapeet.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over file storage. The dev profile uses {@link LocalStorageService}
 * (disk); a Supabase Storage implementation can be plugged in for production
 * behind this same interface without touching callers.
 */
public interface StorageService {

    /**
     * Persists the uploaded file and returns an opaque storage key, stored in
     * {@code notes.file_url}.
     */
    String store(MultipartFile file);

    /** Loads a previously stored file as a streamable resource. */
    Resource loadAsResource(String key);

    /** Removes a stored file; missing files are ignored. */
    void delete(String key);
}
