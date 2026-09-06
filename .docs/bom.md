# BOM central — tout le hardware portal6

Vue consolidée des achats matériels de **tous** les projets portal6. Règle : toute
ligne matérielle naît ou se met à jour **ici** ; un projet qui a besoin d'un BOM
détaillé (comme la radio) garde son doc dédié, et cette page n'en porte que le
résumé + lien.

Statuts : ✅ possédé · 🛒 à acheter · ⏸ bloqué (voir « Débloqué par ») · ❓ à clarifier

## Domotique / app remote

| Article | Statut | Prix | Débloqué par |
|---|---|---|---|
| Multiprises Zigbee ×2 | ✅ possédé | — | ❓ marque/modèle à identifier ([zigbee-multiprises.md](zigbee-multiprises.md)) |
| **Coordinateur Zigbee** — choix pressenti : **Sonoff Dongle Max (Dongle-M)**, EFR32MG24 + ESP32, Ethernet/PoE/Wi-Fi/USB — l'Ethernet le rend indépendant de l'emplacement de la tour | 🛒 | ~40–50 € | Tour Docker allumée en continu ; l'utiliser en **Zigbee pur** (pas de multipan Zigbee+Thread) ; PoE seulement si switch PoE, sinon USB-C |
| *(alternatives écartées : Dongle-E ~25 € — USB only, colle le coordinateur à la tour ; SLZB-06 — même idée Ethernet mais le Max est plus récent/complet)* | — | — | — |
| Tour Docker (Home Assistant + AzuraCast) | ❓ | — | Machine déjà identifiée côté radio ([design-serveur-azuracast.md](hardware/design-serveur-azuracast.md)) — confirmer que c'est la même |
| Shield TV Pro, barre TCL, télé, téléphone | ✅ possédé | — | — |

## TRMNL (affichage e-ink)

| Article | Statut | Prix | Débloqué par |
|---|---|---|---|
| TRMNL (premier device généraliste) | ✅ possédé — **TRMNL OG** (800 × 480, 1-bit), confirmé le 2026-09-06 | — | — |
| **Developer Edition** (addon, une fois, par device — 1 seul suffit pour créer les plugins) | 🛒 **à acheter maintenant** | 20 $ | Débloqué : les 2 premiers private plugins sont écrits ([trmnl-dashboard.md](trmnl-dashboard.md)), ils ne peuvent pas être poussés sans l'addon |
| TRMNL X 10.3" / devices spécialisés | ⏸ plus tard | — | Use cases validés sur le premier device |

## Radio Brandt RK 711S

Détail complet et audité dans [hardware/bom.md](hardware/bom.md) — rien n'est
commandé, achats par vagues avec conditions de déblocage. Résumé :

| Poste | Estimation | Note |
|---|---|---|
| Vague 1 — Outillage | 195–220 € | **Seul achat non bloqué, à faire en premier** |
| Vagues 2–6 — Matériel | 376–606 € | Étalé sur les phases, chaque vague a ses conditions |
| **Total projet** | **≈ 570–825 €** | + 10–15 % de casse/apprentissage |

## Synthèse — ce qui est achetable maintenant

1. **Outillage radio** (~200 €) — non bloqué, sert aussi à tout le reste du bricolage.
2. **Dev Edition TRMNL** (20 $) — **à acheter tout de suite** : les 2 premiers private plugins sont écrits et attendent l'addon pour être poussés.
3. **Clé Zigbee** (~25–40 €) — dès que la tour tourne (sinon elle dort dans un tiroir).

## Journal

### 2026-08-30
Création du BOM central : agrégation domotique (clé Zigbee à acheter), TRMNL
(Dev Edition 20 $), et résumé du BOM radio existant. À clarifier : TRMNL déjà
possédé ?, la tour Docker = machine AzuraCast ?

### 2026-09-06
TRMNL passé en ✅ possédé (setup en cours). Dev Edition débloquée, différée au premier private plugin.

### 2026-09-06 (bis)
Device confirmé **TRMNL OG**. Dev Edition passée de « différée » à « à acheter
maintenant » : les deux private plugins sont écrits, l'addon est le seul blocage.
