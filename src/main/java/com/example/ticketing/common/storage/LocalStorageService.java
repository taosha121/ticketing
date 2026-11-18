package com.example.ticketing.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path uploadDir;
    private final String baseUrl;

    public LocalStorageService(
            @Value("${app.storage.local-dir}") String localDir,
            @Value("${app.storage.base-url}") String baseUrl) {
        this.uploadDir = Path.of(localDir).toAbsolutePath();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory: " + this.uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        try {
            file.transferTo(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
        return baseUrl + "/" + filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
