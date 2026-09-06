# App remote — télécommande à tout faire

Petite app mobile perso : piloter les lampes (multiprises Zigbee) et, en phase 2,
couper le son de la TV. Une seule app, minimale, qui remplace les télécommandes
pénibles et les apps constructeur.

## Questions ouvertes

- [ ] Le téléphone est-il Android ? (supposé oui vu le profil Kotlin — à confirmer,
  si iPhone dans le foyer → argument KMP)
- [ ] Mapping exact des 8 boutons : 2 colonnes = 2 multiprises ? Combien de prises
  pilotables par multiprise (2 ? 4 ?) et combien de lampes par prise ?
- [ ] Noms des boutons : par lampe ("Salon", "Biblio"…) ou par prise physique ?
- [ ] Backend lights : Home Assistant (cf. [zigbee-multiprises.md](zigbee-multiprises.md))
  — dépend de l'identification des multiprises.

## Décisions

- **v1 Android natif, Kotlin + Jetpack Compose (Material 3).** KMP reste ouvert
  (le cœur métier — clients HA/Shield — dans un module commun si on y va), mais on
  ne paie pas le coût multiplateforme tant qu'un seul téléphone est concerné.
- L'app ne parle **jamais Zigbee directement** (le téléphone n'a pas de radio
  Zigbee) : elle parle à un backend sur le LAN qui, lui, tient la radio.
  Backend pressenti : Home Assistant sur la tour Docker (déjà planifié dans
  `home/README.md`). Détail dans [zigbee-multiprises.md](zigbee-multiprises.md).
- Emplacement du code : `apps/remote/` (le dossier `apps/` du monorepo existe pour ça).
- **Scaffold créé le 2026-08-30** : projet Gradle single-module, package
  `com.portal6.remote`, minSdk 26 / target 35, Compose BOM + Material 3.
  L'UI est branchée sur un **`MockLightsRepository`** (état en mémoire) derrière
  l'interface `LightsRepository` — contrat : `lights: StateFlow<List<Light>>`,
  `toggle(entityId)`, `setAll(on)`. Le futur client HA implémentera la même
  interface (les `entityId` sont déjà au format HA `switch.multiprise_a_prise_1`).
- Labels des 8 boutons : placeholders A1-A4 / B1-B4 dans `data/Light.kt`
  (`DefaultLights`) — à renommer quand le mapping réel sera connu.
- Le Mac de dev n'a ni JDK ni Android Studio ni SDK : build à faire après
  installation d'Android Studio (le wrapper Gradle jar n'est pas commité,
  `gradle wrapper` le génère).

## UI v1

- **Bottom bar, 2 tabs : Lights / TV.**
- **Tab Lights** :
  - grille **2 colonnes × 4 boutons** (toggle par prise/lampe, état on/off visible) ;
  - un **switch "All"** (tout allumer) ;
  - un bouton **"Turn off"** (tout éteindre d'un coup — le geste du soir).
- **Tab TV** : placeholder en v1, spec dans [tv-mute.md](tv-mute.md) — le bouton
  central sera un gros **MUTE**.
- Pas de login, pas de settings élaborés en v1 : l'URL/token du backend en
  configuration simple (voire en dur au début, l'app ne sort pas du foyer).

## Architecture cible

```
[App Kotlin] --REST/WebSocket--> [Home Assistant (tour Docker)] --Zigbee--> multiprises
     |
     +--------ADB ou Android TV Remote protocol--> [Shield TV Pro] --CEC--> TV / barre TCL
```

- Lights : API REST HA (`POST /api/services/switch/turn_on`, entité par prise) +
  WebSocket pour l'état temps réel. Auth par long-lived access token.
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
