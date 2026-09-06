# Musique — relance du domaine

Doc vivant du domaine musique côté **logiciel** (ETL, `music.db`, bibliothèque `M:\music`,
streaming salon). Le pipeline et les scripts sont décrits dans le [README racine](../README.md) ;
la partie **hardware/serveur** (Brandt RK 711S, AzuraCast) a ses notes dans
[hardware/](hardware/README.md) ; les anciennes notes datées (`AAAA-MM-JJ-*.md`) sont ici aussi, à la racine de `.docs/`.

## Questions ouvertes

- [ ] **Ré-exporter Spotify avec les IDs de titres.** `item_row()` dans
  `export_library.py` jette `track.id`/`track.uri` : l'export n'a que artiste/titre/album,
  d'où `platform_refs` = 0. Sans IDs, ni push ni dédup Spotify. Un run API (Premium,
  quota 24 h) après une modif d'une ligne.
- [ ] **Périmètre du scan** : le scan courant ne couvre que `/mnt/m/music` ; le scan
  initial couvrait `/mnt/m` (`radio/Radio-Library` 575 fichiers, `downloads/` 146). Les
  réintégrer au référentiel ou les acter hors périmètre ?
- [ ] **Étendre `scan_library.py`** aux tags `genre`, `comment`, `grouping`, `bpm`,
  `rating` : c'est là que vivent les playlists automatiques iTunes « faites au tag »
  — aujourd'hui le scan ne les lit pas, on ne peut ni les inventorier ni les migrer.
- [ ] **Groupes de playlists** : pour l'instant dans le `localStorage` du navigateur
  (exportables en JSON depuis le portail). À rapatrier dans le vault
  (`data/music/playlist_groups.json`) puis dans la DB ?
- [ ] **Quel axe relancer en premier ?** Trois fronts indépendants (voir ci-dessous) :
  (A) référentiel `music.db` complet sur cette machine, (B) streaming salon Plex +
  Symfonium, (C) serveur AzuraCast Phase 3. Proposition : A → B → C (A rend B et C
  meilleurs, B est le quick win d'usage).
- [ ] **Rebuild `music.db`** : la base importée date du 2026-08-20 00:29 (scan
  pré-quarantaine, 88 223 fichiers). Relancer `npm run build:db` sur le scan post-quarantaine
  (85 041 lignes) et mesurer le taux de match local ↔ Spotify.
- [ ] Fingerprint (`npm run fingerprint`) : run interrompu à 3 420 / 44 529 fichiers
  (7,7 %, ~1,4 fichier/s, ETA ~8 h). Cache et `retag_plan.csv` importés → reprendre ou
  abandonner cette piste ?
- [ ] Extended streaming history Spotify : demandée ? reçue ? (`exports/` est vide.)
- [ ] Faut-il ajouter `.fingerprint_cache.json` et `retag_plan.csv` à la sauvegarde du vault ?
  (Le vault `data/` n'est ni versionné ni sauvegardé ailleurs que sur cette machine.)

## État au 2026-09-06 (après import de `spotify-toolkit`)

Le repo `C:\DevLab\spotify-toolkit` est l'**ancêtre** de portal6 (dernier commit du
2026-08-20 : « Renommage spotify-toolkit → portal6 »). Le code y est identique à portal6
(seules les fins de ligne diffèrent) ; ce qui manquait ici, c'était tout le **hors-git** :
`.env`, vault `data/`, bases SQLite. Importé ce jour — l'ancien repo peut être archivé.

**Convention de chemins** : les scripts (`build_db.py`, `dedup_library.py`,
`fingerprint_library.py`) lisent tous **`data/music/`**, pas `data/`. Le README racine
qui dit `data/library_scan.csv` est obsolète sur ce point.

| Brique | État sur cette machine |
|---|---|
| `.env` | ✅ importé (Spotify, Google service account, Last.fm, AcoustID…) |
| `data/music/extract_spotify.xlsx` | ✅ importé (export du 2026-08-19, 14 017 titres / 110 playlists) |
| `data/music/library_scan.csv` | ✅ scan **post-quarantaine** du 2026-08-20 23:36 (85 041 lignes), déplacé depuis `data/` |
| `data/music/library_scan.2026-08-19.pre-quarantaine.csv` | archive : scan initial (88 224 lignes) |
| Dédup locale | **Tranché** : le run du 2026-08-20 00:28 a produit 4 513 lignes = 2 049 groupes (un `keep` chacun) + 2 464 candidats mis en quarantaine (`*.2026-08-20-0028.*`), le `.ps1` a été exécuté (`_a_trier` sur `M:`), puis le rescan de 23:36 donne un rapport vide → la bibliothèque est propre |
| `plugin/db/music.db` | ✅ importée (build du 2026-08-20 00:29, **antérieure à la quarantaine** → à reconstruire) : 90 758 tracks, 88 223 files, 13 968 playlist_tracks, 13 917 enrichment, 0 platform_refs |
| `plugin/db/places*.db` | ✅ importées (domaine Lieux, voir [lieux.md](lieux.md)) |
| Fingerprint | cache + `retag_plan.csv` importés, run interrompu à 7,7 % |
| `exports/` (historique d'écoute) | vide |
| Plex Media Server | tourne en natif sur la machine (avec xTeVe) — `M:\music` pas encore ajouté comme bibliothèque. ⚠️ **Migration vers Docker préparée** ([plex-docker.md](plex-docker.md)) : créer la bibliothèque Musique **après** la bascule, directement dans le conteneur (chemin `/data/m/music/library`, à ajouter aux montages), sinon elle serait à refaire |
| AzuraCast | Phases 1–2 validées le 2026-08-20, **re-vérifiées le 2026-09-06** (méthode officielle inchangée, parc Docker sain, ports 80/443/2022/8000 libres, `/mnt/m` monté). ✅ **DÉPLOYÉ le 2026-09-06** (Phase 3 franchie) : conteneurs `azuracast` + `azuracast_updater`, canal stable, interface joignable sur `http://localhost` et `http://192.168.0.5`. Reste : créer le super-admin, puis **Phase 4** (brancher `M:\music` sans duplication). Détail : [hardware/design-serveur-azuracast.md](hardware/design-serveur-azuracast.md) |

`M:` contient : `music/` (library, playlists, tracks, workspace), `radio/`, `downloads/`,
`_a_trier/`.

## Les trois axes

**A — Référentiel complet (`music.db`)**
1. ~~Ré-exporter le Sheet « extract spotify »~~ → fait, importé depuis spotify-toolkit.
2. `npm run build:db` → matching local ↔ Spotify (le README annonçait ~5 % de match :
   à mesurer ici, et c'est le vrai chantier — normalisation artiste/titre, fingerprint
   via `npm run fingerprint`).
3. ~~Rejouer la dédup et comprendre le rapport vide~~ → expliqué (quarantaine déjà faite).

**B — Streaming salon (Plex + Symfonium)** — choix actés le 2026-08-20
1. Ajouter `M:\music\library` (pas tout `M:\music` : le workspace est du brouillon)
   comme bibliothèque *Musique* dans Plex.
2. Symfonium sur le téléphone → serveur Plex. Zéro code.
3. Ce qui rend B meilleur : la structuration amont (tags propres, dédup) = l'axe A.

**C — Serveur AzuraCast, Phase 3** — plan dans
[hardware/design-serveur-azuracast.md](hardware/design-serveur-azuracast.md)
1. Action utilisateur : activer l'intégration WSL Ubuntu dans Docker Desktop.
2. `./docker.sh install` canal Stable dans `/var/azuracast` (méthode validée).
3. STOP / VÉRIFIER Phase 3 (ports, interface accessible) avant toute station.

## Constats chiffrés (portail, 2026-09-06)

Mesurés par `apps/web/build_manifest.py` sur la DB du 2026-08-20 et le scan courant
(vue détaillée : `apps/web/music-data.html`).

- **Recoupement local ↔ Spotify : 4 408 titres sur 13 917 = 31,7 %** (le README disait
  ~5 % — faux). 9 509 titres Spotify n'ont pas de fichier ; 76 841 fichiers locaux sont
  hors Spotify.
- **Par playlist** : 4 playlists 100 % couvertes en local, 102 partielles, 4 à zéro.
  32 % des 13 968 entrées de playlists ont un fichier.
- **Tags locaux** : 44 439 fichiers sur 85 040 (52 %) sans artiste, même après devinette
  depuis le nom de fichier. 80 % des fichiers sont dans `workspace`, 20 % dans `library`.
  Le matching se fait sur artiste+titre normalisés → c'est la cause directe du 31,7 %.
- **Doublons Spotify** (onglet `doublons` de l'export) : 677 intra-playlist (à supprimer
  sans risque), 2 972 inter-playlists (pas forcément une erreur).
- **Enrichissement** : genres 95 %, pays 84 %, features audio ReccoBeats ~20 %.

## Modèle de sync — cible du mediacenter

Ce que François veut : une vue par playlist, une colonne par plateforme (Spotify /
local / iTunes), un **maître** par playlist et un bouton **push** qui propage. En local,
l'enjeu est de **créer une bibliothèque iTunes qui matche les références** du
référentiel, pas de réparer l'existante.

**Décisions**
1. **Le référentiel `music.db` est le pivot, jamais une plateforme.** Chaque plateforme
   est une projection : `platform_refs(track_id, platform, external_id)` porte les IDs
   Spotify, les chemins locaux (`files`) et, demain, les persistent IDs iTunes.
2. **Un maître par playlist**, choisi dans le portail ; par défaut Spotify (c'est la
   source actuelle). Le push va toujours *du maître vers les autres* — pas de fusion
   bidirectionnelle, trop de conflits silencieux.
3. **Push = script Python `--dry-run` d'abord**, jamais une action directe depuis le
   navigateur : le portail est statique, il affiche la commande et les prérequis.
4. **iTunes se construit depuis la DB**, pas depuis le scan : on génère les playlists
   (`.m3u`/XML) à partir des `files` matchés → pas de doublon par construction, et les
   tags « bricolés » de l'ancienne bibliothèque ne sont plus le support des playlists.
5. **Dédup** : local = déjà fait (quarantaine), à rejouer après chaque nouveau scan ;
   Spotify = intra d'abord (677), inter par groupe de playlists ensuite.

**Prérequis, dans l'ordre** : (1) ré-export Spotify avec IDs → `platform_refs` ;
(2) rebuild `music.db` sur un scan à périmètre tranché ; (3) scan étendu aux tags
genre/comment/grouping ; (4) script `plugin/etl/music/sync/push.py` ; (5) génération
iTunes.

## Journal

### 2026-09-06
Création du doc (relance du domaine). État des lieux machine : scan local présent,
export Spotify absent, DB jamais construite, Plex natif sans bibliothèque musique,
Phase 3 AzuraCast toujours bloquée par l'intégration WSL de Docker Desktop. Trois axes
posés (A référentiel, B Plex/Symfonium, C AzuraCast), ordre proposé A → B → C.

### 2026-09-06 (bis) — import de spotify-toolkit
Constat : `spotify-toolkit` = ancêtre de portal6, code identique, seul le hors-git
manquait. Importés : `.env`, `extract_spotify.xlsx`, caches fingerprint + `retag_plan.csv`,
rapport de dédup complet et liste de quarantaine (sous noms datés), scan pré-quarantaine
(archivé), Takeout Lieux, `music.db` / `places*.db`. Le scan récent a été déplacé de
`data/` vers `data/music/` (chemin lu par les scripts). Deux questions ouvertes tranchées
(export Spotify retrouvé, rapport de dédup vide = bibliothèque déjà nettoyée). Prochaine
étape axe A : `npm run build:db` sur le scan post-quarantaine.

### 2026-09-06 (ter) — point setup Docker / AzuraCast (axe C)
Inventaire Docker en lecture seule + re-vérification des sources officielles AzuraCast
(dépôt brut, le site répond 403 aux fetchs). Rien n'a changé côté projet : 2 services,
images `ghcr.io/azuracast/*`, `/var/azuracast` comme base, `docker.sh` refusant tout ce
qui n'est pas Linux/Darwin. Côté machine : `/var/azuracast` n'existe pas, aucun volume
`azuracast_*`, ports 80/443/2022/8000/8005 libres, Ubuntu WSL en 26.04 LTS avec 953 Go
libres, `/mnt/m` monté en 9p. **Le blocage est unique et inchangé : l'intégration WSL de
Docker Desktop n'est pas activée pour Ubuntu.** Séquence Phase 3 complète écrite dans
[hardware/design-serveur-azuracast.md](hardware/design-serveur-azuracast.md).

### 2026-09-06 (quater) — point data + portail web
Inventaire complet de la donnée musique (vault, DB, scans) mesuré par script, exposé
dans un portail statique `apps/web/` (accueil + `music.html` mediacenter + `music-data.html`
inventaire). Découvertes : recoupement réel 31,7 % (pas 5 %) ; l'export Spotify n'a pas
d'IDs de titres (cause de `platform_refs` = 0) ; le scan courant a rétréci le périmètre à
`m:/music` ; 52 % des fichiers sans artiste ; le scan ne lit pas genre/comment/grouping.
Modèle de sync posé (DB pivot, maître par playlist, push dry-run, iTunes généré depuis
la DB). Chiffre de dédup corrigé (2 049 groupes / 2 464 quarantainés).
