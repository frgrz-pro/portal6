# .docs — notes de design vivantes

Méthodo de travail du repo : **chaque session Claude commence ici**. Un document par
sujet, enrichi au fil des sessions (pas un fichier par date — les fichiers datés de
`docs/` sont l'ancien historique du domaine musique, on n'y touche pas).

## Règles

1. **Un doc = un sujet** (une app, un sous-système, une décision structurante).
2. Chaque doc garde en bas une section **Journal** avec une entrée datée par session
   qui l'a modifié (`### 2026-08-30` + 2-3 lignes : ce qui a été décidé/appris).
3. Les décisions actées vont dans le corps du doc ; le journal ne fait que tracer.
4. Les questions non tranchées vivent dans une section **Questions ouvertes** en
   tête de doc — c'est la todo-list de la session suivante.
5. Nouveau sujet → nouveau doc + une ligne dans l'index ci-dessous.
6. **Tout besoin matériel passe par [bom.md](bom.md)** (le BOM central, tous
   projets confondus) : une ligne avec statut, prix estimé et condition de
   déblocage. Les BOM détaillés par projet (ex. `hardware/bom.md`) restent la
   source du détail ; le central agrège et pointe.
7. La skill `.claude/skills/root-session/` automatise ce rituel — l'invoquer en
   début de session (`/root-session`).

## Index

- [app-remote.md](app-remote.md) — l'app télécommande (Kotlin) : UI, stack, architecture
- [zigbee-multiprises.md](zigbee-multiprises.md) — les 2 multiprises Zigbee : comment ça marche, comment les piloter
- [tv-mute.md](tv-mute.md) — couper le son de la TV : Shield / TCL soundbar / télé (phase 2)
- [infra-reseau.md](infra-reseau.md) — réseau domestique : Mercusys, OpenVPN/NordVPN, IPs
- [trmnl.md](trmnl.md) — setup du premier TRMNL : comptes `.sport` / `.case`, plugin Google Calendar, ordre de mise en route
- [lieux.md](lieux.md) — espace Lieux : état du vault My Maps, clôture du Lot A, todo (design figé dans `docs/`)
- [musique.md](musique.md) — relance du domaine musique : référentiel `music.db`, Plex/Symfonium, AzuraCast Phase 3
- [bom.md](bom.md) — **BOM central** : tout le hardware de tous les projets (statuts, prix, déblocages)
