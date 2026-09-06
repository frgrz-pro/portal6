# features/trmnl — l'écran e-ink TRMNL

Ce que portal6 fabrique pour le device : **un private plugin unique**, le dashboard, et
le générateur du payload qui l'alimente. Le design et les décisions sont dans
[.docs/trmnl-dashboard.md](../../.docs/trmnl-dashboard.md) ; le setup du device (comptes
`.sport` / `.case`) dans [.docs/trmnl.md](../../.docs/trmnl.md).

Device cible : **TRMNL OG, 800 × 480, 1-bit** (noir et blanc pur, pas de gris).

## Ce qui est codable, et ce qui ne l'est pas

| Étage | Dans ce repo ? |
|---|---|
| Plugins du catalogue (Google Calendar, météo…) | ❌ configuration web uniquement |
| Private plugin : markup + réglages | ✅ `plugins/dashboard/src/` |
| Données affichées | ✅ `data/build_dashboard.py` |
| Playlist, refresh, sommeil du device | ❌ dashboard TRMNL |

Prérequis : l'addon **Developer Edition** (20 $ une fois) doit être actif sur le compte
TRMNL, sinon les private plugins n'existent pas.

## Le dashboard

Un seul écran, deux zones :

- **Colonne de gauche** — l'agenda de la semaine, 7 jours, tous agendas confondus, le
  jour courant en inverse vidéo (seul repère possible en 1-bit).
- **Reste de l'écran** — soleil (lever, coucher, golden hour, blue hour), lune (disque
  dessiné à la phase réelle, lunes remarquables), marées, houle et météo.

Le soleil et la lune sont **calculés localement** par [`data/astro.py`](data/astro.py)
(NOAA + Meeus, sans dépendance, sans quota). La météo, la mer et le niveau d'eau
viennent d'Open-Meteo (gratuit, sans clé) ; les heures de marée sont dérivées des
extremums de `sea_level_height_msl`.

### Pourquoi un gist secret

Un plugin TRMNL n'a **qu'une seule stratégie de données**, or le dashboard mélange du
public et du privé :

- **Webhook** exclu — le payload fusionné pèse ~3,2 Ko, la limite est de 2 Ko.
- **Polling sur ce repo** exclu — portal6 est public, l'agenda perso n'y a pas sa place.

→ Le workflow publie le payload dans un **gist secret** et TRMNL le poll à une URL non
listée, connue de lui seul. Même modèle de confiance que l'« adresse secrète iCal » de
Google, déjà utilisée en entrée. Pour un vrai contrôle d'accès, l'alternative est un
dépôt privé lu via `polling_headers` — cf. le doc de design.

## Développer le plugin

Prévisualisation locale avec [`trmnlp`](https://github.com/usetrmnl/trmnlp), sans rien
installer grâce à l'image Docker :

```bash
docker run --pull always --rm -p 4567:4567 -v "$(pwd)/features/trmnl/plugins/dashboard:/plugin" trmnl/trmnlp serve --bind 0.0.0.0
```

Puis http://localhost:4567 — rechargement à chaque sauvegarde d'un `.liquid`. `trmnlp
build --png` rend directement l'image 800 × 480 telle que le device l'affichera.

Pousser vers TRMNL (`TRMNL_API_KEY` = la clé **de compte**, préfixe `user_`, sur
`trmnl.com/account` — pas la clé de device) :

```bash
docker run --rm -e TRMNL_API_KEY -v "$(pwd)/features/trmnl/plugins/dashboard:/plugin" trmnl/trmnlp push
```

## Générer les données

```bash
npm run trmnl:dashboard -- --print              # affiche le payload, ne publie rien
npm run trmnl:dashboard -- --out preview.json   # écrit un fichier (jeu de preview)
npm run trmnl:dashboard                         # publie dans le gist (GIST_ID + GITHUB_TOKEN)
```

Le spot (latitude, longitude, fuseau) se règle dans [`data/config.json`](data/config.json).
⚠️ Ce fichier est **commité dans un repo public** : n'y mettre qu'un lieu public
(plage, port), jamais les coordonnées du domicile.

## Arborescence

```
features/trmnl/
├── data/
│   ├── astro.py             éphémérides soleil & lune (sans dépendance)
│   ├── build_ciel_mer.py     partie publique du payload (soleil, lune, mer, météo)
│   ├── build_agenda.py       lecture des .ics privés + rognage
│   ├── build_dashboard.py    fusionne les deux et publie dans le gist secret
│   └── config.json           spot, fuseau — public, à renseigner
└── plugins/dashboard/        .trmnlp.yml + src/{settings.yml, full.liquid}
```
