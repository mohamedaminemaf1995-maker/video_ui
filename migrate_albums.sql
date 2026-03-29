-- Migration SQL pour peupler la table album et video_album
-- Run this after Hibernate has auto-created the new tables

-- 1. Insert all unique album names into the album table
INSERT INTO album (name) 
SELECT DISTINCT TRIM(album) 
FROM video 
WHERE album IS NOT NULL AND album != ''
ON CONFLICT(name) DO NOTHING;

-- 2. Populate video_album junction table from existing album strings
INSERT INTO video_album (video_id, album_id)
SELECT DISTINCT v.id, a.id
FROM video v
CROSS JOIN album a
WHERE v.album IS NOT NULL 
  AND v.album != ''
  AND (
    v.album = a.name
    OR v.album LIKE a.name || ',%'
    OR v.album LIKE '%,' || a.name
    OR v.album LIKE '%,' || a.name || ',%'
  )
ON CONFLICT DO NOTHING;

-- Verify migration was successful
SELECT COUNT(*) as total_albums FROM album;
SELECT COUNT(*) as total_video_album_mappings FROM video_album;
