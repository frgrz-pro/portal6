#!/usr/bin/env python3
"""Éphémérides soleil & lune — aucune dépendance externe.

Deux algorithmes classiques, tronqués à la précision utile pour un écran e-ink :

- **Soleil** : position basse précision NOAA (~0,01°). Les heures (lever, coucher,
  golden hour, blue hour) sont trouvées par balayage minute par minute puis
  interpolation linéaire — robuste, et 1441 évaluations coûtent quelques ms.
- **Lune** : théorie de Meeus (*Astronomical Algorithms*, chap. 47) réduite à ses
  termes principaux — ~0,5' en longitude, ~200 km en distance. Assez pour la phase
  au pourcent, la date des pleines/nouvelles lunes à quelques minutes, et le test
  « superlune » (distance au périgée).

Les « lunes spéciales » (superlune, micro-lune, lune bleue, éclipse) dérivent de ces
mêmes calculs — voir `special_moons()`. L'éclipse est une **estimation** fondée sur la
latitude écliptique de la Lune à la pleine lune, pas un calcul de contacts.
"""

from datetime import datetime, timedelta, timezone
from math import acos, atan2, asin, cos, degrees, floor, radians, sin

J2000 = 2451545.0
AU_KM = 149597870.7

# Seuils d'élévation solaire (degrés) qui découpent la journée.
ALT_SUNRISE = -0.833   # bord supérieur du disque + réfraction atmosphérique
ALT_GOLDEN_LO = -4.0   # début (matin) / fin (soir) de la golden hour
ALT_GOLDEN_HI = 6.0    # limite haute de la golden hour
ALT_BLUE_LO = -6.0     # crépuscule civil = fin de la blue hour

# Distances de référence de la Lune (km) : périgée ≈ 356 500, apogée ≈ 406 700.
SUPERMOON_MAX_KM = 360000
MICROMOON_MIN_KM = 405000


# --------------------------------------------------------------------------- temps

def julian_day(dt: datetime) -> float:
    """Jour julien d'un datetime **aware** (converti en UTC)."""
    dt = dt.astimezone(timezone.utc)
    year, month = dt.year, dt.month
    if month <= 2:
        year, month = year - 1, month + 12
    a = floor(year / 100)
    b = 2 - a + floor(a / 4)
    day = dt.day + (dt.hour + (dt.minute + dt.second / 60) / 60) / 24
    return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5


def from_julian(jd: float) -> datetime:
    """Datetime UTC correspondant à un jour julien."""
    return datetime(2000, 1, 1, 12, tzinfo=timezone.utc) + timedelta(days=jd - J2000)


def _acos(x: float) -> float:
    """acos borné : protège des dépassements de ±1 dus aux arrondis."""
    return acos(max(-1.0, min(1.0, x)))


# --------------------------------------------------------------------------- soleil

def sun_ecliptic(jd: float) -> tuple[float, float]:
    """Longitude écliptique apparente (degrés) et distance (UA) du Soleil."""
    n = jd - J2000
    mean_lon = (280.460 + 0.9856474 * n) % 360
    anomaly = radians((357.528 + 0.9856003 * n) % 360)
    lon = mean_lon + 1.915 * sin(anomaly) + 0.020 * sin(2 * anomaly)
    dist = 1.00014 - 0.01671 * cos(anomaly) - 0.00014 * cos(2 * anomaly)
    return lon % 360, dist


def sun_altitude(jd: float, lat: float, lon: float) -> float:
    """Élévation du Soleil (degrés) au-dessus de l'horizon, pour un lieu donné."""
    n = jd - J2000
    lam = radians(sun_ecliptic(jd)[0])
    eps = radians(23.439 - 0.0000004 * n)
    right_asc = atan2(cos(eps) * sin(lam), cos(lam))
    decl = asin(sin(eps) * sin(lam))
    gmst = (18.697374558 + 24.06570982441908 * n) % 24
    hour_angle = radians((gmst * 15 + lon - degrees(right_asc)) % 360)
    return degrees(asin(sin(radians(lat)) * sin(decl)
                        + cos(radians(lat)) * cos(decl) * cos(hour_angle)))


def sun_events(day: datetime, lat: float, lon: float) -> dict[str, datetime | None]:
    """Instants des franchissements de seuils solaires, sur la journée locale de `day`.

    `day` est un datetime aware ; seule sa date (dans son fuseau) compte. Les valeurs
    retournées sont dans ce même fuseau, ou None si le seuil n'est pas franchi ce
    jour-là (jour polaire / nuit polaire).
    """
    tz = day.tzinfo
    start = day.replace(hour=0, minute=0, second=0, microsecond=0)
    samples = [(i, sun_altitude(julian_day(start + timedelta(minutes=i)), lat, lon))
               for i in range(1441)]

    def crossing(threshold: float, rising: bool) -> datetime | None:
        for (i0, a0), (i1, a1) in zip(samples, samples[1:]):
            if (rising and a0 < threshold <= a1) or (not rising and a0 >= threshold > a1):
                frac = (threshold - a0) / (a1 - a0)
                return (start + timedelta(minutes=i0 + frac)).astimezone(tz)
        return None

    noon_minute = max(samples, key=lambda s: s[1])[0]
    return {
        "dawn_blue_start": crossing(ALT_BLUE_LO, True),
        "sunrise": crossing(ALT_SUNRISE, True),
        "golden_am_end": crossing(ALT_GOLDEN_HI, True),
        "golden_pm_start": crossing(ALT_GOLDEN_HI, False),
        "sunset": crossing(ALT_SUNRISE, False),
        "golden_pm_end": crossing(ALT_GOLDEN_LO, False),
        "dusk_blue_end": crossing(ALT_BLUE_LO, False),
        "noon": (start + timedelta(minutes=noon_minute)).astimezone(tz),
    }


# ----------------------------------------------------------------------------- lune

# Meeus 47.A / 47.B, termes principaux : (D, M, M', F) -> coefficient.
_LON_TERMS = [
    ((0, 0, 1, 0), 6288774), ((2, 0, -1, 0), 1274027), ((2, 0, 0, 0), 658314),
    ((0, 0, 2, 0), 213618), ((0, 1, 0, 0), -185116), ((0, 0, 0, 2), -114332),
    ((2, 0, -2, 0), 58793), ((2, -1, -1, 0), 57066), ((2, 0, 1, 0), 53322),
    ((2, -1, 0, 0), 45758), ((0, 1, -1, 0), -40923), ((1, 0, 0, 0), -34720),
    ((0, 1, 1, 0), -30383), ((2, 0, 0, -2), 15327), ((0, 0, 1, 2), -12528),
    ((0, 0, 1, -2), 10980),
]
_DIST_TERMS = [
    ((0, 0, 1, 0), -20905355), ((2, 0, -1, 0), -3699111), ((2, 0, 0, 0), -2955968),
    ((0, 0, 2, 0), -569925), ((0, 1, 0, 0), 48888), ((0, 0, 0, 2), -3149),
    ((2, 0, -2, 0), 246158), ((2, -1, -1, 0), -152138), ((2, 0, 1, 0), -170733),
    ((2, -1, 0, 0), -204586), ((0, 1, -1, 0), -129620), ((1, 0, 0, 0), 108743),
    ((0, 1, 1, 0), 104755), ((2, 0, 0, -2), 10321),
]
_LAT_TERMS = [
    ((0, 0, 0, 1), 5128122), ((0, 0, 1, 1), 280602), ((0, 0, 1, -1), 277693),
    ((2, 0, 0, -1), 173237), ((2, 0, -1, 1), 55413), ((2, 0, -1, -1), 46271),
    ((2, 0, 0, 1), 32573), ((0, 0, 2, 1), 17198), ((2, 0, 1, -1), 9266),
    ((0, 0, 2, -1), 8822), ((2, -1, 0, -1), 8216), ((2, 0, -2, -1), 4324),
    ((2, 0, 1, 1), 4200),
]


def moon_ecliptic(jd: float) -> tuple[float, float, float]:
    """Longitude et latitude écliptiques (degrés) + distance (km) de la Lune."""
    t = (jd - J2000) / 36525
    mean_lon = 218.3164477 + 481267.88123421 * t
    args = [
        radians(297.8501921 + 445267.1114034 * t),   # D  — élongation moyenne
        radians(357.5291092 + 35999.0502909 * t),    # M  — anomalie moyenne du Soleil
        radians(134.9633964 + 477198.8675055 * t),   # M' — anomalie moyenne de la Lune
        radians(93.2720950 + 483202.0175233 * t),    # F  — argument de latitude
    ]

    def total(terms, fn):
        return sum(coef * fn(sum(c * a for c, a in zip(mult, args))) for mult, coef in terms)

    lon = (mean_lon + total(_LON_TERMS, sin) / 1e6) % 360
    lat = total(_LAT_TERMS, sin) / 1e6
    dist = 385000.56 + total(_DIST_TERMS, cos) / 1000
    return lon, lat, dist


def moon_illumination(jd: float) -> tuple[float, bool]:
    """Fraction éclairée (0–1) du disque lunaire, et True si la Lune est croissante."""
    sun_lon, sun_dist = sun_ecliptic(jd)
    lon, lat, dist = moon_ecliptic(jd)
    elong = degrees(_acos(cos(radians(lat)) * cos(radians(lon - sun_lon))))
    r_sun = sun_dist * AU_KM
    phase_angle = atan2(r_sun * sin(radians(elong)), dist - r_sun * cos(radians(elong)))
    return (1 + cos(phase_angle)) / 2, (lon - sun_lon) % 360 < 180


def phase_name(fraction: float, waxing: bool) -> str:
    """Libellé français de la phase, à partir de la fraction éclairée."""
    if fraction < 0.02:
        return "Nouvelle lune"
    if fraction > 0.98:
        return "Pleine lune"
    if 0.48 < fraction < 0.52:
        return "Premier quartier" if waxing else "Dernier quartier"
    if fraction < 0.48:
        return "Croissant" if waxing else "Croissant décroissant"
    return "Gibbeuse croissante" if waxing else "Gibbeuse décroissante"


def moon_age_days(jd: float) -> float:
    """Âge de la lunaison en jours (0 = nouvelle lune), estimé par l'élongation."""
    return _elongation(jd) / 360 * 29.530588853


def _elongation(jd: float) -> float:
    """Écart de longitude Lune − Soleil, en degrés (0 = nouvelle lune, 180 = pleine)."""
    return (moon_ecliptic(jd)[0] - sun_ecliptic(jd)[0]) % 360


def find_phases(jd_start: float, days: int = 60) -> list[tuple[float, str]]:
    """Instants (jour julien) des nouvelles et pleines lunes sur `days` jours.

    Balayage horaire de l'élongation, puis bissection sur le passage par 0° (nouvelle
    lune) ou 180° (pleine lune).
    """
    out: list[tuple[float, str]] = []
    step = 1 / 24
    prev_jd, prev = jd_start, _elongation(jd_start)
    jd = jd_start + step
    while jd < jd_start + days:
        cur = _elongation(jd)
        if prev < 180 <= cur:
            out.append((_refine(prev_jd, jd, 180.0), "full"))
        if cur < prev:  # repassage 360° -> 0°
            out.append((_refine(prev_jd, jd, 0.0), "new"))
        prev_jd, prev, jd = jd, cur, jd + step
    return sorted(out)


def _refine(lo: float, hi: float, target: float) -> float:
    """Bissection sur l'instant où l'élongation vaut `target` (0 ou 180 degrés)."""
    def delta(jd: float) -> float:
        return ((_elongation(jd) - target + 180) % 360) - 180

    for _ in range(40):
        mid = (lo + hi) / 2
        if delta(lo) * delta(mid) <= 0:
            hi = mid
        else:
            lo = mid
    return (lo + hi) / 2


def special_moons(jd_start: float, tz, days: int = 120) -> list[dict]:
    """Pleines lunes remarquables à venir dans les `days` prochains jours.

    - **Superlune** : pleine lune à moins de 360 000 km.
    - **Micro-lune** : pleine lune à plus de 405 000 km.
    - **Lune bleue** : deuxième pleine lune d'un même mois civil (fuseau local) —
      d'où la fenêtre élargie du recensement, pour voir la pleine lune précédente.
    - **Éclipse** : *estimation* par la latitude écliptique β de la Lune à la pleine
      lune (|β| < 0,45° → totale, la « lune rousse » ; < 1,0° → partielle ; < 1,55° →
      pénombrale). Un vrai calcul de contacts demanderait des éphémérides complètes ;
      ces événements sont marqués `estimated: true` pour être annoncés avec prudence.
    """
    window = [jd for jd, kind in find_phases(jd_start - 40, days + 80) if kind == "full"]
    by_month: dict[tuple[int, int], list[float]] = {}
    for jd in window:
        local = from_julian(jd).astimezone(tz)
        by_month.setdefault((local.year, local.month), []).append(jd)

    events: list[dict] = []
    for jd in window:
        if jd < jd_start:
            continue
        local = from_julian(jd).astimezone(tz)
        _, lat, dist = moon_ecliptic(jd)
        tags: list[str] = []
        if dist < SUPERMOON_MAX_KM:
            tags.append("SUPERLUNE")
        elif dist > MICROMOON_MIN_KM:
            tags.append("MICRO-LUNE")
        siblings = by_month.get((local.year, local.month), [])
        if len(siblings) > 1 and jd != min(siblings):
            tags.append("LUNE BLEUE")
        if abs(lat) < 0.45:
            tags.append("ÉCLIPSE TOTALE")
        elif abs(lat) < 1.0:
            tags.append("ÉCLIPSE PARTIELLE")
        elif abs(lat) < 1.55:
            tags.append("ÉCLIPSE PÉNOMBRALE")
        if tags:
            events.append({
                "when": local,
                "tags": tags,
                "distance_km": round(dist),
                "estimated": any(tag.startswith("ÉCLIPSE") for tag in tags),
            })
    return events


def moon_svg_path(fraction: float, waxing: bool, cx: float, cy: float, r: float) -> str:
    """Chemin SVG de la **partie éclairée** du disque lunaire.

    Le terminateur se projette en demi-ellipse de demi-axe `r * (2f - 1)` : positif en
    phase gibbeuse (il déborde du côté sombre), négatif en croissant. Le Liquid ne sait
    pas faire de trigonométrie — on précalcule donc le `d` complet ici.
    """
    rx = r * (2 * fraction - 1)
    # Demi-disque éclairé à droite (Lune croissante, hémisphère nord), puis retour par
    # le terminateur ; pour la Lune décroissante, la figure est miroitée en x.
    # En coordonnées SVG (y vers le bas), sweep=1 dessine l'arc dans le sens horaire.
    # Croissant (rx < 0) : le terminateur bombe du côté éclairé ; gibbeuse : du côté sombre.
    outer_sweep = 1 if waxing else 0
    terminator_sweep = 1 if rx >= 0 else 0
    if not waxing:
        terminator_sweep = 1 - terminator_sweep
    top, bottom = f"{cx} {cy - r}", f"{cx} {cy + r}"
    return (f"M {top} "
            f"A {r} {r} 0 0 {outer_sweep} {bottom} "
            f"A {abs(rx):.2f} {r} 0 0 {terminator_sweep} {top} Z")
