package com.local.ar44.repo;

import com.local.ar44.dto.Album;
import com.local.ar44.dto.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByName(String name);
    
    @Query("select distinct a.name from Album a order by a.name")
    List<String> findDistinctAlbumNames();
    
    @Query("select distinct v from Video v join v.albums a where lower(a.name) = lower(:albumName)")
    List<Video> findVideosByAlbumName(String albumName);
}
