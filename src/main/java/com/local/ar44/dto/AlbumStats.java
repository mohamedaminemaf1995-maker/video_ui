package com.local.ar44.dto;

import lombok.Data;

@Data
public class AlbumStats {
    private String album;
    private Long count;

    public AlbumStats(String album, Long count) {
        this.album = album;
        this.count = count;
    }

    // getters
}