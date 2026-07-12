package com.vidyapeet.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Abstraction over file storage. The dev profile uses {@link LocalStorageService}
 * (disk); a Supabase Storage implementation can be plugged in for production
 * behind this same interface without touching callers.
 */
public interface StorageService {

    /** Allowed content type(s) and per-file cap for the existing PDF uploads (notes/library). */
    Set<String> PDF_CONTENT_TYPES = Set.of("application/pdf");
    long PDF_MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    /** Allowed content types and per-file cap for question images (protects the Supabase free-tier budget). */
    Set<String> IMAGE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    long IMAGE_MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    /**
     * Persists the uploaded file using the PDF policy (only {@code application/pdf},
     * up to 10 MB) and returns an opaque storage key. Existing library/notes callers
     * use this overload unchanged.
     */
    default String store(MultipartFile file) {
        return store(file, PDF_CONTENT_TYPES, PDF_MAX_BYTES);
    }

    /**
     * Persists the uploaded file after validating it against the supplied content-type
     * allow-list and size cap, and returns an opaque storage key whose extension is
     * derived from the file's content type.
     *
     * <p>The file is rejected (with a descriptive {@code 400} error and no stored key)
     * when it is empty, exceeds {@code maxBytes}, or has a content type outside
     * {@code allowedContentTypes}.
     *
     * @param file                the uploaded file
     * @param allowedContentTypes the set of accepted content types (e.g. image types or PDF)
     * @param maxBytes            the maximum allowed file size in bytes
     * @return the opaque storage key for the persisted file
     */
    String store(MultipartFile file, Set<String> allowedContentTypes, long maxBytes);

    /** Loads a previously stored file as a streamable resource. */
    Resource loadAsResource(String key);

    /** Removes a stored file; missing files are ignored. */
    void delete(String key);

    /**
     * Derives the stored-key file extension (including the leading dot) from a validated
     * content type. Assumes the content type has already passed the allow-list check.
     */
    static String extensionForContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
