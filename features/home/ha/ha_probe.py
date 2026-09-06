"""Sonde Home Assistant : vérifie l'URL/token de .env et liste les switch/light.

Usage : python home/ha/ha_probe.py [entity_id à basculer]
Lit HA_URL (défaut http://localhost:8123) et HA_TOKEN dans .env à la racine.
"""
from __future__ import annotations

import sys
from pathlib import Path

import requests
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[2]
env = dotenv_values(ROOT / ".env")
url = (env.get("HA_URL") or "http://localhost:8123").rstrip("/")
token = env.get("HA_TOKEN")
if not token:
    sys.exit("HA_TOKEN manquant dans .env (créer un jeton longue durée dans HA).")

headers = {"Authorization": f"Bearer {token}"}
r = requests.get(f"{url}/api/", headers=headers, timeout=5)
r.raise_for_status()
print("HA répond :", r.json().get("message"))

states = requests.get(f"{url}/api/states", headers=headers, timeout=5).json()
targets = [s for s in states if s["entity_id"].split(".")[0] in ("switch", "light", "input_boolean")]
for s in targets:
    print(f"  {s['entity_id']:40s} {s['state']}")
if not targets:
    print("  (aucun switch/light — ajouter l'intégration Demo dans HA)")

if len(sys.argv) > 1:
    entity = sys.argv[1]
    domain = entity.split(".")[0]
    resp = requests.post(
        f"{url}/api/services/{domain}/toggle",
        headers=headers, json={"entity_id": entity}, timeout=5,
    )
    resp.raise_for_status()
    print(f"toggle {entity} →", [s["state"] for s in resp.json() if s["entity_id"] == entity])
