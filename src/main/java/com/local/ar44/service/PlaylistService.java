package com.local.ar44.service;

import com.local.ar44.dto.Playlist;
import com.local.ar44.dto.Video;

import com.local.ar44.repo.PlaylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Comparator;
import java.util.Collections;
import com.local.ar44.dto.Video;
import com.local.ar44.repo.VideoRepository;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaylistService {
    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private VideoRepository videoRepository;

    public List<Playlist> getAllPlaylistsWithThumbnails() {
        List<Playlist> playlists = playlistRepository.findAll();
        for (Playlist p : playlists) {
            if (p.getVideos() != null) {
                p.setVideosCount(p.getVideos().size());
                // Prend jusqu'à 4 thumbnails des vidéos de la playlist
                List<String> thumbs = p.getVideos().stream()
                    .filter(v -> v.getThumbnailUrl() != null && !v.getThumbnailUrl().isEmpty())
                    .limit(4)
                    .map(Video::getThumbnailUrl)
                    .collect(Collectors.toList());
                p.setThumbnails(thumbs);
            } else {
                p.setVideosCount(0);
                p.setThumbnails(Collections.emptyList());
            }
        }
        return playlists;
    }

    @Transactional
    public void createPlaylist(String name) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setCreatedAt(java.time.LocalDateTime.now());
        playlistRepository.save(playlist);
    }

    @Transactional
    public void addVideosToPlaylist(Long playlistId, java.util.List<?> videoIds) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow();
        if (playlist.getVideos() == null) playlist.setVideos(new java.util.HashSet<>());
        for (Object idObj : videoIds) {
            Long vid = null;
            if (idObj instanceof Number n) vid = n.longValue();
            else if (idObj instanceof String s) try { vid = Long.parseLong(s); } catch (Exception ignored) {}
            if (vid != null) {
                videoRepository.findById(vid).ifPresent(v -> playlist.getVideos().add(v));
            }
        }
        playlistRepository.save(playlist);
    }
}
