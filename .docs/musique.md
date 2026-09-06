# Musique — relance du domaine

Doc vivant du domaine musique côté **logiciel** (ETL, `music.db`, bibliothèque `M:\music`,
streaming salon). Le pipeline et les scripts sont décrits dans le [README racine](../README.md) ;
la partie **hardware/serveur** (Brandt RK 711S, AzuraCast) a ses notes dans
[hardware/](hardware/README.md) ; les anciennes notes datées (`AAAA-MM-JJ-*.md`) sont ici aussi, à la racine de `.docs/`.

## Questions ouvertes

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
| Dédup locale | **Tranché** : le run du 2026-08-20 00:28 a trouvé 4 513 groupes / 2 463 fichiers à mettre en quarantaine (`*.2026-08-20-0028.*`), le `.ps1` a été exécuté (`_a_trier` sur `M:`), puis le rescan de 23:36 donne un rapport vide → la bibliothèque est propre |
| `plugin/db/music.db` | ✅ importée (build du 2026-08-20 00:29, **antérieure à la quarantaine** → à reconstruire) : 90 758 tracks, 88 223 files, 13 968 playlist_tracks, 13 917 enrichment, 0 platform_refs |
| `plugin/db/places*.db` | ✅ importées (domaine Lieux, voir [lieux.md](lieux.md)) |
| Fingerprint | cache + `retag_plan.csv` importés, run interrompu à 7,7 % |
| `exports/` (historique d'écoute) | vide |
| Plex Media Server | tourne en natif sur la machine (avec xTeVe) — `M:\music` pas encore ajouté comme bibliothèque |
| AzuraCast | Phases 1–2 validées le 2026-08-20 ; **Phase 3 bloquée** : Docker n'est pas exposé dans WSL Ubuntu (intégration WSL à activer dans Docker Desktop → Settings → Resources → WSL integration) |

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
