package com.local.ar44.service;

import com.local.ar44.dto.AppConfig;
import com.local.ar44.dto.Video;
import com.local.ar44.repo.AppConfigRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ThumbnailService {

    private final AppConfigRepository appConfigRepository;
    private final ThumbnailStorageService thumbnailStorageService;

    public ThumbnailService(AppConfigRepository appConfigRepository,
                            ThumbnailStorageService thumbnailStorageService) {
        this.appConfigRepository = appConfigRepository;
        this.thumbnailStorageService = thumbnailStorageService;
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
                "-vf", "scale=180:-1",
                "-q:v", "24",
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