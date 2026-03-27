package com.local.ar44.controller;

import com.local.ar44.dto.AlbumStats;
import com.local.ar44.dto.AppConfig;
import com.local.ar44.dto.Video;
import com.local.ar44.dto.VideoResponse;
import com.local.ar44.repo.AppConfigRepository;
import com.local.ar44.repo.VideoRepository;
import com.local.ar44.service.ThumbnailStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoRepository videoRepository;
    private final AppConfigRepository appConfigRepository;

    private final ThumbnailStorageService thumbnailStorageService;

    public VideoController(VideoRepository videoRepository,
                           AppConfigRepository appConfigRepository,
                           ThumbnailStorageService thumbnailStorageService) {
        this.videoRepository = videoRepository;
        this.appConfigRepository = appConfigRepository;
        this.thumbnailStorageService = thumbnailStorageService;
    }

    // ========================
    // 🔹 HOST MANAGEMENT
    // ========================
    private String resolveHost(HttpSession session) {
        String host = (String) session.getAttribute("mediaHost");

        if (host == null || host.isBlank()) {
            host = appConfigRepository.findAll()
                    .stream()
                    .findFirst()
                    .map(AppConfig::getMediaHost)
                    .orElse("192.168.1.30");

            session.setAttribute("mediaHost", host);
        }
        return host;
    }
    private String buildUrl(String host, String fileName) {
        return "http://" + host + "/" + fileName;
    }

    private VideoResponse toResponse(Video video, String host) {

        String fileName = video.getFileName();

        // 🔥 fallback automatique
        if (fileName == null || fileName.isBlank()) {
            fileName = video.getTitle();
        }

        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setFileName(fileName);
        response.setDurationMs(video.getDurationMs());
        response.setCreator(video.getCreator());
        response.setAlbum(video.getAlbum());
        response.setFavorite(video.getFavorite());
        response.setSourceIndex(video.getSourceIndex());
        response.setUrl(buildUrl(host, fileName));

        return response;
    }

    // ========================
    // 🎬 GET ALL VIDEOS
    // ========================
    @GetMapping
    public List<VideoResponse> getVideos(HttpSession session) {
        String host = resolveHost(session);

        return videoRepository.findAll()
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }

    // ========================
    // 🔍 FILTERS
    // ========================

    @GetMapping("/by-creator")
    public List<VideoResponse> getByCreator(@RequestParam String creator, HttpSession session) {
        String host = resolveHost(session);
        return videoRepository.findByCreatorIgnoreCase(creator)
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }

    @GetMapping("/by-album")
    public List<VideoResponse> getByAlbum(@RequestParam String album, HttpSession session) {
        String host = resolveHost(session);
        return videoRepository.findByAlbumIgnoreCase(album)
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }

    @GetMapping("/search")
    public List<VideoResponse> search(@RequestParam String q, HttpSession session) {
        String host = resolveHost(session);
        return videoRepository.findByTitleContainingIgnoreCase(q)
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }

    // ========================
    // 📊 META DATA
    // ========================

    @GetMapping("/creators")
    public List<String> getCreators() {
        return videoRepository.findDistinctCreators();
    }

    @GetMapping("/albums")
    public List<String> getAlbums() {
        return videoRepository.findDistinctAlbums();
    }

    // 🔥 NOUVEAU : albums + count
    @GetMapping("/albums/stats")
    public List<AlbumStats> getAlbumsStats() {
        return videoRepository.findAll().stream()
                .collect(Collectors.groupingBy(Video::getAlbum, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> new AlbumStats(e.getKey(), e.getValue()))
                .toList();
    }

    // ========================
    // ➕ CREATE VIDEO
    // ========================
    @GetMapping("/create")
    public Video createVideo(
            @RequestParam String fileName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false) String album,
            @RequestParam(required = false) Long duration
    ) {

        Video v = new Video();
        v.setFileName(fileName);
        v.setTitle(title != null ? title : fileName);
        v.setCreator(creator);
        v.setAlbum(album);
        v.setDurationMs(duration);

        return videoRepository.save(v);
    }

    // ========================
    // ✏️ UPDATE VIDEO
    // ========================
    @GetMapping("/update")
    public Video updateVideo(
            @RequestParam Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false) String album
    ) {
        Video v = videoRepository.findById(id).orElseThrow();

        if (title != null) v.setTitle(title);
        if (creator != null) v.setCreator(creator);
        if (album != null) v.setAlbum(album);

        return videoRepository.save(v);
    }

    // ========================
    // ❌ DELETE VIDEO
    // ========================
    @GetMapping("/delete")
    public String deleteVideo(@RequestParam Long id) {
        videoRepository.deleteById(id);
        return "Video supprimée : " + id;
    }

    @GetMapping("/album/set")
    public String setAlbum(@RequestParam Long id, @RequestParam String album) {
        Video v = videoRepository.findById(id).orElseThrow();
        v.setAlbum(album);
        videoRepository.save(v);
        return "Album mis à jour";
    }
    @GetMapping("/source-index/increase")
    public String increaseSourceIndex(@RequestParam Long id) {
        Video v = videoRepository.findById(id).orElseThrow();

        Integer current = v.getSourceIndex();
        if (current == null) current = 0;
        if(current < 5) {
            v.setSourceIndex(current + 1);
            videoRepository.save(v);
        }
        return "SourceIndex augmenté";
    }
    @GetMapping("/source-index/decrease")
    public String decreaseSourceIndex(@RequestParam Long id) {
        Video v = videoRepository.findById(id).orElseThrow();

        Integer current = v.getSourceIndex();
        if (current == null) current = 0;

        if (current > 0) {
            v.setSourceIndex(current - 1);
        }

        videoRepository.save(v);
        return "SourceIndex diminué";
    }


    @GetMapping("/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@RequestParam Long id) {
        try {
            Path thumbPath = thumbnailStorageService.getThumbPath(id);

            if (!Files.exists(thumbPath) || Files.size(thumbPath) == 0) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(thumbPath.toFile());

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/favorite/toggle")
    public String toggleFavorite(@RequestParam Long id) {
        Video v = videoRepository.findById(id).orElseThrow();

        boolean nowFav = v.getFavorite() == null || !v.getFavorite();
        v.setFavorite(nowFav);
        if (nowFav) {
            v.setFavoriteAt(LocalDateTime.now());
        } else {
            v.setFavoriteAt(null);
        }

        videoRepository.save(v);

        return "OK";
    }

    @GetMapping("/favorites")
    public List<VideoResponse> getFavorites(HttpSession session) {
        String host = resolveHost(session);

        return videoRepository.findByFavoriteTrueOrderByFavoriteAtDesc()
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }
}
