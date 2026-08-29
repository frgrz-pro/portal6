# Portal6-home — domotique & affichage TRMNL

Deuxième domaine du monorepo (après la musique) : écosystème autour du/des écrans e-ink
**TRMNL** — affichage d'infos du foyer (calendriers, météo, marées, courses) et visualisation
de la domotique. Vision : un premier TRMNL généraliste pour découvrir et valider les use cases,
puis plusieurs devices spécialisés (les playlists sont par device, le mirroring existe).

## Ce qu'est un TRMNL (l'essentiel, vérifié août 2026)

- Écran e-ink **affichage uniquement** : le device tire une image du serveur, il n'envoie
  jamais de commande. Pas de tactile sur l'OG (1 bouton reset) ; le TRMNL X (10.3",
  avril 2026) a une bande gestuelle mais uniquement pour naviguer dans la playlist.
- **Widgets custom : oui, sans limite.** Le catalogue (~100 plugins officiels, 850+
  communautaires) est complété par les **Private Plugins** : templates Liquid + HTML/CSS
  (framework officiel `trmnl.com/framework`), alimentés par **polling** (TRMNL fetch notre
  URL : JSON, RSS, CSV) ou **webhook** (on pousse le JSON, max 12/h). Nécessite l'addon
  **Developer Edition — 20 $ une fois, par device** (un seul device Dev suffit pour créer
  les plugins, utilisables ensuite sur tous les devices du compte).
- Refresh minimum 15 min (5 min avec abonnement TRMNL+). Polling : 15/60/360/720/1440 min.
- **BYOS (Build Your Own Server)** : on peut pointer le device vers son propre serveur
  (implémentation officielle : Terminus, Ruby ; existe aussi en FastAPI/Next/Laravel).
  Zéro dépendance au cloud TRMNL, zéro limite — option pour plus tard, quand les use cases
  seront rodés et qu'on aura un serveur qui tourne (la tour Docker prévue pour AzuraCast).
- Multi-devices : playlist, refresh et sommeil **par device** ; Playlist Scheduler
  (créneaux horaires) ; Mashups (4 plugins par écran) ; mirroring parent → enfants.

## Use cases

### 1. Calendriers sport & eSport → Google Calendar « sport »

Les calendriers .ics classiques (foot, rally, F1) marchent déjà. Pour l'eSport, pas de
source ICS officielle. **Piste écartée après test (2026-08-29)** :
[snwfdhmp/esports-ics](https://github.com/snwfdhmp/esports-ics) (pont Liquipedia → ICS)
ne renvoie que les matchs du jour — la page Liquipedia:Matches qu'il scrape n'a aucune
profondeur de planning, et il ratait même des matchs LEC du jour. Inutilisable avec le
refresh ~24 h des ICS Google Calendar.

**Piste écartée n°2 (même jour) : les feeds per-team de
[snutij/esport_ics](https://github.com/snutij/esport_ics)** — qualité OK mais un .ics
par équipe crée des doublons dans l'agenda dès que deux équipes suivies se rencontrent
(KC vs G2 apparaît deux fois : Google ne dédoublonne pas entre agendas).

**Solution retenue : un .ics PAR LIGUE, généré par `calendars/esports/build_ics.py`**
(API non officielle lolesports, données Riot — planning complet par leagueId) :

- **saison régulière** : seulement les matchs des équipes suivies (clé `teams` de
  `leagues.json`, codes lolesports — ex. LEC → `["KC", "G2"]`) ;
- **phases finales** (Playoffs, Finals — tout blockName hors « Week N ») : bracket
  complet, placeholders `TBD vs TBD` inclus ; l'UID étant l'id du match Riot, l'event
  est mis à jour en place (pas dupliqué) quand les équipes se définissent.

Validé le 2026-08-29 sur la LCK (playoffs programmés : `TBD vs Gen.G` du 1/09 →
`TBD vs TBD — Finals` du 13/09). LEC seul actif dans `leagues.json` pour l'instant.

**Placeholders manuels** (`manual_events` dans `leagues.json`) : quand des dates de
phases finales sont annoncées publiquement mais pas encore chargées dans l'API Riot,
on les saisit à la main ; elles s'effacent automatiquement dès que l'API expose le
bracket officiel. En place pour les playoffs LEC Summer 2026 : Madrid 5-6/09 (Bo5 à
12h et 17h CEST le samedi, 17h le dimanche), finales à Nice 18-20/09 (Bo5 à 17h) —
source : annonce lolesports « 2026 LEC Summer: Format, Roadtrips, Tickets ». Jours
intermédiaires éventuels non annoncés, à compléter si besoin.

**Hébergement** : portal6 étant public sur GitHub, les .ics générés sont **commités**
dans `calendars/esports/ics/<jeu>/<ligue>.ics` et servis par raw.githubusercontent.com.
Le workflow `.github/workflows/esports-ics.yml` regénère 4×/jour (cron) et ne commite
que s'il y a un vrai changement (DTSTAMP stable = DTSTART, pas l'heure de génération).
Abonnement Google Calendar (une fois pour toutes) :
`https://raw.githubusercontent.com/frgrz-pro/portal6/main/home/calendars/esports/ics/lol/lec.ics`

Autres jeux (CS2 Vitality, RL KC/M8/Vitality, CoD M8) : brancher PandaScore tier
gratuit sur le même générateur, même logique par ligue/équipes.

Côté TRMNL : plugin **Google Calendar** officiel (OAuth) → le calendrier « sport »
s'affiche tel quel.

### 2. Domotique — multiprises Zigbee

**Piloter depuis le TRMNL : non** (architecture pull/display-only, aucun canal de commande).
**Afficher l'état : oui**, via Home Assistant :

- [usetrmnl/trmnl-home-assistant](https://github.com/usetrmnl/trmnl-home-assistant)
  (add-on officiel : screenshot d'un dashboard HA, dithering, push webhook), ou
- composants HACS qui poussent des états d'entités vers un private plugin webhook
  (trmnl-sensor-push, ha-trmnl-sensor-blaster).

Prérequis : un Home Assistant qui tourne (la tour Docker) + coordinateur Zigbee
(clé USB) + les 2 multiprises appairées. Le pilotage lui-même se fait depuis le
téléphone (app HA) ; le TRMNL affiche conso/états.

### 3. Météo + marées

- **Météo** : plugins météo natifs au catalogue TRMNL — zéro travail. Sinon private
  plugin sur [Open-Meteo](https://open-meteo.com) (gratuit, sans clé).
- **Marées** : pas de plugin France au catalogue (les recipes marées existants sont
  NOAA/US). Options, par ordre de préférence :
  1. **WorldTides API** (worldtides.info) — crédits très bon marché, données mondiales,
     private plugin en polling sur un petit endpoint à nous (ou payload statique regénéré
     par cron).
  2. API marine **Open-Meteo** (hauteur du niveau de la mer, gratuit) — courbe de marée
     approximative, suffisant pour un affichage.
  3. Scraping maree.info / données SHOM — fragile ou payant, en dernier recours.
  → à trancher quand on saura pour quel port/spot.

### 4. Courses & recettes

Deux étages :

- **Étage données (portal6)** : consolider un référentiel de recettes (même logique que
  la DB musique : vault → SQLite), enrichissement (ingrédients, saisons, tags), génération
  de menus et de listes de courses.
- **Étage usage** :
  - *Quick win sans rien construire* : **Todoist** (plugin TRMNL officiel + app mobile
    gratuite) comme liste de courses — le TRMNL affiche la liste, le téléphone la porte
    au supermarché, cochage en rayon.
  - *Cible* : petit service portal6 (API + page mobile) qui gère recettes + liste de
    courses ; le TRMNL la poll en private plugin, le téléphone y accède en PWA. La liste
    peut être générée depuis les recettes choisies pour la semaine.

## Roadmap

1. Découverte : compte TRMNL, addon Developer Edition (20 $), premier private plugin
   « hello world » pour valider le workflow Liquid/polling
2. Calendrier sport complet : abonnements esports-ics (Liquipedia) dans le Google
   Calendar « sport » + plugin Google Calendar sur le device
3. Météo (catalogue) + marées (WorldTides, private plugin)
4. Courses v1 avec Todoist (plugin catalogue + mobile)
5. Home Assistant sur la tour + clé Zigbee + affichage état des multiprises
6. Recettes : vault + DB + génération de listes de courses (v2 du use case courses)
7. Multi-TRMNL : specialisation par device (playlists dédiées), éventuellement BYOS/Terminus
