package com.local.ar44.dto;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_watch_log")
@Data
public class VideoWatchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long videoId;
    private String page;
    private String username;
    private Integer watchedSeconds;
    private LocalDateTime watchedAt;
}
