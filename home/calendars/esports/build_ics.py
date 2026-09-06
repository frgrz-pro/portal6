#!/usr/bin/env python3
"""Génère un fichier .ics par ligue eSport.

Deux sources selon la clé "source" de l'entrée dans leagues.json :
- (défaut) API lolesports : non officielle mais alimentée par Riot (celle du site
  lolesports.com). Clé publique connue, stable depuis des années.
- "pandascore" (CS2…) : API PandaScore, token dans la variable d'environnement
  PANDASCORE_TOKEN (ou le .env à la racine du repo). Le calendrier agrège les matchs
  des équipes suivies ("team_ids") et les phases finales des circuits suivis
  ("league_ids", stages dont le nom matche playoff/final/knockout, hors qualifiers) —
  brackets TBD inclus, mis à jour en place quand les équipes se qualifient (UID = id
  du match PandaScore).

Filtrage par ligue (clé "teams" de leagues.json, codes d'équipe lolesports) :
- saison régulière (blockName « Week N ») : seuls les matchs d'une équipe suivie ;
- phases finales (Playoffs, Finals…) : tous les matchs, y compris les placeholders
  TBD vs TBD — l'UID étant l'id du match Riot, l'event est mis à jour (pas dupliqué)
  quand les équipes se définissent.
Sans clé "teams" : tous les matchs de la ligue.

Un calendrier peut agréger plusieurs compétitions (clé "leagueIds", liste — ex.
« international » : First Stand + MSI + Worlds) ; le libellé de chaque event vient
alors de la compétition du match.

Placeholders manuels (clé "manual_events" : [{start, title, hours?}]) : dates de phases
finales annoncées publiquement mais pas encore chargées dans l'API Riot. Ils ne sont
émis QUE tant que l'API n'a aucun match de phase finale à venir pour la ligue — dès que
Riot programme le bracket, les données officielles les remplacent automatiquement.

Usage :
    python build_ics.py [--out DIR]        # génère <out>/lol/<slug>.ics pour chaque ligue de leagues.json
"""

import argparse
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

REGULAR_SEASON_RE = re.compile(r"^week\s*\d+", re.IGNORECASE)
PS_API = "https://api.pandascore.co"
PS_PLAYOFF_STAGE_RE = re.compile(r"playoff|final|knockout", re.IGNORECASE)
PS_EXCLUDE_SERIE_RE = re.compile(r"qualifier", re.IGNORECASE)
PS_BO_HOURS = {1: 2, 3: 3, 5: 5}

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


def manual_vevent(me: dict, slug: str) -> str:
    start = datetime.strptime(me["start"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    end = start + timedelta(hours=me.get("hours", 3))
    stamp = start.strftime("%Y%m%dT%H%M%SZ")
    return "\n".join([
        "BEGIN:VEVENT",
        f"UID:manual-{slug}-{stamp}@portal6-esports-ics",
        f"DTSTAMP:{stamp}",
        f"DTSTART:{stamp}",
        f"DTEND:{end.strftime('%Y%m%dT%H%M%SZ')}",
        f"SUMMARY:{ics_escape(me['title'])}",
        "DESCRIPTION:Placeholder (dates annoncées) — sera remplacé par le planning officiel Riot",
        "END:VEVENT",
    ])


def wrap_calendar(slug: str, label: str, vevents: list[str]) -> str:
    body = "\n".join([
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//portal6//esports-ics//FR",
        f"X-WR-CALNAME:{label} (eSport)",
        "X-WR-TIMEZONE:UTC",
        *vevents,
        "END:VCALENDAR",
    ])
    print(f"  {slug}: {len(vevents)} events")
    return body + "\n"


def pandascore_token() -> str:
    tok = os.environ.get("PANDASCORE_TOKEN", "")
    if not tok:
        env_file = Path(__file__).parents[3] / ".env"
        if env_file.exists():
            for line in env_file.read_text().splitlines():
                if line.startswith("PANDASCORE_TOKEN="):
                    tok = line.split("=", 1)[1].strip()
    if not tok:
        raise RuntimeError("PANDASCORE_TOKEN manquant (env ou .env racine)")
    return tok


def ps_get(path: str, token: str, params: dict) -> list[dict]:
    url = f"{PS_API}{path}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.load(resp)


def ps_match_to_vevent(m: dict) -> str | None:
    begin = m.get("begin_at") or m.get("scheduled_at")
    if not begin:
        return None
    start = datetime.strptime(begin, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    names = [(o.get("opponent") or {}).get("acronym") or (o.get("opponent") or {}).get("name") or "TBD"
             for o in m.get("opponents") or []]
    while len(names) < 2:
        names.append("TBD")
    versus = " vs ".join(names)
    event_name = " ".join(x for x in [(m.get("league") or {}).get("name"),
                                      (m.get("serie") or {}).get("full_name")] if x)
    round_name = (m.get("name") or "").split(":")[0].strip()
    if "vs" in round_name.lower():  # pas de préfixe de round dans le nom du match
        round_name = ""
    bo = m.get("number_of_games")
    summary = f"{versus} — {event_name}" + (f" · {round_name}" if round_name else "")
    desc = " · ".join(x for x in [event_name, round_name, f"Bo{bo}" if bo else "",
                                  (m.get("tournament") or {}).get("name")] if x)
    end = start + timedelta(hours=PS_BO_HOURS.get(bo, 3))
    stamp = start.strftime("%Y%m%dT%H%M%SZ")
    return "\n".join([
        "BEGIN:VEVENT",
        f"UID:ps-{m['id']}@portal6-esports-ics",
        f"DTSTAMP:{stamp}",
        f"DTSTART:{stamp}",
        f"DTEND:{end.strftime('%Y%m%dT%H%M%SZ')}",
        f"SUMMARY:{ics_escape(summary)}",
        f"DESCRIPTION:{ics_escape(desc)}",
        "END:VEVENT",
    ])


def ps_manual_vevent(me: dict, slug: str) -> str:
    start = me["startDate"].replace("-", "")
    end_excl = (datetime.strptime(me["endDate"], "%Y-%m-%d") + timedelta(days=1)).strftime("%Y%m%d")
    return "\n".join([
        "BEGIN:VEVENT",
        f"UID:manual-{slug}-{start}@portal6-esports-ics",
        f"DTSTAMP:{start}T000000Z",
        f"DTSTART;VALUE=DATE:{start}",
        f"DTEND;VALUE=DATE:{end_excl}",
        f"SUMMARY:{ics_escape(me['title'])}",
        "DESCRIPTION:Placeholder (dates annoncées) — le bracket détaillé apparaîtra quand l'organisateur l'aura programmé",
        "END:VEVENT",
    ])


def build_pandascore_calendar(slug: str, cfg: dict, now: datetime) -> str:
    token = pandascore_token()
    game = cfg.get("videogame", "csgo")
    matches: dict[int, dict] = {}
    playoff_days: set[str] = set()  # jours (YYYY-MM-DD) couverts par un bracket réel de l'API
    for team_id in cfg.get("team_ids", []):
        for m in ps_get(f"/{game}/matches/upcoming", token,
                        {"filter[opponent_id]": team_id, "sort": "begin_at", "page[size]": 50}):
            matches[m["id"]] = m
    for league_id in cfg.get("league_ids", []):
        for m in ps_get(f"/{game}/matches/upcoming", token,
                        {"filter[league_id]": league_id, "sort": "begin_at", "page[size]": 50}):
            if not PS_PLAYOFF_STAGE_RE.search((m.get("tournament") or {}).get("name") or ""):
                continue
            if PS_EXCLUDE_SERIE_RE.search((m.get("serie") or {}).get("full_name") or ""):
                continue
            matches[m["id"]] = m
            if m.get("begin_at"):
                playoff_days.add(m["begin_at"][:10])
    ordered = sorted(matches.values(), key=lambda m: m.get("begin_at") or m.get("scheduled_at") or "")
    vevents = [v for m in ordered if (v := ps_match_to_vevent(m))]
    for me in cfg.get("manual_events", []):
        if me["endDate"] < now.strftime("%Y-%m-%d"):
            continue
        covered = any(me["startDate"] <= d <= me["endDate"] for d in playoff_days)
        if not covered:  # l'API n'a pas encore le bracket de cet événement
            vevents.append(ps_manual_vevent(me, slug))
    return wrap_calendar(slug, cfg["label"], vevents)


def build_calendar(slug: str, cfg: dict, now: datetime) -> str:
    team_codes = set(cfg.get("teams") or [])
    league_ids = cfg.get("leagueIds") or [cfg["leagueId"]]
    events = [ev for lid in league_ids for ev in fetch_schedule(lid)]
    # plusieurs compétitions dans un même calendrier : le libellé vient du match lui-même
    vevents = [v for ev in events
               if wanted(ev, team_codes)
               and (v := event_to_vevent(ev, (ev.get("league") or {}).get("name") or cfg["label"], now))]
    api_has_bracket = any(
        ev.get("state") != "completed"
        and not REGULAR_SEASON_RE.match(ev.get("blockName") or "")
        for ev in events
    )
    if not api_has_bracket:
        vevents += [manual_vevent(me, slug) for me in cfg.get("manual_events", [])
                    if datetime.strptime(me["start"], "%Y-%m-%dT%H:%M:%SZ")
                       .replace(tzinfo=timezone.utc) > now - timedelta(days=KEEP_PAST_DAYS)]
    return wrap_calendar(slug, cfg["label"], vevents)


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
            builder = build_pandascore_calendar if cfg.get("source") == "pandascore" else build_calendar
            try:
                (out_dir / f"{slug}.ics").write_text(builder(slug, cfg, now))
            except Exception as exc:  # une ligue en échec ne doit pas bloquer les autres
                failures.append(f"{game}/{slug}: {exc}")
    if failures:
        print("Échecs :", *failures, sep="\n  ", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
