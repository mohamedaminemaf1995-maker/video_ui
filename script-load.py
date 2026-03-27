import re

pattern = r"(TIMESTAMP '[^']+'|'[^']*'|NULL|TRUE|FALSE|\d+)"

def clean_title(file_name):
    if file_name == "NULL":
        return "NULL"
    return file_name.replace(".mp4'", "'")

def convert(line):
    parts = re.findall(pattern, line)

    if len(parts) < 11:
        return None

    id_ = parts[0]
    album = parts[1]
    created_at = parts[2]
    creator = parts[3]
    duration_ms = parts[4]
    source_index = parts[6]
    file_name = parts[8]
    url = parts[9]
    favorite = parts[10]

    thumbnail_url = "NULL"
    title = clean_title(file_name)

    return (
        "INSERT INTO public.video "
        "(id, album, created_at, creator, duration_ms, file_name, source_index, thumbnail_url, title, url, favorite)\n"
        f"VALUES ({id_}, {album}, {created_at}, {creator}, {duration_ms}, {file_name}, {source_index}, {thumbnail_url}, {title}, {url}, {favorite});\n"
    )

input_file = "backup.sql"
output_file = "output.sql"

with open(input_file, "r", encoding="utf-8") as f, open(output_file, "w", encoding="utf-8") as out:
    for num, line in enumerate(f, start=1):
        line = line.strip()

        if not line.startswith("("):
            continue

        sql = convert(line)
        if sql is None:
            print(f"Ligne ignorée {num}: {line[:120]}")
            continue

        out.write(sql)

print("✅ Conversion terminée -> output.sql")