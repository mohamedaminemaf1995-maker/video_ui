package com.local.ar44.service;

import com.local.ar44.dto.AppConfig;
import com.local.ar44.dto.Video;
import com.local.ar44.repo.AppConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ThumbnailService {

    private final AppConfigRepository appConfigRepository;
    private final ThumbnailStorageService thumbnailStorageService;

    // configurable width and quality
    private final int thumbWidth;
    private final int thumbQuality;

    public ThumbnailService(AppConfigRepository appConfigRepository,
                            ThumbnailStorageService thumbnailStorageService,
                            @Value("${thumbnail.width:180}") int thumbWidth,
                            @Value("${thumbnail.quality:4}") int thumbQuality) {
        this.appConfigRepository = appConfigRepository;
        this.thumbnailStorageService = thumbnailStorageService;
        this.thumbWidth = thumbWidth;
        this.thumbQuality = thumbQuality;
    }

    public Path generateIfMissing(Video video) throws Exception {
        Path thumbPath = thumbnailStorageService.getThumbPath(video.getId());

        if (Files.exists(thumbPath) && Files.size(thumbPath) > 0) {
            return thumbPath;
        }

        String fileName = video.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = video.getTitle();
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("Nom de fichier introuvable pour video id=" + video.getId());
        }

        String host = appConfigRepository.findAll()
                .stream()
                .findFirst()
                .map(AppConfig::getMediaHost)
                .orElse("192.168.1.30");

        String videoUrl = "http://" + host + "/" + fileName;

        String seek = resolveSeekTime(video.getDurationMs());

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-ss", seek,
                "-i", videoUrl,
                "-frames:v", "1",
                "-vf", "scale=" + thumbWidth + ":-1",
                "-q:v", String.valueOf(thumbQuality),
                thumbPath.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0 || !Files.exists(thumbPath) || Files.size(thumbPath) == 0) {
            throw new RuntimeException("Echec génération thumbnail pour video id=" + video.getId());
        }

        return thumbPath;
    }

    private String resolveSeekTime(Long durationMs) {
        if (durationMs == null) {
            return "3";
        }

        long tenMinutesMs = 10 * 60 * 1000L;
        return durationMs < tenMinutesMs ? "3" : "35";
    }
}