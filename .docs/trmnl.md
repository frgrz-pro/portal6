# TRMNL — setup du premier device

Le premier TRMNL généraliste : compte, plugins, et surtout l'affichage du calendrier
sport. Les use cases et la fiche technique du TRMNL (pull-only, Dev Edition, BYOS…)
sont dans [features/home/README.md](../features/home/README.md) — ce doc porte le **setup concret** et
les décisions de comptes.

## Questions ouvertes

- [ ] **Agenda unique : abonnements ICS (A) ou écriture par l'API (B) ?** Voir la
  section « Un seul agenda `trmnl.sport` » ci-dessous — c'est la décision qui débloque
  tout le reste (couleurs, partage vers `.case`, catégories).
- [ ] **Liste des catégories** et couleur associée (Google n'en propose que 11).
- [ ] **D'où viennent les chaînes TV** pour foot / rally / F1 ? Les abonnements ICS
  tiers ne les portent pas, et portal6 ne génère que l'eSport. Sans source, la
  description « chaîne » ne pourra être remplie que pour l'eSport.
- [ ] `PANDASCORE_TOKEN` est **absent du `.env` local** (constaté le 2026-09-06) :
  `build_ics.py` ne peut pas regénérer CS2 depuis le poste, seulement en CI via le
  secret GitHub. À remettre si on veut itérer en local.
- [x] ~~Un seul agenda « sport » ou un agenda par abonnement ?~~ → **tranché le
  2026-09-06 : un seul, `trmnl.sport`.** Reste à choisir *comment* (A ou B ci-dessus).
- [ ] Refresh souhaité pour l'écran calendrier (15 min suffit largement : les ICS
  côté Google ne se rafraîchissent que toutes les ~24 h de toute façon).

## Décisions

### Le device : TRMNL OG (800 × 480, 1-bit)

Confirmé le 2026-09-06. Toute la conception des écrans part de là — les layouts des
private plugins sont dimensionnés pour 800 × 480 en noir et blanc pur (pas de gris).

### Developer Edition : prise

Tranché le 2026-09-06 : on la prend maintenant, puisque les deux premiers écrans
maison sont conçus (cf. [trmnl-dashboard.md](trmnl-dashboard.md)). Sans l'addon,
l'onglet Private Plugins n'existe pas côté TRMNL.

**Où l'activer** — impérativement connecté avec le compte **`.case`** (celui qui possède
le device ; c'est sur ce compte que vivront les plugins) :

1. Dans l'app TRMNL : *menu déroulant du device → icône engrenage → descendre jusqu'à
   « Developer perks » → upgrade*. C'est un achat unique qui débloque l'API et les
   private plugins **définitivement sur le compte**.
2. Ou directement : <https://trmnl.com/upgrade>.

À noter : l'addon se compte **par device**, mais un seul device en Dev Edition suffit
pour *créer* des plugins utilisables ensuite sur tous les devices du compte — donc 20 $
une fois, pas 20 $ par écran futur.

### Comptes — `.sport` (données) et `.case` (device)

Deux comptes Google distincts :

- **`.sport`** : le compte qui **possède** les agendas sport (abonnements ICS foot /
  rally / F1 + les .ics eSport générés par portal6). Il reste la source ; on n'y touche pas.
- **`.case`** : **nouveau compte créé pour le TRMNL** (compte TRMNL + OAuth du plugin
  Google Calendar). Isole le device des comptes perso : révocable, pas de scope OAuth
  sur les vrais agendas.

**Liaison retenue : partage d'agenda Google, pas OAuth sur `.sport`.** Le plugin
Google Calendar TRMNL s'authentifie sur **un seul** compte Google et liste les agendas
visibles de ce compte — y compris les agendas **partagés** avec lui. Donc :

1. Dans Google Calendar de `.sport` : *Paramètres de l'agenda « sport » → Partager avec
   des personnes spécifiques → ajouter `.case` en « Afficher tous les détails de
   l'événement »*. (À répéter par agenda s'il y en a plusieurs.)
2. Dans `.case` : accepter l'invitation (mail) — l'agenda apparaît dans la liste
   « Autres agendas ».
3. Sur TRMNL (compte `.case`) : plugin **Google Calendar** → *Connect* (OAuth `.case`)
   → cocher le/les agendas partagés → layout (jour / semaine / liste) → ajouter à la
   playlist du device.

Alternative écartée : OAuth du plugin directement sur `.sport`. Marche aussi, mais couple
le device au compte de données et multiplie les scopes OAuth sur un compte qui sert à
autre chose.

### Un seul agenda `trmnl.sport` — décidé, mais deux façons de le faire

**Décision du 2026-09-06 (François)** : viser **un agenda unique `trmnl.sport`** plutôt
qu'un agenda par abonnement — N agendas, c'est N partages vers `.case` à refaire à
chaque nouvelle ligue. Les catégories passent par la **couleur** et un **label**, et la
description ne porte qu'**une seule information : la chaîne** (`beIN Sports 1 (FR)`,
`twitch.tv/otplol_ (FR)`…).

Trois contraintes constatées avant de construire :

1. **Un abonnement ICS = un agenda Google.** On ne peut pas verser plusieurs
   abonnements dans un agenda unique : Google crée un agenda par URL. Un agenda
   réellement unique suppose que portal6 **écrive** les événements via l'API Google
   Calendar.
2. **La couleur est portée par l'agenda, pas par l'événement** — sauf sur un agenda
   qu'on possède et qu'on écrit par l'API (`colorId`, 11 couleurs). Sur un abonnement,
   la couleur par événement est impossible. Google **ignore aussi `CATEGORIES`** en
   ICS : les « labels » ne passent pas par un abonnement.
3. **Le TRMNL OG est 1-bit : les couleurs ne s'affichent pas sur le device.** Elles ne
   servent qu'au téléphone et au web. Sur l'écran, la catégorie doit être rendue
   autrement — c'est le rôle du code court déjà affiché à côté de l'heure.

D'où deux options, à trancher :

| | **A — abonnements ICS (statu quo amélioré)** | **B — agenda unique écrit par l'API** |
|---|---|---|
| Agendas | 1 par ligue/sport | 1 seul, `trmnl.sport` |
| Couleur | par agenda — donc *déjà* une couleur par catégorie si catégorie = ligue | par événement, libre |
| Labels | impossibles (Google ignore `CATEGORIES`) | possibles |
| Partage vers `.case` | 1 par agenda, à refaire à chaque ajout | 1 fois, définitivement |
| Chaîne en description | ✅ (on génère les .ics) | ✅ |
| Coût de dev | ~0 | module de sync idempotente (créer/mettre à jour/supprimer) |

**Recommandation : B**, précisément pour la raison qui motive la demande — c'est la
seule option qui supprime la corvée récurrente du partage par agenda. Le service
account Google existe déjà (`GOOGLE_SERVICE_ACCOUNT_FILE` dans `.env`) ; il suffit de
partager `trmnl.sport` avec son adresse pour qu'il puisse y écrire. Les .ics générés ne
disparaissent pas : ils deviennent l'entrée de la synchronisation.

**La chaîne vient d'un mapping par ligue, pas de l'API.** Vérifié le 2026-09-06 :
`getEventDetails` de lolesports renvoie `streams: []` sur les prochains matchs — les
streams ne sont publiés que très tard. Or une ligue diffuse toujours sur les mêmes
canaux : une clé `broadcast` dans `leagues.json` (FR et EN) est plus robuste qu'un
appel par match, et gratuite. PandaScore expose bien `streams_list` par match pour
CS2 — utilisable en complément, pas en socle.

### Ordre de setup du device

1. Créer le compte Google `.case` → créer le compte TRMNL avec ce mail → *claim* du
   device (Wi-Fi via le portail captif du TRMNL, code affiché à l'écran).
2. Plugin **Google Calendar** (ci-dessus). Premier écran utile = validation du device.
3. Plugin **météo** du catalogue (zéro travail) → mashup calendrier + météo si le layout
   s'y prête.
4. Playlist + refresh : calendrier en refresh 15–60 min, sommeil la nuit
   (Device → Sleep mode) pour économiser la batterie.
5. Plus tard, avec la Dev Edition : marées (WorldTides), Todoist courses, état HA.

### Ce que ça change côté portal6

Rien à coder pour ce jalon : les .ics eSport sont déjà générés et servis par GitHub
([features/home/calendars/esports/](../features/home/calendars/esports/)). Vérifier seulement que les
4 abonnements (LEC, LCK, international, CS2) sont bien **dans le compte `.sport`**
(et pas dans un autre compte) avant de partager.

## Journal

### 2026-09-06
Création du doc. Décision : le TRMNL vit sur un nouveau compte `.case` ; les agendas
sport restent dans `.sport` et sont **partagés** vers `.case` (le plugin Google Calendar
ne lit qu'un compte). Ordre de setup posé. Rien à coder côté repo pour ce jalon.

### 2026-09-06 (bis) — cap sur les écrans
Device confirmé **TRMNL OG** et **Developer Edition** décidée : les deux questions
ouvertes correspondantes sont tranchées et sorties de la liste. La construction des
écrans part dans son propre doc, [trmnl-dashboard.md](trmnl-dashboard.md) ; ce doc-ci
reste sur les comptes et la mise en route.

### 2026-09-06 (ter) — lune blanche, tags blancs
Retour d'écran : le disque lunaire en gris `#c8c8c8` sortait trop sombre sur l'e-ink →
disque **blanc**, ombre noire inchangée ; quelques cratères en deux gris (`#aaa` mers,
`#555` anneaux et points, rien sous 2 px) dessinés sous l'ombre, sur la grande lune
seulement (la frise 7 jours à 21 px reste unie). Tags de l'agenda : typo blanche sur
la pastille `#aaa`. Rendu vérifié via `trmnlp build --png --color-depth 2` (copie
temporaire du plugin avec `variables:` injectées depuis `build_dashboard.py`).
