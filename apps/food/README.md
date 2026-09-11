# apps/food — Food, l'app Android du frigo et des recettes

App Android (Kotlin + Jetpack Compose, Material 3, **DB SQLite embarquée** via SQLDelight),
même modèle que [`apps/ha-remote/`](../ha-remote/README.md). Design et décisions dans
[`.docs/food.md`](../../.docs/food.md). Un prototype web de la même logique vit dans
[`apps/food-web/`](../food-web/README.md).

```
apps/food/
├── seed/                    # SOURCE DE VÉRITÉ du catalogue (partagée avec le proto web)
│   ├── ingredients.json     #   73 ingrédients : id, nom, unité, catégorie
│   └── recipes.json         #   38 recettes : quantités PAR PERSONNE, moments, étapes
├── tools/gen_seed.py        # JSON → data/Seed.kt (commité : le build n'a pas besoin de Python)
└── app/src/main/
    ├── sqldelight/…/Food.sq # schéma + requêtes typées (SQLDelight génère FoodDatabase)
    └── java/com/portal6/food/
        ├── FoodApp.kt / MainActivity.kt    # singleton de process + Scaffold 3 onglets
        ├── domain/Models.kt                # Kotlin pur : couverture, deck, moments, formats
        ├── data/FoodRepository.kt          # DB : flows de lecture, écritures en transaction, seed
        ├── data/Seed.kt                    # GÉNÉRÉ
        └── ui/                             # FoodViewModel, IdeasScreen (swipe), RecipeSheet,
                                            # LikedScreen, StockScreen, Theme
```

## Ce que fait l'app

- **Idées** : une carte = une recette du moment (auto selon l'heure : matin 6–10, midi 11–14,
  goûter 15–17, soir 18–22 ; chips pour forcer). Glisser à droite / ♥ = validée, à gauche / ✕ =
  passée 3 jours, toucher = fiche. Les **restes** passent en premier (→ = j'en mange une part).
- **Fiche** : slider personnes (1–8), quantités recalculées, **jauge jaune** = tout en stock,
  sinon « manque X ». **Ajuster** édite les quantités par personne (la recette devient
  « ajustée », source `user`). **Je cuisine** : parts mangées maintenant → stock décrémenté
  (jamais sous 0), le reste des parts va dans Restes, la recette sort des validées.
- **Validées** : restes (−1 part) + recettes gardées avec leur jauge.
- **Stock** : par catégorie, +/− (50 g, 100 ml, 1 pièce), saisie directe, recherche, filtre
  « en stock », ajout d'ingrédient. Le slider 👥 de la barre du haut pilote toutes les jauges.

Tout est local : aucune permission, aucune connexion. La DB `food.db` est dans le stockage
privé de l'app ; le catalogue seed est injecté au premier lancement et complété ensuite
sans écraser les recettes ajustées.

## Build & install (Mac, sans Android Studio — cf. `.docs/setup-dev-mac.md`)

```bash
cd apps/food
./gradlew assembleDebug            # APK : app/build/outputs/apk/debug/app-debug.apk (≈ 17 Mo)
./gradlew testDebugUnitTest        # tests JVM de la logique (domain/)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Catalogue modifié ? `python3 tools/gen_seed.py` puis rebuild. Le `.sq` est en dialecte
SQLite 3.18 (Android 8) : pas d'`UPSERT`, on fait insert-or-ignore + update.
