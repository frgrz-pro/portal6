#!/usr/bin/env python3
"""Pousse une grille editoriale versionnee vers AzuraCast.

Le repo decide, AzuraCast execute (cf. .docs/hardware/design-programmation-editoriale.md §10).
Ce script ne prend AUCUNE decision editoriale : il traduit un fichier
`stations/*.json` en playlists + creneaux AzuraCast, et rien d'autre.

Usage :
    python features/radio/push_schedule.py stations/stage_303.json --dry-run
    python features/radio/push_schedule.py stations/stage_303.json
    python features/radio/push_schedule.py stations/stage_303.json --fill

Modes :
    (defaut)    cree/met a jour les playlists et leurs creneaux horaires.
                Independant des medias : marche meme si le scan n'est pas fini.
    --fill      remplit en plus le contenu des playlists a partir des prefixes
                `sources`. Necessite que les medias soient indexes.
    --dry-run   affiche ce qui serait fait, n'ecrit rien.

Prerequis : AZURACAST_API_KEY dans le .env a la racine du repo.
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_BASE = "http://localhost"


def load_env():
    env = {}
    path = ROOT / ".env"
    if not path.exists():
        sys.exit(f"{path} introuvable")
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip().strip('"').strip("'")
    return env


class Azuracast:
    def __init__(self, base, key):
        self.base = base.rstrip("/") + "/api"
        self.key = key

    def call(self, path, method="GET", body=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(
            self.base + path, data=data, method=method,
            headers={"X-API-Key": self.key, "Accept": "application/json",
                     "Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                raw = r.read().decode("utf-8")
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")[:500]
            raise SystemExit(f"HTTP {e.code} sur {method} {path}\n{detail}")


def hhmm(value):
    """'20:00' -> 2000 (format entier attendu par AzuraCast, verifie empiriquement)."""
    h, m = value.split(":")
    return int(h) * 100 + int(m)


def find_station(az, shortcode):
    for s in az.call("/admin/stations"):
        if s.get("short_name") == shortcode or s.get("shortcode") == shortcode:
            return s
    return None


def create_station(az, meta):
    """Cree la station decrite par le bloc `station` du JSON.

    La storage location media est *reprise d'une station existante* : toutes les
    stations lisent les memes fichiers physiques, aucune copie (cf. le doc §4).
    Le port du frontend est laisse a AzuraCast, qui alloue dans la plage 8000-8496.
    """
    storage = None
    for s in az.call("/admin/stations"):
        storage = az.call(f"/admin/station/{s['id']}").get("media_storage_location")
        if storage:
            break
    payload = {
        "name": meta["name"],
        "short_name": meta["shortcode"],
        "description": meta.get("identity", ""),
        "genre": ", ".join(meta.get("genres", [])),
        "timezone": meta.get("timezone", "Europe/Paris"),
        "frontend_type": "icecast",
        "backend_type": "liquidsoap",
        "is_enabled": True,
        "enable_public_page": True,
        "enable_requests": False,
        "enable_streamers": False,
    }
    if storage:
        payload["media_storage_location"] = storage
    return az.call("/admin/stations", "POST", payload)


def build_schedule_items(grid, key):
    """Agrege tous les creneaux de la grille qui pointent sur cette playlist."""
    items = []
    for slot in grid:
        if slot["playlist"] != key:
            continue
        items.append({
            "start_time": hhmm(slot["start"]),
            "end_time": hhmm(slot["end"]),
            "start_date": None,
            "end_date": None,
            "days": slot["days"],
            "loop_once": False,
        })
    return items


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("station_file", help="Chemin du JSON de grille (relatif a features/radio/ ou absolu)")
    ap.add_argument("--fill", action="store_true", help="Remplit aussi le contenu des playlists")
    ap.add_argument("--create", action="store_true",
                    help="Cree la station dans AzuraCast si elle n'existe pas (bloc `station` du JSON)")
    ap.add_argument("--dry-run", action="store_true", help="N'ecrit rien")
    args = ap.parse_args()

    path = Path(args.station_file)
    if not path.is_absolute() and not path.exists():
        path = Path(__file__).resolve().parent / args.station_file
    if not path.exists():
        sys.exit(f"Fichier de grille introuvable : {args.station_file}")

    spec = json.loads(path.read_text(encoding="utf-8"))
    meta = spec["station"]
    playlists = spec["playlists"]
    grid = spec["schedule"]

    env = load_env()
    key = env.get("AZURACAST_API_KEY")
    if not key:
        sys.exit("AZURACAST_API_KEY absente du .env")
    az = Azuracast(env.get("AZURACAST_BASE_URL", DEFAULT_BASE), key)

    station = find_station(az, meta["shortcode"])
    if not station:
        if not args.create:
            sys.exit(f"Station '{meta['shortcode']}' introuvable dans AzuraCast — "
                     f"la creer d'abord (nom : {meta['name']}), ou relancer avec --create.")
        if args.dry_run:
            print(f"[CREE] station {meta['name']} ({meta['shortcode']}) — dry-run, rien d'ecrit")
            return
        station = create_station(az, meta)
        print(f"[CREE] station {meta['name']} ({meta['shortcode']}) -> id {station['id']}")
    sid = station["id"]
    print(f"Station {sid} — {station.get('name')}  ({len(playlists)} playlists, {len(grid)} creneaux)")

    existing = {p["name"]: p for p in az.call(f"/station/{sid}/playlists")}

    for pkey, pdef in playlists.items():
        title = pdef["title"]
        items = build_schedule_items(grid, pkey)
        payload = {
            "name": title,
            "type": "default",
            "source": "songs",
            "order": pdef.get("order", "shuffle"),
            "weight": pdef.get("weight", 3),
            "is_enabled": True,
            "avoid_duplicates": True,
            "include_in_requests": False,
            "schedule_items": items,
        }
        cur = existing.get(title)
        verb = "MAJ " if cur else "CREE"
        print(f"  [{verb}] {title:30} weight={payload['weight']} order={payload['order']:10} creneaux={len(items)}")
        if args.dry_run:
            continue
        if cur:
            az.call(f"/station/{sid}/playlist/{cur['id']}", "PUT", payload)
            pid = cur["id"]
        else:
            pid = az.call(f"/station/{sid}/playlists", "POST", payload)["id"]
        pdef["_id"] = pid

    if not args.fill:
        print("\nCreneaux poses. Le CONTENU des playlists n'a pas ete touche (--fill pour cela).")
        return

    # --- remplissage ---
    media = az.call(f"/station/{sid}/files?rowCount=100000&current=1")
    rows = media.get("rows", []) if isinstance(media, dict) else media
    print(f"\n{len(rows)} medias indexes.")
    if not rows:
        print("Aucun media : le scan n'est pas termine. Remplissage saute.")
        return

    for pkey, pdef in playlists.items():
        pid = pdef.get("_id")
        if not pid:
            continue
        prefixes = tuple(pdef.get("sources", []))
        paths = [r["path"] for r in rows if r.get("path", "").startswith(prefixes)]
        print(f"  [{pdef['title']}] {len(paths)} fichiers correspondent a {prefixes}")
        if args.dry_run or not paths:
            continue
        az.call(f"/station/{sid}/playlist/{pid}/empty", "DELETE")
        # L'action batch « playlist » est la seule voie API pour assigner des medias :
        # /playlist/{id}/import attend un fichier M3U en multipart, pas une liste d'ids.
        for chunk in (paths[i:i + 200] for i in range(0, len(paths), 200)):
            az.call(f"/station/{sid}/files/batch", "PUT",
                    {"do": "playlist", "files": chunk, "dirs": [], "playlists": [pid]})


if __name__ == "__main__":
    main()
