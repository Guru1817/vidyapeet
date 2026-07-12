package com.vidyapeet.storage;

import com.vidyapeet.common.exception.Exceptions;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared upload-policy validation for {@link StorageService} implementations.
 *
 * <p>Rejects empty, oversized, or disallowed-type files with a descriptive
 * {@code 400} error (via {@link Exceptions}) so no storage key is ever produced
 * for an invalid upload.
 */
final class StoragePolicy {

    private StoragePolicy() {
    }

    /**
     * Validates the uploaded file against the allow-list and size cap.
     *
     * @return the (non-null) content type when the file is acceptable
     * @throws com.vidyapeet.common.exception.ApiException if the file is empty, too large, or a disallowed type
     */
    static String validate(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw Exceptions.badRequest("File is required and must not be empty.");
        }
        if (file.getSize() > maxBytes) {
            throw Exceptions.badRequest(
                    "File exceeds the " + humanReadable(maxBytes) + " limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !containsIgnoreCase(allowedContentTypes, contentType)) {
            throw Exceptions.badRequest(
                    "Only " + describe(allowedContentTypes) + " files up to "
                            + humanReadable(maxBytes) + " are allowed.");
        }
        return contentType;
    }

    private static boolean containsIgnoreCase(Set<String> allowed, String contentType) {
        return allowed.stream().anyMatch(t -> t.equalsIgnoreCase(contentType));
    }

    private static String describe(Set<String> allowedContentTypes) {
        return allowedContentTypes.stream()
                .map(StoragePolicy::shortName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String shortName(String contentType) {
        int slash = contentType.indexOf('/');
        String subtype = slash >= 0 ? contentType.substring(slash + 1) : contentType;
        return subtype.toUpperCase();
    }

    private static String humanReadable(long bytes) {
        long mb = bytes / (1024 * 1024);
        if (mb > 0 && bytes % (1024 * 1024) == 0) {
            return mb + " MB";
        }
        long kb = bytes / 1024;
        if (kb > 0 && bytes % 1024 == 0) {
            return kb + " KB";
        }
        return bytes + " bytes";
    }
}
