# App remote — télécommande à tout faire

Petite app mobile perso : piloter les lampes (multiprises Zigbee), écouter les
web-radios AzuraCast de la maison et, en phase 2, couper le son de la TV. Une seule
app, minimale, qui remplace les télécommandes pénibles et les apps constructeur.

## Questions ouvertes

- [ ] ~~Le téléphone est-il Android ?~~ → **les deux** (2026-09-08) : le S20 Ultra sert
  au dev, mais François a scanné le QR d'installation avec **un iPhone**. Ça relance
  l'argument KMP (cœur HA/modes partagé, UI SwiftUI) — mais un build iOS installable
  par lien demande Mac + Xcode + compte développeur Apple + HTTPS. À arbitrer : est-ce
  que l'iPhone doit vraiment avoir l'app, ou l'app HA officielle suffit-elle ?
- [x] ~~Mapping exact des 8 boutons~~ → **2 multiprises × 4 prises, confirmé
  (Shelly Power Strip 4 Gen4)** ; la grille 2 × 4 est la bonne.
- [x] ~~Noms des boutons : par lampe ou par prise ?~~ → **génériques, tranché le
  2026-09-07** : A1…A4 / B1…B4 restent les libellés ; le sens est porté par les
  configs de pièce (scènes nommées), pas par les boutons.
- [x] ~~Tester sur le téléphone~~ → **fait le 2026-09-07** : S20 Ultra branché sur
  HA (`192.168.0.5:8123`), Mode 2 défini depuis l'app = `scene.mode_2` (A2, A3, B3)
  vérifié par l'API. Reste à tester depuis le MOES : touche 2 → ces 3 prises.
- [ ] **Définir les modes 3 et 4 depuis l'app** : leurs scènes HA sont vides (tout
  off) parce qu'ils avaient été définis en mode démo, donc stockés sur le téléphone
  seulement. Le MOES joue bien `scene.mode_3/4` (logbook HA) → il éteint tout.
- [ ] Interprétation « 4 modes ↔ 4 boutons » : retenu **mode n = touche n**, identique
  sur chacun des 4 MOES (voir Décisions). Si François voulait plutôt « un MOES = un
  mode », seule l'automatisation HA change (`ha_modes_setup.py`), pas l'app.
- [ ] Tuile Modes : un appui passe au mode suivant parmi 2-4. Alternative si ça
  ne colle pas à l'usage : une tuile par mode.
- [x] ~~Touche n = ON seulement~~ → **toggle depuis le 2026-09-07** : 2e appui sur la
  même touche (mode actif, prises allumées) = tout éteindre.
- [x] ~~Mode sélectionné non persisté~~ → sans objet : il se déduit de l'état réel des
  prises (voir UI v1).

- [ ] **Onglet TRMNL : v1 = interrupteurs de playlist, ou aussi les créneaux ?**
  Cadré le 2026-09-07 (voir Décisions). À trancher par l'usage : si François veut
  surtout « ce soir je veux l'écran X », les interrupteurs suffisent ; si c'est
  « le matin l'agenda, le soir la mer », c'est le schedule qu'il faut exposer.
- [ ] Onglet TRMNL : où stocker la clé compte `user_…` ? Même mécanique que le jeton
  HA (Réglages, stockage privé) — mais c'est une **2e clé** à pousser sur le téléphone
  (skill `android`, François lance la commande qui lit `.env`).

## Décisions

- **v1 Android natif, Kotlin + Jetpack Compose (Material 3).** KMP reste ouvert
  (le cœur métier — clients HA/Shield — dans un module commun si on y va), mais on
  ne paie pas le coût multiplateforme tant qu'un seul téléphone est concerné.
- L'app ne parle **jamais Zigbee directement** (le téléphone n'a pas de radio
  Zigbee) : elle parle à un backend sur le LAN qui, lui, tient la radio.
  Backend pressenti : Home Assistant sur la tour Docker (déjà planifié dans
  `features/home/README.md`). Détail dans [zigbee-multiprises.md](zigbee-multiprises.md).
- Emplacement du code : `apps/ha-remote/` (le dossier `apps/` du monorepo existe pour ça).
- **Scaffold créé le 2026-08-30** : projet Gradle single-module, package
  `com.portal6.haremote`, minSdk 26 / target 35, Compose BOM + Material 3.
  L'UI est branchée sur un **`MockLightsRepository`** (état en mémoire) derrière
  l'interface `LightsRepository` — contrat : `lights: StateFlow<List<Light>>`,
  `toggle(entityId)`, `setAll(on)`. Le futur client HA implémentera la même
  interface (les `entityId` sont déjà au format HA `switch.multiprise_a_prise_1`).
- Labels des 8 boutons : placeholders A1-A4 / B1-B4 dans `data/Light.kt`
  (`DefaultLights`) — à renommer quand le mapping réel sera connu.
- **Configs de pièce (2026-09-06)** : une « config » est un instantané nommé de
  l'état on/off des prises d'une pièce — une scène. C'est une notion applicative
  qui survit au passage du mock au vrai client HA (les clés sont des `entityId`).
  Persistées en `SharedPreferences` + JSON `org.json` : volume minuscule, lecture
  synchrone (ce dont les tuiles ont besoin), **zéro dépendance ajoutée** — DataStore
  aurait imposé de l'asynchrone pour 8 booléens.
- **Les dépôts sont des singletons de process** (`Portal6App` + `AppContainer`,
  injection manuelle, pas de Hilt) : les tuiles des réglages rapides tournent hors
  Activity et doivent voir le même état que l'UI.
- **Deux tuiles dans le volet des réglages rapides** (`TileService`) :
  « Salon » (un appui applique la config suivante, en boucle ; sous-titre = config
  active) et « Mute TV ». Un bouton de l'app les ajoute au volet en un tap
  (`requestAddTileService`, Android 13+) ; en dessous l'ajout reste manuel.
- **`TvRepository` mocké**, calqué sur `LightsRepository` : la tuile Mute et
  l'onglet TV partagent un état persisté, donc tout est câblé et testable. Seul
  l'envoi réel de `KEYCODE_VOLUME_MUTE` manque (phase 2, [tv-mute.md](tv-mute.md)).
- L'état courant des prises est lui aussi persisté, mais c'est une **béquille du
  mock** : avec HA branché, l'état de vérité vient du backend, pas du disque.
- **Modes (2026-09-07)** — remplace les « configs de pièce ». **4 modes, noms
  génériques « Mode 1..4 », un par touche du bouton MOES** ; même numéro, même
  effet depuis le mur ou depuis l'app.
  - **Mode 1 = tout on/off**, câblé : si une prise est allumée → tout s'éteint,
    sinon tout s'allume. Pas éditable.
  - **Modes 2-4 = scènes définies dans l'app** (appui long sur le mode → 8
    interrupteurs, ou « Prendre l'état actuel »). Elles vivent **côté HA**
    (`scene.mode_2..4`, écrites par l'API config des scènes) : c'est ce qui
    permet aux automatisations des boutons physiques de jouer exactement la
    même chose. Le mode dont l'état correspond aux prises est mis en avant.
  - Mode démo (HA non configuré) : modes 2-4 en `SharedPreferences`.
- **Client Home Assistant réel (2026-09-07)** : `data/ha/HaClient` (OkHttp —
  REST pour états/services/scènes, WebSocket `state_changed` avec reconnexion
  et resynchro à chaque connexion), `HaLightsRepository`, `HaModesRepository`.
  Onglet **Réglages** : URL + jeton (stockage privé de l'app, jamais dans le
  code) avec bouton « Tester ». Un `BackendHolder` reconstruit les dépôts à
  chaque changement de réglages ; l'UI et les tuiles parlent à des dépôts
  « délégués » stables. Sans réglages → mode démo (mocks), utile hors LAN.
  Manifest : `usesCleartextTraffic` (HA en http sur le LAN).
- **Onglet TRMNL (cadré le 2026-09-07)** — gérer la rotation des écrans du TRMNL
  depuis l'app. Faisable : l'API compte TRMNL expose la playlist, endpoints listés
  dans [trmnl-dashboard.md](trmnl-dashboard.md) (section « L'API compte pilote la
  playlist »). L'app parle **directement à `trmnl.com`** (pas via HA : l'intégration
  HA officielle ne gère que batterie et sommeil). Contenu v1 :
  - une ligne par instance de plugin du compte (`GET /api/plugin_settings`), avec un
    **switch = dans la rotation ou pas** : item présent et `visible:true` → ON ;
    présent et `visible:false` → OFF (`PATCH /api/playlists/items/{id}`) ; absent de la
    playlist → OFF, l'allumer fait un `POST …/playlist_items` ;
  - réordonner par glisser-déposer → `PUT …/playlist_items/order` ;
  - en tête : batterie, dernier ping, **refresh** (choix 5/15/60 min → `PATCH
    /api/devices/{id}`) ;
  - un bouton « Uniquement celui-ci » qui masque tous les autres (l'équivalent
    pratique d'un « afficher X », faute d'endpoint « afficher maintenant »).
  - Limite à afficher dans l'UI : le changement n'est visible qu'au prochain check-in
    du device (15 min par défaut) — pas d'instantané, c'est le modèle pull du TRMNL.
  - Architecture : `data/trmnl/TrmnlClient` (OkHttp, même style que `HaClient`),
    `TrmnlRepository` (mock sans clé), clé `user_…` dans Réglages. Le device n'est
    pas choisi en v1 (un seul TRMNL) mais le code garde le `device_id`.
- **Mini player radio (2026-09-07)** : onglet **Radio** qui consomme les flux
  AzuraCast. Source = l'API publique `GET /api/nowplaying` (aucun jeton : liste
  des stations publiques, mount par défaut = URL du flux, artiste/titre/pochette,
  live, auditeurs). Lecture par **Media3** (ExoPlayer + `MediaSessionService`
  `player/RadioService`) : le flux survit à l'écran éteint, notification média
  standard, coupure au débranchement du casque, focus audio géré. L'UI ne parle au
  lecteur que via un `MediaController` — une seule vérité, la même pour la
  notification. Un flux live ne se « pause » pas : Stop arrête et libère le flux.
  URL du serveur dans Réglages (défaut `http://192.168.0.5`, testable sur
  `https://demo.azuracast.com`). Pochette chargée à la main via OkHttp — pas de lib
  d'images pour une vignette. Permission `POST_NOTIFICATIONS` demandée au premier
  Play (sans elle, la lecture marche mais la notification n'apparaît pas).
- ~~Le Mac de dev n'a ni JDK ni Android Studio ni SDK~~ → **poste de dev = PC Windows depuis le 2026-09-06**, outillage complet et wrapper Gradle commité, cf. [setup-dev-windows.md](setup-dev-windows.md). (Ancienne note : build à faire après
  installation d'Android Studio (le wrapper Gradle jar n'est pas commité,
  `gradle wrapper` le génère.)

## UI v1

- **Bottom bar, 4 tabs : Lights / TV / Radio / Réglages** (depuis le 2026-09-07).
- **Tab Lights** :
  - une rangée de **4 modes** (Mode 1 = tout on/off, Modes 2-4 = scènes ;
    appui = jouer, appui long = redéfinir ; le mode qui correspond à l'état
    des prises est mis en avant) ;
  - grille **4 colonnes × 2 rangées** (rangée A, rangée B) de **tuiles lampes**
    (ambre + ampoule pleine = allumée, gris + ampoule vide = éteinte). Hors
    édition, un appui bascule la prise ;
  - **un seul switch** (le rocker vertical `SocketSwitch`, maquette de François
    du 2026-09-07) pour le mode sélectionné : ON = jouer le mode (ses prises
    allumées, **les autres éteintes**), OFF = tout éteindre. Plus de switch « All »
    ni de bouton « Turn off » : le switch les remplace ;
  - **le mode sélectionné se déduit de l'état réel des prises** : si elles
    correspondent à un mode (joué depuis l'app, un MOES ou HA), c'est lui, switch ON ;
    sinon le dernier choix de l'utilisateur, switch OFF. L'app suit donc le mur en
    temps réel (WebSocket HA) ;
  - **appui sur un mode = le sélectionner et le jouer** ; **2e appui** sur le mode
    sélectionné (2-4, ou un mode encore vide) = **édition** : les tuiles deviennent
    des cases à cocher (A1, A3, B2…), Enregistrer (≥ 1 prise) ou Annuler → retour
    au switch avec ce mode sélectionné. Le mode 1 n'est pas éditable (= toutes les
    prises). Le switch est **couché, centré**, le nom du mode et ses prises dessous. Le filtre
    est stocké tel quel dans la scène HA `mode_n` (cochées → `on`, autres → `off`),
    donc rien ne change côté boutons MOES ;
  - la ligne d'état de la liaison HA (connecté / hors ligne / mode démo).
- **Tab TV** : placeholder en v1, spec dans [tv-mute.md](tv-mute.md) — le bouton
  central sera un gros **MUTE**.
- **Tab Radio** : carte « en cours » (pochette, station, artiste — titre, LIVE ·
  streamer, bouton Stop / spinner de connexion) + liste des stations (Play/Stop,
  titre en cours, nombre d'auditeurs). Un appui sur une station la joue, un appui
  sur celle qui joue l'arrête, une autre station bascule le flux.
- **Tab Réglages** : URL + jeton HA, « Tester », « Enregistrer » ; URL du serveur
  AzuraCast + « OK ». Pas de login.

## Architecture cible

```
[App Kotlin] --REST/WebSocket--> [Home Assistant (PC Docker)] --Wi-Fi--> multiprises Shelly
                                        ^-- Zigbee (ZHA) -- boutons MOES : touche n = Mode n
     |
     +--HTTP /api/nowplaying + flux MP3/Icecast--> [AzuraCast (PC Docker, :80)]
     |
     +--------ADB ou Android TV Remote protocol--> [Shield TV Pro] --CEC--> TV / barre TCL
```

- Lights : API REST HA (`POST /api/services/switch/turn_on`, entité par prise) +
  WebSocket pour l'état temps réel. Auth par long-lived access token. **Fait.**
- Modes : scènes HA `scene.mode_2..4` lues/écrites par `/api/config/scene/config/<id>`,
  jouées par `scene.turn_on` ; le mode 1 se calcule côté client. Côté MOES, la touche n
  est un **toggle** : « mode n actif » = sa scène est la dernière chose qui a touché
  aux prises (horodatage de `scene.mode_n` vs `last_changed` des prises, marge 5 s) et
  au moins une prise est allumée → tout éteindre ; sinon `scene.turn_on`. Aucun helper.
- Radio : `GET /api/nowplaying` (public) toutes les 15 s tant que l'onglet est
  affiché ; lecture ExoPlayer du mount par défaut. AzuraCast tourne sur la tour
  (`:80`, Docker WSL), cf. [hardware/design-serveur-azuracast.md](hardware/design-serveur-azuracast.md). **Fait.**
- TV : cf. [tv-mute.md](tv-mute.md), phase 2.
- Tout fonctionne **en LAN uniquement** en v1 (cf. [infra-reseau.md](infra-reseau.md)
  — le NordVPN du routeur ne donne pas d'accès entrant).

## Journal

### 2026-08-30
Création du doc. Cadrage v1 : Android/Compose, 2 tabs (Lights/TV), grille 2×4 +
All/Turn off, backend HA pressenti. TV repoussée en phase 2 (use case = mute).
Scaffold complet de l'app dans `apps/remote/` (Compose, bottom bar, grille 2×4,
All/Turn off, tab TV maquette MUTE, backend mocké derrière `LightsRepository`).
Non compilé : pas d'outillage Android sur le Mac — première étape de la
prochaine session dev = installer Android Studio et builder.

### 2026-09-06
Poste de dev = PC Windows : Android Studio, JDK 21, SDK 35 installés par winget +
sdkmanager ([setup-dev-windows.md](setup-dev-windows.md)) ; `gradlew` + jar commités.
Un Home Assistant de dev tourne en Docker Desktop (`features/home/ha/`) pour brancher le
vrai client HA sur l'intégration Demo en attendant le coordinateur Zigbee.
**Premier build réussi** : `gradlew assembleDebug` → `app-debug.apk` (15,6 Mo, 4 min à froid). HA de dev démarré (`portal6-ha`, http://localhost:8123), onboarding à faire.

**Renommage `remote` → `ha-remote`** : dossier `apps/ha-remote/`, projet Gradle
`Portal6HaRemote`, package et `applicationId` `com.portal6.haremote`, label de l'appli
`Portal6 HA Remote`. Motif : lever l'ambiguïté avec la télécommande TV/Shield — cette app
est le client Home Assistant du foyer. Le doc garde son nom (`app-remote.md`) : le sujet,
lui, n'a pas changé.

**Configs de pièce + tuiles des réglages rapides** : persistance locale
(`ConfigStore`), dépôts en singletons de process, tuiles « Salon » (défilement des
configs) et « Mute TV », `TvRepository` mocké, onglet TV rendu vivant. Build OK,
non encore testé sur device.

### 2026-09-07
**Backend réel disponible** : Home Assistant (`http://192.168.0.5:8123` sur le LAN)
expose `switch.multiprise_a_prise_1…4` et `switch.multiprise_b_prise_1…4` — les
identifiants codés dans `DefaultLights` depuis le scaffold, sans rien changer.
Multiprises = Shelly Wi-Fi (pas Zigbee), sans incidence pour l'app qui ne parle
qu'à HA. Prochaine étape dev : `HaLightsRepository` + écran Réglages (URL, token).

### 2026-09-07 (bis) — modes + client HA
Demande François : « 4 modes pour nos 4 boutons, noms génériques ; mode 1 = all
on/off, modes 2-4 à définir via l'app ». Implémenté : notion de **Mode** (remplace
les configs de pièce, `RoomConfig`/`Room` supprimés), scènes HA `mode_2..4`
comme source de vérité partagée avec les boutons MOES, **client HA réel**
(OkHttp REST + WebSocket), onglet Réglages, tuile « Modes » (cycle 2→3→4).
Côté HA : `features/home/ha/ha_modes_setup.py` (scènes + automatisations
touche n → mode n, idempotent). **Build OK** (`assembleDebug`, APK 17 Mo) après
une correction (fonctions locales mutuellement récursives dans `HaClient`).

### 2026-09-07 (ter) — test sur téléphone + design des interrupteurs
L'émulateur gèle → **Galaxy S20 Ultra (Android 13) branché en USB**, APK debug
installé et lancé via adb sans crash. Puis, sur maquette de François (rocker vertical
ambre), **`SocketSwitch`** remplace les cartes « ampoule » de la grille des prises ;
passage en 4 × 2 (rangée A / rangée B) parce que l'interrupteur est haut et étroit.

### 2026-09-07 (quater) — UX « un seul switch »
Demande François : modes en haut, tuiles lampes, **un seul switch**. Sélectionner un
mode 2-4 ouvre son édition, les tuiles servent de filtre, Enregistrer → le switch
pilote ce mode (ON = filtre allumé / reste éteint, OFF = tout éteint). Implémenté
sans toucher au modèle ni à HA (une scène = un filtre). Retiré : dialogue d'édition,
switch All, bouton Turn off. Testé sur le S20 Ultra : édition A1+A3+B2, ON, OFF.

### 2026-09-07 (nonies) — toggle MOES + app qui suit le mur
Demandes François : 2e appui sur une touche MOES = éteindre ; l'app doit refléter un
appui sur le MOES. HA : automatisation `bouton_n_modes` réécrite (touche n = toggle,
sans helper, sur les horodatages). App : mode sélectionné déduit de l'état des prises.
Testé en simulant `zha_event` par l'API : mode 2 → off → mode 2 → off, et l'app passe
sur Mode 2 / switch ON quand la touche 2 est jouée. APK publié via HA `/local/`.

### 2026-09-07 (octies) — « le MOES n'est pas synchro »
Faux positif : le logbook HA montre touche 3/4 → `scene.mode_3/4` activées, mais ces
scènes étaient vides (modes définis en mode démo, jamais poussés dans HA). Corrigé dans
l'app : une scène « tout off » est affichée « à définir » (et n'est plus « sélectionnée »
automatiquement quand tout est éteint).

### 2026-09-07 (septies) — app branchée sur le vrai HA
Jeton poussé par adb, app connectée : Mode 2 défini sur le téléphone se retrouve
dans `scene.mode_2` côté HA (A2, A3, B3). Boucle app ↔ HA ↔ MOES fermée côté données.

### 2026-09-07 (sexies) — skill `android`
Skill projet `.claude/skills/android/` : adb (détection, install, lancement, capture,
tap) et **pousser du texte dans un champ** via `input text` pour éviter la saisie au
clavier. Règle : un secret (jeton HA) n'est jamais lu/tapé par Claude — François lance
lui-même la commande qui lit `.env`.

### 2026-09-07 (quinquies)
Ajustements François : 1er appui sur un mode = le jouer (et le sélectionner), 2e appui
= éditer ; switch horizontal centré avec le label dessous. Corrigé : switch ON du mode 1
= tout allumer (et non basculer). Vérifié sur le téléphone.

### 2026-09-07 (nonies) — onglet TRMNL cadré
Demande : piloter la rotation des écrans TRMNL depuis l'app. Vérifié que l'API compte
le permet (playlist visible/order/schedule, refresh du device) → onglet TRMNL cadré
(switch par écran, ordre, refresh, « uniquement celui-ci »). Pas encore codé ; reste à
trancher schedule-ou-pas en v1 et la 2e clé dans Réglages (Questions ouvertes).

### 2026-09-07 (decies) — mini player AzuraCast
Demande François : « consommer les flux AzuraCast depuis l'app ». Onglet Radio,
`AzuraClient` (`/api/nowplaying`), `RadioService` Media3 (ExoPlayer + session média),
`RadioViewModel` (sondage 15 s + `MediaController`), URL AzuraCast dans Réglages.
Deux dépendances ajoutées (`media3-exoplayer`, `media3-session`), APK 20 Mo. Le serveur
de la maison n'a pas encore de station → **testé sur `demo.azuracast.com` depuis le
S20 Ultra** : liste, pochette, titre en cours, lecture (décodeur MP3 + notification
média vus dans logcat), Play/Stop alternés. Piège adb : rediriger `screencap` depuis
PowerShell corrompt le PNG (BOM) → passer par bash ; `wm dismiss-keyguard` déverrouille.
