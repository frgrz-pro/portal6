"""Modes portal6 côté Home Assistant : scènes `mode_2..4` + automatisations des boutons MOES.

Idempotent — à relancer après chaque bouton appairé. Lit dans `.env` :
  HA_URL, HA_TOKEN, et ZIGBEE_BOUTON_<n>_IEEE pour chaque bouton (n = 1..4).

Convention (cf. .docs/zigbee-multiprises.md, .docs/app-remote.md) :
  touche 1 (endpoint 1) → Mode 1 = tout allumer / tout éteindre
  touche 2..4           → scene.turn_on scene.mode_<touche> ; 2e appui sur la même
                          touche (mode actif, prises allumées) → tout éteindre
  appui long            → tout éteindre
Les scènes sont créées vides (tout off) si absentes ; l'app les redéfinit.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

import requests
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[3]
env = dotenv_values(ROOT / ".env")
URL = (env.get("HA_URL") or "http://localhost:8123").rstrip("/")
H = {"Authorization": f"Bearer {env.get('HA_TOKEN', '')}", "Content-Type": "application/json"}
ALL8 = [f"switch.multiprise_{m}_prise_{n}" for m in "ab" for n in range(1, 5)]
ANY_ON_EXPR = "states.switch | selectattr('entity_id', 'in', %s) | selectattr('state', 'eq', 'on') | list | count > 0" % ALL8
ANY_ON = "{{ %s }}" % ANY_ON_EXPR
# « Le mode de la touche pressée est actif » : sa scène a été jouée (état = horodatage),
# aucune prise n'a bougé depuis (à 5 s près, le temps que la scène s'applique), et
# il reste au moins une prise allumée. Pas de helper : tout vient des horodatages HA.
MODE_ACTIVE = (
    "{% set ts = states('scene.mode_' ~ trigger.event.data.endpoint_id) %}"
    "{% if not ts.startswith('20') %}false{% else %}"
    "{% set since = (expand(ALL8) | map(attribute='last_changed') | max) - as_datetime(ts) %}"
    "{{ since.total_seconds() < 5 and (ANY_ON_EXPR) }}{% endif %}"
).replace("ALL8", str(ALL8)).replace("ANY_ON_EXPR", ANY_ON_EXPR)


def put(kind: str, obj_id: str, cfg: dict) -> None:
    r = requests.post(f"{URL}/api/config/{kind}/config/{obj_id}", headers=H, json=cfg, timeout=15)
    print(f"  {kind}/{obj_id} -> {r.status_code} {r.text[:60]}")


def main() -> None:
    if not env.get("HA_TOKEN"):
        sys.exit("HA_TOKEN manquant dans .env")
    print("scènes")
    for n in (2, 3, 4):
        r = requests.get(f"{URL}/api/config/scene/config/mode_{n}", headers=H, timeout=10)
        if r.status_code == 200:
            print(f"  scene/mode_{n} existe déjà, conservée")
            continue
        put("scene", f"mode_{n}", {"id": f"mode_{n}", "name": f"Mode {n}", "entities": {e: "off" for e in ALL8}})

    print("automatisations")
    buttons = {int(m.group(1)): v for k, v in env.items() if (m := re.fullmatch(r"ZIGBEE_BOUTON_(\d)_IEEE", k)) and v}
    if not buttons:
        print("  aucun ZIGBEE_BOUTON_n_IEEE dans .env")
    for n, ieee in sorted(buttons.items()):
        put("automation", f"bouton_{n}_modes", {
            "alias": f"Bouton {n} — touche n → Mode n",
            "description": "MOES TS0044 : touche 1 = tout on/off, touches 2-4 = scènes mode_2..4 (2e appui = tout éteindre). Généré par features/home/ha/ha_modes_setup.py.",
            "mode": "queued",
            "triggers": [{"trigger": "event", "event_type": "zha_event",
                          "event_data": {"device_ieee": ieee, "command": "remote_button_short_press"}}],
            "actions": [{"choose": [{
                "conditions": [{"condition": "template", "value_template": "{{ trigger.event.data.endpoint_id == 1 }}"}],
                "sequence": [{"if": [{"condition": "template", "value_template": ANY_ON}],
                              "then": [{"action": "switch.turn_off", "target": {"entity_id": ALL8}}],
                              "else": [{"action": "switch.turn_on", "target": {"entity_id": ALL8}}]}],
            }], "default": [{"if": [{"condition": "template", "value_template": MODE_ACTIVE}],
                             "then": [{"action": "switch.turn_off", "target": {"entity_id": ALL8}}],
                             "else": [{"action": "scene.turn_on",
                                       "target": {"entity_id": "scene.mode_{{ trigger.event.data.endpoint_id }}"}}]}]}],
        })
        put("automation", f"bouton_{n}_long", {
            "alias": f"Bouton {n} — appui long → tout éteindre",
            "description": "Généré par features/home/ha/ha_modes_setup.py.",
            "mode": "single",
            "triggers": [{"trigger": "event", "event_type": "zha_event",
                          "event_data": {"device_ieee": ieee, "command": "remote_button_long_press"}}],
            "actions": [{"action": "switch.turn_off", "target": {"entity_id": ALL8}}],
        })
        # l'ancienne automatisation « touche n → prise n » est remplacée
        r = requests.delete(f"{URL}/api/config/automation/config/bouton_{n}_simple", headers=H, timeout=10)
        if r.status_code == 200:
            print(f"  automation/bouton_{n}_simple supprimée (remplacée)")


if __name__ == "__main__":
    main()
