package com.vidyapeet.storage;

import com.vidyapeet.common.exception.Exceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Stores files in a private Supabase Storage bucket via the Storage REST API.
 * Active only under the {@code prod} profile and marked {@link Primary} so it
 * supersedes {@link LocalStorageService} there; local dev keeps using disk.
 *
 * <p>Files are kept in a <em>private</em> bucket and streamed back through the
 * application's access-controlled download endpoints, so the returned key never
 * changes format ({@code <uuid>.pdf}) and no schema/caller changes are needed.
 */
@Service
@Profile("prod")
@Primary
public class SupabaseStorageService implements StorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private final RestClient client;
    private final String serviceKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${vidyapeet.storage.supabase.url}") String url,
            @Value("${vidyapeet.storage.supabase.service-key}") String serviceKey,
            @Value("${vidyapeet.storage.supabase.bucket:vidyapeet-files}") String bucket) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("vidyapeet.storage.supabase.url (SUPABASE_URL) must be set in the prod profile.");
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("vidyapeet.storage.supabase.service-key (SUPABASE_SERVICE_KEY) must be set in the prod profile.");
        }
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.client = RestClient.builder()
                .baseUrl(url.replaceAll("/+$", ""))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .defaultHeader("apikey", serviceKey)
                .build();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw Exceptions.badRequest("File is required and must not be empty.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw Exceptions.badRequest("File exceeds the 10 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw Exceptions.badRequest("Only PDF files are allowed.");
        }

        String key = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file.", e);
        }

        try {
            client.post()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Failed to store file in Supabase (HTTP " + e.getStatusCode().value() + ").", e);
        }
        return key;
    }

    @Override
    public Resource loadAsResource(String key) {
        try {
            byte[] data = client.get()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .retrieve()
                    .body(byte[].class);
            if (data == null || data.length == 0) {
                throw Exceptions.notFound("Stored file is no longer available.");
            }
            return new ByteArrayResource(data) {
                @Override
                public String getFilename() {
                    return key;
                }
            };
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw Exceptions.notFound("Stored file is no longer available.");
            }
            throw new IllegalStateException(
                    "Failed to load file from Supabase (HTTP " + e.getStatusCode().value() + ").", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.delete()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ignored) {
            // Best-effort cleanup, mirroring LocalStorageService (missing files ignored).
        }
    }
}
