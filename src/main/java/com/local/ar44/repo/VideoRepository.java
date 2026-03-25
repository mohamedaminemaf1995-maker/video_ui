package com.local.ar44.repo;

import com.local.ar44.dto.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByCreatorIgnoreCase(String creator);
    List<Video> findByAlbumIgnoreCase(String album);
    List<Video> findByTitleContainingIgnoreCase(String title);

    @Query("select distinct v.creator from Video v where v.creator is not null order by v.creator")
    List<String> findDistinctCreators();

    @Query("select distinct v.album from Video v where v.album is not null order by v.album")
    List<String> findDistinctAlbums();
}
