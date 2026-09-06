#!/usr/bin/env python3
"""Pousse l'agenda de la semaine vers le private plugin TRMNL « Agenda semaine ».

**Pourquoi un webhook et pas du polling ?** portal6 est un repo public : les
événements personnels ne peuvent pas y être commités comme le sont les .ics eSport ou
le payload Ciel & Mer. En stratégie *webhook*, le script POSTe le payload directement
à TRMNL — rien n'est stocké dans le repo, et les URLs ICS privées restent des secrets
GitHub Actions.

Contrepartie : **TRMNL plafonne le payload webhook à 2 Ko**. `fit_payload()` rogne donc
progressivement (longueur des titres, nombre de jours, nombre d'événements par jour)
jusqu'à passer sous la limite, et signale ce qui a été coupé.

Configuration :
    config.json          clé `agendas` — les agendas PUBLICS (URL non secrète).
    AGENDA_ICS_URLS      les agendas PRIVÉS, un par ligne, au format `CODE|URL`. L'URL est
                         l'« adresse secrète au format iCal » de Google Calendar
                         (Paramètres de l'agenda → Intégrer l'agenda). CODE est une
                         abréviation de 1-3 lettres affichée à côté de l'événement.
    TRMNL_WEBHOOK_URL    URL affichée sur la page du private plugin côté TRMNL.
    AGENDA_DAYS          nombre de jours à afficher (défaut : 7).

Usage :
    python features/trmnl/data/build_agenda.py [--print] [--days 7]
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

try:
    import icalendar
    import recurring_ical_events
except ImportError:  # pragma: no cover — message plus utile que la stacktrace
    sys.exit("Dépendances manquantes : pip install icalendar recurring-ical-events")

ROOT = Path(__file__).resolve().parent
WEBHOOK_LIMIT_BYTES = 2048
HTTP_TIMEOUT = 30
JOURS = ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"]


def am_pm(moment: datetime) -> str:
    """Heure au format court : 9pm, 1:30pm, 12am. Les minutes nulles sont omises."""
    hour = moment.hour % 12 or 12
    suffix = "am" if moment.hour < 12 else "pm"
    return f"{hour}:{moment.minute:02d}{suffix}" if moment.minute else f"{hour}{suffix}"


def lighten(title: str, tag: str) -> str:
    """Retire du titre le mot que le tag porte déjà, puis nettoie les séparateurs.

    « Liverpool vs Man. City - PL » avec le tag « Liverpool » devient « vs Man. City
    - PL » puis « Man. City - PL ». Sur une colonne de 200 px, cette répétition coûte
    une ligne entière par événement.
    """
    if not tag:
        return title
    out = re.sub(rf"\b{re.escape(tag)}\b", "", title, flags=re.IGNORECASE)
    out = re.sub(r"\s+", " ", out).strip()
    out = re.sub(r"^(?:vs\.?|[-—|:·])\s*", "", out, flags=re.IGNORECASE).strip()
    out = re.sub(r"\s*[-—|:·]\s*$", "", out).strip()
    return out or title


def read_sources(cfg: dict | None = None) -> list[tuple[str, str]]:
    """Agendas à lire : ceux de `config.json`, puis ceux de AGENDA_ICS_URLS.

    Les deux origines ne se valent pas, et c'est délibéré : un agenda **public** a une
    URL qui n'est pas un secret, elle vit donc dans `config.json`, versionnée et
    relisible. Un agenda **privé** est adressé par une URL secrète, qui n'a rien à
    faire dans un repo public — il passe par l'environnement.
    """
    sources = [(a.get("tag") or a["code"], a["url"]) for a in (cfg or {}).get("agendas", [])]
    raw = os.environ.get("AGENDA_ICS_URLS", "").strip()
    if not raw:
        if not sources:
            sys.exit("Aucun agenda : ni `agendas` dans config.json, ni AGENDA_ICS_URLS.")
        return sources
    for line in raw.replace(";", "\n").splitlines():
        line = line.strip()
        if not line:
            continue
        code, _, url = line.partition("|")
        if not url:
            sys.exit(f"Ligne AGENDA_ICS_URLS mal formée (attendu `CODE|URL`) : {line!r}")
        sources.append((code.strip(), url.strip()))
    return sources


def fetch_events(url: str, start: datetime, end: datetime) -> list[dict]:
    """Récupère un .ics et développe les occurrences (RRULE comprises) sur la fenêtre."""
    with urllib.request.urlopen(url, timeout=HTTP_TIMEOUT) as resp:
        calendar = icalendar.Calendar.from_ical(resp.read())
    return recurring_ical_events.of(calendar).between(start, end)


def normalize(component, code: str, tz: ZoneInfo) -> dict | None:
    """Ramène un VEVENT à ce qui s'affiche : jour, heure, titre, code d'agenda."""
    start = component.get("DTSTART")
    if start is None:
        return None
    value = start.dt
    all_day = not isinstance(value, datetime)
    if all_day:
        day, label = value, "journée"
    else:
        local = value.astimezone(tz) if value.tzinfo else value.replace(tzinfo=tz)
        day, label = local.date(), am_pm(local)
    return {
        "day": day,
        # Minutes depuis minuit ; -1 place les événements « journée » en tête.
        "sort": -1 if all_day else local.hour * 60 + local.minute,
        "time": label,
        "title": lighten(str(component.get("SUMMARY", "(sans titre)")), code),
        "cal": code,
    }


def group_by_day(events: list[dict], first: date, days: int) -> list[dict]:
    """Regroupe par journée, en gardant les jours vides (le calendrier reste lisible)."""
    buckets: dict[date, list[dict]] = {}
    for event in events:
        buckets.setdefault(event["day"], []).append(event)
    out = []
    for offset in range(days):
        current = first + timedelta(days=offset)
        items = sorted(buckets.get(current, []), key=lambda e: e["sort"])
        out.append({
            "label": f"{JOURS[current.weekday()]} {current.day:02d}",
            "today": current == first,
            "events": [{"time": e["time"], "title": e["title"], "cal": e["cal"]}
                       for e in items],
        })
    return out


def fit_payload(days: list[dict], generated_at: str) -> tuple[dict, list[str]]:
    """Rogne le payload jusqu'à passer sous la limite de 2 Ko du webhook TRMNL.

    Ordre de sacrifice : d'abord la longueur des titres, puis le nombre d'événements
    par jour, puis les derniers jours de la fenêtre. Retourne le payload et la liste
    des coupes appliquées, pour que l'appelant puisse les afficher.
    """
    notes: list[str] = []
    week = len(days)
    # Paliers du plus doux au plus dur. Les jours passent en dernier : sur un écran
    # « semaine », amputer la fenêtre est une perte pire que des titres raccourcis.
    for max_title, max_per_day, max_days in [
        (46, 99, week), (38, 99, week), (32, 99, week), (28, 8, week),
        (26, 6, week), (24, 5, week), (22, 4, week), (20, 4, week),
        (20, 3, week), (18, 3, 6), (18, 2, 5), (16, 2, 4),
    ]:
        trimmed = []
        dropped = 0
        for day in days[:max_days]:
            kept = day["events"][:max_per_day]
            dropped += len(day["events"]) - len(kept)
            trimmed.append({
                "label": day["label"],
                "today": day["today"],
                "events": [{
                    "time": e["time"],
                    "title": e["title"][:max_title].rstrip(" ,-"),
                    "cal": e["cal"],
                } for e in kept],
            })
        payload = {
            "generated_at": generated_at,
            "days": trimmed,
            "count": sum(len(d["events"]) for d in trimmed),
            "hidden": dropped + sum(len(d["events"]) for d in days[max_days:]),
        }
        body = json.dumps({"merge_variables": payload}, ensure_ascii=False)
        if len(body.encode("utf-8")) <= WEBHOOK_LIMIT_BYTES:
            if max_title < 46:
                notes.append(f"titres tronqués à {max_title} caractères")
            if payload["hidden"]:
                notes.append(f"{payload['hidden']} événement(s) masqué(s)")
            if max_days < len(days):
                notes.append(f"fenêtre réduite à {max_days} jours")
            return payload, notes
    sys.exit("Impossible de tenir dans les 2 Ko du webhook, même au maximum de rognage.")


def post(url: str, payload: dict) -> None:
    body = json.dumps({"merge_variables": payload}, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url, data=body, method="POST",
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT) as resp:
            print(f"TRMNL a répondu {resp.status} ({len(body)} octets envoyés)")
    except urllib.error.HTTPError as exc:
        sys.exit(f"Webhook refusé ({exc.code}) : {exc.read().decode('utf-8', 'replace')}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", default=str(ROOT / "config.json"))
    parser.add_argument("--days", type=int, default=int(os.environ.get("AGENDA_DAYS", 7)))
    parser.add_argument("--print", action="store_true",
                        help="affiche le payload sans l'envoyer (aucun secret exposé)")
    args = parser.parse_args()

    cfg = json.loads(Path(args.config).read_text(encoding="utf-8"))
    tz = ZoneInfo(cfg["spot"]["timezone"])
    now = datetime.now(tz)
    first = now.date()
    window_start = datetime.combine(first, time.min, tzinfo=tz)
    window_end = window_start + timedelta(days=args.days)

    events = []
    for code, url in read_sources(cfg):
        for component in fetch_events(url, window_start, window_end):
            normalized = normalize(component, code, tz)
            if normalized:
                events.append(normalized)

    payload, notes = fit_payload(group_by_day(events, first, args.days),
                                 now.strftime("%d/%m %Hh%M"))
    if notes:
        print("Rognage :", ", ".join(notes))

    if args.print:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return

    webhook = os.environ.get("TRMNL_WEBHOOK_URL")
    if not webhook:
        sys.exit("TRMNL_WEBHOOK_URL est vide — copier l'URL depuis la page du plugin.")
    post(webhook, payload)


if __name__ == "__main__":
    main()
