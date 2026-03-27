-- Migration Flyway: créer la table `video` si elle n'existe pas, puis ajouter la colonne `favorite_at` si elle manque

-- 1) Créer la table `video` avec les colonnes de base (idempotent)
CREATE TABLE IF NOT EXISTS public.video (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR,
    file_name VARCHAR,
    duration_ms BIGINT,
    url VARCHAR,
    creator VARCHAR,
    album VARCHAR,
    source_index INTEGER,
    thumbnail_url VARCHAR,
    created_at TIMESTAMP,
    favorite BOOLEAN DEFAULT FALSE
);

-- 2) Ajouter la colonne favorite_at si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'video' AND column_name = 'favorite_at'
    ) THEN
        ALTER TABLE public.video ADD COLUMN favorite_at TIMESTAMP;
    END IF;
END$$;
