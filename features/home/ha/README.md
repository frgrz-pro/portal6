# home/ha — Home Assistant (dev local + tour Docker)

Backend domotique de l'app remote (design : [`.docs/zigbee-multiprises.md`](../../../.docs/zigbee-multiprises.md),
[`.docs/app-remote.md`](../../../.docs/app-remote.md)).

## Dev local (PC Windows, Docker Desktop)

```powershell
cd home\ha
docker compose up -d
```

Puis http://localhost:8123 → créer le compte admin (onboarding). Pour avoir des
interrupteurs factices à piloter depuis l'app avant l'arrivée du coordinateur
Zigbee : **Paramètres → Appareils et services → Ajouter → « Demo »** (crée des
`switch.*`, `light.*`…), ou déclarer des `input_boolean` dans `config/configuration.yaml`.

Token pour l'app : profil utilisateur (en bas à gauche) → **Sécurité → Jetons
d'accès longue durée → Créer**. À stocker dans `.env` (racine du repo, jamais
commité) sous `HA_URL` / `HA_TOKEN`.

Test rapide de l'API depuis le venv :

```powershell
& "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" home\ha\ha_probe.py
```

## Prod (tour Docker)

Même compose, avec `network_mode: host` (voir commentaires) et le dongle Zigbee
déclaré dans ZHA. Le dossier `config/` est ignoré par git : l'instance de la tour
et celle de dev sont indépendantes.
