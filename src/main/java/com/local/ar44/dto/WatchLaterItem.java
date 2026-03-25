package com.local.ar44.dto;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WatchLaterItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Video video;

    private LocalDateTime addedAt;
}
