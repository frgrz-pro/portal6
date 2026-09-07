# home/ha — Home Assistant (dev local + tour Docker)

Backend domotique de l'app remote (design : [`.docs/zigbee-multiprises.md`](../../../.docs/zigbee-multiprises.md),
[`.docs/app-remote.md`](../../../.docs/app-remote.md)).

## Dev local (PC Windows, Docker Desktop)

```powershell
cd features\home\ha
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
& "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" features\home\ha\ha_probe.py
```

## Zigbee : Sonoff Dongle-P sur le PC Windows

Docker Desktop ne passe pas l'USB au conteneur : `zigbee_bridge.py` expose le
port COM du dongle en TCP, et ZHA s'y connecte comme à un coordinateur Ethernet.

1. **Pilote CP210x** (une fois — fait le 2026-09-07, dongle sur **COM4**) :
   Windows Update ne le propose pas ; zip *CP210x Universal Windows Driver* de
   silabs.com (catalogue signé WHQL), puis en admin
   `pnputil /add-driver silabser.inf /install`. Vérif : un `COMx` apparaît.
2. **Pont, test à la main** (détecte le COM tout seul par VID:PID `10C4:EA60`) :

   ```powershell
   & "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" features\home\ha\zigbee_bridge.py -v
   ```

3. **Pont en continu** — tâche planifiée `portal6-zigbee-bridge` (à l'ouverture
   de session, redémarrage auto ×5, journal dans `zigbee_bridge.log`, hors git).
   Un déclencheur « à l'ouverture de session » exige un PowerShell **admin** :

   ```powershell
   $pyw = "$env:USERPROFILE\.venvs\portal6-home\Scripts\pythonw.exe"
   $a = New-ScheduledTaskAction -Execute $pyw -Argument '"C:\DevLab\portal6\features\home\ha\zigbee_bridge.py" --log "C:\DevLab\portal6\features\home\ha\zigbee_bridge.log"'
   $t = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
   $s = New-ScheduledTaskSettingsSet -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
   Register-ScheduledTask -TaskName portal6-zigbee-bridge -Action $a -Trigger $t -Settings $s -RunLevel Limited -Force
   Start-ScheduledTask -TaskName portal6-zigbee-bridge
   ```

   Contrôle : `Get-ScheduledTask portal6-zigbee-bridge` → *Running*, et
   `Get-NetTCPConnection -LocalPort 6638 -State Listen`. Le pont réessaie tout
   seul si le dongle est débranché ou si HA se déconnecte.
4. **ZHA** : Paramètres → Appareils et services → Ajouter → *Zigbee Home
   Automation* → radio **ZNP (Texas Instruments)** → port
   `socket://host.docker.internal:6638`. Nouveau réseau, puis « Ajouter un
   appareil » et mettre la multiprise en appairage.

Le réseau Zigbee vit dans `config/zigbee.db` (hors git) : penser à la
sauvegarde ZHA (Paramètres → Zigbee → Télécharger la sauvegarde) une fois les
appareils appairés.

## Modes : scènes + automatisations des boutons MOES

`ha_modes_setup.py` (idempotent, à relancer après chaque bouton appairé) crée
les scènes `mode_2..4` si absentes et, pour chaque `ZIGBEE_BOUTON_<n>_IEEE` de
`.env`, les automatisations « touche 1 = tout on/off, touches 2-4 =
`scene.mode_<touche>`, appui long = tout éteindre » :

```powershell
& "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" features\home\ha\ha_modes_setup.py
```

Les scènes sont ensuite redéfinies depuis l'app (`apps/ha-remote`, appui long
sur un mode) via l'API config des scènes — ne pas les éditer à la main en
parallèle.

## Prod (tour Linux, si elle arrive)

Même compose, avec `network_mode: host` (voir commentaires) et le dongle en
`/dev/ttyUSB0` direct dans ZHA (plus de pont). Le dossier `config/` est ignoré
par git : chaque instance est indépendante.
