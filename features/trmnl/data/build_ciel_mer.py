#!/usr/bin/env python3
"""Génère le payload JSON du private plugin TRMNL « Ciel & Mer ».

Sorties : `features/trmnl/payloads/ciel-mer.json`, commité et servi par
raw.githubusercontent.com. TRMNL le récupère en **polling** (stratégie sans serveur,
même principe que les .ics eSport). Le fichier ne contient que des données publiques.

Sources (toutes gratuites et sans clé) :
- **Soleil / Lune** : calculées localement par `astro.py` (aucun appel réseau).
- **Météo** : Open-Meteo Forecast API.
- **Mer & marées** : Open-Meteo Marine API. Les heures de pleine/basse mer sont
  dérivées des extremums de `sea_level_height_msl`, échantillonné à l'heure et affiné
  par interpolation parabolique — précision de l'ordre de ±15 min, sans coefficient.
  Pour des heures et coefficients officiels, il faudra passer au SHOM ou à WorldTides
  (cf. `.docs/trmnl-dashboard.md`).

Usage :
    python features/trmnl/data/build_ciel_mer.py [--out FICHIER] [--print]
"""

import argparse
import json
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

import astro

FORECAST_API = "https://api.open-meteo.com/v1/forecast"
MARINE_API = "https://marine-api.open-meteo.com/v1/marine"
TIDE_HORIZON_HOURS = 30   # de quoi toujours avoir 2 pleines mers et 2 basses mers
# Le coefficient de marée est défini À BREST et vaut pour toute la côte française :
# coef = 100 x marnage / (2 x U), avec U = 3,05 m l'unité de hauteur de Brest.
# Le calculer sur le marnage local donnerait un nombre faux — d'où ce point fixe.
BREST = (48.38, -4.50)
BREST_UNIT_M = 3.05
# Seuils français usuels du régime de marée. On n'affiche un badge que pour les deux
# extrêmes : une marée moyenne n'a rien à signaler, et un badge permanent ne se
# remarque plus.
TIDE_REGIMES = [(100, "GRANDE VIVE-EAU"), (90, "VIVE-EAU"), (46, ""), (0, "MORTE-EAU")]
HTTP_TIMEOUT = 30

ROOT = Path(__file__).resolve().parent
DEFAULT_OUT = ROOT.parent / "payloads" / "ciel-mer.json"

# Codes WMO d'Open-Meteo -> libellé court (l'écran est étroit, on reste télégraphique).
WMO = {
    0: "Ciel clair", 1: "Peu nuageux", 2: "Nuages épars", 3: "Couvert",
    45: "Brouillard", 48: "Brouillard givrant",
    51: "Bruine faible", 53: "Bruine", 55: "Bruine forte",
    56: "Bruine verglaçante", 57: "Bruine verglaçante",
    61: "Pluie faible", 63: "Pluie", 65: "Pluie forte",
    66: "Pluie verglaçante", 67: "Pluie verglaçante",
    71: "Neige faible", 73: "Neige", 75: "Neige forte", 77: "Grésil",
    80: "Averses", 81: "Averses", 82: "Averses fortes",
    85: "Averses de neige", 86: "Averses de neige",
    95: "Orage", 96: "Orage grêle", 99: "Orage grêle",
}
CARDINALS = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
             "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"]
# strftime dépend de la locale du système : sur un runner GitHub elle est en C, donc
# en anglais. On formate les dates à la main plutôt que de parier sur `locale`.
JOURS = ["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"]
MOIS = ["janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"]


def fetch_json(url: str, params: dict) -> dict:
    full = f"{url}?{urllib.parse.urlencode(params)}"
    with urllib.request.urlopen(full, timeout=HTTP_TIMEOUT) as resp:
        return json.loads(resp.read())


def cardinal(degrees: float | None) -> str:
    if degrees is None:
        return "—"
    return CARDINALS[round(degrees / 22.5) % 16]


def hhmm(moment: datetime | None) -> str:
    return moment.strftime("%Hh%M") if moment else "—"


def duration(start: datetime | None, end: datetime | None) -> str:
    if not start or not end:
        return "—"
    minutes = round((end - start).total_seconds() / 60)
    return f"{minutes // 60}h{minutes % 60:02d}"


# ------------------------------------------------------------------------- marées

def parse_hours(block: dict, tz: ZoneInfo) -> list[datetime]:
    """Convertit le tableau `time` d'Open-Meteo (heures locales naïves) en aware."""
    return [datetime.fromisoformat(t).replace(tzinfo=tz) for t in block["time"]]


def tide_extremes(times: list[datetime], levels: list[float | None],
                  now: datetime, coefficients: list[tuple[datetime, int]]) -> list[dict]:
    """Pleines et basses mers à venir, extraites des extremums du niveau de la mer.

    L'échantillonnage horaire d'Open-Meteo ne donne l'extremum qu'à l'heure près : on
    ajuste par la parabole passant par les trois points encadrants, ce qui ramène
    l'erreur à une quinzaine de minutes en régime de marée classique.
    """
    out: list[dict] = []
    for i in range(1, len(levels) - 1):
        prev, cur, nxt = levels[i - 1], levels[i], levels[i + 1]
        if prev is None or cur is None or nxt is None:
            continue
        # `>=` d'un côté seulement : au pas horaire, un extremum s'étale souvent sur
        # deux points de même niveau. La comparaison stricte des deux côtés les rate
        # tous ; celle-ci retient le dernier point du plateau, une fois exactement.
        is_high, is_low = cur >= prev and cur > nxt, cur <= prev and cur < nxt
        if not (is_high or is_low):
            continue
        curvature = prev - 2 * cur + nxt
        offset = 0.5 * (prev - nxt) / curvature if curvature else 0.0
        moment = times[i] + timedelta(hours=max(-0.5, min(0.5, offset)))
        if moment < now or moment > now + timedelta(hours=TIDE_HORIZON_HOURS):
            continue
        coef = min(coefficients, key=lambda c: abs(c[0] - moment))[1] if coefficients else None
        out.append({
            "kind": "PM" if is_high else "BM",
            "coef": coef,
            "label": "Pleine mer" if is_high else "Basse mer",
            "time": moment.strftime("%Hh%M"),
            "day": "demain" if moment.date() > now.date() else "",
            # Ordonnée du sommet de la parabole : c - b²/(4a).
            "height_m": round(cur - (nxt - prev) ** 2 / (8 * curvature), 2) if curvature
                        else round(cur, 2),
        })
    return out[:4]


def local_extremes(times: list[datetime], levels: list[float | None]) -> list[tuple]:
    """Extremums bruts d'une série de niveau : (instant, 'PM'|'BM', hauteur)."""
    out = []
    for i in range(1, len(levels) - 1):
        prev, cur, nxt = levels[i - 1], levels[i], levels[i + 1]
        if prev is None or cur is None or nxt is None:
            continue
        if cur >= prev and cur > nxt:
            out.append((times[i], "PM", cur))
        elif cur <= prev and cur < nxt:
            out.append((times[i], "BM", cur))
    return out


def tide_coefficients(tz: ZoneInfo) -> list[tuple[datetime, int]]:
    """Coefficients de marée à venir, calculés sur la série de Brest.

    Un coefficient par transition PM<->BM, daté du milieu de la transition. Il est
    **dérivé**, pas officiel : l'échantillonnage horaire d'Open-Meteo aplatit les pics,
    l'écart au SHOM est de l'ordre de 3 à 5 points.
    """
    data = fetch_json(MARINE_API, {
        "latitude": BREST[0], "longitude": BREST[1], "timezone": str(tz),
        "hourly": "sea_level_height_msl", "forecast_days": 3,
    })
    times = parse_hours(data["hourly"], tz)
    extremes = local_extremes(times, data["hourly"].get("sea_level_height_msl") or [])
    out = []
    for (t1, k1, v1), (t2, k2, v2) in zip(extremes, extremes[1:]):
        if k1 == k2:
            continue
        coef = round(100 * abs(v1 - v2) / (2 * BREST_UNIT_M))
        out.append((t1 + (t2 - t1) / 2, max(20, min(120, coef))))
    return out


def tide_regime(tides: list[dict]) -> str:
    """Régime en cours, d'après le coefficient de la **prochaine** marée.

    Pas le pic de la fenêtre : celle-ci couvre 30 h, pendant lesquelles le coefficient
    peut monter de 41 à 53. Un dashboard décrit le présent — annoncer une vive-eau
    encore à deux jours serait un contresens.
    """
    coefs = [t["coef"] for t in tides if t.get("coef")]
    if not coefs:
        return ""
    return next(label for threshold, label in TIDE_REGIMES if coefs[0] >= threshold)


# ------------------------------------------------------------------------ pression

# Seuils barométriques usuels, en hPa sur 3 h. C'est la variation qui est lisible,
# pas la valeur absolue : 1008 hPa en hausse rapide n'annonce pas la même chose que
# 1008 hPa en chute.
PRESSURE_STEPS = [
    (1.6, "hausse rapide", "up"), (0.6, "hausse", "up"),
    (-0.6, "stable", "flat"), (-1.6, "baisse", "down"),
]


def pressure_trend(times: list[datetime], values: list[float | None],
                   now: datetime) -> tuple[float, str, str]:
    """Variation de pression sur 3 h : (delta hPa, libellé, sens de la flèche)."""
    usable = [i for i, v in enumerate(values) if v is not None]
    if len(usable) < 4:
        return 0.0, "—", "flat"
    idx = min(usable, key=lambda i: abs(times[i] - now))
    past = max(idx - 3, usable[0])
    delta = values[idx] - values[past]
    for threshold, label, arrow in PRESSURE_STEPS:
        if delta >= threshold:
            return delta, label, arrow
    return delta, "baisse rapide", "down"


# -------------------------------------------------------------------------- payload

def build(cfg: dict) -> dict:
    spot = cfg["spot"]
    tz = ZoneInfo(spot["timezone"])
    now = datetime.now(tz)
    lat, lon = spot["latitude"], spot["longitude"]
    sea_lat = cfg["marine"].get("latitude") or lat
    sea_lon = cfg["marine"].get("longitude") or lon

    forecast = fetch_json(FORECAST_API, {
        "latitude": lat, "longitude": lon, "timezone": spot["timezone"],
        "current": "temperature_2m,apparent_temperature,weather_code,pressure_msl,"
                   "wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        "hourly": "pressure_msl",
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        "forecast_days": 1, "past_days": 1, "wind_speed_unit": "kmh",
    })
    marine = fetch_json(MARINE_API, {
        "latitude": sea_lat, "longitude": sea_lon, "timezone": spot["timezone"],
        "hourly": "wave_height,wave_period,wave_direction,"
                  "sea_surface_temperature,sea_level_height_msl",
        "forecast_days": 3,
    })

    sun = astro.sun_events(now, lat, lon)
    jd = astro.julian_day(now)
    fraction, waxing = astro.moon_illumination(jd)
    phases = astro.find_phases(jd, days=45)
    next_full = next((p for p, kind in phases if kind == "full"), None)
    next_new = next((p for p, kind in phases if kind == "new"), None)
    specials = astro.special_moons(jd, tz, days=cfg.get("special_moons_days", 120))

    hours = parse_hours(marine["hourly"], tz)
    idx = min(range(len(hours)), key=lambda i: abs(hours[i] - now))
    sea = marine["hourly"]

    def at_now(key: str):
        values = sea.get(key) or []
        return values[idx] if idx < len(values) else None

    tides = tide_extremes(hours, sea.get("sea_level_height_msl") or [], now,
                          tide_coefficients(tz))
    current = forecast["current"]
    # `past_days: 1` (nécessaire à la tendance de pression) ajoute hier en tête des
    # tableaux journaliers : il faut retrouver l'index d'aujourd'hui, pas prendre [0].
    daily = forecast["daily"]
    today = now.strftime("%Y-%m-%d")
    d = daily["time"].index(today) if today in daily["time"] else -1
    air_times = parse_hours(forecast["hourly"], tz)
    delta, trend_label, trend_arrow = pressure_trend(
        air_times, forecast["hourly"].get("pressure_msl") or [], now)

    return {
        "spot": spot["label"],
        "generated_at": now.strftime("%d/%m %Hh%M"),
        "date": f"{JOURS[now.weekday()]} {now.day} {MOIS[now.month - 1]}".capitalize(),
        "sun": {
            "sunrise": hhmm(sun["sunrise"]),
            "sunset": hhmm(sun["sunset"]),
            "daylight": duration(sun["sunrise"], sun["sunset"]),
            "golden_am": f"{hhmm(sun['sunrise'])} – {hhmm(sun['golden_am_end'])}",
            "golden_pm": f"{hhmm(sun['golden_pm_start'])} – {hhmm(sun['golden_pm_end'])}",
            "blue_pm": f"{hhmm(sun['golden_pm_end'])} – {hhmm(sun['dusk_blue_end'])}",
            "golden_pm_start": hhmm(sun["golden_pm_start"]),
            "noon": hhmm(sun["noon"]),
        },
        "moon": {
            "phase": astro.phase_name(fraction, waxing),
            "illumination": round(fraction * 100),
            "age": round(astro.moon_age_days(jd), 1),
            "distance_km": round(astro.moon_ecliptic(jd)[2]),
            "svg_path": astro.moon_svg_path(fraction, waxing, 46, 46, 40),
            "next_full": astro.from_julian(next_full).astimezone(tz).strftime("%d/%m %Hh%M")
                         if next_full else "—",
            "next_new": astro.from_julian(next_new).astimezone(tz).strftime("%d/%m %Hh%M")
                        if next_new else "—",
            "specials": [{
                "when": ev["when"].strftime("%d/%m"),
                "tags": " · ".join(ev["tags"]),
                "distance_km": ev["distance_km"],
                "estimated": ev["estimated"],
            } for ev in specials[:3]],
        },
        "tides": tides,
        "tide_regime": tide_regime(tides),
        "sea": {
            "wave_height": at_now("wave_height"),
            "wave_period": at_now("wave_period"),
            "wave_direction": cardinal(at_now("wave_direction")),
            "water_temp": at_now("sea_surface_temperature"),
        },
        "weather": {
            "label": WMO.get(current.get("weather_code"), "—"),
            "temp": round(current.get("temperature_2m", 0)),
            "feels_like": round(current.get("apparent_temperature", 0)),
            "temp_max": round(daily["temperature_2m_max"][d]),
            "temp_min": round(daily["temperature_2m_min"][d]),
            "rain_probability": daily["precipitation_probability_max"][d],
            "wind": round(current.get("wind_speed_10m", 0)),
            "gusts": round(current.get("wind_gusts_10m", 0)),
            "wind_direction": cardinal(current.get("wind_direction_10m")),
            "pressure": round(current.get("pressure_msl", 0)),
            "pressure_delta": f"{delta:+.1f}".replace(".", ","),
            "pressure_trend": trend_label,
            "pressure_arrow": trend_arrow,
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", default=str(ROOT / "config.json"))
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    parser.add_argument("--print", action="store_true", help="affiche le payload sans l'écrire")
    args = parser.parse_args()

    cfg = json.loads(Path(args.config).read_text(encoding="utf-8"))
    if not cfg["spot"]["latitude"] and not cfg["spot"]["longitude"]:
        sys.exit(f"Renseigner le spot (latitude/longitude) dans {args.config}.")

    payload = build(cfg)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if args.print:
        print(text)
        return
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8")
    print(f"{out} — {len(text)} octets")


if __name__ == "__main__":
    main()
