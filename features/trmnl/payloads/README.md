# payloads — données servies aux private plugins TRMNL

Fichiers **générés**, commités volontairement : c'est GitHub qui les sert en brut
(`raw.githubusercontent.com`) et TRMNL qui vient les chercher en polling. Même
pattern que les .ics eSport — aucune infra à héberger.

| Fichier | Produit par | Regénéré par |
|---|---|---|
| `ciel-mer.json` | [`../data/build_ciel_mer.py`](../data/build_ciel_mer.py) | [`.github/workflows/trmnl-ciel-mer.yml`](../../../.github/workflows/trmnl-ciel-mer.yml), toutes les 3 h |

⚠️ **Tout ce qui atterrit ici est public.** Les données privées (agenda perso) ne
passent pas par ce dossier : elles vont directement à TRMNL par webhook, cf.
[`../data/build_agenda.py`](../data/build_agenda.py).

Ne rien éditer à la main : le prochain run du workflow écrase le fichier.
