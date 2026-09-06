# Portal6 — instructions Claude

Portal6 est le **hub des besoins perso** de François : chaque besoin de la vie
courante est segmenté en domaine dans ce monorepo, avec l'objectif de se
simplifier la vie. Domaines actuels :

- **music** (`plugin/`, `data/`) — bibliothèque musicale unifiée (ETL, SQLite)
- **home** (`features/home/`) — domotique (Home Assistant `ha/`, Shield `tv/`), affichage TRMNL, calendriers sport/eSport
- **hardware** (`.docs/hardware/`) — web-radio Brandt RK 711S (notes de design uniquement)
- **apps** (`apps/`) — applications (`apps/ha-remote/`, télécommande Kotlin/Compose)

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
- **Git : ne jamais push depuis le shell.** Les pushes se font via **GitHub Desktop**
  (compte perso frgrz-pro) — GitKraken n'est plus installé sur ce poste.
- **Commit local : autorisé sans demander** (acté le 2026-09-06). Claude committe de
  lui-même le travail qu'il vient de produire, message en français préfixé du domaine.
  Deux règles : **ne committer que son propre périmètre** (`git commit -- <chemins>`,
  jamais un `git add -A` — d'autres sessions travaillent en parallèle et laissent des
  fichiers stagés), et **jamais de push**.
- Secrets : `.env` et `service-account.json` existent à la racine — ne jamais les
  committer ni les afficher.
- Task runner : npm ; les scripts restent en Python (venv hors repo, cf. `setup/`).

## Raccourcis

### `CQLS` — « c'est quoi la suite »

Déclencheurs : `CQLS`, « c'est quoi la suite », « la suite ? », « on fait quoi
maintenant ».

Réponse attendue : **la prochaine action, pas un récapitulatif.**

- Source = les sections **Questions ouvertes** et les phases **STOP / VÉRIFIER**
  des docs `.docs/` concernés (relus sur disque), pas la mémoire de la conversation.
- Séparer explicitement **🔴 action François** (ce qui bloque et que Claude ne peut
  pas faire : cliquer dans une UI, brancher, acheter, arbitrer) de **▶️ action
  Claude** (ce qui peut partir tout de suite).
- Une ligne par action, verbe à l'infinitif, avec le doc source en lien.
- Terminer par **une seule** recommandation : par quoi commencer, et pourquoi.
- Si plusieurs domaines sont en cours, les lister par domaine — ne pas fondre en
  une liste plate.
- Zéro re-explication de ce qui vient d'être fait.

## Ton of voice — réponses

Sauf demande explicite du contraire, réponses **synthétiques**, style log de
terminal : le user doit comprendre d'un coup d'œil ce qui se passe et comment
résoudre.

- Bullet lists courtes, pas de paragraphes narratifs.
- Blocs de phrase de 2 lignes max.
- Aller au fait : constat → action → next step. Pas de préambule, pas de récap
  de ce qui vient d'être fait si c'est visible.
- Détail long / explication de fond uniquement si demandé.
