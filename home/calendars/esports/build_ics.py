#!/usr/bin/env python3
"""Génère un fichier .ics par ligue eSport, à partir de l'API lolesports.

Source LoL : API non officielle mais alimentée par Riot (celle du site lolesports.com).
Clé publique connue, stable depuis des années — si elle casse un jour, fallback PandaScore.

Filtrage par ligue (clé "teams" de leagues.json, codes d'équipe lolesports) :
- saison régulière (blockName « Week N ») : seuls les matchs d'une équipe suivie ;
- phases finales (Playoffs, Finals…) : tous les matchs, y compris les placeholders
  TBD vs TBD — l'UID étant l'id du match Riot, l'event est mis à jour (pas dupliqué)
  quand les équipes se définissent.
Sans clé "teams" : tous les matchs de la ligue.

Usage :
    python build_ics.py [--out DIR]        # génère <out>/lol/<slug>.ics pour chaque ligue de leagues.json
"""

import argparse
import json
import re
import sys
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

REGULAR_SEASON_RE = re.compile(r"^week\s*\d+", re.IGNORECASE)

API = "https://esports-api.lolesports.com/persisted/gw/getSchedule"
API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z"  # clé publique du site lolesports.com
KEEP_PAST_DAYS = 2      # garde les matchs terminés récents (résultat visible dans l'agenda)
EVENT_HOURS = 2         # durée par défaut d'un event (l'API ne donne pas de durée)


def fetch_schedule(league_id: str) -> list[dict]:
    events: list[dict] = []
    page_token = None
    for _ in range(10):  # garde-fou ; la page par défaut est centrée sur maintenant, on suit "newer"
        url = f"{API}?hl=en-US&leagueId={league_id}"
        if page_token:
            url += f"&pageToken={page_token}"
        req = urllib.request.Request(url, headers={"x-api-key": API_KEY})
        with urllib.request.urlopen(req, timeout=30) as resp:
            schedule = json.load(resp)["data"]["schedule"]
        events.extend(schedule["events"])
        page_token = (schedule.get("pages") or {}).get("newer")
        if not page_token:
            break
    return events


def ics_escape(text: str) -> str:
    return text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;")


def wanted(ev: dict, team_codes: set[str]) -> bool:
    if not team_codes:
        return True
    if not REGULAR_SEASON_RE.match(ev.get("blockName") or ""):
        return True  # phase finale : bracket complet, placeholders TBD inclus
    codes = {t.get("code") for t in (ev.get("match") or {}).get("teams", [])}
    return bool(codes & team_codes)


def event_to_vevent(ev: dict, label: str, now: datetime) -> str | None:
    start = datetime.strptime(ev["startTime"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    if ev.get("state") == "completed" and start < now - timedelta(days=KEEP_PAST_DAYS):
        return None
    match = ev.get("match") or {}
    teams = [t.get("name") or "TBD" for t in match.get("teams", [])]
    versus = " vs ".join(teams) if teams else "TBD vs TBD"
    block = ev.get("blockName") or ""
    strategy = match.get("strategy") or {}
    fmt = f"Bo{strategy['count']}" if strategy.get("count") else ""
    summary = f"{versus} — {label}" + (f" {block}" if block else "")
    desc = " · ".join(x for x in [label, block, fmt] if x)
    uid = f"{match.get('id') or ev['startTime']}@portal6-esports-ics"
    end = start + timedelta(hours=EVENT_HOURS)
    # DTSTAMP = DTSTART (et pas now) : sortie stable si rien n'a changé,
    # le cron GitHub Actions ne commite alors que les vraies mises à jour
    stamp = start.strftime("%Y%m%dT%H%M%SZ")
    return "\n".join([
        "BEGIN:VEVENT",
        f"UID:{uid}",
        f"DTSTAMP:{stamp}",
        f"DTSTART:{start.strftime('%Y%m%dT%H%M%SZ')}",
        f"DTEND:{end.strftime('%Y%m%dT%H%M%SZ')}",
        f"SUMMARY:{ics_escape(summary)}",
        f"DESCRIPTION:{ics_escape(desc)}",
        "END:VEVENT",
    ])


def build_calendar(slug: str, cfg: dict, now: datetime) -> str:
    team_codes = set(cfg.get("teams") or [])
    events = fetch_schedule(cfg["leagueId"])
    vevents = [v for ev in events
               if wanted(ev, team_codes) and (v := event_to_vevent(ev, cfg["label"], now))]
    body = "\n".join([
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//portal6//esports-ics//FR",
        f"X-WR-CALNAME:{cfg['label']} (eSport)",
        "X-WR-TIMEZONE:UTC",
        *vevents,
        "END:VCALENDAR",
    ])
    print(f"  {slug}: {len(vevents)} events")
    return body + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default=str(Path(__file__).parent / "ics"))
    args = parser.parse_args()

    leagues = json.loads((Path(__file__).parent / "leagues.json").read_text())
    now = datetime.now(timezone.utc)
    failures = []
    for game, entries in leagues.items():
        out_dir = Path(args.out) / game
        out_dir.mkdir(parents=True, exist_ok=True)
        for slug, cfg in entries.items():
            try:
                (out_dir / f"{slug}.ics").write_text(build_calendar(slug, cfg, now))
            except Exception as exc:  # une ligue en échec ne doit pas bloquer les autres
                failures.append(f"{game}/{slug}: {exc}")
    if failures:
        print("Échecs :", *failures, sep="\n  ", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
