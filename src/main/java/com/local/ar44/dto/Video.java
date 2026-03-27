package com.local.ar44.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String fileName;
    private Long durationMs;
    private String url;
    private String creator;
    private String album;
    private Integer sourceIndex; // vlc:id
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private Boolean favorite = false;
    private LocalDateTime favoriteAt;
}
