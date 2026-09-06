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
- [ ] **Marées : approximation ou source officielle ?** Aujourd'hui les heures de pleine
  et basse mer sont *dérivées* du niveau d'eau horaire d'Open-Meteo (précision ~±15 min,
  pas de coefficient). Si ça ne suffit pas → SHOM (officiel, payant) ou WorldTides
  (crédits bon marché, clé API à mettre en secret).
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

⚠️ `settings.yml` doit porter l'**`id`** du plugin côté TRMNL. Sans lui, chaque
`trmnlp push` crée un *nouveau* plugin au lieu de mettre à jour le sien — on se retrouve
vite avec dix doublons. L'id se récupère avec `trmnlp list` après la première création,
puis se commite. Les deux `settings.yml` du repo ont la ligne prête, commentée.

## Décisions

### Le pattern de données : GitHub sert les payloads, comme il sert les .ics

Pour toute donnée publiable, on **réutilise le pattern déjà en place pour les .ics
eSport** : un script Python génère un JSON, GitHub Actions le regénère par cron et ne
commite qu'en cas de vrai changement, et le consommateur va le chercher sur
`raw.githubusercontent.com`. Zéro serveur à héberger, zéro secret côté TRMNL,
historique des payloads en cadeau.

**Pour les données privées, ce pattern est interdit** : portal6 est public. L'agenda
perso passe donc en webhook — le payload va de GitHub Actions directement à TRMNL, sans
transiter par le repo, et les adresses iCal secrètes restent des secrets Actions.

Règle générale : **donnée publiable → polling sur un payload commité ; donnée privée →
webhook, en tenant dans 2 Ko.**

### Écran 1 — « Ciel & Mer » (polling)

Un seul écran qui répond à « qu'est-ce que le ciel et la mer font aujourd'hui » :

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

**Marées sans clé API.** Open-Meteo Marine expose `sea_level_height_msl`, un niveau
d'eau horaire qui *inclut la marée*. On en extrait les extremums, affinés par la
parabole passant par les trois points encadrants — précision de l'ordre de ±15 min.
Pas de coefficient de marée (ça, c'est du SHOM). Piste retenue faute de besoin plus
précis pour l'instant ; WorldTides reste l'option de repli, cf. Questions ouvertes.

**Disque lunaire.** Liquid ne sait pas faire de trigonométrie : le chemin SVG de la
partie éclairée est **précalculé en Python** et transmis dans le payload. Le terminateur
se projette en demi-ellipse de demi-axe `r·(2f−1)`, signé — positif en phase gibbeuse,
négatif en croissant. Rendu vérifié sur les 8 phases avant commit.

### Écran 2 — « Agenda semaine » (webhook)

Une colonne par jour sur 7 jours, tous agendas confondus, le jour courant en inverse
vidéo, un code court par agenda à côté de l'heure. Le plugin Google Calendar du
catalogue sait afficher *un* agenda proprement ; il ne sait pas fabriquer cette vue-là.

Les .ics sont lus avec `icalendar` + `recurring-ical-events` plutôt qu'avec un parseur
maison : les agendas réels sont pleins de règles de récurrence, et les développer à la
main est le genre de code qui a l'air de marcher jusqu'au premier événement mensuel.

Le rognage progressif imposé par la limite de 2 Ko sacrifie, dans l'ordre : la longueur
des titres, puis le nombre d'événements par jour, puis les derniers jours de la fenêtre.
Ce qui est coupé est compté et affiché dans la barre de titre (`+n masqués`) — un écran
qui ment sur ce qu'il montre serait pire qu'un écran incomplet.

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
