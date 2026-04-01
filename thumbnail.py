import os
import re
import subprocess
from pathlib import Path
from urllib.parse import quote
from concurrent.futures import ThreadPoolExecutor, as_completed

import psycopg2
from psycopg2.extras import RealDictCursor


# =========================================================
# CONFIG
# =========================================================
DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "dbname": "db_video",
    "user": "postgres",
    "password": "123456",
}

OUTPUT_DIR = "thumbnails"
FFMPEG_PATH = "ffmpeg"

CAPTURE_TIME = "00:00:10"
THUMBNAIL_SIZE = "240:-1"   # ex: 160:-1 / 320:-1 / 640:-1
JPEG_QUALITY = "6"          # plus grand = plus léger, 2 = meilleure qualité
OVERWRITE = False
MAX_WORKERS = 6             # essaye 4, 6 ou 8
FFMPEG_TIMEOUT = 60         # secondes

VIDEO_TABLE = "video"
VIDEO_ID_COLUMN = "id"
VIDEO_FILE_NAME_COLUMN = "file_name"

APP_CONFIG_TABLE = "app_config"
APP_CONFIG_ID_COLUMN = "id"
APP_CONFIG_MEDIA_HOST_COLUMN = "media_host"


# =========================================================
# DB
# =========================================================
def get_connection():
    return psycopg2.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        dbname=DB_CONFIG["dbname"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        cursor_factory=RealDictCursor
    )


def get_media_host(conn) -> str:
    query = f"""
        SELECT {APP_CONFIG_MEDIA_HOST_COLUMN}
        FROM {APP_CONFIG_TABLE}
        WHERE {APP_CONFIG_ID_COLUMN} = %s
    """
    with conn.cursor() as cur:
        cur.execute(query, (1,))
        row = cur.fetchone()

    if not row:
        raise ValueError("Aucune ligne trouvée dans app_config pour id = 1")

    media_host = row.get(APP_CONFIG_MEDIA_HOST_COLUMN)
    if not media_host:
        raise ValueError("media_host est vide pour app_config.id = 1")

    return normalize_media_host(media_host)


def get_videos(conn):
    query = f"""
        SELECT {VIDEO_ID_COLUMN}, {VIDEO_FILE_NAME_COLUMN}
        FROM {VIDEO_TABLE}
        WHERE {VIDEO_FILE_NAME_COLUMN} IS NOT NULL
          AND TRIM({VIDEO_FILE_NAME_COLUMN}) <> ''
        ORDER BY {VIDEO_ID_COLUMN}
    """
    with conn.cursor() as cur:
        cur.execute(query)
        return cur.fetchall()


# =========================================================
# HELPERS
# =========================================================
def normalize_media_host(media_host: str) -> str:
    media_host = media_host.strip()

    if not media_host.startswith("http://") and not media_host.startswith("https://"):
        media_host = "http://" + media_host

    if not media_host.endswith("/"):
        media_host += "/"

    return media_host


def build_video_url(media_host: str, file_name: str) -> str:
    file_name = file_name.strip().lstrip("/")
    return media_host + quote(file_name)


def safe_file_stem(file_name: str, video_id: int) -> str:
    stem = Path(file_name).stem.strip()
    if not stem:
        stem = f"video_{video_id}"

	# Remplace uniquement les caractères interdits, mais PAS les espaces
    stem = re.sub(r'[<>:"/\\|?*]', "_", stem)
    # Ne remplace plus les espaces par des underscores
    # stem = re.sub(r"\s+", "_", stem).strip("_")

    if not stem:
        stem = f"video_{video_id}"

    return stem


# =========================================================
# FFMPEG
# =========================================================
def generate_thumbnail(video_url: str, output_path: str) -> tuple[bool, str]:
    cmd = [
        FFMPEG_PATH,
        "-loglevel", "error",
        "-threads", "1",
        "-ss", CAPTURE_TIME,
        "-i", video_url,
        "-frames:v", "1",
        "-vf", f"scale={THUMBNAIL_SIZE}",
        "-q:v", JPEG_QUALITY,
        "-an",
        "-sn",
        "-dn",
    ]

    cmd.append("-y" if OVERWRITE else "-n")
    cmd.append(output_path)

    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=FFMPEG_TIMEOUT
        )

        if result.returncode == 0:
            return True, ""

        return False, result.stderr.strip()

    except subprocess.TimeoutExpired:
        return False, f"Timeout ffmpeg après {FFMPEG_TIMEOUT}s"
    except FileNotFoundError:
        return False, "ffmpeg introuvable dans le PATH"
    except Exception as e:
        return False, str(e)


def process_video(video: dict, media_host: str) -> dict:
    video_id = video[VIDEO_ID_COLUMN]
    file_name = video[VIDEO_FILE_NAME_COLUMN]

    video_url = build_video_url(media_host, file_name)
    output_name = safe_file_stem(file_name, video_id) + ".jpg"
    output_path = os.path.join(OUTPUT_DIR, output_name)

    if os.path.exists(output_path) and not OVERWRITE:
        return {
            "id": video_id,
            "file_name": file_name,
            "status": "skip",
            "output_path": output_path,
            "error": ""
        }

    ok, error = generate_thumbnail(video_url, output_path)

    return {
        "id": video_id,
        "file_name": file_name,
        "status": "ok" if ok else "error",
        "output_path": output_path,
        "error": error
    }


# =========================================================
# MAIN
# =========================================================
def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    conn = None
    try:
        conn = get_connection()
        media_host = get_media_host(conn)
        videos = get_videos(conn)

        print(f"[INFO] media_host = {media_host}")
        print(f"[INFO] vidéos trouvées = {len(videos)}")
        print(f"[INFO] taille thumbnails = {THUMBNAIL_SIZE}")
        print(f"[INFO] workers = {MAX_WORKERS}")

        if not videos:
            return

        ok_count = 0
        skip_count = 0
        error_count = 0

        with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            futures = [executor.submit(process_video, video, media_host) for video in videos]

            for idx, future in enumerate(as_completed(futures), start=1):
                result = future.result()
                status = result["status"]
                file_name = result["file_name"]

                if status == "ok":
                    ok_count += 1
                    print(f"[{idx}/{len(videos)}] OK   {file_name}")
                elif status == "skip":
                    skip_count += 1
                    print(f"[{idx}/{len(videos)}] SKIP {file_name}")
                else:
                    error_count += 1
                    print(f"[{idx}/{len(videos)}] ERR  {file_name} -> {result['error']}")

        print("\n===== RÉSUMÉ =====")
        print(f"Succès    : {ok_count}")
        print(f"Skip      : {skip_count}")
        print(f"Erreurs   : {error_count}")
        print(f"Dossier   : {os.path.abspath(OUTPUT_DIR)}")

    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    main()