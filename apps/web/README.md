# apps/web — le portail Portal6

Petit site **statique** (HTML + CSS + un fichier JS), sans build, sans dépendance,
sans framework. Une entrée par domaine ; seule **Music** est remplie pour l'instant.

```
apps/web/
├── index.html              # hub : une carte par domaine
├── music.html              # MEDIACENTER : playlists × plateformes, groupes, push
├── music-dedup.html        # DOUBLONS : résolution façon dupeGuru (local, Spotify intra/inter)
├── music-proposals.html    # PROPOSITIONS : découpes des monolithes, déplacements de titres
├── music-data.html         # inventaire brut de la donnée musique (vault, scans, DB, trous)
├── assets/style.css        # feuille de style unique (clair/sombre auto)
├── assets/plan.js          # moteur de plan partagé : barre, Commit changes, revue, historique
├── assets/mediacenter.js   # rendu + état utilisateur de music.html
├── assets/dedup.js         # rendu + décisions de music-dedup.html
├── assets/proposals.js     # rendu + décisions de music-proposals.html
├── assets/inventory.js     # rendu de music-data.html
├── data/manifest.js        # GÉNÉRÉ, versionné — agrégats : window.PORTAL6
├── data/dedup.js           # GÉNÉRÉ, non versionné — détail des doublons (chemins)
├── data/proposals.js       # GÉNÉRÉ, non versionné — détail des propositions (titres)
└── build_manifest.py       # produit les trois fichiers data/
```

## Lancer

Depuis le terminal `p6` (WSL, venv activé) :

```bash
npm run web
```

(équivalent : `python -m http.server 8712 --directory apps/web` avec le python du
venv. Sur Windows natif, `python` du PATH est le stub Microsoft Store : utiliser
`%LOCALAPPDATA%\Programs\Python\Python312\python.exe`.)

Puis <http://localhost:8712>. Un simple double-clic sur `index.html` marche aussi :
le manifeste est un `.js` (et non un `.json`) précisément pour que `file://`
n'ait pas besoin de `fetch`.

## Régénérer les chiffres

```bash
npm run web:manifest   # = python apps/web/build_manifest.py
```

Le script lit **uniquement** `plugin/db/music.db` et `data/music/` — jamais `M:`
directement (règle du repo : seuls les scripts d'ETL scannent le disque musique,
et c'est François qui les lance). Il ne produit que des **agrégats** : comptes,
tailles, taux, noms de dossiers sur deux niveaux. Aucun chemin de fichier musical,
aucune donnée nominative.

Comptez ~2 min : les deux `library_scan*.csv` font 20 Mo chacun et sont dépouillés
ligne à ligne.

## Ce que fait — et ne fait pas — le mediacenter

- **Fait** : liste les 110 playlists avec, par plateforme, ce que la DB sait
  (Spotify = titres, Local = titres ayant un fichier, iTunes = rien encore) ; statut
  ✓ / ◐ / ✗ ; filtre, recherche ; **groupes** de playlists et **maître** par playlist,
  mémorisés dans `localStorage` (`portal6.music`), exportables/importables en JSON.
- **Ne fait pas** : écrire sur Spotify, sur le disque ou dans iTunes. *Push* ouvre un
  panneau qui montre les prérequis manquants et la commande ; les cartes *Doublons* et
  *Propositions* renvoient vers les vues d'action ci-dessous. Voir `.docs/musique.md`,
  « Modèle de sync ».

## Doublons et propositions : marquer → Commit changes → plan JSON

Les deux vues d'action fonctionnent pareil (`assets/plan.js`) :

1. On **marque** : action par copie (quarantaine / supprimer / ignorer, ★ pour changer
   la référence), clic sur une playlist pour en retirer un titre, Créer / Rejeter une
   sous-playlist (renommable), Déplacer / Retirer un titre hors profil. Des presets
   marquent toute la vue filtrée. Tout est mémorisé dans `localStorage`.
2. **Commit changes** (barre du bas) ouvre la revue : regroupé par type d'action, chaque
   ligne décochable, total et volume libérable.
3. **Valider** fige le plan : JSON téléchargé (`dedup_plan.<id>.json` /
   `playlist_plan.<id>.json`), commande à lancer affichée, plan ajouté à l'historique
   (statut déclaratif à appliquer / appliqué / abandonné, réouverture possible).

Format du plan : `{ id, title, created, status, items: [{ id, kind, label, detail,
meta }] }`. Les scripts qui le consomment (`apply_dedup_plan.py`,
`apply_playlist_plan.py`) **n'existent pas encore** — le portail fixe le contrat.

## Pourquoi `data/manifest.js` est versionné

C'est un artefact dérivé, donc en principe gitignoré comme `plugin/db/*.db`. Il est
malgré tout commité : ~55 Ko d'agrégats, ça rend le portail lisible depuis n'importe
quelle machine sans le vault, et ça donne un « dernier état connu » daté. La page
affiche toujours sa date de génération en pied de page — si elle est vieille,
relancer le script.

## Ajouter un domaine

1. Dans `build_manifest.py`, une fonction qui agrège la donnée du domaine, ajoutée
   au dict retourné par `build()`.
2. Une page `<domaine>.html` sur le modèle de `music.html`.
3. La carte correspondante dans `index.html` (enlever la classe `disabled`).
