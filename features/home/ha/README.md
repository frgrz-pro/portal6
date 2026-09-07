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

## Zigbee : Sonoff Dongle-P sur le PC Windows

Docker Desktop ne passe pas l'USB au conteneur : `zigbee_bridge.py` expose le
port COM du dongle en TCP, et ZHA s'y connecte comme à un coordinateur Ethernet.

1. **Pilote CP210x** (une fois) : Windows Update → mises à jour facultatives →
   pilotes « Silicon Labs », ou le zip *CP210x Universal Windows Driver* de
   silabs.com puis, en admin, `pnputil /add-driver silabser.inf /install`.
   Vérif : un `COMx` apparaît dans le Gestionnaire de périphériques.
2. **Pont, test à la main** (détecte le COM tout seul par VID:PID `10C4:EA60`) :

   ```powershell
   & "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" features\home\ha\zigbee_bridge.py -v
   ```

3. **Pont en continu** — tâche planifiée à l'ouverture de session (Docker
   Desktop démarre au même moment) :

   ```powershell
   schtasks /Create /TN "portal6-zigbee-bridge" /SC ONLOGON /RL LIMITED /F /TR "\"$env:USERPROFILE\.venvs\portal6-home\Scripts\pythonw.exe\" \"C:\DevLab\portal6\features\home\ha\zigbee_bridge.py\""
   ```

   Puis `schtasks /Run /TN portal6-zigbee-bridge`. Le pont réessaie tout seul
   si le dongle est débranché ou si HA se déconnecte.
4. **ZHA** : Paramètres → Appareils et services → Ajouter → *Zigbee Home
   Automation* → radio **ZNP (Texas Instruments)** → port
   `socket://host.docker.internal:6638`. Nouveau réseau, puis « Ajouter un
   appareil » et mettre la multiprise en appairage.

Le réseau Zigbee vit dans `config/zigbee.db` (hors git) : penser à la
sauvegarde ZHA (Paramètres → Zigbee → Télécharger la sauvegarde) une fois les
appareils appairés.

## Prod (tour Linux, si elle arrive)

Même compose, avec `network_mode: host` (voir commentaires) et le dongle en
`/dev/ttyUSB0` direct dans ZHA (plus de pont). Le dossier `config/` est ignoré
par git : chaque instance est indépendante.
