# Portal6 — instructions Claude

Portal6 est le **hub des besoins perso** de François : chaque besoin de la vie
courante est segmenté en domaine dans ce monorepo, avec l'objectif de se
simplifier la vie. Domaines actuels :

- **music** (`plugin/`, `data/`) — bibliothèque musicale unifiée (ETL, SQLite)
- **home** (`features/home/`) — domotique (Home Assistant `ha/`, Shield `tv/`), affichage TRMNL, calendriers sport/eSport
- **hardware** (`.docs/hardware/`) — web-radio Brandt RK 711S (notes de design uniquement)
- **apps** (`apps/`) — applications (`apps/remote/`, télécommande Kotlin/Compose)

## Méthodo de travail — .docs/ (IMPORTANT)

La façon de travailler de François : **des notes de design vivantes dans `.docs/`
à la racine**. Chaque session consiste à créer ou enrichir ces documents avant/en
plus du code.

- **En début de session, invoquer la skill `root-session`** (`.claude/skills/root-session/`)
  qui charge l'index `.docs/README.md` et applique le rituel.
- Un doc = un sujet ; décisions dans le corps, entrée datée dans la section
  **Journal** du doc, questions non tranchées dans **Questions ouvertes**.
- Nouveau sujet → nouveau doc + ligne dans l'index de `.docs/README.md`.
- **BOM central dans `.docs/bom.md`** : tout besoin/achat matériel de n'importe
  quel projet y est tracé (statut, prix, condition de déblocage).
- Les notes datées `AAAA-MM-JJ-*.md` à la racine de `.docs/` sont des archives (ex-`docs/`) :
  on les lit, on n'y écrit plus.

## Conventions

- **Langue : français** pour les docs, README, messages de commit.
- **Git : ne jamais push depuis le shell.** Les pushes se font via GitKraken
  (compte perso frgrz-pro). Committer localement est OK quand demandé.
- Secrets : `.env` et `service-account.json` existent à la racine — ne jamais les
  committer ni les afficher.
- Task runner : npm ; les scripts restent en Python (venv hors repo, cf. `setup/`).
