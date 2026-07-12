package com.vidyapeet.storage;

import com.vidyapeet.common.exception.ApiException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 4: Storage type and size policy
 *
 * <p>For any uploaded file, {@link StorageService#store} accepts it if and only if its
 * content type is in the allowed set (image types for question images, PDF for existing
 * callers) and its size is within the configured per-file limit; disallowed types and
 * oversized files are rejected with a descriptive error and no key is produced.
 *
 * <p>Validates: Requirements 5.1, 5.4, 5.9, 8.4
 *
 * <p>Runs against a real {@link LocalStorageService} backed by a per-property temp
 * directory and Spring's {@link MockMultipartFile}, so the 100+ iterations never touch
 * Supabase.
 */
class StoragePolicyPropertyTest {

    /**
     * Pool of content types spanning the allowed image/PDF types plus disallowed types
     * (other image subtypes, text, binary) and the {@code null} content type.
     */
    private static final String NULL_CONTENT_TYPE = "__null__";

    private LocalStorageService storageService;

    @BeforeProperty
    void setUp() throws IOException {
        Path tempDir = Files.createTempDirectory("vidyapeet-storage-policy-pbt");
        tempDir.toFile().deleteOnExit();
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Property(tries = 100)
    void acceptsIffAllowedTypeAndWithinSize(@ForAll("uploads") Upload upload) {
        Set<String> allowed = upload.allowedContentTypes();
        long maxBytes = upload.maxBytes();
        String contentType = upload.contentType(); // may be null
        byte[] content = new byte[upload.size()];

        MockMultipartFile file =
                new MockMultipartFile("file", "upload.bin", contentType, content);

        boolean typeAllowed = contentType != null
                && allowed.stream().anyMatch(t -> t.equalsIgnoreCase(contentType));
        boolean withinSize = content.length > 0 && content.length <= maxBytes;
        boolean shouldAccept = typeAllowed && withinSize;

        if (shouldAccept) {
            String key = storageService.store(file, allowed, maxBytes);

            // A key is produced and it carries the extension derived from the content type.
            assertThat(key).isNotBlank();
            assertThat(key).endsWith(StorageService.extensionForContentType(contentType));
            // The stored file is retrievable by that key.
            assertThat(storageService.loadAsResource(key).exists()).isTrue();
        } else {
            // Rejected with a descriptive 400 error and no key ever returned.
            assertThatThrownBy(() -> storageService.store(file, allowed, maxBytes))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        ApiException api = (ApiException) ex;
                        assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(api.getMessage()).isNotBlank();
                    });
        }
    }

    /**
     * Generates uploads across both allow-list policies (image and PDF), varied content
     * types (allowed, disallowed, and {@code null}), and sizes that straddle the size cap
     * (empty, small, exactly at the cap, and over the cap).
     */
    @Provide
    Arbitrary<Upload> uploads() {
        Arbitrary<Set<String>> policies =
                Arbitraries.of(StorageService.IMAGE_CONTENT_TYPES, StorageService.PDF_CONTENT_TYPES);

        // Small maxBytes keeps allocation cheap while still exercising the boundary logic.
        Arbitrary<Long> maxBytes = Arbitraries.longs().between(64L, 4096L);

        Arbitrary<String> contentTypes = Arbitraries.of(
                "image/png", "image/jpeg", "image/webp", // allowed for images
                "application/pdf",                        // allowed for PDF
                "image/gif", "image/bmp", "text/plain",   // disallowed
                "application/octet-stream",               // disallowed
                NULL_CONTENT_TYPE                          // null content type
        );

        return Combinators.combine(policies, maxBytes, contentTypes)
                .flatAs((policy, cap, contentType) -> {
                    // Sizes straddling the cap: empty, under, exactly at, and over.
                    Arbitrary<Integer> sizes = Arbitraries.oneOf(
                            Arbitraries.just(0),
                            Arbitraries.integers().between(1, (int) (long) cap),
                            Arbitraries.just((int) (long) cap),
                            Arbitraries.integers().between((int) (long) cap + 1, (int) (long) cap + 512)
                    );
                    return sizes.map(size ->
                            new Upload(policy, cap, normalize(contentType), size));
                });
    }

    private static String normalize(String contentType) {
        return NULL_CONTENT_TYPE.equals(contentType) ? null : contentType;
    }

    /** A generated upload scenario: policy (allow-list + cap) plus the file's type and size. */
    record Upload(Set<String> allowedContentTypes, long maxBytes, String contentType, int size) {
    }
}
