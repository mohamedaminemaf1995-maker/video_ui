package com.local.ar44.dto;

import lombok.Data;
import java.util.List;

@Data
public class VideoResponse {
    private Long id;
    private String title;
    private String fileName;
    private String url;
    private Long durationMs;
    private String creator;
    private List<String> albums;
    private Integer sourceIndex;
    private Boolean favorite;

    // getters/setters
}