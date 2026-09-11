#!/usr/bin/env python3
"""Génère app/src/main/java/com/portal6/food/data/Seed.kt depuis seed/*.json.

    python3 apps/food/tools/gen_seed.py

Le JSON reste la source de vérité (partagée avec le prototype web apps/food-web) ;
le .kt généré est commité pour que le build Gradle n'ait pas besoin de Python.
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
SEED = HERE / "seed"
OUT = HERE / "app/src/main/java/com/portal6/food/data/Seed.kt"


def k(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def main() -> None:
    ings = json.loads((SEED / "ingredients.json").read_text(encoding="utf-8"))
    recipes = json.loads((SEED / "recipes.json").read_text(encoding="utf-8"))
    known = {i["id"] for i in ings}
    lines = [
        "package com.portal6.food.data",
        "",
        "// GÉNÉRÉ par tools/gen_seed.py depuis seed/*.json — ne pas éditer à la main.",
        "",
        "class SeedIngredient(val id: String, val name: String, val unit: String, val category: String)",
        "",
        "class SeedRecipe(",
        "    val id: String, val name: String, val emoji: String, val moments: List<String>, val minutes: Int,",
        "    val ingredients: List<Pair<String, Double>>, val steps: List<String>,",
        ")",
        "",
        "object Seed {",
        "    val ingredients: List<SeedIngredient> = listOf(",
    ]
    for i in ings:
        lines.append(f"        SeedIngredient({k(i['id'])}, {k(i['name'])}, {k(i['unit'])}, {k(i['category'])}),")
    lines += ["    )", "", "    val recipes: List<SeedRecipe> = listOf("]
    for r in recipes:
        for ing_id, _ in r["ingredients"]:
            assert ing_id in known, f"{r['id']}: ingrédient inconnu {ing_id}"
        moments = ", ".join(k(m) for m in r["moments"])
        ings_k = ", ".join(f"{k(i)} to {float(q)}" for i, q in r["ingredients"])
        steps = ", ".join(k(s) for s in r.get("steps", []))
        lines.append(
            f"        SeedRecipe({k(r['id'])}, {k(r['name'])}, {k(r.get('emoji', '🍽️'))}, listOf({moments}), "
            f"{int(r.get('minutes', 0))},\n            listOf({ings_k}),\n            listOf({steps})),"
        )
    lines += ["    )", "}", ""]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"{OUT.relative_to(HERE.parent.parent)} : {len(ings)} ingrédients, {len(recipes)} recettes")


if __name__ == "__main__":
    main()
