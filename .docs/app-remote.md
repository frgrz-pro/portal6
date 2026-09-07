# App remote — télécommande à tout faire

Petite app mobile perso : piloter les lampes (multiprises Zigbee) et, en phase 2,
couper le son de la TV. Une seule app, minimale, qui remplace les télécommandes
pénibles et les apps constructeur.

## Questions ouvertes

- [ ] Le téléphone est-il Android ? (supposé oui vu le profil Kotlin — à confirmer,
  si iPhone dans le foyer → argument KMP)
- [x] ~~Mapping exact des 8 boutons~~ → **2 multiprises × 4 prises, confirmé
  (Shelly Power Strip 4 Gen4)** ; la grille 2 × 4 est la bonne.
- [x] ~~Noms des boutons : par lampe ou par prise ?~~ → **génériques, tranché le
  2026-09-07** : A1…A4 / B1…B4 restent les libellés ; le sens est porté par les
  configs de pièce (scènes nommées), pas par les boutons.
- [ ] **Tester sur le téléphone** : installer l'APK, onglet Réglages → URL
  `http://192.168.0.5:8123` + jeton → « Tester » puis « Enregistrer » ; vérifier
  que les 8 prises suivent HA en temps réel et que définir un mode dans l'app
  change bien ce que fait la touche du bouton MOES.
- [ ] Interprétation « 4 modes ↔ 4 boutons » : retenu **mode n = touche n**, identique
  sur chacun des 4 MOES (voir Décisions). Si François voulait plutôt « un MOES = un
  mode », seule l'automatisation HA change (`ha_modes_setup.py`), pas l'app.
- [ ] Tuile Modes : un appui passe au mode suivant parmi 2-4. Alternative si ça
  ne colle pas à l'usage : une tuile par mode.

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
- ~~Le Mac de dev n'a ni JDK ni Android Studio ni SDK~~ → **poste de dev = PC Windows depuis le 2026-09-06**, outillage complet et wrapper Gradle commité, cf. [setup-dev-windows.md](setup-dev-windows.md). (Ancienne note : build à faire après
  installation d'Android Studio (le wrapper Gradle jar n'est pas commité,
  `gradle wrapper` le génère.)

## UI v1

- **Bottom bar, 3 tabs : Lights / TV / Réglages** (depuis le 2026-09-07).
- **Tab Lights** :
  - une rangée de **4 modes** (Mode 1 = tout on/off, Modes 2-4 = scènes ;
    appui = jouer, appui long = redéfinir ; le mode qui correspond à l'état
    des prises est mis en avant) ;
  - grille **2 colonnes × 4 boutons** (toggle par prise, état on/off visible) ;
  - un **switch "All"** (tout allumer) ;
  - un bouton **"Turn off"** (tout éteindre d'un coup — le geste du soir) ;
  - la ligne d'état de la liaison HA (connecté / hors ligne / mode démo).
- **Tab TV** : placeholder en v1, spec dans [tv-mute.md](tv-mute.md) — le bouton
  central sera un gros **MUTE**.
- **Tab Réglages** : URL + jeton HA, « Tester », « Enregistrer ». Pas de login.

## Architecture cible

```
[App Kotlin] --REST/WebSocket--> [Home Assistant (PC Docker)] --Wi-Fi--> multiprises Shelly
                                        ^-- Zigbee (ZHA) -- boutons MOES : touche n = Mode n
     |
     +--------ADB ou Android TV Remote protocol--> [Shield TV Pro] --CEC--> TV / barre TCL
```

- Lights : API REST HA (`POST /api/services/switch/turn_on`, entité par prise) +
  WebSocket pour l'état temps réel. Auth par long-lived access token. **Fait.**
- Modes : scènes HA `scene.mode_2..4` lues/écrites par `/api/config/scene/config/<id>`,
  jouées par `scene.turn_on` ; le mode 1 se calcule côté client.
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
