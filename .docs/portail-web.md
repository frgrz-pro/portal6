# Portail web — `apps/web/`

Vue web du hub : une entrée par domaine, en lecture seule sur ce que le repo sait.
Le code et le mode d'emploi sont dans [apps/web/README.md](../apps/web/README.md) ;
ici, les choix et ce qui reste à trancher.

## Questions ouvertes

- [ ] **Persistance des actions utilisateur** (groupes de playlists, maître par
  playlist) : `localStorage` aujourd'hui, export/import JSON. Où les ranger pour de
  bon — vault `data/music/`, ou table dans `music.db` ?
- [ ] **Backend ou pas ?** Le portail est statique ; les boutons Push / dédup
  affichent la commande. Le jour où on veut cliquer pour de vrai, il faut un petit
  serveur local (Flask/FastAPI dans le venv) qui lance les scripts. Pas avant que
  les scripts existent.
- [ ] Autres domaines (home, lieux, hardware, apps) : cartes grisées, à remplir quand
  il y a une donnée à montrer.

## Décisions

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

## Journal

### 2026-09-06
Création : accueil + inventaire de la donnée musique, puis vue mediacenter à la
demande de François (playlists, colonnes par plateforme, groupes, boutons Push /
dédup en mode « affiche la commande »). Vérifié dans le navigateur.
