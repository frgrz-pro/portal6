# Food — `apps/food/`

Le frigo et les placards dans une DB, et des recettes proposées façon Tinder selon le
moment de la journée. **App mobile Kotlin/Compose avec DB embarquée** (même modèle que
`ha-remote`) : code et mode d'emploi dans [apps/food/README.md](../apps/food/README.md).
Le prototype web de la première passe est conservé dans `apps/food-web/` (même logique,
même catalogue). Ici, les choix et ce qui reste à trancher.

## Questions ouvertes

- [ ] **Saisie du stock** : aujourd'hui à la main (liste par catégorie, +/−). Pistes pour
  aller plus vite : scan de ticket de caisse (photo → LLM → lignes), ou liste de courses
  générée depuis les recettes validées (les « manque » deviennent la liste).
- [ ] **Recettes : catalogue seed de 36 recettes.** Faut-il un formulaire « nouvelle
  recette » dans l'app, ou on enrichit `seed/recipes.json` à la main / via Claude ?
  Le serveur sait déjà créer une recette (`POST /api/recipes`), il manque l'écran.
- [ ] **Péremption** : le stock n'a pas de date. Utile pour le frigo (yaourts, viande),
  inutile pour le placard. À ajouter seulement si le besoin se fait sentir.
- [ ] **iPhone : quelle voie ?** François veut du cross-platform ; l'app est Android
  (Compose) avec la logique métier en Kotlin pur (`domain/`) et le schéma SQLDelight, tous
  deux réutilisables en Kotlin Multiplatform. Passer en KMP/Compose Multiplatform exige
  **Xcode** sur le Mac (absent : seuls les Command Line Tools sont installés) + un compte
  Apple pour installer sur l'iPhone. À trancher : installer Xcode (~12 Go) et tenter la
  cible iOS, ou rester Android + proto web pour l'iPhone.
- [ ] **Vérifier sur le S20** : vérifié sur l'émulateur Pixel 6 / Android 15 du Mac, pas
  encore sur le vrai téléphone (pas branché). Brancher, `adb install -r`, cf. skill `android`.
- [ ] **Garder le proto web ?** `apps/food-web/` (Python stdlib) marche et sert de banc
  d'essai ; à supprimer si l'app mobile suffit.
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

- **App mobile, DB embarquée** (2026-09-11, pivot) : la première passe était un serveur
  web Python + page ; François voulait une app mobile Kotlin avec DB interne. Refait sur le
  modèle de `ha-remote` : Kotlin + Compose + Material 3, un module `app/`, Gradle 8.10.2 /
  AGP 8.7.3 / Kotlin 2.0.21, sans Android Studio. **SQLDelight** plutôt que Room : le
  schéma `.sq` et la logique `domain/` (Kotlin pur) sont réutilisables tels quels en KMP
  si la cible iOS arrive. Dialecte SQLite 3.18 (Android 8) → pas d'UPSERT.
- **Catalogue versionné en JSON** (`apps/food/seed/`), source de vérité partagée entre
  l'app et le proto web ; `tools/gen_seed.py` le transforme en `Seed.kt` commité (le build
  Gradle n'a pas besoin de Python). Injecté au premier lancement, complété ensuite
  **sans écraser** les recettes ajustées (source `user`).
- **Proto web conservé** dans `apps/food-web/` (serveur stdlib, DB `data/food/food.db`,
  port 8714) : même règles, utile pour tester vite dans un navigateur et comme fallback
  iPhone en attendant une vraie cible iOS.
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
- Port **8714** pour le proto web (8712 = portail WSL, 8713 = preview). `npm run food`.
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

### 2026-09-11 (bis) — pivot app mobile
François : « c'était une app mobile cross-platform Kotlin avec DB interne que je voulais »,
« sur le même modèle que ha-remote ». Refait en `apps/food/` : Kotlin/Compose, SQLDelight,
mêmes écrans et règles que le proto (deck swipe animé, fiche en bottom sheet, ajustement,
Je cuisine, restes, stock). `./gradlew assembleDebug` : **BUILD SUCCESSFUL**, APK 16,6 Mo ;
5 tests JVM de la logique (couverture, deck, moments, formats) au vert. Téléphone non
branché → **émulateur Android installé sur le Mac** (cf. setup-dev-mac.md) et parcours
complet vérifié dessus via adb : stock +/−, recherche, jauge jaune, fiche, Je cuisine
(œufs 4 → 1, chocolat 100 → 0, 1 part en reste, historique), carte reste, swipe droite /
gauche, tap = fiche, Ajuster. Piège corrigé : deux détecteurs de gestes (tap + drag)
se marchaient dessus sur un swipe rapide → un seul détecteur manuel. Proto web déplacé
en `apps/food-web/`.

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
