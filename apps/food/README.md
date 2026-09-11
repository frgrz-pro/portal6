# apps/food — le frigo, les placards et les recettes

Stock d'ingrédients en DB, recettes proposées en **swipe** selon le moment de la journée,
slider « personnes » + jauge « j'ai tout ? », et « Je cuisine » qui décrémente le stock.
Les choix de design sont dans [.docs/food.md](../../.docs/food.md).

```
apps/food/
├── server.py           # serveur stdlib (http.server + sqlite3) : API JSON + statique, port 8714
├── seed/
│   ├── ingredients.json  # catalogue d'ingrédients (id, nom, unité, catégorie)
│   └── recipes.json      # recettes seed : quantités PAR PERSONNE, moments, étapes
└── static/
    ├── index.html       # une page, trois onglets : Idées / Validées / Stock
    ├── app.js           # deck swipe, jauge, dialogue recette (slider, ajustement, cuisine), stock
    └── style.css
```

La DB est `data/food/food.db` (vault, non versionnée), créée et remplie depuis `seed/`
au premier lancement. Les recettes seed manquantes sont ajoutées à chaque démarrage sans
écraser celles déjà ajustées.

## Lancer

```bash
npm run food            # ou : python3 apps/food/server.py [--port 8714] [--reset]
```

Puis <http://localhost:8714> (ou `http://<ip-du-mac>:8714` depuis le téléphone — le
serveur écoute sur `0.0.0.0`). `--reset` supprime la DB et repart du seed.

## Utiliser

- **Idées** : une carte = une recette du moment (auto selon l'heure, chips pour forcer).
  → droite / ♥ = validée ; ← / ✕ = passée 3 jours ; toucher = détail. Les **restes** passent
  en premier (→ = j'en mange une part).
- **Détail** : slider personnes, jauge (jaune = tout est en stock), ligne par ligne
  « manque X ». **Ajuster** édite les quantités par personne (la recette devient la tienne).
  **Je cuisine** : parts mangées maintenant → stock décrémenté, le reste en Restes.
- **Stock** : par catégorie, +/− par pas (50 g, 100 ml, 1 pièce), saisie directe, ajout
  d'ingrédient.

## API (JSON)

| Route | Effet |
|---|---|
| `GET /api/state` | tout l'état (ingrédients, stock, recettes, swipes, restes, cuisines, inbox) |
| `PUT /api/stock/{id}` `{qty}` ou `{delta}` | fixe ou décale le stock (jamais < 0) |
| `POST /api/ingredients` `{name, unit, category}` | nouvel ingrédient |
| `POST /api/recipes` · `PUT /api/recipes/{id}` | crée / ajuste (nom, minutes, moments, étapes, `ingredients: [{ingredient_id, qty}]` par personne) |
| `POST /api/swipes` `{recipe_id, verdict: like\|skip}` · `DELETE /api/swipes/{id}` | verdict Tinder |
| `POST /api/cook` `{recipe_id, persons, eaten}` | décrémente le stock, crée le reste, retire des validées |
| `POST /api/leftovers/{id}/eat` `{portions}` | mange une part de reste |
| `POST /api/inbox` `{kind, text, url}` | dépôt brut (cible du Raccourci iOS — phase 2) |

Toute écriture renvoie le nouvel état complet.
