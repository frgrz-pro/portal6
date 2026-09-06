# TRMNL — setup du premier device

Le premier TRMNL généraliste : compte, plugins, et surtout l'affichage du calendrier
sport. Les use cases et la fiche technique du TRMNL (pull-only, Dev Edition, BYOS…)
sont dans [features/home/README.md](../features/home/README.md) — ce doc porte le **setup concret** et
les décisions de comptes.

## Questions ouvertes

- [ ] Le device est un **TRMNL OG** ou un **TRMNL X** ? (impacte la taille des layouts
  des futurs private plugins ; le BOM le porte en ✅ « à confirmer »).
- [ ] Les calendriers sport dans le compte `.sport` sont-ils **un seul agenda « sport »**
  qui agrège les abonnements ICS, ou **un agenda par abonnement** (foot, rally, F1,
  LEC, LCK, CS2…) ? → détermine ce qu'on partage vers `.case` et ce qu'on coche dans
  le plugin.
- [ ] Refresh souhaité pour l'écran calendrier (15 min suffit largement : les ICS
  côté Google ne se rafraîchissent que toutes les ~24 h de toute façon).
- [ ] Prendre la **Developer Edition (20 $)** dès maintenant ou seulement quand le
  premier private plugin (marées) arrive ? Le calendrier Google n'en a pas besoin.

## Décisions

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
