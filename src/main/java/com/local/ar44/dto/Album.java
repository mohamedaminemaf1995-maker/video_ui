package com.local.ar44.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "album")
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "videos")
@ToString(exclude = "videos")
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "albums")
    private Set<Video> videos = new HashSet<>();

    public Album(String name) {
        this.name = name;
    }
}
