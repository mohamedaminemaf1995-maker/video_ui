package com.local.ar44.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import lombok.Data;

@Entity
@Data
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDateTime createdAt;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "playlist_video",
        joinColumns = @JoinColumn(name = "playlist_id"),
        inverseJoinColumns = @JoinColumn(name = "video_id")
    )
    private Set<Video> videos;

    // Champs non persistés pour l'affichage
    private int videosCount;
    private List<String> thumbnails;

    public Set<Video> getVideos() { return videos; }
    public void setVideos(Set<Video> videos) { this.videos = videos; }

    public int getVideosCount() {
        return videosCount;
    }
    public void setVideosCount(int videosCount) {
        this.videosCount = videosCount;
    }
    public List<String> getThumbnails() {
        return thumbnails;
    }
    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }
}
