# Multiprises Zigbee — comprendre et piloter

2 multiprises Zigbee, plusieurs lampes branchées sur chaque prise. Objectif : les
piloter depuis l'app remote ([app-remote.md](app-remote.md)).

## Questions ouvertes

- [ ] **Marque et modèle exacts des multiprises** (étiquette dessous / boîte).
  C'est LA question bloquante : elle détermine la compatibilité et le nombre de
  prises pilotables individuellement.
- [ ] Vendues avec un hub/passerelle constructeur (Tuya, etc.) ou nues ?
- [ ] Chaque prise est-elle commutable individuellement, ou la multiprise
  s'allume/s'éteint en bloc ? (+ ports USB pilotables ?)
- [ ] La tour Docker est-elle montée/allumée en continu ? (prérequis Home Assistant)

## Comment marche Zigbee (l'essentiel)

- Zigbee est une **radio mesh** basse conso (2.4 GHz), **pas du Wi-Fi** : ni le
  téléphone ni le routeur Mercusys ne peuvent parler aux multiprises directement.
- Il faut un **coordinateur** : soit le hub du constructeur (souvent cloud, app
  fermée), soit une **clé USB Zigbee** branchée sur une machine qui fait tourner
  un logiciel domotique. Les devices s'« appairent » au coordinateur une fois,
  puis les devices alimentés (comme des multiprises) servent aussi de répéteurs
  du mesh.
- Tant qu'aucun coordinateur n'est en place, les multiprises sont juste… des
  multiprises (les boutons physiques marchent, rien n'est pilotable).

## Options de pilotage

| Option | Principe | Verdict |
|---|---|---|
| A. Hub constructeur + app/cloud | ex. passerelle Tuya + Smart Life | Rapide mais cloud, app fermée, API pénible — contraire à l'esprit du projet |
| B. **Home Assistant + clé USB Zigbee** | ZHA (intégré) ou Zigbee2MQTT | **Recommandé** — local, API propre pour l'app, déjà planifié dans `features/home/README.md` (affichage TRMNL) |
| C. Zigbee2MQTT seul + MQTT | plus léger que HA | Possible, mais HA apporte l'UI d'appairage, l'historique, et le pont TRMNL déjà prévu |

## Le point qui ne s'esquive pas : un hôte Home Assistant 24/7

Le dongle (USB **ou** Ethernet) n'est que la radio. Dans tous les cas il faut une
machine allumée en continu qui fait tourner Home Assistant — le Mercusys ne peut
pas l'héberger, la Shield non plus, le Mac n'est pas allumé 24/7.

- **Dongle USB** → se branche physiquement **sur cette machine-là**. Pas de
  machine = le dongle USB ne se branche nulle part.
- **Dongle Ethernet (Dongle Max)** → se branche au réseau, mais HA doit quand
  même tourner quelque part ; l'Ethernet ne fait que découpler l'emplacement de
  la radio de celui du serveur.

Candidats hôte, par ordre de préférence :

| Hôte | Verdict |
|---|---|
| **Tour Docker** (déjà prévue pour AzuraCast) | **Plan A** — zéro achat, mutualise radio + domotique + TRMNL |
| Raspberry Pi 4/5 dédié + HA OS | Plan B si la tour tarde — ~80-120 € tout compris, la solution HA la plus documentée |
| Mini-PC N100 d'occasion/neuf | Plan B' — ~120-150 €, plus costaud qu'un Pi, pourrait même remplacer la tour |

## Plan retenu (option B)

1. Identifier les multiprises → vérifier sur [zigbee2mqtt.io/supported-devices](https://www.zigbee2mqtt.io/supported-devices/)
   et la liste ZHA.
2. Acheter le coordinateur — **choix pressenti (2026-08-30) : Sonoff Dongle Max
   (Dongle-M)**, ~40-50 €, EFR32MG24 + ESP32, connecté en **Ethernet** (ou PoE,
   ou USB-C en secours) : le coordinateur vit sur le réseau, indépendant de
   l'emplacement de la tour. Support officiel ZHA et Zigbee2MQTT, console web.
   Réserves connues : produit récent (reviews janv. 2026), doc/portail web
   perfectibles, et surtout **ne pas activer le mode multipan Zigbee+Thread**
   (instable partout) — on le configure en coordinateur Zigbee pur, Thread reste
   une option future en reflashant. Alternatives écartées : Dongle-E (USB only),
   SLZB-06 (équivalent Ethernet, moins récent). Ligne au [BOM central](bom.md).
3. Home Assistant en Docker sur la tour + ZHA (commencer simple ; migrer vers
   Zigbee2MQTT seulement si un device est mal supporté par ZHA).
4. Appairer les 2 multiprises → chaque prise devient une entité `switch.xxx`.
5. L'app appelle l'API REST HA :
   - `POST /api/services/switch/turn_on` / `turn_off` avec `{"entity_id": "switch.xxx"}`
   - « All / Turn off » = un groupe HA ou un appel avec la liste d'entités.
   - État temps réel via l'API WebSocket HA.
   - Auth : long-lived access token (profil utilisateur HA), stocké dans l'app.

Bonus alignement : le même HA alimente l'affichage d'état sur le TRMNL
(use case n°2 de `features/home/README.md`) — un seul backend pour les deux projets.

## Journal

### 2026-08-30
Création du doc. Vulgarisation Zigbee (besoin d'un coordinateur), choix option B
(HA + clé USB, aligné avec le plan TRMNL). Bloquant : identifier marque/modèle
des multiprises. Coordinateur pressenti après recherche : **Sonoff Dongle Max**
(Ethernet/PoE, ZHA + Z2M officiels) — en Zigbee pur, sans multipan.

### 2026-09-06
`features/home/ha/docker-compose.yml` écrit : HA seul pour le dev (Docker Desktop Windows,
intégration Demo pour des `switch.*` factices), sections mosquitto/zigbee2mqtt en
commentaire pour la tour. Sonde `ha_probe.py` (liste + toggle d'une entité via
`.env` HA_URL/HA_TOKEN). Toujours bloquant : marque/modèle des multiprises.
