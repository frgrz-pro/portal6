# features/trmnl — écrans e-ink TRMNL

Ce que portal6 fabrique pour le device TRMNL : les **private plugins** (templates
Liquid versionnés) et les **payloads** qui les alimentent. Le design, les décisions et
les questions ouvertes sont dans [.docs/trmnl-dashboard.md](../../.docs/trmnl-dashboard.md) ;
le setup du device (comptes `.sport` / `.case`) dans [.docs/trmnl.md](../../.docs/trmnl.md).

Device cible : **TRMNL OG, 800 × 480, 1-bit** (noir et blanc pur, pas de gris).

## Ce qui est codable, et ce qui ne l'est pas

| Étage | Dans ce repo ? |
|---|---|
| Plugins du catalogue (Google Calendar, météo…) | ❌ configuration web uniquement |
| Private plugins : markup + réglages | ✅ `plugins/<nom>/src/` |
| Données affichées | ✅ `data/` → `payloads/`, ou webhook |
| Playlist, refresh, sommeil du device | ❌ dashboard TRMNL |

Prérequis : l'addon **Developer Edition** (20 $ une fois) doit être actif sur le compte
TRMNL, sinon les private plugins n'existent pas.

## Les deux plugins

### `ciel-mer` — polling

Soleil (lever/coucher, golden hour, blue hour), Lune (phase, disque dessiné, pleines
lunes remarquables), marées, houle, météo. Toutes les données sont **publiques** :
`build_ciel_mer.py` écrit `payloads/ciel-mer.json`, le workflow le commite, et TRMNL
va le chercher sur `raw.githubusercontent.com` — même pattern que les .ics eSport,
zéro serveur.

Le soleil et la lune sont calculés localement par [`data/astro.py`](data/astro.py)
(NOAA + Meeus, sans dépendance) ; la météo, la mer et le niveau d'eau viennent
d'Open-Meteo (gratuit, sans clé).

### `agenda-semaine` — webhook

La semaine à venir, tous agendas confondus, une colonne par jour. Les événements sont
**privés** et le repo est public : rien n'est commité. `build_agenda.py` lit les
adresses iCal secrètes depuis des secrets GitHub et POSTe le payload directement à
TRMNL. Contrepartie : **2 Ko maximum par envoi**, d'où le rognage progressif
(`fit_payload`) qui coupe d'abord les titres, puis les événements, puis les jours.

## Développer un plugin

Le rendu se prévisualise en local avec [`trmnlp`](https://github.com/usetrmnl/trmnlp),
sans rien installer grâce à l'image Docker :

```bash
docker run --pull always --rm -p 4567:4567 \
  -v "$(pwd)/features/trmnl/plugins/ciel-mer:/plugin" trmnl/trmnlp serve --bind 0.0.0.0
```

Puis http://localhost:4567 — le serveur recharge à chaque sauvegarde d'un `.liquid`.

Pousser vers TRMNL (`TRMNL_API_KEY` depuis les réglages du compte) :

```bash
docker run --rm -e TRMNL_API_KEY -v "$(pwd)/features/trmnl/plugins/ciel-mer:/plugin" trmnl/trmnlp push
```

## Générer les données

```bash
npm run trmnl:ciel-mer          # écrit payloads/ciel-mer.json
npm run trmnl:agenda -- --print # payload agenda affiché, rien n'est envoyé
```

Le spot (latitude, longitude, fuseau) se règle dans [`data/config.json`](data/config.json).
⚠️ Ce fichier et le payload qu'il produit sont **commités dans un repo public** :
n'y mettre qu'un lieu public (plage, port), jamais les coordonnées du domicile.

## Arborescence

```
features/trmnl/
├── data/
│   ├── astro.py            éphémérides soleil & lune (sans dépendance)
│   ├── build_ciel_mer.py   → payloads/ciel-mer.json (polling)
│   ├── build_agenda.py     → webhook TRMNL (rien n'est écrit sur disque)
│   └── config.json         spot, fuseau — public, à renseigner
├── payloads/               payloads commités, servis par raw.githubusercontent.com
└── plugins/
    ├── ciel-mer/           .trmnlp.yml + src/{settings.yml, *.liquid}
    └── agenda-semaine/
```
