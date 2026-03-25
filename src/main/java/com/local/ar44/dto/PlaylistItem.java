package com.local.ar44.dto;

import jakarta.persistence.*;

@Entity
public class PlaylistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Playlist playlist;

    @ManyToOne
    private Video video;

    private Integer position;
}
