package com.local.ar44.service;

import com.local.ar44.dto.Video;
import com.local.ar44.repo.VideoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class DbLoader implements CommandLineRunner {



    private final VideoRepository videoRepository;
    private final ThumbnailService thumbnailService;
    private final H2DumpService dumpService;
    public DbLoader(H2DumpService dumpService, VideoRepository videoRepository,
                    ThumbnailService thumbnailService) {
        this.dumpService = dumpService;
        this.videoRepository = videoRepository;
        this.thumbnailService = thumbnailService;
    }
    @Override
    public void run(String... args) {
        try {
            File file = new File("./backup.sql");
            if (file.exists() && file.length() > 0) {
                dumpService.load();
            }
            List<Video> videos = videoRepository.findAll();

            System.out.println("Préparation thumbnails: " + videos.size() + " vidéos");

            int ok = 0;
            int ko = 0;

            for (Video video : videos) {
                try {
                    thumbnailService.generateIfMissing(video);
                    ok++;
                } catch (Exception e) {
                    ko++;
                }
            }

            System.out.println("Thumbnails générés. OK=" + ok + ", KO=" + ko);
        } catch (Exception e) {
            System.out.println("❌ Erreur chargement dump: " + e.getMessage());
        }
    }
}
