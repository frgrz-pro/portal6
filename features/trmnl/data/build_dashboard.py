#!/usr/bin/env python3
"""Génère le payload unique du dashboard TRMNL (Ciel & Mer + agenda de la semaine).

Un plugin TRMNL n'a **qu'une seule stratégie de données**. Comme le dashboard mélange
du public (soleil, lune, marées, météo) et du privé (agenda perso), il a fallu trancher :

- Le **webhook** est exclu : mesuré à ~3,2 Ko une fois fusionné, contre 2 Ko de limite.
- Le **polling sur le repo public** est exclu : portal6 est public, les événements
  personnels n'ont rien à y faire.

→ **Polling sur un gist secret.** Le payload est publié dans un gist non listé ; seule
son URL brute, connue de TRMNL et de personne d'autre, permet d'y accéder. C'est le
même modèle de confiance que l'« adresse secrète au format iCal » de Google Calendar,
déjà utilisée en entrée. Pour un vrai contrôle d'accès, l'alternative est un dépôt privé
lu via `polling_headers` (cf. `.docs/trmnl-dashboard.md`).

Configuration (variables d'environnement) :
    AGENDA_ICS_URLS   un agenda par ligne, `CODE|URL` (adresse secrète iCal Google)
    GIST_ID           identifiant du gist secret à mettre à jour
    GITHUB_TOKEN      jeton avec la portée `gist`

Usage :
    python features/trmnl/data/build_dashboard.py --print      # rien n'est publié
    python features/trmnl/data/build_dashboard.py --out F.json # écrit un fichier
    python features/trmnl/data/build_dashboard.py              # publie dans le gist
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

import build_ciel_mer

ROOT = Path(__file__).resolve().parent
GIST_API = "https://api.github.com/gists"
GIST_FILENAME = "dashboard.json"
HTTP_TIMEOUT = 30


def build_agenda_block(cfg: dict, days: int) -> dict:
    """Agenda de la semaine, au format attendu par la colonne de gauche du template.

    Importé paresseusement : `build_agenda` dépend d'icalendar, dont on n'a pas besoin
    quand on ne régénère que la partie publique.
    """
    import build_agenda

    tz = ZoneInfo(cfg["spot"]["timezone"])
    now = datetime.now(tz)
    start = datetime.combine(now.date(), time.min, tzinfo=tz)

    events = []
    for code, url in build_agenda.read_sources():
        for component in build_agenda.fetch_events(url, start, start + timedelta(days=days)):
            normalized = build_agenda.normalize(component, code, tz)
            if normalized:
                events.append(normalized)

    grouped = build_agenda.group_by_day(events, now.date(), days)
    # Trois événements par jour tiennent dans la colonne ; au-delà on compte les
    # absents plutôt que de laisser croire que la journée est vide.
    shown = [{
        "label": day["label"],
        "today": day["today"],
        "events": [{"time": e["time"], "title": e["title"][:40], "cal": e["cal"]}
                   for e in day["events"][:3]],
    } for day in grouped]
    total = sum(len(day["events"]) for day in grouped)
    return {
        "days": shown,
        "count": total,
        "hidden": total - sum(len(day["events"]) for day in shown),
    }


def publish_gist(gist_id: str, token: str, payload: dict) -> None:
    body = json.dumps({"files": {GIST_FILENAME: {
        "content": json.dumps(payload, ensure_ascii=False, indent=2)}}}).encode("utf-8")
    request = urllib.request.Request(
        f"{GIST_API}/{gist_id}", data=body, method="PATCH",
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT) as resp:
            print(f"Gist mis à jour (HTTP {resp.status}, {len(body)} octets)")
    except urllib.error.HTTPError as exc:
        sys.exit(f"Publication refusée ({exc.code}) : {exc.read().decode('utf-8', 'replace')}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", default=str(ROOT / "config.json"))
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--out", help="écrit le payload dans ce fichier au lieu de le publier")
    parser.add_argument("--print", action="store_true", help="affiche le payload, ne publie rien")
    parser.add_argument("--no-agenda", action="store_true",
                        help="partie publique seulement (aucune dépendance icalendar)")
    args = parser.parse_args()

    cfg = json.loads(Path(args.config).read_text(encoding="utf-8"))
    if not cfg["spot"]["latitude"] and not cfg["spot"]["longitude"]:
        sys.exit(f"Renseigner le spot (latitude/longitude) dans {args.config}.")

    payload = build_ciel_mer.build(cfg)
    payload["agenda"] = ({"days": [], "count": 0, "hidden": 0} if args.no_agenda
                         else build_agenda_block(cfg, args.days))

    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.print:
        print(text)
        return
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8")
        print(f"{args.out} — {len(text)} octets")
        return

    gist_id, token = os.environ.get("GIST_ID"), os.environ.get("GITHUB_TOKEN")
    if not gist_id or not token:
        sys.exit("GIST_ID et GITHUB_TOKEN sont requis pour publier (ou utiliser --out / --print).")
    publish_gist(gist_id, token, payload)


if __name__ == "__main__":
    main()
