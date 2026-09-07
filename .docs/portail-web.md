# Portail web — `apps/web/`

Vue web du hub : une entrée par domaine, en lecture seule sur ce que le repo sait.
Le code et le mode d'emploi sont dans [apps/web/README.md](../apps/web/README.md) ;
ici, les choix et ce qui reste à trancher.

## Questions ouvertes

- [ ] **Persistance des actions utilisateur** (groupes de playlists, maître par
  playlist) : `localStorage` aujourd'hui, export/import JSON. Où les ranger pour de
  bon — vault `data/music/`, ou table dans `music.db` ?
- [ ] **Backend ou pas ?** Le portail est statique ; les vues d'action produisent un
  **plan JSON** téléchargé, et affichent la commande. Le jour où on veut cliquer pour
  de vrai, il faut un petit serveur local (Flask/FastAPI dans le venv) qui lance les
  scripts. Pas avant que les scripts existent.
- [ ] **Scripts consommateurs des plans** — à écrire, ils n'existent pas :
  `plugin/etl/music/local/apply_dedup_plan.py` (quarantaine / suppression / ignore /
  référence), `plugin/etl/music/spotify/apply_dedup_plan.py` (intra, inter, ignore) et
  `plugin/etl/music/spotify/apply_playlist_plan.py` (create / move / remove). Le format
  du plan est fixé côté portail (voir Décisions) ; les scripts sont `--dry-run` par défaut.
- [ ] Le téléchargement du JSON passe par un `<a download>` : à vérifier dans le
  navigateur de François (le navigateur intégré de Claude bloque les téléchargements).
- [ ] **Portail joignable du téléphone** : servir `apps/web` depuis Windows sur
  `0.0.0.0:8712` (tâche planifiée comme le pont Zigbee) + règle pare-feu, pour que le
  launcher soit utilisable ailleurs que sur la tour.

## Décisions

- **L'accueil est un launcher.** `index.html` = une tuile par app de la maison
  (Home Assistant `:8123`, routeur Mercusys `192.168.0.1`, TRMNL, Music `:8712`,
  AzuraCast `:80`, Plex `:32400`), ouverte dans un nouvel onglet, avec un témoin
  vert/rouge par `fetch` en `no-cors` (réponse opaque = ça répond, erreur réseau =
  arrêté). Aucune donnée lue. Ajouter une app = une tuile dans `index.html`, pas de
  config séparée tant qu'il y en a moins d'une dizaine. Les tuiles sont rangées par
  **catégorie** (Home : HA, routeur, TRMNL ; Media : Playlist Manager, AzuraCast, Plex) —
  plus de cartes « domaines à venir », elles ne servaient à rien. `music.html` s'appelle
  **Playlist Manager** dans la nav.
- **Tuile « HA Remote » (catégorie Home) avec QR d'installation** (2026-09-07) :
  `publish_apk.py` copie l'APK debug dans **`features/home/ha/config/www/`** (servi par
  HA sur `http://192.168.0.5:8123/local/ha-remote.apk`) et génère le QR (`segno`, pure
  Python) + `apps/web/dl/apk.js` pour la tuile. **Pas via le portail** : le serveur 8712
  tourne côté WSL et n'écoute que sur `127.0.0.1` (invisible du LAN), alors que HA sur
  8123 est déjà joint par le téléphone. À relancer après chaque build.
- **Le QR pointe sur une page d'installation, pas sur l'APK** (`install.html`, copiée
  dans le `www` de HA) : elle détecte l'appareil — Android → téléchargement immédiat de
  l'APK ; iPhone → « pas encore d'app iOS » (HA Remote est Android-only) ; ordinateur →
  les deux. Le bouton iOS s'activera tout seul le jour où `www/ios/manifest.plist`
  existe (`itms-services://`, qui exige HTTPS + IPA signé ad hoc + Mac/Xcode + compte
  développeur Apple — rien de tout ça n'existe aujourd'hui).
- **Statique, zéro build, zéro framework.** Node n'est installé nulle part sur ce
  poste (ni Windows ni WSL) — les scripts `npm run` du `package.json` ne tournent
  pas ici. Python sert le dossier (`python -m http.server`).
- **La donnée est un `.js` généré** (`data/manifest.js` → `window.PORTAL6`), pas un
  `.json` : ça marche en double-clic (`file://`). Commité malgré son statut d'artefact
  dérivé : 55 Ko d'agrégats, lisible sans le vault, daté en pied de page.
- **Le générateur ne touche jamais `M:`** — il lit `music.db` et `data/music/`.
  Il ne sort que des agrégats (pas de chemins de fichiers musicaux).
- **Deux pages musique** : `music.html` = mediacenter (playlists × plateformes,
  groupes, push) ; `music-data.html` = inventaire brut de la donnée (vault, scans,
  tables, trous). La première est l'outil, la seconde le diagnostic.
- Le modèle de sync (maître, push, iTunes) est documenté dans
  [musique.md](musique.md), section « Modèle de sync ».
- **Vues d'action = liste de résolution + plan de changements.** Deux pages :
  `music-dedup.html` (fichiers locaux façon dupeGuru — groupe, ★ référence, action par
  copie ; Spotify intra ; Spotify inter avec playlists cliquables) et
  `music-proposals.html` (découpes des monolithes en volumes, déplacements des titres
  hors profil). Les deux partagent `assets/plan.js` : barre fixe « N actions en
  attente », **Commit changes** = revue façon plan Jira (regroupé par type, chaque
  ligne décochable), **Valider** = JSON téléchargé + commande + historique des plans
  (statut à appliquer / appliqué / abandonné, réouverture).
- **Le plan JSON est le contrat d'interface** entre le portail et les scripts :
  `{ id, title, created, status, items: [{ id, kind, label, detail, meta }] }`.
  `kind` ∈ dédup : `quarantine | delete | ignore | ref | intra | inter | spot_ignore` ;
  playlists : `create | move | remove`. `meta` porte ce dont le script a besoin (chemin,
  playlists à retirer / garder, titres d'une playlist à créer, cible d'un déplacement).
  Le navigateur n'écrit jamais ailleurs que dans `localStorage`.
- **Deux fichiers de détail non versionnés** à côté du manifeste : `data/dedup.js`
  (2,1 Mo, chemins de fichiers) et `data/proposals.js` (770 Ko, titres). Le manifeste
  agrégé reste commité ; les pages affichent « à générer » si le détail manque.
- **Dédup locale : le rapport daté sert de matière** tant que le rapport courant est
  vide (il a déjà été appliqué → les actions visent `_a_trier` : supprimer = purger,
  ignorer = restaurer). Un nouveau scan + dédup remplacera cette source.
- Les archives annuelles « (All) » sont exclues des décisions inter (grisées) : un
  titre présent dans « 2023 (All) » et une playlist thématique n'est pas un doublon.
- Perf : pas de `<select>` par ligne (366 × 110 options rendaient la page inutilisable)
  → une `<datalist>` partagée ; listes longues paginées par 150.

## Journal

### 2026-09-06
Création : accueil + inventaire de la donnée musique, puis vue mediacenter à la
demande de François (playlists, colonnes par plateforme, groupes, boutons Push /
dédup en mode « affiche la commande »). Vérifié dans le navigateur.

### 2026-09-06 (bis) — vues d'action
Ajout des vues Doublons (local / Spotify intra / inter) et Propositions (découpes,
déplacements) avec le moteur de plan partagé (`plan.js`) : marquer → Commit changes →
revue → JSON + commande + historique. Générateur étendu (lecture xlsx sans openpyxl,
`dedup.js` / `proposals.js` non versionnés). Vérifié dans le navigateur : 1 170
copies marquées d'un clic, plan de 5 changements validé, chip inter, création de
sous-playlist renommée, déplacement vers la reco. Le preview tourne sur 8713 (8712
occupé par le serveur WSL).

### 2026-09-07 — accueil launcher
François se perdait entre les URLs : `index.html` devient le hub de redirection vers
les six apps (HA, routeur, TRMNL, Music, AzuraCast, Plex) avec témoin de disponibilité.
Vérifié dans le navigateur : les six répondent, le témoin passe au rouge sur un port
fermé. Puis nettoyage à la demande de François : plus de texte d'intro, tuiles en deux
catégories Home / Media, section « domaines du repo » supprimée, Music renommé
Playlist Manager. Piège rencontré : `style.css` en cache navigateur → lien versionné.

### 2026-09-07 (ter) — QR de l'APK
Tuile HA Remote dans Home, QR à scanner depuis le téléphone. Premier essai via le portail
(8712) raté depuis l'iPhone : 8712 = WSL, `127.0.0.1` seulement. Bascule sur HA `/local/`
(`config/www` créé, HA redémarré une fois). `segno` ajouté au venv `portal6-home` et à
`requirements.txt`. Vérifié : QR chargé, `/local/ha-remote.apk` → 200, 17 Mo.

### 2026-09-08 — page d'installation par appareil
François a scanné le QR avec son iPhone et reçu l'APK. Le QR pointe désormais sur
`install.html` (servie par HA), qui aiguille selon l'appareil ; côté iOS, message honnête :
pas d'app iOS, et ce qu'il faudrait pour en installer une par lien. Vérifié dans le
navigateur (page servie, détection UA testée sur un UA iPhone et un UA Android).
