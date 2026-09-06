# Plex — outillage de migration vers Docker

Outillage de la bascule **install native Windows → conteneur**. Le design, les
décisions et les risques sont dans [`.docs/plex-docker.md`](../../../.docs/plex-docker.md) ;
ce README n'est que le **runbook**.

> **Rien ici n'est destructif pour l'install native.** Tout le travail se fait sur une
> **copie** posée sur `M:\backup\`. L'original `%LOCALAPPDATA%\Plex Media Server` n'est
> jamais modifié — c'est lui, le rollback.

## Contenu

| Fichier | Rôle |
|---|---|
| `plex.service.yml` | Le bloc de service à fusionner dans `C:\docker\media\docker-compose.yml` (référence versionnée du stack qui vit hors repo) |
| `export_preferences.ps1` | Registre Windows → `Preferences.xml` : **sans lui, le conteneur démarre en serveur neuf** |
| `rewrite_paths.sql` | Réécriture des 10 947 chemins Windows → chemins conteneur, à passer à `Plex SQLite.exe` |
| `check_paths.py` | Contrôle avant/après : compte les chemins Windows vs conteneur, refuse un état mixte |

## Prérequis

- Plex natif **complètement arrêté** (sinon la copie de base est incohérente).
- Réservation DHCP posée sur `192.168.0.5` ↔ MAC Wi-Fi `EC-3A-56-BD-04-5A` (sinon
  `ADVERTISE_IP` se périme). Marche à suivre Mercusys :
  [`.docs/infra-reseau.md`](../../../.docs/infra-reseau.md) § Réservations DHCP.
- ~13 Go libres sur `M:` (381 Go dispo).

---

## Runbook

### Phase 0 — Arrêt et copie de travail

```powershell
Stop-Service PlexUpdateService
Stop-Process -Name 'Plex Media Server','Plex Tuner Service','PlexScriptHost' -Force -ErrorAction SilentlyContinue
Get-Process -Name 'Plex*' -ErrorAction SilentlyContinue   # doit ne rien renvoyer
```

```powershell
$stamp   = Get-Date -Format 'yyyy-MM-dd'
$staging = "M:\backup\plex-migration-$stamp"
robocopy "$env:LOCALAPPDATA\Plex Media Server" "$staging\Plex Media Server" /E /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /XD Cache Updates "Crash Reports" Diagnostics
```

`Cache`, `Updates`, `Crash Reports` et `Diagnostics` sont exclus : Plex les régénère,
et ça évite de recopier ~1,5 Go pour rien. **Attendu : ~11 Go copiés.**

> **STOP.** `robocopy` doit sortir avec un code < 8. Vérifier que
> `$staging\Plex Media Server\Plug-in Support\Databases\com.plexapp.plugins.library.db`
> pèse bien ~138 Mo.

### Phase 1 — Préférences (registre → XML)

```powershell
.\export_preferences.ps1 -Destination "$staging\Plex Media Server\Preferences.xml"
```

> **STOP.** Le script doit afficher `MachineIdentifier : present`. S'il échoue sur une
> valeur vitale manquante, **ne pas poursuivre** : le conteneur repartirait en serveur
> neuf et les comptes partagés perdraient le serveur.
> Le fichier produit contient le `PlexOnlineToken` — il vit sur `M:\`, hors du repo, et
> ne doit jamais être committé.

### Phase 2 — Réécriture des chemins

```powershell
$db = "$staging\Plex Media Server\Plug-in Support\Databases\com.plexapp.plugins.library.db"
python check_paths.py $db
```

Attendu **avant** : `10947` en colonne WINDOWS, `0` en CONTENEUR.

```powershell
Get-Content rewrite_paths.sql -Raw | & "C:\Program Files\Plex\Plex Media Server\Plex SQLite.exe" $db
python check_paths.py $db
```

Le binaire doit répondre `ok` (le `PRAGMA integrity_check` final du script SQL).
Attendu **après** : `0` en WINDOWS, `10947` en CONTENEUR.

> **STOP.** Tout état mixte (`check_paths.py` sort en erreur) = réécriture incomplète :
> supprimer `$staging` et reprendre en phase 0. Ne jamais démarrer le conteneur sur une
> base mixte.

### Phase 3 — Import dans le volume Docker

```powershell
docker volume create plex_config
docker volume create plex_transcode
docker run --rm -v plex_config:/config -v "${staging}:/import:ro" alpine sh -c "mkdir -p '/config/Library/Application Support' && cp -a '/import/Plex Media Server' '/config/Library/Application Support/'"
docker run --rm -v plex_config:/config alpine ls -la "/config/Library/Application Support/Plex Media Server"
```

> **STOP.** Le dernier `ls` doit montrer `Preferences.xml`, `Metadata/`, `Media/`,
> `Plug-in Support/`.

### Phase 4 — Démarrage du conteneur

Fusionner le bloc de `plex.service.yml` dans `C:\docker\media\docker-compose.yml`
(services **et** section `volumes:`), puis :

```powershell
cd C:\docker\media
docker compose config
docker compose up -d plex
docker compose logs -f plex
```

> **STOP.** `http://192.168.0.5:32400/web` doit répondre, et le serveur apparaître **sous
> son nom d'origine** — pas comme un serveur neuf à réclamer. Les 6 bibliothèques doivent
> être là avec leurs compteurs (Series 5 117, Animes 2 929, TV Programs 397, Films 753,
> TV Recordings 0, YouTube 889).

### Phase 5 — Validation des médias

Par bibliothèque : lecture directe d'un fichier, poster présent, épisode vu toujours vu.
Puis depuis un **client du LAN** (vérifier qu'il n'est pas compté comme distant : Réglages
→ État → il ne doit pas y avoir de transcodage pour du direct play) et depuis un **compte
partagé**.

### Phase 6 — Live TV / DVR

Le conteneur `plex` et `xteve` sont dans le même projet compose, donc sur le même réseau :
le tuner se déclare en `http://xteve:34400`. Re-mapper l'EPG, recréer les enregistrements
programmés, et vérifier qu'un enregistrement atterrit dans `/data/h/31-TV Recordings`
(= `H:\31-TV Recordings`).

### Phase 7 — Débrancher le natif (après plusieurs jours d'usage réel)

```powershell
Set-Service PlexUpdateService -StartupType Disabled
Remove-ItemProperty "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run" -Name "Plex Media Server"
```

La désinstallation ne vient qu'après. `%LOCALAPPDATA%\Plex Media Server` reste en place
tant qu'on n'a pas une sauvegarde du volume qui tourne.

---

## Rollback (valable jusqu'à la phase 7 incluse)

```powershell
docker compose -f C:\docker\media\docker-compose.yml stop plex
Start-Service PlexUpdateService
& "C:\Program Files\Plex\Plex Media Server\Plex Media Server.exe"
```

L'install native n'a pas été touchée.

## Sauvegarde du `/config` conteneurisé (une fois en régime)

```powershell
docker run --rm -v plex_config:/c -v M:/backup:/b alpine tar czf "/b/plex_config-$(Get-Date -f yyyy-MM-dd).tgz" -C /c .
```

---

## Pièges rencontrés à l'exécution (2026-09-06)

**1. `export_preferences.ps1` était en UTF-8 sans BOM → erreur de parsing.**
Windows PowerShell 5.1 lit un `.ps1` sans BOM en **ANSI** : le tiret cadratin `—` d'un
message devenait `â€"`, dont le `"` fermait la chaîne prématurément
(`Unexpected token 'Plex'`). Corrigé en ajoutant le **BOM UTF-8** au fichier, contenu
inchangé. **Tout `.ps1` de ce dossier contenant un caractère non-ASCII doit être enregistré
avec BOM** — ou rester en ASCII pur, comme `plex.service.yml`.

**2. Python n'est pas dans le PATH.** Le venv `portal6` vit dans **WSL**
(`~/.venvs/portal6`), inutilisable ici puisque la base à inspecter est côté Windows. Le
launcher `py` est absent. Interpréteur à utiliser :

```powershell
C:\Users\Franc\AppData\Local\Programs\Python\Python312\python.exe check_paths.py $db
```

`check_paths.py` n'utilise que la stdlib (`sqlite3`) : aucun venv nécessaire.

**3. La copie embarque un `-wal` de 141 Mo** (plus un `-shm`), et les 4 sauvegardes
automatiques de Plex (`…library.db-AAAA-MM-JJ`, ~137 Mo chacune). C'est **normal et sain** :
la copie suit l'arrêt propre de PMS, donc le WAL est cohérent et `Plex SQLite.exe` le
checkpointe à l'ouverture. Ne pas supprimer le `-wal` à la main avant la réécriture.

**4. Socket fantôme sur 32400.** Après l'arrêt de PMS, `Get-NetTCPConnection -LocalPort
32400 -State Listen` montrait encore un listener appartenant à un **PID mort**. Sans effet
sur les phases 0 à 3 ; **à revérifier avant la phase 4**, car il pourrait gêner le bind du
conteneur. Un redémarrage de Windows le purge.

**5. `Stop-Service PlexUpdateService` exige une élévation.** À lancer dans un PowerShell
**administrateur** — les trois processus (`Plex Media Server`, `Plex Tuner Service`,
`PlexScriptHost`) se tuent sans élévation, le service non.

### Chiffres réels de la phase 0

`63 361` fichiers · `48 206` dossiers · **10,87 Go** en **3 min 21** (~84 Mo/s),
0 échec, code robocopy `1` (< 8). Base à `138,3 Mo`, conforme à l'attendu.
