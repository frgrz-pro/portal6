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
| **Coordinateur Zigbee** — **Sonoff Zigbee 3.0 USB Dongle Plus « Dongle-P »** (CC2652P, USB) | ✅ acheté 2026-09-07 | ~25 € | Branché sur le PC Windows Docker ; reste à installer le pilote CP210x + lancer le pont TCP ([zigbee-multiprises.md](zigbee-multiprises.md)) |
| *(le Dongle Max Ethernet pressenti le 2026-08-30 n'a pas été retenu : l'hôte Docker est à portée d'USB, moitié moins cher)* | — | — | — |
| Tour Docker (Home Assistant + AzuraCast) | ✅ de fait : le PC Windows (Docker Desktop) héberge HA, Plex, AzuraCast | — | ❓ machine définitive ou étape ? Si une tour Linux arrive, le dongle USB la suit |
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
3. ~~Clé Zigbee~~ — achetée (Dongle-P), plus rien à acheter côté domotique.

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

### 2026-09-07
Coordinateur Zigbee ✅ : Sonoff Dongle-P (USB) acheté à la place du Dongle Max.
« Tour Docker » = le PC Windows, de fait.
