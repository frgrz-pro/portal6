r"""Verifie l'etat des chemins dans une base Plex (avant / apres reecriture).

Lecture seule, via le sqlite3 de Python : les 4 colonnes concernees sont des
tables ordinaires, donc lisibles sans le SQLite custom de Plex. L'ECRITURE, elle,
doit passer par `Plex SQLite.exe` (cf. rewrite_paths.sql).

    python check_paths.py "<chemin>\com.plexapp.plugins.library.db"

Sortie : le compte de valeurs encore en chemins Windows (doit tomber a 0 apres
reecriture) et le compte de valeurs deja en chemins conteneur.
Code de sortie 0 si la base est coherente (100 % Windows ou 100 % conteneur),
1 si elle est dans un etat mixte — c'est-a-dire une reecriture incomplete.
"""

import re
import sqlite3
import sys

# Lettres de lecteur portant des medias Plex (cf. .docs/plex-docker.md §1).
DRIVES = "DGHI"

# (table, colonne, forme) ; les 4 seules colonnes concernees, etablies par un scan
# exhaustif de toutes les tables reelles de la base le 2026-09-06.
COLUMNS = [
    ("media_parts", "file", "brute"),
    ("section_locations", "root_path", "brute"),
    ("media_streams", "url", "url"),
    ("metadata_items", "guid", "url"),
]

WINDOWS = {
    "brute": re.compile(r"^[" + DRIVES + r"]:\\", re.IGNORECASE),
    "url": re.compile(r"^file:///[" + DRIVES + r"]:/", re.IGNORECASE),
}
CONTENEUR = {
    "brute": re.compile(r"^/data/[" + DRIVES.lower() + r"]/", re.IGNORECASE),
    "url": re.compile(r"^file:///data/[" + DRIVES.lower() + r"]/", re.IGNORECASE),
}


def main(db_path):
    con = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
    con.text_factory = lambda b: b.decode("utf-8", "replace")

    print(f"Base : {db_path}\n")
    print(f"{'TABLE.COLONNE':<32} {'WINDOWS':>9} {'CONTENEUR':>10}   EXEMPLE")
    print("-" * 112)

    total_win = 0
    total_lin = 0
    for table, col, forme in COLUMNS:
        rows = [
            v
            for (v,) in con.execute(
                f'select "{col}" from "{table}" '
                f'where "{col}" is not null and "{col}" <> \'\''
            )
        ]
        win = [v for v in rows if WINDOWS[forme].match(v)]
        lin = [v for v in rows if CONTENEUR[forme].match(v)]
        sample = (win or lin or [""])[0][:60]
        print(f"{table + '.' + col:<32} {len(win):>9} {len(lin):>10}   {sample}")
        total_win += len(win)
        total_lin += len(lin)

    print("-" * 112)
    print(f"{'TOTAL':<32} {total_win:>9} {total_lin:>10}")
    print()

    if total_win and total_lin:
        print("ETAT MIXTE — la reecriture est incomplete. Ne pas demarrer le conteneur.")
        return 1
    if total_win:
        print("Base encore en chemins Windows : reecriture pas encore faite.")
    else:
        print("Base entierement en chemins conteneur : reecriture OK.")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1]))
