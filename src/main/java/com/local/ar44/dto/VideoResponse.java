package com.local.ar44.dto;

import lombok.Data;

@Data
public class VideoResponse {
    private Long id;
    private String title;
    private String fileName;
    private String url;
    private Long durationMs;
    private String creator;
    private String album;
    private Integer sourceIndex;

    // getters/setters
}