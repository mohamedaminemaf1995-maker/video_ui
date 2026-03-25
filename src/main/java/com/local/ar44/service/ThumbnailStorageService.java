package com.local.ar44.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

@Service
public class ThumbnailStorageService {

    private final Path thumbsDir;

    public ThumbnailStorageService() throws IOException {
        this.thumbsDir = Files.createTempDirectory("video-thumbs-");
        System.out.println("Thumb dir: " + thumbsDir.toAbsolutePath());
    }

    public Path getThumbsDir() {
        return thumbsDir;
    }

    public Path getThumbPath(Long videoId) {
        return thumbsDir.resolve(videoId + ".jpg");
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (Files.exists(thumbsDir)) {
                Files.walk(thumbsDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }
}
