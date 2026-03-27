package com.local.ar44.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ThumbnailStorageService {

    private final Path thumbsDir;

    public ThumbnailStorageService(
            @Value("${app.thumbnails.dir:thumbnails}") String thumbnailsDir
    ) {
        this.thumbsDir = Paths.get(thumbnailsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(thumbsDir);
    }

    public Path getThumbPath(Long videoId) {
        return thumbsDir.resolve(videoId + ".jpg");
    }

    public Path getThumbPath(String videoFileName) {
        return thumbsDir.resolve(toThumbnailName(videoFileName));
    }

    public String toThumbnailName(String videoFileName) {
        if (videoFileName == null || videoFileName.isBlank()) {
            throw new IllegalArgumentException("videoFileName ne doit pas être vide");
        }

        String cleanName = Paths.get(videoFileName).getFileName().toString();
        int dotIndex = cleanName.lastIndexOf('.');

        String baseName = (dotIndex > 0)
                ? cleanName.substring(0, dotIndex)
                : cleanName;

        return baseName + ".jpg";
    }

    public boolean exists(String videoFileName) {
        return Files.exists(getThumbPath(videoFileName));
    }

    public boolean exists(Long videoId) {
        return Files.exists(getThumbPath(videoId));
    }

    public void cleanupTempIfNeeded() {
        // No-op
    }
}