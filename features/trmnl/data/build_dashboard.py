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
    AGENDA_ICS_URLS   agendas PRIVÉS, un par ligne, `CODE|URL` (adresse secrète iCal).
                      Les agendas publics, eux, sont dans config.json.
    GIST_ID           identifiant du gist secret à mettre à jour
    GIST_TOKEN        jeton avec la seule portée `gist`

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
# Budget de la colonne agenda, en pixels. Liquid ne sait pas mesurer du texte : on
# estime ici le nombre de lignes qu'occupera chaque événement pour remplir la colonne
# au plus juste, plutôt que d'imposer un quota arbitraire par jour. Ce qui ne rentre
# pas est COMPTÉ, jamais silencieusement perdu.
AGENDA_BUDGET_PX = 405
AGENDA_CHARS_PER_LINE = 27   # pas 33 : le retour a la ligne se fait sur les mots,
                             # donc le remplissage reel est plus lache que la largeur brute
AGENDA_LINE_PX = 14
AGENDA_DAY_HEADER_PX = 21
AGENDA_DAY_GAP_PX = 18       # gap 9 + filet 2 + padding 7
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
    for code, url in build_agenda.read_sources(cfg):
        for component in build_agenda.fetch_events(url, start, start + timedelta(days=days)):
            normalized = build_agenda.normalize(component, code, tz)
            if normalized:
                events.append(normalized)

    grouped = build_agenda.group_by_day(events, now.date(), days)
    shown, used = [], 0
    for day in grouped:
        if not day["events"]:
            continue
        cost = AGENDA_DAY_HEADER_PX + (AGENDA_DAY_GAP_PX if shown else 0)
        if used + cost >= AGENDA_BUDGET_PX:
            break
        kept, used = [], used + cost
        for event in day["events"]:
            title = event["title"][:60]
            width = len(event["time"]) + len(event["cal"]) + len(title) + 3
            lines = max(1, -(-width // AGENDA_CHARS_PER_LINE))
            if used + lines * AGENDA_LINE_PX > AGENDA_BUDGET_PX:
                break
            used += lines * AGENDA_LINE_PX
            kept.append({"time": event["time"], "title": title, "cal": event["cal"]})
        if kept:
            shown.append({"label": day["label"], "today": day["today"], "events": kept})
        else:
            used -= cost  # l'en-tête seul ne sert à rien, on rend la place
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

    gist_id = os.environ.get("GIST_ID")
    # GITHUB_TOKEN est accepté en second : c'est le nom qu'impose GitHub Actions.
    token = os.environ.get("GIST_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not gist_id or not token:
        sys.exit("GIST_ID et GIST_TOKEN sont requis pour publier (ou utiliser --out / --print).")
    publish_gist(gist_id, token, payload)


if __name__ == "__main__":
    main()
