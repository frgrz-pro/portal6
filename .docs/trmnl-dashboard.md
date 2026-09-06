# TRMNL — construire le dashboard

Ce doc porte la **fabrication des écrans** : ce qui se code, comment, et les écrans
retenus. Le setup du device et les décisions de comptes sont dans [trmnl.md](trmnl.md) ;
les use cases et la fiche technique du TRMNL dans
[features/home/README.md](../features/home/README.md). Le code produit vit dans
[features/trmnl/](../features/trmnl/).

## Questions ouvertes

- [ ] **Quel spot pour la mer et les marées ?** `features/trmnl/data/config.json` attend
  une latitude/longitude. Deux points sont possibles : le point *à terre* (soleil, lune,
  météo) et un point *en mer* proche (Open-Meteo renvoie des nulls sur une maille
  terrestre). Rappel : ce fichier est commité dans un repo public → lieu public
  uniquement, jamais le domicile.
- [ ] **Marées : passer au SHOM ?** ⚠️ **Priorité relevée le 2026-09-06** après le
  premier run sur le vrai spot (Côtes-d'Armor, baie de Saint-Malo). Les extremums
  dérivés du niveau horaire d'Open-Meteo tombent à **5h25 / 7h13 / 5h25** d'intervalle
  au lieu des ~6h10 d'un régime semi-diurne : le calcul est correct (vérifié à la main
  sur la série brute), mais **l'échantillonnage horaire ne résout pas les pics aplatis**.
  L'erreur réelle est plutôt de ±30-40 min que des ±15 min annoncés. Sur une côte à
  12 m de marnage, ça ne suffit pas pour décider d'y aller. → viser le **SHOM**
  (officiel, coefficients inclus) ou WorldTides.
- [ ] **Température de l'eau douteuse.** Open-Meteo annonce ~21 °C sur ce point en
  septembre, là où la Côte d'Émeraude est plutôt à 17-18 °C. Maille SST trop grossière
  près de la côte. À recouper avant de faire confiance au chiffre affiché.
- [ ] **Quels agendas dans l'écran semaine ?** Un code court par agenda est affiché à
  côté de l'heure (`P` perso, `S` sport…). À arrêter en même temps que la question
  « un agenda sport ou plusieurs » de [trmnl.md](trmnl.md).
- [ ] **Fraîcheur météo.** Le payload est regénéré toutes les 3 h par GitHub Actions
  alors que TRMNL peut le poller toutes les heures : la température instantanée peut
  avoir jusqu'à 3 h. Acceptable pour l'instant ; sinon il faut un vrai endpoint
  (Cloudflare Worker, ou BYOS quand la tour Docker tournera).
- [ ] **Écran sport/eSport dédié ?** Les .ics sont déjà générés ; un private plugin
  ferait un bien meilleur rendu que la liste brute du plugin Google Calendar. Pas
  demandé pour l'instant, noté comme candidat n°3.
- [ ] Noms traditionnels des pleines lunes (lune des moissons, lune du loup…) : à
  ajouter ou pas ? La table est anglo-américaine, l'intérêt est décoratif.

## Ce qui est pilotable par le code (vérifié le 2026-09-06)

Trois étages, dont deux seulement vivent dans le repo :

| Étage | Codable ? | Où |
|---|---|---|
| Plugins du catalogue (Google Calendar, météo) | ❌ configuration web | dashboard TRMNL |
| **Private plugins** : markup Liquid + réglages | ✅ | `features/trmnl/plugins/` |
| **Données** affichées | ✅ | `features/trmnl/data/` |
| Playlist, refresh, sommeil, mashups | ❌ | dashboard TRMNL |

Deux contraintes structurantes :

- **Developer Edition obligatoire** (20 $ une fois) : sans l'addon, l'onglet Private
  Plugins n'existe pas. Décision : on la prend maintenant (BOM mis à jour).
- **Webhook plafonné à 2 Ko** par envoi (12 envois/h). Le **polling** n'a pas cette
  limite — c'est lui qu'on privilégie dès que la donnée est publiable.

### Les deux clés TRMNL — ne pas les confondre

Piège rencontré le 2026-09-06 : `trmnlp list` répondait `401 Invalid API key` avec une
clé pourtant valide. TRMNL expose **deux clés de nature différente** :

| Clé | Où | Sert à |
|---|---|---|
| **Clé de device** (avec le MAC) | `trmnl.com/devices/<id>/developer/edit` | lire/servir l'écran (`GET /api/display`), BYOS |
| **Clé de compte**, préfixe `user_` | `trmnl.com/account` | gérer les private plugins — c'est celle de `trmnlp` |

La clé de compte **n'apparaît qu'une fois la Developer Edition active**. Dans `.env`
(local, jamais commité) les identifiants du device sont donc nommés explicitement
`TRMNL_DEVICE_MAC` et `TRMNL_DEVICE_KEY`, pour que `TRMNL_API_KEY` reste réservé à la
clé de compte attendue par `trmnlp`.

### Outillage : `trmnlp`

⚠️ **`trmnlp build --png` rend en 1-bit par défaut.** Depuis le passage du device en
4 niveaux de gris, il faut `--color-depth 2`, sinon la preview dithère les gris en
damier : un fond gris avec du texte par-dessus y paraît illisible alors qu'il est net
sur l'écran. Constaté le 2026-09-06 — plusieurs itérations de mise en page ont été
jugées sur un rendu qui ne correspondait plus au device.

[`usetrmnl/trmnlp`](https://github.com/usetrmnl/trmnlp) est le serveur de dev officiel :
il rend les templates Liquid avec le design system TRMNL, en HTML et en PNG, avec
rechargement à la sauvegarde. Disponible en image Docker (aucune dépendance Ruby à
installer) et en gem. `trmnlp push` téléverse `src/` vers le compte TRMNL avec une
`TRMNL_API_KEY` — donc un plugin se versionne, se relit en diff, et se déploie en CI.

Structure imposée par l'outil, adoptée telle quelle :

```
plugins/<nom>/
├── .trmnlp.yml        config du dev local — jamais envoyée à TRMNL
└── src/
    ├── settings.yml   définition du plugin (nom, stratégie, URL, refresh) — envoyée
    └── *.liquid       un fichier par taille : full, half_horizontal, half_vertical, quadrant
```

⚠️ **`trmnlp push` réécrit `settings.yml`** dans la forme canonique du serveur : les
commentaires n'y survivent pas, et les champs vides du serveur y réapparaissent. Ne rien
documenter dans ce fichier — il est géré par l'outil, pas à la main.

⚠️ `settings.yml` doit porter l'**`id`** du plugin côté TRMNL. Sans lui, chaque
`trmnlp push` crée un *nouveau* plugin au lieu de mettre à jour le sien — on se retrouve
vite avec dix doublons. L'id se récupère avec `trmnlp list` après la première création,
puis se commite. Les deux `settings.yml` du repo ont la ligne prête, commentée.

## Décisions

### Un payload unique, publié dans un gist secret

Le premier réflexe était de réutiliser le pattern des .ics eSport : générer un JSON,
le commiter, le laisser servir par `raw.githubusercontent.com`. **Le choix d'un
dashboard unique l'interdit**, et le raisonnement mérite d'être tracé — un plugin TRMNL
n'a qu'**une seule stratégie de données**, or l'écran mélange du public (soleil, lune,
marées, météo) et du privé (agenda perso) :

| Piste | Verdict |
|---|---|
| Webhook (push vers TRMNL, rien ne transite par le repo) | ❌ payload fusionné **mesuré à ~3,2 Ko**, limite 2 Ko |
| Polling sur un payload commité | ❌ portal6 est public, l'agenda perso n'y a pas sa place |
| Polling sur un **gist secret** | ✅ retenu |
| Polling sur un dépôt privé + `polling_headers` | 🔒 plus sûr, plus lourd — repli si besoin |

**Retenu : gist secret.** Le workflow y publie `dashboard.json` ; TRMNL le poll à une
URL non listée, connue de lui seul. C'est le même modèle de confiance que l'« adresse
secrète au format iCal » de Google Calendar — déjà utilisée en *entrée* de la chaîne,
donc on n'ajoute pas d'hypothèse nouvelle. Si un vrai contrôle d'accès devient
nécessaire, le repli est un dépôt privé lu via `polling_headers` (en-tête
`Authorization`), qui gate réellement l'accès au lieu de miser sur l'obscurité de l'URL.

⚠️ Conséquence : **l'URL de polling ne doit pas être commitée**. Elle passe par un
champ personnalisé (`polling_url: "##{{ payload_url }}"`), dont la valeur vit dans
l'instance TRMNL — un `trmnlp push` ne l'écrase donc pas.

⚠️ **La syntaxe est `{{ champ }}`, pas `##{{ champ }}`.** La doc TRMNL note les
variables `##{{ api_key }}` dans ses exemples de polling URL ; le `##` est un artefact
de notation, pas de la syntaxe. Poussé tel quel, il est pris **littéralement** et
l'appel échoue avec `the url is not a complete http(s) url` — l'interpolation, elle,
avait bien eu lieu. Constaté le 2026-09-06 après un premier push erroné.

⚠️ **Piège de l'URL du gist.** Le `raw_url` que renvoie l'API GitHub contient un SHA de
version : `.../raw/d275acb…/dashboard.json`. Collée telle quelle, elle **fige l'écran sur
la version du jour**, définitivement. C'est l'URL **sans SHA** qu'il faut —
`.../raw/dashboard.json` — qui sert toujours la dernière révision.

### Agendas publics dans le repo, agendas privés dans l'environnement

Les agendas sport de François (foot, rugby, F1, WRC, escalade) sont des agendas Google
**publics** : leur URL n'est pas un secret. Ils vivent donc dans `config.json`, clé
`agendas`, versionnés et relisibles — avec le `code` court affiché à l'écran. Seuls les
agendas *privés* passent par `AGENDA_ICS_URLS`. La distinction est portée par
`read_sources()`, pas laissée à la discipline de l'utilisateur.

Règle générale, toujours valable pour le reste du repo : **donnée publiable → payload
commité ; donnée privée → jamais dans le repo.**

### L'écran unique — agenda à gauche, ciel et mer à droite

**Décision du 2026-09-06 (François) : un seul dashboard, pas deux écrans.** L'agenda
de la semaine occupe une colonne à gauche (~212 px), le reste de l'écran porte le ciel
et la mer :

| Bloc | Contenu |
|---|---|
| Soleil | lever, coucher, durée du jour, **golden hour** matin et soir, **blue hour** |
| Lune | disque dessiné à la phase réelle, % éclairé, prochaines PL/NL, **lune remarquable** à venir |
| Marées | les 4 prochaines pleines/basses mers, heure et hauteur |
| Mer & météo | houle (hauteur, période, direction), température de l'eau, air, vent, ciel |

**Le soleil et la lune sont calculés dans le repo, pas appelés.** `features/trmnl/data/astro.py`
implémente la position solaire NOAA et la théorie lunaire de Meeus tronquée à ses termes
principaux — aucune dépendance, aucun quota, ça marche hors ligne. Les heures viennent
d'un balayage minute par minute de l'élévation solaire aux seuils qui définissent
réellement ces moments : golden hour entre −4° et +6°, blue hour entre −6° et −4°,
lever/coucher à −0,833° (réfraction comprise).

**Lunes spéciales** dérivées des mêmes calculs : superlune (< 360 000 km), micro-lune
(> 405 000 km), lune bleue (2ᵉ pleine lune d'un mois civil) et **éclipse estimée** par
la latitude écliptique de la Lune à la pleine lune (|β| < 0,45° → totale, la « lune
rousse »). Cette dernière est une estimation, pas un calcul de contacts : elle est
marquée comme telle dans le payload et affichée avec la mention « estimation ».
Validation du 2026-09-06 : l'algorithme sort une éclipse pénombrale au 21/02/2027, qui
existe bien au catalogue.

**Coefficient de marée : calculé à Brest, pas localement.** Open-Meteo ne fournit
aucun coefficient. Mais le coefficient français est **défini à Brest et vaut pour toute
la côte** : `coef = 100 × marnage / (2 × U)`, avec `U = 3,05 m` l'unité de hauteur de
Brest. On récupère donc une seconde série de niveau d'eau à Brest et on en dérive un
coefficient par transition PM↔BM, rattaché aux marées locales par proximité temporelle.
Le calculer sur le marnage de Saint-Jacut aurait donné un nombre faux.

Validé le 2026-09-06 sur la forme de la courbe, qui est le vrai test : 40 le 6 septembre
(deux jours après le dernier quartier), montée régulière à 90 le 10, nouvelle lune le 11.
C'est exactement le cycle attendu. Reste **dérivé, pas officiel** : l'échantillonnage
horaire aplatit les pics, l'écart au SHOM est de l'ordre de 3 à 5 points.

**Régime affiché seulement quand il est notable.** Badge `MORTE-EAU` (coef < 46),
`VIVE-EAU` (≥ 90), `GRANDE VIVE-EAU` (≥ 100) ; rien entre les deux — un badge permanent
ne se remarque plus. Il se base sur la **prochaine** marée, pas sur le pic des 30 h
affichées : annoncer une vive-eau encore à deux jours serait un contresens.

**Basse mer en plein, pleine mer en contour.** C'est l'étale basse qui décide si on peut
sortir sur l'estran — à Saint-Jacut, l'accès aux Ébihens en dépend. C'est donc elle qui
doit sauter aux yeux, pas l'inverse.

**Marées sans clé API.** Open-Meteo Marine expose `sea_level_height_msl`, un niveau
d'eau horaire qui *inclut la marée*. On en extrait les extremums, affinés par la
parabole passant par les trois points encadrants — précision de l'ordre de ±15 min.
Pas de coefficient de marée (ça, c'est du SHOM). Piste retenue faute de besoin plus
précis pour l'instant ; WorldTides reste l'option de repli, cf. Questions ouvertes.

**Disque lunaire.** Liquid ne sait pas faire de trigonométrie : le chemin SVG de la
partie éclairée est **précalculé en Python** et transmis dans le payload. Le terminateur
se projette en demi-ellipse de demi-axe `r·(2f−1)`, signé — positif en phase gibbeuse,
négatif en croissant. Rendu vérifié sur les 8 phases avant commit.

### La colonne agenda

Sept jours empilés verticalement, tous agendas confondus, trois événements maximum par
jour, le jour courant en inverse vidéo — seul repère visuel possible en 1-bit. Le plugin
Google Calendar du catalogue sait afficher *un* agenda proprement ; il ne sait pas
fabriquer cette vue-là, ni la juxtaposer au reste.

Les .ics sont lus avec `icalendar` + `recurring-ical-events` plutôt qu'avec un parseur
maison : les agendas réels sont pleins de règles de récurrence, et les développer à la
main est le genre de code qui a l'air de marcher jusqu'au premier événement mensuel.

**Pas de quota par jour : un remplissage à la place disponible.** Liquid ne sait pas
mesurer du texte, et `overflow: hidden` coupe sans prévenir. `build_dashboard.py` estime
donc en amont le nombre de lignes de chaque événement (largeur du texte / caractères par
ligne) et remplit la colonne jusqu'au budget de 405 px. Deux calibrages appris au rendu :
l'écart entre journées vaut **18 px** (gap 9 + filet 2 + padding 7), et il ne passe que
**27 caractères par ligne**, pas 33 — le retour à la ligne se faisant sur les mots, le
remplissage réel est plus lâche que la largeur brute.

L'estimateur se trompe volontairement du côté prudent : mieux vaut un peu de blanc
qu'une ligne tronquée sur un écran qui se met à jour sans surveillance.

Ce qui ne rentre pas est **compté** et affiché (`+n autres`), pas silencieusement perdu :
un écran qui ment sur ce qu'il montre serait pire qu'un écran incomplet.

## Journal

### 2026-09-06
Création du doc et de `features/trmnl/`. Vérifié : private plugins = Liquid + HTML/CSS
versionnables, `trmnlp` (Docker) pour la preview locale et le push, Developer Edition
obligatoire, webhook plafonné à 2 Ko. Décidé le pattern « donnée publique → payload
commité + polling / donnée privée → webhook ». Deux écrans produits : **Ciel & Mer**
(soleil, golden hour, lune et lunes spéciales, marées, houle, météo — éphémérides
calculées dans le repo, marées dérivées d'Open-Meteo sans clé) et **Agenda semaine**
(7 jours, tous agendas, poussé en webhook depuis des secrets Actions). Géométrie du
disque lunaire validée visuellement sur les 8 phases. Reste bloquant : le spot
(lat/lon) et les URLs iCal.

### 2026-09-06 (bis) — les clés, et un piège évité
Device joignable et identifié : appel `GET /api/display` réussi (HTTP 200, firmware
`trmnl_og/FW1.8.16.bin` — l'OG est reconfirmé côté serveur). Mais `trmnlp list` renvoie
**401** : les identifiants disponibles sont ceux du **device**, pas ceux du **compte**.
D'où la section « Les deux clés TRMNL » et le renommage en `TRMNL_DEVICE_*` dans `.env`.
La clé de compte (`user_…`, sur `trmnl.com/account`) n'existera qu'après achat de la
Developer Edition — toujours le seul verrou. Découvert aussi : `settings.yml` doit porter
un `id`, sinon chaque push duplique le plugin ; la ligne est en place, commentée.

### 2026-09-06 (ter) — un seul dashboard
**Décision (François) : un écran unique, pas deux.** Les plugins `ciel-mer` et
`agenda-semaine` fusionnent en un seul plugin `dashboard`, agenda en colonne de gauche.
Conséquence non évidente : un plugin n'ayant **qu'une stratégie de données**, et le
payload fusionné pesant **~3,2 Ko** (mesuré) contre 2 Ko de limite webhook, le webhook
devient impossible — donc polling, donc une URL récupérable, donc **pas** le repo public
puisque l'agenda est privé. D'où le **gist secret**, et l'URL de polling qui ne doit pas
être commitée. Rendu validé en PNG via `trmnlp build` : mise en page corrigée trois fois
(débordements hors écran, texte noir sur fond noir dans les blocs inversés, largeur du
conteneur flex non contrainte).

### 2026-09-06 (quater) — le plugin existe
Dev Edition active : `trmnlp list` répond (plus de 401) et `trmnlp push` a créé
**`portal6 — Dashboard`, id 470032**. Confirmé par le serveur au passage : le schéma
`custom_fields` (`keyname` / `field_type` / `name` / `description`) est le bon, et
`polling_url: "##{{ payload_url }}"` est accepté — l'URL du gist secret restera donc
hors du repo. Appris : `trmnlp push` **réécrit `settings.yml`** (commentaires perdus),
et y inscrit lui-même l'`id`. Reste à brancher : gist + champ « URL du payload », spot,
et ajout à la playlist.

### 2026-09-06 (quinquies) — la chaîne est bouclée
Le dashboard tourne de bout en bout : `build_dashboard.py` → gist secret → polling TRMNL
→ écran du device. Vérifié depuis l'API device (`filename` renouvelé, image passée de
2041 à 7046 octets, toutes les valeurs présentes) et pas seulement en rendu local.
Spot **Saint-Jacut**, 10 agendas publics, 13 événements sur 7 jours.
Deux erreurs corrigées en route, toutes deux issues d'une lecture trop littérale de la
doc : le `##` de `##{{ champ }}` n'est pas de la syntaxe, et `past_days: 1` décale les
tableaux journaliers d'Open-Meteo (les min/max affichaient la veille).
Reste à valider : le workflow GitHub Actions n'a pas encore tourné pour de vrai.
