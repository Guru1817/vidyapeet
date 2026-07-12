package com.vidyapeet.storage;

import com.vidyapeet.common.exception.Exceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores files on the local filesystem. Suitable for local development; for
 * production on ephemeral free tiers, swap in a Supabase Storage implementation.
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${vidyapeet.storage.local-dir:./uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory: " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        String contentType = StoragePolicy.validate(file, allowedContentTypes, maxBytes);

        String key = UUID.randomUUID().toString().replace("-", "")
                + StorageService.extensionForContentType(contentType);
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw Exceptions.badRequest("Invalid file path.");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file.", e);
        }
        return key;
    }

    @Override
    public Resource loadAsResource(String key) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) {
                throw Exceptions.badRequest("Invalid file key.");
            }
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw Exceptions.notFound("Stored file is no longer available.");
            }
            return resource;
        } catch (java.net.MalformedURLException e) {
            throw Exceptions.notFound("Stored file is no longer available.");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path target = root.resolve(key).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
