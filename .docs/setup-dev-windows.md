# Setup dev Windows — Android, Home Assistant, TV

Le poste de dev est désormais le **PC Windows 11** (i5-8600K, 16 Go, Docker
Desktop déjà présent) — plus le Mac évoqué dans [app-remote.md](app-remote.md).
Ce doc trace ce qui est installé, comment, et les pièges rencontrés.

## Questions ouvertes

- [ ] Émulateur : l'image `android-35 google_apis x86_64` est installée mais
  aucun AVD n'est créé ; vérifier que WHPX (Windows Hypervisor Platform) est
  actif avant d'en créer un — sinon on teste sur le vrai téléphone (préférable).
- [ ] Le téléchargement Chrome de l'installeur Android Studio est mort à 1,4 Go
  et un `Invoke-WebRequest` vers dl.google.com a aussi été coupé : connexion
  instable vers Google (NordVPN du routeur ?). À surveiller, cf. [infra-reseau.md](infra-reseau.md).
- [ ] Premier lancement d'Android Studio : pointer le wizard sur le SDK existant
  (`%LOCALAPPDATA%\Android\Sdk`) plutôt que de le laisser retélécharger.

## Décisions

- **Installation par winget, pas par le .exe téléchargé à la main.** Le
  « .exe qui ne s'installe pas » était en réalité un `Unconfirmed *.crdownload`
  (téléchargement Chrome inachevé) dans `Downloads\Web Browser`. winget
  télécharge l'installeur officiel avec vérification de hash et le rejoue tout
  seul. Piège : forcer `--source winget`, sinon la source `msstore` fait planter
  la commande (erreur 0x8a15003b).
- **SDK installé en ligne de commande** (`cmdline-tools` + `sdkmanager`) plutôt
  que d'attendre le wizard de Studio : reproductible, et l'app se builde sans
  ouvrir l'IDE. Licences acceptées via un fichier de `y` redirigé (le pipe
  PowerShell vers le .bat ne passe pas).
- **Un seul `adb`** : celui du SDK (`platform-tools`). Le paquet winget
  `Google.PlatformTools` installé au départ a été retiré pour éviter deux
  versions d'adb qui se tuent mutuellement.
- **Wrapper Gradle commité** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`
  8.10.2, récupérés du tag officiel) — la note « `gradle wrapper` à générer »
  d'app-remote.md est caduque.
- **Python côté Windows dans un venv hors repo** `%USERPROFILE%\.venvs\portal6-home`
  (androidtvremote2, requests, websockets, zeroconf, python-dotenv) — distinct
  du venv WSL `~/.venvs/portal6` du domaine musique (`setup/bootstrap.sh`),
  parce qu'adb et les tests réseau vers la Shield sont plus simples nativement.
- **Home Assistant de dev en Docker Desktop** (`features/home/ha/docker-compose.yml`) :
  même fichier que la future prod sur la tour, sans host networking sur
  Windows. Sert à développer le client HA de l'app contre une vraie API avec
  l'intégration *Demo*, avant l'arrivée du coordinateur Zigbee.

## Inventaire (2026-09-06)

| Brique | Version | Où | Comment |
|---|---|---|---|
| Android Studio | 2026.1.4.7 | `C:\Program Files\Android\Android Studio` | `winget install Google.AndroidStudio --source winget` |
| JDK Temurin | 21.0.12 | `C:\Program Files\Eclipse Adoptium\jdk-21…` (`JAVA_HOME` machine) | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Android SDK | platform 35, build-tools 35.0.0, platform-tools 37.0.1, emulator, image android-35 x86_64 | `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME` user) | `sdkmanager` (cmdline-tools 13114758) |
| Python | 3.12.10 (user, dans le PATH) | `%LOCALAPPDATA%\Programs\Python\Python312` | `winget install Python.Python.3.12` |
| venv outils | androidtvremote2 0.3.2 … | `%USERPROFILE%\.venvs\portal6-home` | `python -m venv` + pip |
| Docker Desktop | 29.5.2 | déjà présent | — |

PATH utilisateur enrichi de `platform-tools`, `cmdline-tools\latest\bin`,
`emulator` : **ouvrir un nouveau terminal** pour en bénéficier.

## Vérifications

```powershell
adb --version
java -version
sdkmanager --list_installed
cd apps\ha-remote ; .\gradlew.bat assembleDebug
cd home\ha ; docker compose up -d      # puis http://localhost:8123
```

Outils créés cette session : `features/home/ha/` (compose + `ha_probe.py`),
`features/home/tv/` (`shield_mute_adb.ps1`, `shield_remote.py`) — READMEs sur place.

## Git — GitHub Desktop (2026-09-06)

État vérifié : GitHub Desktop 3.6.4 installé (`%LOCALAPPDATA%\GitHubDesktop`),
**connecté au compte `frgrz-pro`** (credential Windows `GitHub - https://api.github.com/frgrz-pro`),
et le repo `C:\DevLab\portal6` est déjà dans sa liste (comme `spotify-toolkit`).
GitKraken n'est pas installé — CLAUDE.md mis à jour en conséquence : les pushes se font
depuis GitHub Desktop, jamais depuis le shell.

- Identité git (globale et repo) : `François Grzybowski <fc.grzybowski.pro@gmail.com>`,
  cohérente avec `frgrz-pro`. `core.autocrlf=true` vient du gitconfig système de Git for Windows.
- `.gitattributes` ajouté à la racine : `* text=auto` (tous les blobs indexés étaient
  déjà en LF, donc aucun re-commit massif), `.sh`/`.py` forcés LF pour WSL, `.ps1`/`.bat`
  en CRLF, binaires (`kmz`, `xlsx`, `db`, `jar`) exclus de la conversion. Règle la
  dérive « seules les fins de ligne diffèrent » vue entre portal6 et spotify-toolkit.
- Reste à faire par François dans GitHub Desktop : *Push origin* (la branche `main`
  a au moins un commit d'avance sur `origin/main`).

## npm absent

`node`/`npm` ne sont installés **ni sous Windows ni dans WSL** : les scripts
`npm run …` du `package.json` sont documentaires. Lancer les scripts Python
directement (Windows : `%LOCALAPPDATA%\Programs\Python\Python312\python.exe`,
WSL : `~/.venvs/portal6/bin/python`). Le `python` du PATH Windows est le stub
Microsoft Store, qui ne fait rien.

## Journal

### 2026-09-06
Création du doc. Diagnostic : l'installeur « qui ne s'installe pas » était un
`.crdownload` incomplet. Installation complète par winget + sdkmanager (Studio,
JDK 21, SDK 35, Python 3.12, venv outils), wrapper Gradle commité, HA de dev en
compose, scripts de validation mute Shield. **Premier build réussi** : `gradlew assembleDebug` → `app-debug.apk` (15,6 Mo, 4 min à froid). HA de dev démarré (`portal6-ha`, http://localhost:8123), onboarding à faire.
Plus tard dans la journée, François a réorganisé le repo : `home/` → `features/home/`
(dont `ha/` et `tv/`), `hardware/` → `.docs/hardware/`, notes datées `docs/` → `.docs/`.
Références corrigées (README, CLAUDE.md, skill, workflow eSport, `build_ics.py` qui
cherche `.env` un niveau plus haut, READMEs outils) ; conteneur HA recréé sur le
nouveau chemin de `config/`. ⚠️ L'URL raw des .ics change : réabonner Google Calendar.

### 2026-09-06 (bis) — GitHub Desktop, npm
Vérification GitHub Desktop (installé, connecté frgrz-pro, repo déjà ajouté) ;
`.gitattributes` ajouté ; CLAUDE.md passe de GitKraken à GitHub Desktop. Constat :
node/npm absents partout → scripts npm non fonctionnels sur ce poste.
