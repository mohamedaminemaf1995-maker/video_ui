package com.local.ar44.controller;

import com.local.ar44.dto.Album;
import com.local.ar44.dto.AlbumStats;
import com.local.ar44.dto.AppConfig;
import com.local.ar44.dto.UpdateVideoRequest;
import com.local.ar44.dto.Video;
import com.local.ar44.dto.VideoResponse;
import com.local.ar44.repo.AlbumRepository;
import com.local.ar44.repo.AppConfigRepository;
import com.local.ar44.repo.VideoRepository;
import com.local.ar44.service.ThumbnailStorageService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final VideoRepository videoRepository;
    private final AppConfigRepository appConfigRepository;
    private final AlbumRepository albumRepository;
    private final ThumbnailStorageService thumbnailStorageService;

    public VideoController(VideoRepository videoRepository,
                           AppConfigRepository appConfigRepository,
                           AlbumRepository albumRepository,
                           ThumbnailStorageService thumbnailStorageService) {
        this.videoRepository = videoRepository;
        this.appConfigRepository = appConfigRepository;
        this.albumRepository = albumRepository;
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
        response.setAlbums(video.getAlbums().stream()
                .map(Album::getName)
                .sorted()
                .toList());
        response.setFavorite(video.getFavorite());
        response.setSourceIndex(video.getSourceIndex());
        response.setUrl(buildUrl(host, fileName));
        response.setFavoriteOrder(video.getFavoriteOrder());
        
        // 🔗 Log pour les URLs des thumbnails
        String thumbUrl = "/api/videos/thumbnail?id=" + video.getId();
        log.debug("[THUMBNAIL-URL] ID: {}, FileName: {}, ThumbURL: {}", 
                video.getId(), fileName, thumbUrl);

        return response;
    }

    private void assignAlbumsToVideo(Video video, String albumString) {
        video.getAlbums().clear();
        if (albumString != null && !albumString.isBlank()) {
            String[] albumNames = albumString.split(",");
            for (String albumName : albumNames) {
                String trimmed = albumName.trim();
                if (!trimmed.isBlank()) {
                    Album a = albumRepository.findByName(trimmed)
                            .orElseGet(() -> albumRepository.save(new Album(trimmed)));
                    video.getAlbums().add(a);
                }
            }
        }
    }

    // new overload to accept a list of album names
    private void assignAlbumsToVideo(Video video, List<String> albums) {
        video.getAlbums().clear();
        if (albums == null) return;
        for (String name : albums) {
            if (name == null) continue;
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            Album a = albumRepository.findByName(trimmed)
                    .orElseGet(() -> albumRepository.save(new Album(trimmed)));
            video.getAlbums().add(a);
        }
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
        return albumRepository.findVideosByAlbumName(album)
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
        List<String> creators = videoRepository.findDistinctCreators()
                .stream()
                .filter(c -> c != null && !c.isBlank())
                .toList();

        // Si aucun créateur enregistré, proposer un élément de retour pour filtrer les inconnus
        if (creators.isEmpty()) {
            return List.of("Unknown");
        }

        return creators;
    }

    @GetMapping("/albums")
    public List<String> getAlbums() {
        return albumRepository.findDistinctAlbumNames();
    }

    // 🔥 NOUVEAU : albums + count
    @GetMapping("/albums/stats")
    public List<AlbumStats> getAlbumsStats() {
        return videoRepository.findAll().stream()
                .flatMap(v -> v.getAlbums().stream())
                .collect(Collectors.groupingBy(Album::getName, Collectors.counting()))
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
        assignAlbumsToVideo(v, album);
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
            @RequestParam(required = false) String album,
            @RequestParam(required = false) Integer sourceIndex
    ) {
        Video v = videoRepository.findById(id).orElseThrow();

        if (title != null) v.setTitle(title);
        if (creator != null) v.setCreator(creator);
        if (album != null) assignAlbumsToVideo(v, album);
        if (sourceIndex != null) {
            int requested = Math.max(0, Math.min(5, sourceIndex));
            v.setSourceIndex(requested);
        }

        return videoRepository.save(v);
    }

    // New RESTful update using JSON body
    @PutMapping("/{id}")
    public Video updateVideoPut(@PathVariable Long id, @RequestBody UpdateVideoRequest req) {
        Video v = videoRepository.findById(id).orElseThrow();

        if (req.getTitle() != null) v.setTitle(req.getTitle());
        if (req.getCreator() != null) v.setCreator(req.getCreator());
        if (req.getAlbums() != null) assignAlbumsToVideo(v, req.getAlbums());
        if (req.getSourceIndex() != null) {
            int requested = Math.max(0, Math.min(5, req.getSourceIndex()));
            v.setSourceIndex(requested);
        }

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
        assignAlbumsToVideo(v, album);
        videoRepository.save(v);
        return "Album(s) mis à jour";
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
    @GetMapping("/source-index/set")
    public String setSourceIndex(@RequestParam Long id, @RequestParam Integer sourceIndex) {
        Video v = videoRepository.findById(id).orElseThrow();

        if (sourceIndex == null) {
            throw new IllegalArgumentException("sourceIndex is required");
        }

        int requested = Math.max(0, Math.min(5, sourceIndex));
        v.setSourceIndex(requested);
        videoRepository.save(v);

        return "SourceIndex réglé";
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
    public ResponseEntity<Resource> getThumbnail(@RequestParam Long id) throws MalformedURLException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video introuvable"));

        Path path = thumbnailStorageService.getThumbPath(video.getFileName());

        if (!Files.exists(path)) {
            log.warn("[THUMBNAIL-MISS] Thumbnail introuvable pour vidéo:");
            log.warn("  - ID: {}", video.getId());
            log.warn("  - FileName: {}", video.getFileName());
            log.warn("  - Title: {}", video.getTitle());
            log.warn("  - Chemin recherché: {}", path.toAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(resource);
    }

    @GetMapping("/favorite/toggle")
    public String toggleFavorite(@RequestParam Long id) {
        Video v = videoRepository.findById(id).orElseThrow();

        boolean nowFav = v.getFavorite() == null || !v.getFavorite();
        v.setFavorite(nowFav);
        if (nowFav) {
            v.setFavoriteAt(LocalDateTime.now());
            // assign an order at the end
            Optional<Video> top = videoRepository.findTopByFavoriteTrueOrderByFavoriteOrderDesc();
            int nextOrder = top.map(tv -> tv.getFavoriteOrder() == null ? 1 : tv.getFavoriteOrder() + 1).orElse(1);
            v.setFavoriteOrder(nextOrder);
        } else {
            v.setFavoriteAt(null);
            v.setFavoriteOrder(null);
        }

        videoRepository.save(v);

        return "OK";
    }

    @GetMapping("/favorites")
    public List<VideoResponse> getFavorites(HttpSession session) {
        String host = resolveHost(session);

        return videoRepository.findFavoritesOrdered()
                .stream()
                .map(v -> toResponse(v, host))
                .toList();
    }

    @PostMapping("/favorites/reorder")
    public String reorderFavorites(@RequestBody List<Long> orderedIds) {
        if (orderedIds == null) {
            throw new IllegalArgumentException("orderedIds is required");
        }

        List<Video> currentFavs = videoRepository.findByFavoriteTrue();
        Set<Long> currentFavIds = currentFavs.stream().map(Video::getId).collect(Collectors.toSet());

        List<Video> toSave = new ArrayList<>();

        int order = 1;
        for (Long id : orderedIds) {
            Video v = videoRepository.findById(id).orElseThrow();
            v.setFavorite(true);
            v.setFavoriteAt(LocalDateTime.now());
            v.setFavoriteOrder(order++);
            toSave.add(v);
            currentFavIds.remove(id);
        }

        // any remaining currently-favorited videos that were not in the new order -> unfavorite
        for (Long remainingId : currentFavIds) {
            Video v = videoRepository.findById(remainingId).orElseThrow();
            v.setFavorite(false);
            v.setFavoriteAt(null);
            v.setFavoriteOrder(null);
            toSave.add(v);
        }

        videoRepository.saveAll(toSave);

        return "Favorites reordered";
    }
}
