package com.local.ar44.controller;

import com.local.ar44.dto.Playlist;
import com.local.ar44.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    @Autowired
    private PlaylistService playlistService;

    @GetMapping("")
    public List<Playlist> getPlaylists() {
        return playlistService.getAllPlaylistsWithThumbnails();
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPlaylist(@RequestParam String name) {
        playlistService.createPlaylist(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playlistId}/add-videos")
    public ResponseEntity<?> addVideosToPlaylist(@PathVariable Long playlistId, @RequestBody Map<String, Object> body) {
        Object idsObj = body.get("videoIds");
        if (!(idsObj instanceof java.util.List<?> ids)) {
            return ResponseEntity.badRequest().body("videoIds manquant ou invalide");
        }
        playlistService.addVideosToPlaylist(playlistId, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playlistId}/add-video")
    public ResponseEntity<?> addVideoToPlaylist(@PathVariable Long playlistId, @RequestBody Map<String, Object> body) {
        Object idObj = body.get("videoId");
        Long vid = null;
        if (idObj instanceof Number n) vid = n.longValue();
        else if (idObj instanceof String s) try { vid = Long.parseLong(s); } catch (Exception ignored) {}
        if (vid == null) return ResponseEntity.badRequest().body("videoId manquant ou invalide");
        playlistService.addVideosToPlaylist(playlistId, java.util.List.of(vid));
        return ResponseEntity.ok().build();
    }
}
