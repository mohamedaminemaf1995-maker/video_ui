package com.local.ar44.repo;

import com.local.ar44.dto.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    // List<Video> findByCreatorIgnoreCase(String creator); // supprimé : plus de champ creator
    List<Video> findByTitleContainingIgnoreCase(String title);
    List<Video> findByFavoriteTrue();
    List<Video> findByFavoriteTrueOrderByFavoriteAtDesc();

    // Retourne les favoris ordonnés par favoriteOrder asc (nulls last) puis par favoriteAt desc
    @Query("select v from Video v where v.favorite = true order by v.favoriteOrder asc nulls last, v.favoriteAt desc")
    List<Video> findFavoritesOrdered();

    // Trouve la video favorite ayant le plus grand favoriteOrder
    Optional<Video> findTopByFavoriteTrueOrderByFavoriteOrderDesc();

    // @Query supprimé : plus de champ creator
    // List<String> findDistinctCreators();
}
