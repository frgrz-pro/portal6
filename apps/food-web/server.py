#!/usr/bin/env python3
"""Food — serveur de l'app (stdlib uniquement : http.server + sqlite3).

    python apps/food/server.py [--port 8714] [--host 0.0.0.0] [--reset]

Sert `static/` et une API JSON sous `/api/`. La DB vit dans `data/food/food.db`
(vault, non versionné) ; le catalogue `seed/*.json` est injecté au premier lancement,
puis complété à chaque démarrage sans écraser les recettes modifiées par l'utilisateur.
"""
from __future__ import annotations

import argparse
import json
import mimetypes
import sqlite3
import sys
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
STATIC = HERE / "static"
SEED = HERE.parent / "food" / "seed"  # source de vérité partagée avec l'app Kotlin
DB_PATH = ROOT / "data" / "food" / "food.db"

SCHEMA = """
CREATE TABLE IF NOT EXISTS ingredients (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, unit TEXT NOT NULL, category TEXT NOT NULL,
  sort INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS stock (
  ingredient_id TEXT PRIMARY KEY REFERENCES ingredients(id) ON DELETE CASCADE,
  qty REAL NOT NULL DEFAULT 0, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS recipes (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, emoji TEXT NOT NULL DEFAULT '🍽️',
  moments TEXT NOT NULL DEFAULT '[]', minutes INTEGER NOT NULL DEFAULT 0,
  steps TEXT NOT NULL DEFAULT '[]', source TEXT NOT NULL DEFAULT 'seed',
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS recipe_ingredients (
  recipe_id TEXT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
  ingredient_id TEXT NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
  qty REAL NOT NULL, PRIMARY KEY (recipe_id, ingredient_id)
);
CREATE TABLE IF NOT EXISTS swipes (
  recipe_id TEXT PRIMARY KEY REFERENCES recipes(id) ON DELETE CASCADE,
  verdict TEXT NOT NULL, at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS cooks (
  id INTEGER PRIMARY KEY AUTOINCREMENT, recipe_id TEXT NOT NULL,
  persons REAL NOT NULL, eaten REAL NOT NULL, at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS leftovers (
  id INTEGER PRIMARY KEY AUTOINCREMENT, recipe_id TEXT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
  portions REAL NOT NULL, cooked_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS inbox (
  id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, text TEXT NOT NULL,
  url TEXT, status TEXT NOT NULL DEFAULT 'new', at TEXT NOT NULL
);
"""

LOCK = threading.Lock()


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def connect() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(DB_PATH, check_same_thread=False)
    con.row_factory = sqlite3.Row
    con.execute("PRAGMA foreign_keys = ON")
    return con


def seed(con: sqlite3.Connection) -> None:
    """Injecte le catalogue ; n'écrase jamais ce que l'utilisateur a modifié."""
    con.executescript(SCHEMA)
    ings = json.loads((SEED / "ingredients.json").read_text(encoding="utf-8"))
    for i, ing in enumerate(ings):
        con.execute(
            "INSERT INTO ingredients (id, name, unit, category, sort) VALUES (?,?,?,?,?) "
            "ON CONFLICT(id) DO UPDATE SET sort = excluded.sort",
            (ing["id"], ing["name"], ing["unit"], ing["category"], i),
        )
        con.execute(
            "INSERT OR IGNORE INTO stock (ingredient_id, qty, updated_at) VALUES (?, 0, ?)",
            (ing["id"], now()),
        )
    recipes = json.loads((SEED / "recipes.json").read_text(encoding="utf-8"))
    for r in recipes:
        exists = con.execute("SELECT 1 FROM recipes WHERE id = ?", (r["id"],)).fetchone()
        if exists:
            continue
        con.execute(
            "INSERT INTO recipes (id, name, emoji, moments, minutes, steps, source, created_at) "
            "VALUES (?,?,?,?,?,?,'seed',?)",
            (r["id"], r["name"], r.get("emoji", "🍽️"), json.dumps(r["moments"]),
             r.get("minutes", 0), json.dumps(r.get("steps", []), ensure_ascii=False), now()),
        )
        for ing_id, qty in r["ingredients"]:
            con.execute(
                "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, qty) VALUES (?,?,?)",
                (r["id"], ing_id, qty),
            )
    con.commit()


# --- lecture -----------------------------------------------------------------

def state(con: sqlite3.Connection) -> dict:
    ings = [dict(r) for r in con.execute("SELECT * FROM ingredients ORDER BY category, sort, name")]
    stock = {r["ingredient_id"]: r["qty"] for r in con.execute("SELECT * FROM stock")}
    recipes = []
    for r in con.execute("SELECT * FROM recipes ORDER BY name"):
        d = dict(r)
        d["moments"] = json.loads(d["moments"])
        d["steps"] = json.loads(d["steps"])
        d["ingredients"] = [
            {"ingredient_id": x["ingredient_id"], "qty": x["qty"]}
            for x in con.execute(
                "SELECT ri.ingredient_id, ri.qty FROM recipe_ingredients ri "
                "JOIN ingredients i ON i.id = ri.ingredient_id WHERE recipe_id = ? ORDER BY i.sort",
                (r["id"],),
            )
        ]
        recipes.append(d)
    swipes = [dict(r) for r in con.execute("SELECT * FROM swipes")]
    leftovers = [dict(r) for r in con.execute("SELECT * FROM leftovers ORDER BY cooked_at")]
    cooks = [dict(r) for r in con.execute("SELECT * FROM cooks ORDER BY at DESC LIMIT 50")]
    inbox = [dict(r) for r in con.execute("SELECT * FROM inbox WHERE status = 'new' ORDER BY at DESC")]
    return {
        "ingredients": ings, "stock": stock, "recipes": recipes, "swipes": swipes,
        "leftovers": leftovers, "cooks": cooks, "inbox": inbox, "generated": now(),
    }


# --- écriture ----------------------------------------------------------------

class ApiError(Exception):
    def __init__(self, status: int, msg: str):
        super().__init__(msg)
        self.status = status


def _num(v, name="qty", minimum=0.0) -> float:
    try:
        f = float(v)
    except (TypeError, ValueError):
        raise ApiError(400, f"{name} invalide")
    if f < minimum:
        raise ApiError(400, f"{name} < {minimum}")
    return f


def set_stock(con, ing_id: str, body: dict) -> None:
    if not con.execute("SELECT 1 FROM ingredients WHERE id = ?", (ing_id,)).fetchone():
        raise ApiError(404, "ingrédient inconnu")
    if "delta" in body:
        cur = con.execute("SELECT qty FROM stock WHERE ingredient_id = ?", (ing_id,)).fetchone()
        qty = max(0.0, (cur["qty"] if cur else 0.0) + _num(body["delta"], "delta", -1e9))
    else:
        qty = _num(body.get("qty"))
    con.execute(
        "INSERT INTO stock (ingredient_id, qty, updated_at) VALUES (?,?,?) "
        "ON CONFLICT(ingredient_id) DO UPDATE SET qty = excluded.qty, updated_at = excluded.updated_at",
        (ing_id, qty, now()),
    )


def slug(s: str) -> str:
    import re
    import unicodedata
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "_", s).strip("_") or "x"


def add_ingredient(con, body: dict) -> str:
    name = (body.get("name") or "").strip()
    if not name:
        raise ApiError(400, "nom manquant")
    unit = (body.get("unit") or "pièce").strip()
    cat = (body.get("category") or "placard").strip()
    base = slug(name)
    ing_id, n = base, 1
    while con.execute("SELECT 1 FROM ingredients WHERE id = ?", (ing_id,)).fetchone():
        n += 1
        ing_id = f"{base}_{n}"
    sort = con.execute("SELECT COALESCE(MAX(sort), 0) + 1 FROM ingredients").fetchone()[0]
    con.execute("INSERT INTO ingredients (id, name, unit, category, sort) VALUES (?,?,?,?,?)",
                (ing_id, name, unit, cat, sort))
    con.execute("INSERT INTO stock (ingredient_id, qty, updated_at) VALUES (?,?,?)",
                (ing_id, _num(body.get("qty", 0)), now()))
    return ing_id


def upsert_recipe(con, rid: str | None, body: dict) -> str:
    name = (body.get("name") or "").strip()
    ings = body.get("ingredients")
    if rid is None:
        if not name:
            raise ApiError(400, "nom manquant")
        base = slug(name)
        rid, n = base, 1
        while con.execute("SELECT 1 FROM recipes WHERE id = ?", (rid,)).fetchone():
            n += 1
            rid = f"{base}_{n}"
        con.execute(
            "INSERT INTO recipes (id, name, emoji, moments, minutes, steps, source, created_at) "
            "VALUES (?,?,?,?,?,?,'user',?)",
            (rid, name, body.get("emoji") or "🍽️", json.dumps(body.get("moments") or ["midi", "soir"]),
             int(body.get("minutes") or 0), json.dumps(body.get("steps") or [], ensure_ascii=False), now()),
        )
    else:
        if not con.execute("SELECT 1 FROM recipes WHERE id = ?", (rid,)).fetchone():
            raise ApiError(404, "recette inconnue")
        sets, vals = ["source = 'user'"], []
        if name:
            sets.append("name = ?"); vals.append(name)
        for k in ("emoji",):
            if body.get(k):
                sets.append(f"{k} = ?"); vals.append(body[k])
        if "minutes" in body:
            sets.append("minutes = ?"); vals.append(int(body["minutes"] or 0))
        for k in ("moments", "steps"):
            if isinstance(body.get(k), list):
                sets.append(f"{k} = ?"); vals.append(json.dumps(body[k], ensure_ascii=False))
        vals.append(rid)
        con.execute(f"UPDATE recipes SET {', '.join(sets)} WHERE id = ?", vals)
    if isinstance(ings, list):
        con.execute("DELETE FROM recipe_ingredients WHERE recipe_id = ?", (rid,))
        for x in ings:
            ing_id = x.get("ingredient_id")
            if not con.execute("SELECT 1 FROM ingredients WHERE id = ?", (ing_id,)).fetchone():
                raise ApiError(400, f"ingrédient inconnu : {ing_id}")
            qty = _num(x.get("qty"))
            if qty > 0:
                con.execute(
                    "INSERT OR REPLACE INTO recipe_ingredients (recipe_id, ingredient_id, qty) VALUES (?,?,?)",
                    (rid, ing_id, qty),
                )
    return rid


def cook(con, body: dict) -> dict:
    """Décrémente le stock pour N personnes, met les parts non mangées en restes."""
    rid = body.get("recipe_id")
    if not con.execute("SELECT 1 FROM recipes WHERE id = ?", (rid,)).fetchone():
        raise ApiError(404, "recette inconnue")
    persons = _num(body.get("persons", 1), "persons", 0.25)
    eaten = _num(body.get("eaten", 1), "eaten", 0)
    eaten = min(eaten, persons)
    for x in con.execute("SELECT ingredient_id, qty FROM recipe_ingredients WHERE recipe_id = ?", (rid,)):
        need = x["qty"] * persons
        con.execute(
            "UPDATE stock SET qty = MAX(0, qty - ?), updated_at = ? WHERE ingredient_id = ?",
            (need, now(), x["ingredient_id"]),
        )
    rest = round(persons - eaten, 2)
    leftover_id = None
    if rest > 0:
        cur = con.execute("INSERT INTO leftovers (recipe_id, portions, cooked_at) VALUES (?,?,?)",
                          (rid, rest, now()))
        leftover_id = cur.lastrowid
    con.execute("INSERT INTO cooks (recipe_id, persons, eaten, at) VALUES (?,?,?,?)",
                (rid, persons, eaten, now()))
    con.execute("DELETE FROM swipes WHERE recipe_id = ?", (rid,))
    return {"leftover_id": leftover_id, "rest": rest}


def eat_leftover(con, lid: int, body: dict) -> None:
    row = con.execute("SELECT portions FROM leftovers WHERE id = ?", (lid,)).fetchone()
    if not row:
        raise ApiError(404, "reste inconnu")
    portions = _num(body.get("portions", 1), "portions", 0)
    left = round(row["portions"] - portions, 2)
    if left <= 0:
        con.execute("DELETE FROM leftovers WHERE id = ?", (lid,))
    else:
        con.execute("UPDATE leftovers SET portions = ? WHERE id = ?", (left, lid))


# --- HTTP --------------------------------------------------------------------

class Handler(BaseHTTPRequestHandler):
    server_version = "portal6-food/1"
    con: sqlite3.Connection  # posé sur la classe au démarrage

    def log_message(self, fmt, *args):  # journal compact
        sys.stderr.write("%s %s\n" % (self.address_string(), fmt % args))

    # -- helpers
    def _json(self, status: int, payload) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def _body(self) -> dict:
        n = int(self.headers.get("Content-Length") or 0)
        if n == 0:
            return {}
        try:
            body = json.loads(self.rfile.read(n).decode("utf-8"))
        except ValueError:
            raise ApiError(400, "JSON invalide")
        return body if isinstance(body, dict) else {}

    def _static(self, path: str) -> None:
        if path in ("", "/"):
            path = "/index.html"
        target = (STATIC / path.lstrip("/")).resolve()
        if STATIC not in target.parents or not target.is_file():
            self.send_error(404)
            return
        ctype = mimetypes.guess_type(str(target))[0] or "application/octet-stream"
        if ctype.startswith("text/") or ctype.endswith("javascript") or ctype.endswith("json"):
            ctype += "; charset=utf-8"
        data = target.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        self.wfile.write(data)

    def _route(self, method: str) -> None:
        url = urlparse(self.path)
        parts = [p for p in url.path.split("/") if p]
        if not parts or parts[0] != "api":
            if method == "GET":
                self._static(url.path)
            else:
                self.send_error(405)
            return
        try:
            with LOCK:
                result = self._api(method, parts[1:])
                self.con.commit()
        except ApiError as e:
            self.con.rollback()
            self._json(e.status, {"error": str(e)})
        except Exception as e:  # noqa: BLE001 — on veut le message côté client
            self.con.rollback()
            self._json(500, {"error": f"{type(e).__name__}: {e}"})
        else:
            self._json(200, result)

    def _api(self, method: str, p: list[str]):
        con = self.con
        # GET /api/state
        if method == "GET" and p == ["state"]:
            return state(con)
        # PUT /api/stock/{id}  {qty} | {delta}
        if method == "PUT" and len(p) == 2 and p[0] == "stock":
            set_stock(con, p[1], self._body())
            return state(con)
        # POST /api/ingredients {name, unit, category, qty}
        if method == "POST" and p == ["ingredients"]:
            add_ingredient(con, self._body())
            return state(con)
        # DELETE /api/ingredients/{id}
        if method == "DELETE" and len(p) == 2 and p[0] == "ingredients":
            con.execute("DELETE FROM ingredients WHERE id = ?", (p[1],))
            return state(con)
        # POST /api/recipes | PUT /api/recipes/{id} | DELETE /api/recipes/{id}
        if method == "POST" and p == ["recipes"]:
            rid = upsert_recipe(con, None, self._body())
            s = state(con); s["recipe_id"] = rid
            return s
        if method == "PUT" and len(p) == 2 and p[0] == "recipes":
            upsert_recipe(con, p[1], self._body())
            return state(con)
        if method == "DELETE" and len(p) == 2 and p[0] == "recipes":
            con.execute("DELETE FROM recipes WHERE id = ?", (p[1],))
            return state(con)
        # POST /api/swipes {recipe_id, verdict: like|skip} | DELETE /api/swipes/{id}
        if method == "POST" and p == ["swipes"]:
            b = self._body()
            if b.get("verdict") not in ("like", "skip"):
                raise ApiError(400, "verdict ∈ like|skip")
            if not con.execute("SELECT 1 FROM recipes WHERE id = ?", (b.get("recipe_id"),)).fetchone():
                raise ApiError(404, "recette inconnue")
            con.execute("INSERT OR REPLACE INTO swipes (recipe_id, verdict, at) VALUES (?,?,?)",
                        (b["recipe_id"], b["verdict"], now()))
            return state(con)
        if method == "DELETE" and len(p) == 2 and p[0] == "swipes":
            con.execute("DELETE FROM swipes WHERE recipe_id = ?", (p[1],))
            return state(con)
        # POST /api/cook {recipe_id, persons, eaten}
        if method == "POST" and p == ["cook"]:
            info = cook(con, self._body())
            s = state(con); s.update(info)
            return s
        # POST /api/leftovers/{id}/eat {portions}
        if method == "POST" and len(p) == 3 and p[0] == "leftovers" and p[2] == "eat":
            eat_leftover(con, int(p[1]), self._body())
            return state(con)
        # POST /api/inbox {kind: text|url|photo, text, url}  — cible du Raccourci iOS
        if method == "POST" and p == ["inbox"]:
            b = self._body()
            text = (b.get("text") or "").strip()
            url = (b.get("url") or "").strip() or None
            if not text and not url:
                raise ApiError(400, "text ou url requis")
            con.execute("INSERT INTO inbox (kind, text, url, at) VALUES (?,?,?,?)",
                        (b.get("kind") or ("url" if url and not text else "text"), text, url, now()))
            return {"ok": True}
        if method == "DELETE" and len(p) == 2 and p[0] == "inbox":
            con.execute("UPDATE inbox SET status = 'done' WHERE id = ?", (int(p[1]),))
            return state(con)
        raise ApiError(404, "route inconnue")

    def do_GET(self): self._route("GET")
    def do_HEAD(self):
        self.send_response(200); self.send_header("Content-Length", "0"); self.end_headers()
    def do_POST(self): self._route("POST")
    def do_PUT(self): self._route("PUT")
    def do_DELETE(self): self._route("DELETE")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--port", type=int, default=8714)
    ap.add_argument("--reset", action="store_true", help="supprime la DB et repart du seed")
    args = ap.parse_args()
    if args.reset and DB_PATH.exists():
        DB_PATH.unlink()
        print(f"DB supprimée : {DB_PATH}")
    con = connect()
    seed(con)
    Handler.con = con
    n_r = con.execute("SELECT COUNT(*) FROM recipes").fetchone()[0]
    n_i = con.execute("SELECT COUNT(*) FROM ingredients").fetchone()[0]
    print(f"Food · {n_r} recettes, {n_i} ingrédients · {DB_PATH.relative_to(ROOT)}")
    print(f"→ http://localhost:{args.port}  (écoute sur {args.host})")
    srv = ThreadingHTTPServer((args.host, args.port), Handler)
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
