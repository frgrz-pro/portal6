# features/radio — grilles éditoriales des web-radios

L'**intention éditoriale**, versionnée. Le design et les décisions sont dans
[`.docs/hardware/design-programmation-editoriale.md`](../../.docs/hardware/design-programmation-editoriale.md) ;
ce dossier n'est que l'outillage.

> **Principe (doc §10) : le repo décide, AzuraCast exécute.**
> Les grilles vivent ici, pas dans AzuraCast. Une playlist AzuraCast est une liste de
> fichiers, pas une requête : la sélection éditoriale ne peut donc pas y vivre.
> Corollaire : **ne jamais modifier une grille dans l'interface AzuraCast** — le prochain
> push l'écraserait. On modifie le JSON, on repousse.

## Contenu

| Fichier | Rôle |
|---|---|
| `stations/midnight_club.json` | Grille Midnight Club — 8 playlists, 42 créneaux |
| `stations/stage_303.json` | Grille Stage 303 — 6 playlists, 42 créneaux |
| `push_schedule.py` | Traduit une grille en playlists + créneaux AzuraCast |

## Format d'une grille

Traduction fidèle du §19 de la passation : le JSON est la représentation exploitable de la
grille, pas une autre logique.

```json
{ "days": [1,2,3,4], "start": "20:00", "end": "21:00",
  "playlist": "signature", "energy": [3,4], "label": "Signature – Prime Time Session" }
```

- `days` : **1 = lundi … 7 = dimanche**
- `energy` : l'intention E1–E5. **Portée mais pas encore appliquée** (v1, voir plus bas).
- `playlist` : clé dans le bloc `playlists` du même fichier.
- Un bloc qui franchit minuit est **coupé en deux entrées** — AzuraCast n'accepte pas
  `end < start` sur un même jour. C'est pourquoi la signature Full Boost 23h–02h apparaît
  en `23:00→23:59` le vendredi et `00:00→02:00` le samedi.

## Usage

```bash
python features/radio/push_schedule.py stations/midnight_club.json --dry-run
python features/radio/push_schedule.py stations/midnight_club.json
python features/radio/push_schedule.py stations/midnight_club.json --fill
```

Le mode par défaut pose **playlists et créneaux** : il est indépendant des médias et
fonctionne même scan non terminé. `--fill` remplit en plus le contenu des playlists à
partir des préfixes `sources`, et exige des médias indexés.

Le script est **idempotent** : il retrouve les playlists par leur titre et les met à jour.

`--fill` vide la playlist (`DELETE …/playlist/{id}/empty`) puis assigne les médias par
**l'action batch** (`PUT …/files/batch` avec `{"do": "playlist", "files": [...],
"playlists": [id]}`), par paquets de 200 chemins. Ne pas revenir à
`POST …/playlist/{id}/import` : cet endpoint attend un fichier M3U en multipart et répond
`500 No "playlist_file" provided` sur une liste d'ids (constaté le 2026-09-08).

Prérequis : `AZURACAST_API_KEY` dans le `.env` à la racine (jamais commité).

## Détail vérifié empiriquement

Le format d'écriture des `schedule_items` n'est **pas** décrit dans l'OpenAPI d'AzuraCast
(`items: {}`). Il a été établi en créant une playlist jetable puis en la relisant :

```json
{"start_time": 2200, "end_time": 2359, "start_date": null,
 "end_date": null, "days": [1,2,3,4], "loop_once": false}
```

Les heures sont des **entiers `HHMM`**, pas des chaînes.

## État : v1 — sans granularité énergétique

Ce qui marche : la **forme** de la journée est respectée — interlude à 04h, signature à 20h,
live sets, replays, avec les bons poids et le bon ordre de lecture.

Ce qui manque, et pourquoi :

| Manque | Cause |
|---|---|
| Sélection par énergie E1–E5 | **aucun fichier n'a d'énergie** — les 895 fichiers longs n'ont aucune métadonnée (chantier §6.1 du doc) |
| Cooldown 7 jours sur la signature | AzuraCast n'a pas de cooldown ; il faudra le générateur (doc §10.2) |
| Sélection fine par genre | v1 sélectionne par **dossier** (`radio/`, `library/`, `downloads/`), faute de tags |

Le champ `energy` est **déjà porté par les fichiers de grille** : quand le tagging existera,
il sera lu sans refonte.

## Ce que le montage expose

Une station AzuraCast n'a **qu'une** storage location média. D'où le montage en arborescence
sous `/media/lib` :

| Dans AzuraCast | Hôte | Contenu |
|---|---|---|
| `/media/lib/radio` | `M:\radio` | 575 fichiers, dont **558 de +20 min** — les mixtapes |
| `/media/lib/library` | `M:\music\library` | 17 252 tracks tagués |
| `/media/lib/downloads` | `M:\downloads` | 143 DJ sets |

**`M:\music\workspace` est volontairement exclu** (67 763 fichiers de brouillon), ainsi que
`M:\_a_trier` (quarantaine de la dédup).
