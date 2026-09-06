# Musique — relance du domaine

Doc vivant du domaine musique côté **logiciel** (ETL, `music.db`, bibliothèque `M:\music`,
streaming salon). Le pipeline et les scripts sont décrits dans le [README racine](../README.md) ;
la partie **hardware/serveur** (Brandt RK 711S, AzuraCast) a ses notes dans
[hardware/](../hardware/README.md) ; les anciennes notes datées sont dans `docs/`.

## Questions ouvertes

- [ ] **Quel axe relancer en premier ?** Trois fronts indépendants (voir ci-dessous) :
  (A) référentiel `music.db` complet sur cette machine, (B) streaming salon Plex +
  Symfonium, (C) serveur AzuraCast Phase 3. Proposition : A → B → C (A rend B et C
  meilleurs, B est le quick win d'usage).
- [ ] Où est l'export **`extract_spotify.xlsx`** ? Absent de `data/` sur cette machine
  (seul `library_scan.csv` y est). Le Google Sheet « extract spotify » existe toujours ?
  → ré-exporter en .xlsx et déposer dans `data/`.
- [ ] Le rapport de dédup du 2026-08-20 est **vide** (en-tête seul, aucun groupe) sur
  85 040 fichiers scannés : dédup effectivement propre, ou le run a été fait sur un
  sous-ensemble / avec un seuil trop strict ? À rejouer.
- [ ] Extended streaming history Spotify : demandée ? reçue ? (`exports/` est vide.)

## État au 2026-09-06

| Brique | État sur cette machine |
|---|---|
| `data/library_scan.csv` | ✅ 85 040 lignes, scan de `M:\music` du 2026-08-20 (tracks, workspace, playlists…) |
| `data/extract_spotify.xlsx` | ❌ absent → `music.db` ne peut être reconstruite que côté local, sans les 14 017 titres Spotify |
| `plugin/db/music.db` | ❌ jamais construite ici |
| Dédup locale | rapport vide (voir question) ; `_a_trier` existe déjà sur `M:` |
| `exports/` (historique d'écoute) | vide |
| Plex Media Server | tourne en natif sur la machine (avec xTeVe) — `M:\music` pas encore ajouté comme bibliothèque |
| AzuraCast | Phases 1–2 validées le 2026-08-20 ; **Phase 3 bloquée** : Docker n'est pas exposé dans WSL Ubuntu (intégration WSL à activer dans Docker Desktop → Settings → Resources → WSL integration) — vérifié à nouveau ce jour (`docker` introuvable dans la distro, `/var/azuracast` absent) |

`M:` contient : `music/` (library, playlists, tracks, workspace), `radio/`, `downloads/`,
`_a_trier/`.

## Les trois axes

**A — Référentiel complet (`music.db`)**
1. Ré-exporter le Sheet « extract spotify » → `data/extract_spotify.xlsx`.
2. `npm run build:db` → matching local ↔ Spotify (le README annonçait ~5 % de match :
   à mesurer ici, et c'est le vrai chantier — normalisation artiste/titre, fingerprint
   via `npm run fingerprint`).
3. Rejouer la dédup et comprendre le rapport vide.

**B — Streaming salon (Plex + Symfonium)** — choix actés le 2026-08-20
1. Ajouter `M:\music\library` (pas tout `M:\music` : le workspace est du brouillon)
   comme bibliothèque *Musique* dans Plex.
2. Symfonium sur le téléphone → serveur Plex. Zéro code.
3. Ce qui rend B meilleur : la structuration amont (tags propres, dédup) = l'axe A.

**C — Serveur AzuraCast, Phase 3** — plan dans
[hardware/design-serveur-azuracast.md](../hardware/design-serveur-azuracast.md)
1. Action utilisateur : activer l'intégration WSL Ubuntu dans Docker Desktop.
2. `./docker.sh install` canal Stable dans `/var/azuracast` (méthode validée).
3. STOP / VÉRIFIER Phase 3 (ports, interface accessible) avant toute station.

## Journal

### 2026-09-06
Création du doc (relance du domaine). État des lieux machine : scan local présent,
export Spotify absent, DB jamais construite, Plex natif sans bibliothèque musique,
Phase 3 AzuraCast toujours bloquée par l'intégration WSL de Docker Desktop. Trois axes
posés (A référentiel, B Plex/Symfonium, C AzuraCast), ordre proposé A → B → C.
