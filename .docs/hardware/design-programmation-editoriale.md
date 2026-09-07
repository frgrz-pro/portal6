# Design — Programmation éditoriale des stations

**Owner :** François Grzybowski
**Statut :** Stations 1 et 2 définies (grilles + JSON) · Station 3 **en conception**
**Date :** 2026-08-12
**Piste :** SERVEUR — l'infrastructure est décrite dans
[design-serveur-azuracast.md](design-serveur-azuracast.md)

> **À LIRE EN PREMIER.**
> Cette note porte **l'éditorial**, pas la technique : ce que les radios diffusent, quand, et
> pourquoi. Elle doit rester **indépendante du moteur** — AzuraCast/Liquidsoap est
> l'exécutant, pas le cadre. Une grille décidée ici doit pouvoir survivre à un changement
> d'outil.
> Les décisions marquées **figée** ne se rediscutent pas sans raison ; celles marquées
> **proposition** attendent un arbitrage.

---

## 0. La philosophie, en un paragraphe

Des web-radios **H24**, chacune avec une **identité musicale et visuelle propre**. Le but
n'est **pas** de reproduire une radio FM (animateurs, jingles toutes les quinze minutes) mais
de créer une **vibe continue et immersive**. Les **mixtapes et DJ sets longs (1–2 h)** forment
la colonne vertébrale ; les **tracks individuels** aèrent, font varier l'énergie et créent les
transitions. Chaque station possède un **créneau signature** qui met en avant un contenu
particulier.

---

## 1. L'échelle d'énergie E1–E5 — le langage commun

**Figée.** C'est le concept structurant de tout le projet éditorial.

| Niveau | Signification |
|---|---|
| **E1** | Très chill / ambient / contemplatif |
| **E2** | Chill / deep / downtempo |
| **E3** | Groove / énergie moyenne |
| **E4** | Énergique / dancefloor |
| **E5** | Peak / très intense |

Elle sert à : éviter les ruptures brutales · dessiner une **courbe énergétique quotidienne** ·
pondérer les sélections · choisir le bon contenu pour un créneau. Règle implicite : **pas de
E1 → E5 sans transition**.

> **Elle sert aussi au poste.** L'énergie du créneau en cours peut piloter la famille de
> presets et la palette du visualiseur
> ([design-visualiseur.md](design-visualiseur.md)) — un objet qui *sait* ce qu'il joue.
> Cela suppose que l'énergie soit **exposée** par le serveur : ce n'est pas le cas
> aujourd'hui (Q5).

---

## 2. Station 1 — Midnight Club

**Statut :** grille définie · `midnight_club_schedule.json` généré · station créée dans
l'ancienne installation LXC.

**Identité.** Référence aux *Midnight Club*, cercles de course automobile underground
japonais. Univers : Tokyo underground racing, cyberpunk, dystopie, *Ghost in the Shell*,
conduite nocturne, néons, autoroutes urbaines.

**Genres :** Phonk · Vaporwave · Liquid Drum & Bass.
Le phonk est central (grosse collection de tracks individuels) ; les mixtapes restent la
majeure partie de la bibliothèque.

**Principe :** *univers > genre*.

### Signature

| Jours | Créneau | Énergie | Concept |
|---|---|---|---|
| Lun → Jeu | **22h–00h** | E3–E4 | **Night Drive** |
| Ven → Sam | **23h–02h** | E4–E5 | **Full Boost** |
| Dimanche | **21h–23h** | E2–E3 | **Neon Wind-down** |

Structure d'un signature :

```
Opener → Long mix / mixtape → Sweeper / ID → Track segment → Closer
```

**Règle :** ne pas rejouer le même mix de signature avant **7 jours**.

### Grille

**Lundi → Jeudi**

```
00h–02h  Mixtape Phonk dark/racing
02h–03h  Tracks Phonk random pondéré
03h–05h  Mixtape Vaporwave dreamy/ambient
05h–06h  Tracks Phonk downtempo
06h–08h  Mixtape LiquidDnB smooth
08h–10h  Mixtape Vaporwave/Phonk alternance
10h–12h  Replay Signature veille
12h–14h  Mixtape Phonk standard
14h–16h  Tracks LiquidDnB + Vaporwave
16h–18h  Mixtape Vaporwave retro/dystopie
18h–20h  Mixtape Phonk boost progressif
20h–21h  Tracks Phonk random pondéré
21h–22h  Mixtape LiquidDnB groove nocturne
22h–00h  Signature – Night Drive
```

**Vendredi → Samedi**

```
00h–02h  Mixtape Phonk dark/racing
02h–03h  Tracks Phonk
03h–05h  Mixtape Vaporwave dreamy
05h–06h  Tracks Phonk downtempo
06h–08h  Mixtape LiquidDnB
08h–10h  Mixtape Vaporwave/Phonk alternance
10h–12h  Replay Signature veille (vendredi)
12h–14h  Mixtape Phonk
14h–16h  Replay Signature veille (samedi)
16h–18h  Mixtape Vaporwave
18h–20h  Mixtape Phonk boost
20h–21h  Tracks Phonk
21h–23h  Mixtape LiquidDnB
23h–02h  Signature – Full Boost
```

**Dimanche**

```
00h–02h  Mixtape Phonk dark/racing
02h–03h  Tracks Phonk
03h–05h  Mixtape Vaporwave dreamy
05h–06h  Tracks Phonk downtempo
06h–08h  Mixtape LiquidDnB
08h–10h  Mixtape Vaporwave/Phonk alternance
10h–12h  Replay Signature veille
12h–14h  Mixtape Phonk
14h–16h  Replay Signature veille
16h–18h  Mixtape Vaporwave
18h–20h  Mixtape Phonk boost progressif
20h–21h  Tracks Phonk
21h–23h  Signature – Neon Wind-down
23h–00h  Tracks Vaporwave downtempo
```

### Arborescence

```
/MidnightClubRadio
├── Mixtapes/{Phonk, Vaporwave, LiquidDnB}
├── Tracks/{Phonk, Vaporwave, LiquidDnB}
├── Signature/{Openers, Closers, Sweepers, Mixes, Replays}
└── Metadata/{schedule.json, weights.json, history.log}
```

---

## 3. Station 2 — Stage 303

**Statut :** grille définie · `stage_303_schedule.json` généré.

**Identité.** *Stage* → performances live et showcases ; *303* → la Roland TB-303, donc la
culture électronique / acid house.

**Contenu :** performances électroniques live — Boiler Room, Cercle, DJ sets, showcases.
Genres très variés : house, techno, UK bass, electronica…

**Principe :** *performance > genre*. Contrairement à Midnight Club, **on ne construit pas la
programmation autour des genres** : le point commun est « musique électronique + performance
live », et c'est le **type de performance et son énergie** qui comptent.

### Échelle d'énergie, déclinée

| Niveau | Contenu |
|---|---|
| E1 | Ambient / très calme |
| E2 | Deep / smooth / downtempo |
| E3 | Groove / house / UK bass modéré |
| E4 | Techno / bass / peak-time |
| E5 | Très intense / hard / industrial / gros peak |

### Signature — **1 h** (décision figée, après réduction)

| Jours | Créneau | Énergie | Concept |
|---|---|---|---|
| Lun → Jeu | 20h–21h | E3–E4 | Prime Time Session |
| Ven → Sam | 20h–21h | E4–E5 | Prime Time Session |
| Dimanche | 20h–21h | E2–E3 | Neon Wind-down |

### Grille

**Lundi → Jeudi**

| Heure | Bloc | Énergie |
|---|---|---|
| 00–02 | Live Set Techno/House | E4–E5 |
| 02–04 | Live Set Bass/Hybrid | E3–E4 |
| 04–05 | Interlude Ambient/Crowd | E1–E2 |
| 05–07 | Live Set Deep House/Chill | E2–E3 |
| 07–09 | Replay Signature | E3–E4 |
| 09–11 | Live Set | E2–E3 |
| 11–13 | Live Set | E3 |
| 13–14 | Interlude | E1–E2 |
| 14–16 | Live Set | E2–E3 |
| 16–18 | Live Set | E3–E4 |
| 18–20 | Live Set | E3–E4 |
| **20–21** | **Signature** | **E3–E4** |
| 21–23 | Live Set | E3–E4 |
| 23–00 | Interlude / short tracks | E2–E3 |

**Vendredi → Samedi** — même structure, avec 00–02 en E4–E5, 02–04 en E4–E5,
**Signature 20–21 en E4–E5**, 21–23 en E4.

**Dimanche** — courbe plus douce : **Signature 20–21 en E2–E3**, 21–23 en E2–E3,
23–00 en E1–E2.

### Arborescence

```
/Stage303
├── Performances/            ← tous les lives, pas de sous-dossiers par genre
├── Signature/{Sets, Openers, Closers, Sweepers, Replays}
├── Interludes/{Ambient, Crowd, ShortTracks}
└── Metadata/{schedule.json, history.log, weights.json}
```

---

## 4. Station 3 — Hip-Hop (sans nom)

**Statut : en conception.** C'est la station à laquelle François tient particulièrement.

**Principe :** *culture / époque > genre*.

### Contenu

| Bloc | Détail |
|---|---|
| **Cœur — Hip-Hop US 90s** | Golden Era, Boom Bap, East Coast, West Coast, classiques, underground 90s |
| **Rap français historique** | IAM, NTM, Fonky Family, Lunatic… |
| **Récent, en proportion nettement moindre** | Rap FR (Jul, Ninho, PNL…) et US (Drake, Meek Mill, Travis Scott, cloud rap, trap moderne) |

### ⚠️ Décisions à respecter

1. **Le créneau rap FR récent fait 1 heure maximum**, pas 2. (Correction explicite d'une
   proposition antérieure qui prévoyait 16h–18h.)
2. **La répartition 50 % US 90s / 30 % FR oldschool / 20 % récent est une estimation, PAS une
   décision.** Elle sera arbitrée quand les volumes réels de la bibliothèque seront connus
   (§4.1).
3. La philosophie tient : **majoritairement patrimoine / 90s, petite ouverture moderne**.

### 4.1 Ce qu'il faut avant de construire la grille

Volumes approximatifs par catégorie : `US 90s` · `FR oldschool` · `US récent` · `FR récent` ·
`Mixtapes` · `Tracks` — plus les éventuelles préférences par artiste ou époque.

### 4.2 Signature — proposition, non figée

Une **Theme Hour** 20h–21h, thème différent par jour :

| Jour | Thème |
|---|---|
| Lundi | Golden Era Hour |
| Mardi | FR Legacy |
| Mercredi | New Blood |
| Jeudi | Cloud Hour |
| Vendredi | West Coast Special |
| Samedi | East Coast Special |
| Dimanche | FR Chill Sunday |

À vérifier : le signature doit-il rester **1 h** comme sur les deux autres stations ? (cohérence
inter-stations → probablement oui)

### 4.3 Le nom — **aucun choisi**

Pistes proposées, à reprendre :

| Famille | Pistes |
|---|---|
| Golden Era | Boom Bap Radio · Golden Era FM · Cipher 90 · Mic Check · The Breaks |
| Rap français | Hexa Flow · 93 BPM · Rap Citadel · Banlieue Beats · L'Âge d'Or |
| Underground | Back2Back · Block Party Radio · Street Cipher · Tape Deck · Raw Verse |
| Pont ancien/moderne | Legacy & Future · Rhymes & Waves · Urban Archives · Cloud & Boom · Next Block |

### 4.4 Ordre de travail

1. Choisir le **nom**
2. Établir les **volumes** de la bibliothèque (§4.1)
3. Arrêter la **répartition** entre catégories
4. Définir la **courbe énergétique** E1–E5
5. Définir le **signature 1 h**
6. Construire la grille lundi → dimanche
7. Générer `hiphop_schedule.json`

---

## 5. Modèle de données

Le moteur de sélection visé :

```
Station → Schedule → Slot → Content type → Genre / Era / Mood / Energy
       → Candidate files → Weighted selection → History / cooldown → Playback
```

Contrainte de créneau :

```json
{ "type": "tracks", "energy_min": 3, "energy_max": 4, "era": "90s", "culture": "US", "weight": 0.7 }
```

Métadonnées par fichier :

```json
{
  "file": "...", "type": "track", "station": "hiphop",
  "culture": "US", "era": "90s", "genre": "boom_bap",
  "energy": 4, "mood": "classic", "weight": 1.0
}
```

```json
{
  "file": "...", "type": "mixtape", "station": "stage303",
  "energy": 4, "duration": 7200, "source": "live", "weight": 1.0
}
```

---

## 6. Chantiers non implémentés

### 6.1 Tagging automatique de l'énergie

Éviter de tout tagger à la main. Outils : **librosa**, **essentia** — BPM, loudness, RMS,
centroïde spectral, durée, éventuellement genre.

> **Mise en garde à conserver :** l'énergie musicale **ne se résume ni au BPM ni au volume**.
> La bonne approche est **analyse automatique + validation éditoriale**, pas l'automatisme
> seul. Un morceau lent et écrasant est E4, pas E2.

### 6.2 Système de « like »

```
Like élevé → poids de sélection augmenté → morceau plus souvent diffusé
```

À construire comme une **couche externe** (API + base de poids) plutôt qu'en modifiant
AzuraCast. Le poste est le candidat naturel pour émettre les likes — une touche cassette
libre ferait un excellent bouton « j'aime ».

### 6.3 Hub / player

Voir toutes les radios sous forme de grille :

```
AzuraCast → flux → M3U / XMLTV → xTeVe → Plex
```

Un générateur XMLTV dynamique construit à partir des grilles AzuraCast donnerait un vrai guide
de programmes. Non commencé.

### 6.4 Hors périmètre

Le besoin **YouTube / RSS** (suivre uniquement ses abonnements, feed propre, vu/non-vu,
lecture intégrée ; Inoreader étudié mais non self-hosted) est **un autre projet**. Noté ici
pour mémoire, à sortir de ces notes s'il prend forme.

---

## 7. Questions ouvertes

1. **Q1 — Nom de la station 3.** Bloque toute la suite de §4.4.
2. **Q2 — Volumes réels de la bibliothèque Hip-Hop** par catégorie.
3. **Q3 — Répartition** US 90s / FR oldschool / récent — à arbitrer avec Q2.
4. **Q4 — Signature station 3** : 1 h comme les autres, ou format différent ?
5. **Q5 — Exposition de l'énergie E1–E5** au poste : comment le serveur publie-t-il l'énergie
   du créneau en cours ? (fichier statique, API maison, champ AzuraCast détourné)
6. **Q6 — Stations 4+** : y en a-t-il en tête ? Le cadran du poste a de la place
   ([design-brandt-rk711s.md](design-brandt-rk711s.md) §4.1).
7. **Q7 — Où vivent les grilles ?** Dans AzuraCast, dans les `*_schedule.json`, ou les deux ?
   S'il y a deux sources, laquelle fait foi ?

---

## 8. Liste des interdits (DO-NOT)

1. **Ne pas** dépasser **1 h** sur le créneau rap FR récent (§4).
2. **Ne pas** traiter la répartition 50/30/20 comme une décision figée (§4).
3. **Ne pas** rejouer un mix de signature avant 7 jours (§2).
4. **Ne pas** construire la programmation de Stage 303 autour des genres (§3).
5. **Ne pas** dupliquer les fichiers par station : la bibliothèque est unique
   ([design-serveur-azuracast.md](design-serveur-azuracast.md) §4).
6. **Ne pas** faire confiance au BPM seul pour attribuer une énergie (§6.1).
7. **Ne pas** enfermer l'éditorial dans AzuraCast : les grilles doivent survivre à un
   changement de moteur.

---

## 9. Stations candidates 4→8 — analyse de `music.db` (2026-09-06)

Analyse en lecture seule de `plugin/db/music.db` (85 040 fichiers, 110 playlists Spotify
portant `thematique`, `genres_dominants`, `energy`, `valence`) pour répondre à **Q6**
(« Stations 4+ : y en a-t-il en tête ? »).

### 9.1 Le contenu long existe — mais pas là où on le croyait

**Il n'existe aucun dossier `Mixtapes` sur `M:`.** L'arborescence réelle, mesurée sur le
scan qui couvrait tout `M:` :

| Emplacement | Fichiers | dont ≥ 20 min |
|---|---|---|
| `M:\music\workspace` | 67 763 | 181 |
| `M:\music\library` | 17 252 | 13 |
| `M:\radio\Radio-Library` | 575 | **558** |
| `M:\downloads\Book Club Radio` | 125 | 124 |
| `M:\downloads\Big Business HQ` | 21 | 19 |
| `M:\music\playlists` / `tracks` | 25 | 0 |

**895 fichiers de plus de 20 minutes, 1 109 heures cumulées.** La « colonne vertébrale »
posée en §0 est donc réelle — mais elle vit dans **`M:\radio\`**, que le scan courant ne
couvrait plus. Conséquence : elle était **absente de `music.db`**, ce qui rendait toute
mesure de faisabilité fausse par construction. Corrigé côté référentiel
([../musique.md](../musique.md), `npm run scan`).

> **Le critère utile n'est pas le nom du dossier, c'est la durée.** Un fichier de plus de
> 20 min est un mix ou un set ; en dessous de 10 min, c'est un track. C'est cette mesure
> qu'il faut porter dans le référentiel, pas une convention de rangement.
> Réserve : **24 019 fichiers (27 %) n'ont pas de durée lue** — le classement track /
> contenu long est donc incomplet tant que ce trou n'est pas comblé.

### 9.2 Volumes par famille

⚠️ **Ce sont des planchers, pas des mesures.** Les genres ne sont connus que pour les
~13 900 tracks enrichis via Spotify ; la majorité de la bibliothèque locale est sans genre
(52 % des fichiers n'ont même pas d'artiste). Les volumes réels ne sortiront qu'après le
tagging.

| Famille | Tracks locaux | Playlists sources |
|---|---|---|
| Soul / Funk / Disco | 727 | Baltimore 421 · My Playlist #155 357 · Jukebox · Cat Walk · Boogie Nights · Smoothy |
| Downtempo / Trip-hop | 539 | Reading 211 · Space Lullaby 108 · Trippy Abstraction 102 · Orbiting Souls 84 |
| Latin / Tropical | 483 | Orishas 255 · Rainforest Rhapsody 201 · Collectivo 130 · Heat Flow · Onda Cubana |
| Jazz / Lounge | 435 | Blue Moon · Roaring Twenties · Firefly · Strutopia |
| Reggae / Dub / Ska | 422 | Rockers Serenade 314 · Dub Odyssey 62 · Skinhead Symphony |
| Folk / Songwriter | 319 | Driftwood Dreams · Motocicleta · Esperanza · Shamanic |
| House / Techno | 308 | *(territoire de Stage 303)* |
| Afrique / Desert blues | 279 | Creodelic 161 · Rush Hour 129 · Afro Fusion 68 · Wandering Dunes · Baobab |
| Rock / Psyché 60s-70s | 243 | Pirate Radio 128 · Flower Power 50 · Cafe Racer · Anatol |
| Bass UK | 171 | UK Culture 157 *(recoupe Stage 303)* |
| Rap FR | 165 | Nostalgie Urbaine 292 · Arha *(→ station 3)* |
| Chanson française | 158 | Guinguette · Idoles des Jeunes · Starmania 2.0 · La Zad |

### 9.3 Les cinq retenues — **propositions, non figées**

| # | Famille | Socle | Ce qui la justifie |
|---|---|---|---|
| 4 | Soul / Funk / Disco | 727 | Plus gros gisement non couvert. Courbe interne naturelle : soul le jour → disco/boogie le soir (Boogie Nights E=0.83) |
| 5 | Downtempo / Lofi | 539 | **La seule station basse énergie du parc** (Downtempo mesure E=0.17). Tout le reste est E3+ : elle comble un trou de la courbe globale |
| 6 | Latin / Tropical | 483 | Déjà structurée par pays : Brésil, Colombie, Cuba, Argentine, Pérou |
| 7 | Reggae / Dub / Ska | 422 | Identité la plus forte du lot, et vraie courbe : ska/rocksteady le jour → roots → dub la nuit (Dub Odyssey E=0.27) |
| 8 | Afrique / Desert blues | 279 | Socle plus mince mais cohérent, déjà validé par l'usage (Rush Hour et Afro Fusion sont des playlists de rotation) |

**En réserve** : Rock/Psyché 60s (243 — meilleur taux de couverture locale du lot, 54 % sur
le tag `60s`), Chanson française (158), Jazz/Lounge (435 mais recoupe fortement Soul/Funk).

**Écartées** : House/Techno et Bass UK (territoire de Stage 303), Rap FR (station 3).

### 9.4 Le point de méthode qui reste à trancher

Ces cinq familles sont construites **par genre** — or les trois stations existantes suivent
explicitement l'inverse : *univers > genre* (§2), *performance > genre* (§3),
*culture/époque > genre* (§4). « Soul/Funk/Disco » est un rayon de disquaire, pas un
univers.

**Aucune de ces cinq n'est une station tant qu'elle n'a pas son identité** : un nom, un
imaginaire, un créneau signature. Le volume dit seulement laquelle *peut* exister.

### 9.5 Réponse à Q6, et ce que ça change pour le poste

Le cadran du Brandt **manque** d'entrées, il n'en a pas trop :
[design-brandt-rk711s.md](design-brandt-rk711s.md) §4.1 note qu'à 3-4 stations réparties
sur toute la course, on obtient « d'immenses zones mortes et un geste sans récompense ».
Passer à 8 stations sert donc **deux** objectifs à la fois, et allège la Q5 du poste
(« que met-on sur l'échelle pour qu'elle ne soit pas vide »).

---

## 10. Architecture d'exécution — qui décide, qui joue

**Vérifié le 2026-09-07 sur l'OpenAPI de l'instance installée** (`http://localhost/api/openapi.yml`,
170 routes), pas de mémoire — §3 du doc serveur s'applique aussi ici.

### 10.1 Ce qu'AzuraCast sait faire, et ce qu'il ne sait pas

| Besoin (§5, §18) | AzuraCast | Verdict |
|---|---|---|
| Créneaux horaires par jour | `schedule_items` sur chaque playlist | ✅ natif |
| Proportions entre contenus | `weight` (poids relatif entre playlists actives) | ✅ natif |
| Ordre de lecture | `order` : `random` / `shuffle` / `sequential` | ✅ natif |
| Sweepers, IDs, jingles | `type: once_per_x_songs` / `once_per_hour` + `is_jingle` | ✅ natif |
| Anti-répétition courte | `avoid_duplicates` | ⚠️ partiel |
| **Sélection par contrainte** (« US 90s, E3–E4, pas joué récemment ») | — | ❌ **absent** |
| **Cooldown 7 jours** (interdit n°3) | — | ❌ **absent** |
| **Énergie E1–E5** comme critère | — | ❌ **absent** |

Une playlist AzuraCast est une **liste de fichiers explicite**, pas une requête. Il n'existe
pas de « smart playlist ». Le §17 (« faire mieux que : prends un fichier dans tel dossier »)
n'est donc **pas** réalisable dans AzuraCast seul.

### 10.2 Décision : le repo décide, AzuraCast exécute

C'est la seule architecture qui satisfait à la fois §18, §21 (« le système exécute une
intention éditoriale ») et l'interdit n°7 (« ne pas enfermer l'éditorial dans AzuraCast »).

```
.docs / features/radio/          ← l'INTENTION (versionnée, survit au moteur)
  grilles *.json  +  métadonnées fichiers
              ↓
      générateur (repo)          ← la DÉCISION
  contraintes → candidats → pondération → cooldown/historique
              ↓  API
        AzuraCast                ← l'EXÉCUTION
  playlists + schedule_items → Liquidsoap → Icecast
```

Le générateur **remplace périodiquement le contenu** des playlists AzuraCast. La grille et
les poids restent lisibles côté AzuraCast, mais **aucune décision éditoriale n'y est prise**.

**Endpoints utilisés** (tous présents sur l'instance) :

| Route | Usage |
|---|---|
| `POST /station/{id}/playlists` | créer les playlists de la grille |
| `PUT /station/{id}/playlist/{id}` | poser `weight`, `order`, `schedule_items` |
| `POST /station/{id}/playlist/{id}/import` | **pousser la sélection calculée** |
| `DELETE /station/{id}/playlist/{id}/empty` | vider avant repush |
| `GET /station/{id}/files/list` | lire la médiathèque indexée |
| `POST /station/{id}/files/bulk` | écrire les métadonnées en masse |
| `GET /station/{id}/schedule` | relire la grille effective |

### 10.3 Les Custom Fields — comment l'énergie devient visible

`/admin/custom_fields` permet de déclarer des champs maison (`energy`, `era`, `culture`,
`category`, `type`), chacun avec **`auto_assign`** : un tag ID3v2 dont AzuraCast aspire la
valeur automatiquement au scan.

Conséquence : si le repo écrit l'énergie **dans les tags ID3** des fichiers, elle remonte
seule dans AzuraCast, puis dans l'API now-playing. **C'est la réponse à Q5 du présent doc**
(« comment le serveur publie-t-il l'énergie du créneau en cours ? ») **et au besoin du poste**
([design-brandt-rk711s.md](design-brandt-rk711s.md) §8) : le visualiseur peut lire l'énergie
sans qu'on invente une API maison.

### 10.4 Le vrai blocage : aucun fichier n'a d'énergie

Le pipeline ci-dessus est inerte tant que les fichiers ne portent pas de métadonnées :

- **895 fichiers longs** (mixtapes, sets) : **aucun** n'a d'énergie.
- `enrichment.energy` de `music.db` ne couvre que les ~13 900 tracks venus de Spotify, et
  c'est une valeur Spotify, pas une décision éditoriale.
- 52 % des fichiers locaux n'ont même pas d'artiste.

C'est le chantier §6.1, et il conditionne tout le reste.

### 10.5 V1 proposée — une radio qui tourne avant d'être parfaite

**Proposition, à arbitrer.** Attendre le tagging complet, c'est n'avoir aucune station qui
diffuse avant longtemps. La v1 respecte la **structure** de la grille sans sa granularité
énergétique :

1. Playlists par **nature de contenu** (mixtapes longues / tracks / interludes) et par
   famille — ce que les dossiers permettent déjà de distinguer.
2. Créneaux et poids de la grille **appliqués tels quels** : la forme de la journée
   (interlude à 04h, signature à 20h, live sets) est respectée.
3. L'énergie arrive ensuite, **sans refonte** : le générateur filtre déjà sur un champ
   `energy`, il est simplement vide au départ.

Ce qui est perdu en v1 : la courbe E1→E5 fine et le cooldown 7 jours. Ce qui est gagné :
des stations qui diffusent, donc de quoi écouter, corriger et tagger **en écoutant**.
