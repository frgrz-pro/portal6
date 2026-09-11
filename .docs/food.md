# Food — `apps/food/`

Le frigo et les placards dans une DB, et des recettes proposées façon Tinder selon le
moment de la journée. Le code et le mode d'emploi sont dans
[apps/food/README.md](../apps/food/README.md) ; ici, les choix et ce qui reste à trancher.

## Questions ouvertes

- [ ] **Saisie du stock** : aujourd'hui à la main (liste par catégorie, +/−). Pistes pour
  aller plus vite : scan de ticket de caisse (photo → LLM → lignes), ou liste de courses
  générée depuis les recettes validées (les « manque » deviennent la liste).
- [ ] **Recettes : catalogue seed de 36 recettes.** Faut-il un formulaire « nouvelle
  recette » dans l'app, ou on enrichit `seed/recipes.json` à la main / via Claude ?
  Le serveur sait déjà créer une recette (`POST /api/recipes`), il manque l'écran.
- [ ] **Péremption** : le stock n'a pas de date. Utile pour le frigo (yaourts, viande),
  inutile pour le placard. À ajouter seulement si le besoin se fait sentir.
- [ ] **Accès depuis le téléphone** : le serveur écoute sur `0.0.0.0:8714`. Sur le Mac ça
  suffit (`http://<ip-mac>:8714`) ; côté tour Windows/WSL, même problème que le portail
  (127.0.0.1 seulement) — cf. [portail-web.md](portail-web.md).
- [ ] **Partage depuis l'iPhone (phase 2)** : François veut « partager » une recette
  (page web, ou photo d'un bouquin) vers l'app. Piste retenue, à construire : un
  **Raccourci iOS** dans la feuille de partage (voir Décisions). À trancher : où tourne
  le serveur pour être joignable depuis le téléphone hors du Mac (tour Windows ? HA ?).
- [ ] **Parsing texte → recette (phase 2)** : l'OCR brut arrive dans `inbox` ; il faut
  un passage par l'API Claude (`claude-fable-5-1`, clé dans `.env`) pour en sortir
  `{name, minutes, moments, ingredients[{ingredient_id|nouveau, qty par personne}], steps}`
  en rapprochant les ingrédients du catalogue. Écran « À trier » : valider / corriger /
  jeter avant de créer la recette.
- [ ] **Les swipes « non »** cachent la recette 3 jours. Bonne durée ? Faut-il un
  « jamais » (recette détestée) ?

## Décisions

- **Serveur Python stdlib, zéro dépendance** (`http.server` + `sqlite3`) : premier
  domaine du repo qui écrit en DB depuis le navigateur — le portail est resté statique
  parce qu'il ne fait que lire. Pas de Flask/FastAPI tant qu'une seule app en a besoin ;
  si un deuxième domaine a besoin d'un backend, on factorisera.
- **DB dans le vault** : `data/food/food.db` (gitignoré comme tout `data/`). Le
  **catalogue** (ingrédients + recettes) est versionné dans `apps/food/seed/*.json` et
  injecté au premier lancement ; les recettes seed manquantes sont ajoutées à chaque
  démarrage, **sans écraser** celles que François a ajustées (source `user`).
- **Quantités par personne** dans la recette ; le slider « personnes » (1–8) multiplie.
  L'ajustement des quotas édite la recette en DB (pas une copie) : la recette devient
  « la mienne ».
- **Jauge jaune** = tous les ingrédients sont en stock pour le nombre de personnes
  choisi. Remplissage partiel (gris-bleu) = part des ingrédients couverts, ligne par
  ligne « manque X g ».
- **Moment de la journée** (client, heure locale) : matin 6–10, midi 11–14, goûter
  15–17, soir 18–22, sinon tout. Chaque recette porte ses moments (`matin | midi |
  gouter | soir | apero`) ; des chips permettent de forcer un moment.
- **Le deck** : restes d'abord (« Reste : curry, 3 parts »), puis les recettes du moment
  triées par couverture décroissante, hors validées et hors « non » de moins de 3 jours.
- **Swipe droite = validée** (onglet Validées, à cuisiner) ; **« Je cuisine »** = choix
  du nombre de personnes et des parts mangées maintenant → stock décrémenté (jamais en
  dessous de 0), le reste des parts va dans **Restes** (table `leftovers`), la recette
  sort des validées, un historique `cooks` est gardé. Manger un reste = −1 part.
- **Unités du catalogue** : `g`, `ml`, `pièce`, `tranche`, `cas`, `cac`. Sel, poivre,
  eau ne sont pas suivis (toujours là).
- Port **8714** (8712 = portail WSL, 8713 = preview du portail). `npm run food`.
- **Partage iPhone = Raccourci iOS, pas une app native** (2026-09-11) : une PWA ne peut
  pas s'inscrire dans la feuille de partage iOS (pas de Web Share Target sur Safari), et
  une app native = Xcode + compte développeur (cf. [portail-web.md](portail-web.md), même
  impasse que l'APK). Un **Raccourci** « Envoyer à Food » accepte une URL, du texte ou une
  image ; pour une image il enchaîne l'action native **« Extraire le texte de l'image »**
  (OCR Vision, sur l'iPhone, gratuit, hors ligne) ; puis « Obtenir le contenu de l'URL »
  en `POST` JSON sur `http://<serveur>:8714/api/inbox` `{kind, text, url}`. Le serveur
  stocke tel quel (table `inbox`, statut `new`) ; l'app affiche un bandeau « N recettes
  reçues à trier » avec le texte brut. Le parsing en recette structurée = phase 2.

## Journal

### 2026-09-11
Création à la demande de François : stock frigo/placards en DB, recettes en swipe
selon l'heure, validées enregistrées, slider personnes + jauge de couverture, ajustement
des quotas, « Je cuisine » qui décrémente le stock et met les parts non mangées en restes.
Serveur stdlib + front statique, 38 recettes seed, 73 ingrédients. Vérifié dans le
navigateur (mobile 375 px, clair/sombre) : swipe droite/gauche, jauge jaune quand tout est
là, « Je cuisine » 6 personnes / 1 part → stock décrémenté (lait 1000 → 100 ml) + reste de
5 parts, −1 part par swipe, ajustement des quotas persisté (recette → source `user`),
+/− du stock sans saut de scroll. En cours de session, François a ajouté le besoin
**partage iPhone + photo de bouquin (OCR)** : cadré en Raccourci iOS + `POST /api/inbox`
(endpoint livré), parsing en phase 2. DB de test supprimée : premier `npm run food`
repart du seed, stock vide.
