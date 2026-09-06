#!/usr/bin/env python3
"""Construit le manifeste de données du portail web.

Agrège, sans jamais toucher à `M:` directement, tout ce que le repo sait de la
donnée : fichiers du vault `data/`, tables de `plugin/db/music.db`, scans CSV.
Le résultat est écrit dans `apps/web/data/manifest.js` sous forme d'affectation
`window.PORTAL6 = {...}` — un `.js` et non un `.json` pour que `music.html`
s'ouvre aussi en double-clic (`file://` bloque `fetch`).

    python apps/web/build_manifest.py

Sortie : agrégats uniquement (comptes, tailles, taux) — aucun chemin de fichier
musical, aucune donnée nominative.
"""

from __future__ import annotations

import collections
import csv
import datetime as dt
import json
import os
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "apps" / "web" / "data" / "manifest.js"

csv.field_size_limit(10**7)


# --------------------------------------------------------------------------- utils
def human_bytes(n: int) -> str:
    for unit in ("o", "Ko", "Mo", "Go", "To"):
        if n < 1024 or unit == "To":
            return f"{n:.0f} {unit}" if unit == "o" else f"{n:.1f} {unit}"
        n /= 1024
    return f"{n:.1f} To"


def stat_file(rel: str) -> dict | None:
    """Métadonnées d'un fichier du repo, ou None s'il n'existe pas."""
    p = ROOT / rel
    if not p.exists():
        return None
    st = p.stat()
    return {
        "path": rel,
        "size": st.st_size,
        "size_h": human_bytes(st.st_size),
        "mtime": dt.datetime.fromtimestamp(st.st_mtime).strftime("%Y-%m-%d %H:%M"),
    }


def count_lines(rel: str) -> int | None:
    """Lignes non vides — les listes de quarantaine finissent par une ligne blanche."""
    p = ROOT / rel
    if not p.exists():
        return None
    with p.open("rb") as f:
        return sum(1 for line in f if line.strip().strip(b"\xef\xbb\xbf"))


# --------------------------------------------------------------------------- scans
def scan_stats(rel: str) -> dict | None:
    """Dépouille un `library_scan*.csv` : périmètre, tags, extensions, volume."""
    p = ROOT / rel
    if not p.exists():
        return None

    folders: collections.Counter = collections.Counter()
    exts: collections.Counter = collections.Counter()
    total = size = no_tags = no_artist = no_album = 0

    with p.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.reader(f)
        header = next(reader, None)
        if not header:
            return None
        col = {name: i for i, name in enumerate(header)}

        def get(row, name):
            i = col.get(name)
            return row[i] if i is not None and i < len(row) else ""

        for row in reader:
            if not row:
                continue
            total += 1
            # /mnt/m/<top>/<sous-dossier>/... → on garde les deux premiers niveaux
            parts = get(row, "path").replace("\\", "/").split("/")
            folders["/".join(parts[3:5]) if len(parts) > 4 else "/".join(parts[3:])] += 1
            exts[get(row, "extension").lower() or "(sans)"] += 1
            try:
                size += int(get(row, "size_bytes") or 0)
            except ValueError:
                pass
            if get(row, "tags_read") != "yes":
                no_tags += 1
            if not get(row, "artist").strip():
                no_artist += 1
            if not get(row, "album").strip():
                no_album += 1

    return {
        "files": total,
        "size": size,
        "size_h": human_bytes(size),
        "no_tags": no_tags,
        "no_artist": no_artist,
        "no_album": no_album,
        "folders": folders.most_common(12),
        "extensions": exts.most_common(8),
    }


# --------------------------------------------------------------------------- db
def db_stats(rel: str) -> dict | None:
    """Comptes, taux de recoupement et remplissage de l'enrichissement."""
    p = ROOT / rel
    if not p.exists():
        return None

    con = sqlite3.connect(f"file:{p}?mode=ro", uri=True)
    one = lambda q: con.execute(q).fetchone()[0]  # noqa: E731

    tables = [r[0] for r in con.execute(
        "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    counts = {t: one(f'SELECT COUNT(*) FROM "{t}"') for t in tables}

    has_view = bool(con.execute(
        "SELECT 1 FROM sqlite_master WHERE type='view' AND name='v_track_status'").fetchone())
    cross = {}
    if has_view:
        cross = {
            "both": one("SELECT COUNT(*) FROM v_track_status WHERE on_local=1 AND on_spotify=1"),
            "spotify_only": one("SELECT COUNT(*) FROM v_track_status WHERE on_local=0 AND on_spotify=1"),
            "local_only": one("SELECT COUNT(*) FROM v_track_status WHERE on_local=1 AND on_spotify=0"),
        }

    enrich_cols = ["genres", "country", "mood", "energy", "valence",
                   "danceability", "tempo", "acousticness", "instrumentalness", "camelot"]
    enrich_total = counts.get("enrichment", 0)
    enrichment = [
        (c, one(f"SELECT COUNT(*) FROM enrichment WHERE {c} IS NOT NULL AND {c} <> ''"))
        for c in enrich_cols
    ] if enrich_total else []

    playlists = [tuple(r) for r in con.execute(
        "SELECT p.name, COUNT(*) n FROM playlist_tracks pt "
        "JOIN playlists p ON p.id = pt.playlist_id GROUP BY 1 ORDER BY n DESC LIMIT 10")] \
        if counts.get("playlist_tracks") else []

    files_matched = one("SELECT COUNT(*) FROM files WHERE track_id IS NOT NULL") \
        if "files" in counts else 0

    # Vue mediacenter : chaque playlist avec, par plateforme, ce qu'on en sait.
    # Spotify = source ; local = titres de la playlist ayant au moins un fichier.
    playlists_full = [
        {
            "id": r[0], "name": r[1], "source": r[2], "theme": r[3] or "",
            "genres": r[4] or "", "artists": r[5] or "",
            "energy": r[6], "valence": r[7],
            "spotify": r[8], "local": r[9],
        }
        for r in con.execute("""
            SELECT p.id, p.name, p.source, p.thematique, p.genres_dominants,
                   p.artistes_phares, p.energy, p.valence,
                   COUNT(pt.track_id) AS n_spotify,
                   SUM(CASE WHEN EXISTS (SELECT 1 FROM files f WHERE f.track_id = pt.track_id)
                            THEN 1 ELSE 0 END) AS n_local
            FROM playlists p
            LEFT JOIN playlist_tracks pt ON pt.playlist_id = p.id
            GROUP BY p.id ORDER BY p.name COLLATE NOCASE
        """)
    ] if counts.get("playlists") else []

    con.close()
    meta = stat_file(rel) or {}
    return {
        **meta,
        "tables": counts,
        "cross": cross,
        "enrichment": enrichment,
        "enrichment_total": enrich_total,
        "top_playlists": playlists,
        "playlists": playlists_full,
        "files_matched": files_matched,
    }


# --------------------------------------------------------------------------- xlsx
def spotify_dupes(rel: str) -> dict | None:
    """Onglet « doublons » de l'export Spotify, lu sans openpyxl (zip + regex).

    intra = même titre plusieurs fois dans une playlist ; inter = présent dans
    plusieurs playlists. C'est la matière première du nettoyage côté Spotify.
    """
    import re
    import zipfile

    p = ROOT / rel
    if not p.exists():
        return None
    try:
        z = zipfile.ZipFile(p)
        wb = z.read("xl/workbook.xml").decode("utf-8")
        rels = dict(re.findall(r'Id="([^"]+)"[^>]*Target="([^"]+)"',
                               z.read("xl/_rels/workbook.xml.rels").decode("utf-8")))
        sheet = next((rid for name, rid in re.findall(r'<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', wb)
                      if name == "doublons"), None)
        if not sheet:
            return None
        strs = [re.sub(r"<[^>]+>", "", m) for m in
                re.findall(r"<si>(.*?)</si>", z.read("xl/sharedStrings.xml").decode("utf-8"), re.S)]
        xml = z.read("xl/" + rels[sheet].lstrip("/").removeprefix("xl/")).decode("utf-8")
    except (KeyError, zipfile.BadZipFile, UnicodeDecodeError):
        return None

    counts: collections.Counter = collections.Counter()
    for row in re.findall(r"<row [^>]*>(.*?)</row>", xml, re.S)[1:]:
        first = re.search(r"<c([^>]*)>(.*?)</c>", row, re.S)
        if not first:
            continue
        attrs, inner = first.groups()
        v = re.search(r"<v>([^<]*)</v>", inner)
        val = v.group(1) if v else ""
        if re.search(r'\bt="s"', attrs) and val:
            val = strs[int(val)]
        counts[val] += 1
    return {"intra": counts.get("intra", 0), "inter": counts.get("inter", 0)}


# --------------------------------------------------------------------------- main
def build() -> dict:
    vault = [
        ("extract_spotify.xlsx", "Export du Sheet « extract spotify » : 14 017 titres, 110 playlists", "source"),
        ("library_scan.csv", "Scan local courant — périmètre `m:/music` uniquement", "source"),
        ("library_scan.2026-08-19.pre-quarantaine.csv", "Archive : scan initial, périmètre `m:/` complet", "archive"),
        ("local_duplicates_report.2026-08-20-0028.csv", "Rapport de dédup du run qui a servi à la quarantaine", "archive"),
        ("quarantine_paths.2026-08-20-0028.txt", "Liste des fichiers déplacés vers `_a_trier`", "archive"),
        ("local_duplicates_report.csv", "Rapport de dédup du rescan (vide = plus de doublons)", "dérivé"),
        ("quarantine_paths.txt", "Liste de quarantaine du rescan (vide)", "dérivé"),
        ("quarantine_duplicates.ps1", "Script PowerShell de déplacement généré par la dédup", "dérivé"),
        ("retag_plan.csv", "Plan de retag issu du fingerprint (run interrompu)", "dérivé"),
        (".fingerprint_cache.json", "Cache AcoustID du fingerprint", "cache"),
        ("fingerprint.log", "Journal du run de fingerprint", "cache"),
    ]
    files = []
    for name, desc, kind in vault:
        rel = f"data/music/{name}"
        meta = stat_file(rel)
        entry = {"name": name, "desc": desc, "kind": kind, "present": meta is not None}
        if meta:
            entry.update(meta)
            if name.endswith((".csv", ".txt")):
                n = count_lines(rel)
                if n is not None:
                    # Un CSV compte son en-tête : on l'enlève pour donner un nombre d'entrées.
                    entry["lines"] = max(n - 1, 0) if name.endswith(".csv") else n
        files.append(entry)

    exports = ROOT / "exports"
    exports_files = [f.name for f in exports.iterdir() if f.name != ".gitkeep"] if exports.exists() else []

    fp_cache = ROOT / "data/music/.fingerprint_cache.json"
    fingerprint = None
    if fp_cache.exists():
        try:
            fingerprint = {"done": len(json.loads(fp_cache.read_text(encoding="utf-8"))), "total": 44529}
        except (json.JSONDecodeError, UnicodeDecodeError):
            fingerprint = None

    return {
        "generated": dt.datetime.now().strftime("%Y-%m-%d %H:%M"),
        "music": {
            "vault": files,
            "db": db_stats("plugin/db/music.db"),
            "scan": scan_stats("data/music/library_scan.csv"),
            "scan_pre": scan_stats("data/music/library_scan.2026-08-19.pre-quarantaine.csv"),
            "fingerprint": fingerprint,
            "exports": exports_files,
            "spotify_dupes": spotify_dupes("data/music/extract_spotify.xlsx"),
            # Dédup locale : la liste datée = les fichiers effectivement déplacés.
            "local_quarantined": count_lines("data/music/quarantine_paths.2026-08-20-0028.txt"),
            "local_dupes_pending": max((count_lines("data/music/quarantine_paths.txt") or 0), 0),
        },
    }


def main() -> int:
    data = build()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        "// Généré par apps/web/build_manifest.py — ne pas éditer à la main.\n"
        "window.PORTAL6 = " + json.dumps(data, ensure_ascii=False, indent=2) + ";\n",
        encoding="utf-8",
    )
    # Console Windows en cp1252 : pas d'accent ni de flèche dans le print.
    print(f"OK {OUT.relative_to(ROOT)} ({OUT.stat().st_size / 1024:.1f} Ko)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
