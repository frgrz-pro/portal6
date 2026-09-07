# Multiprises Zigbee — comprendre et piloter

2 multiprises Zigbee, plusieurs lampes branchées sur chaque prise. Objectif : les
piloter depuis l'app remote ([app-remote.md](app-remote.md)). Depuis le
2026-09-07, le même réseau accueille aussi **4 interrupteurs physiques Zigbee**
(section dédiée plus bas).

## Questions ouvertes

- [ ] **Qui pilote quoi** : quel bouton (et quel geste) commande quelle prise/lampe ?
  (tableau à remplir dans la section Interrupteurs une fois les multiprises
  appairées et leurs prises nommées).
- [ ] **Latence ZHA sur les TS0044** : des retours communauté signalent ~1 s entre
  appui et action sous ZHA, instantané sous Zigbee2MQTT. À mesurer sur le
  premier bouton appairé ; si c'est gênant, c'est LE déclencheur pour basculer
  sur Z2M (déjà prêt en commentaire dans le compose).

- [ ] **Marque et modèle exacts des multiprises** (étiquette dessous / boîte).
  C'est LA question bloquante : elle détermine la compatibilité et le nombre de
  prises pilotables individuellement.
- [ ] Vendues avec un hub/passerelle constructeur (Tuya, etc.) ou nues ?
- [ ] Chaque prise est-elle commutable individuellement, ou la multiprise
  s'allume/s'éteint en bloc ? (+ ports USB pilotables ?)
- [ ] **Jeton longue durée HA → `.env` `HA_TOKEN`** : sans lui Claude ne peut ni
  sonder les entités (`ha_probe.py`), ni corriger la localisation, ni brancher l'app.
- [ ] **Appairer les 2 multiprises** : bouton d'appairage (5 s, LED clignote) →
  ZHA « Ajouter un appareil ». La fiche ZHA donnera enfin marque/modèle et le
  nombre de prises pilotables — ça tranche les 3 premières questions.
- [ ] Firmware du dongle : Z-Stack **rev 20210708** d'usine. Fonctionne avec ZHA ;
  Koenkk recommande ≥ 20211217 (stabilité, plus de routes). Mise à jour possible
  plus tard via le bootloader série (cc2538-bsl), **pas avant** que le réseau
  soit formé et sauvegardé — pas bloquant.
- [ ] Le PC Windows héberge aujourd'hui toute la pile Docker (HA, Plex, AzuraCast) :
  est-ce « la tour » définitive ou une étape ? Si une machine Linux arrive, le
  dongle la suit (`/dev/ttyUSB0` direct, plus de pont).

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
2. ~~Acheter le coordinateur~~ **Acheté le 2026-09-07 : Sonoff Zigbee 3.0 USB
   Dongle Plus « Dongle-P »** (CC2652P + CP2102N, firmware Z-Stack coordinateur
   d'usine, supporté nativement par ZHA via zigpy-znp). Le Dongle Max Ethernet
   pressenti le 2026-08-30 n'a pas été retenu : le PC qui héberge Docker est
   sous la main, l'USB suffit et coûte moitié moins. Ligne au [BOM central](bom.md).
3. Home Assistant en Docker + ZHA (commencer simple ; migrer vers Zigbee2MQTT
   seulement si un device est mal supporté par ZHA). **Montage Windows** : voir
   section suivante.
4. Appairer les 2 multiprises → chaque prise devient une entité `switch.xxx`.
5. L'app appelle l'API REST HA :
   - `POST /api/services/switch/turn_on` / `turn_off` avec `{"entity_id": "switch.xxx"}`
   - « All / Turn off » = un groupe HA ou un appel avec la liste d'entités.
   - État temps réel via l'API WebSocket HA.
   - Auth : long-lived access token (profil utilisateur HA), stocké dans l'app.

Bonus alignement : le même HA alimente l'affichage d'état sur le TRMNL
(use case n°2 de `features/home/README.md`) — un seul backend pour les deux projets.

## Montage retenu : Dongle-P sur le PC Windows, pont série→TCP (2026-09-07)

Contrainte : HA tourne dans **Docker Desktop (backend WSL2)**, qui ne passe pas
les périphériques USB aux conteneurs. Deux façons de contourner :

| Voie | Principe | Verdict |
|---|---|---|
| **Pont série→TCP sur l'hôte** (`features/home/ha/zigbee_bridge.py`) | Python ouvre le COM du dongle et l'expose sur `0.0.0.0:6638` ; ZHA se connecte en `socket://host.docker.internal:6638`, exactement comme un coordinateur Ethernet | **Retenu** — compose inchangé et portable (une future tour Linux = `/dev/ttyUSB0`), ZHA sait se reconnecter, testé : le conteneur joint bien l'hôte (firewall OK) |
| usbipd-win → WSL2 | attacher l'USB à la VM WSL, `--device /dev/ttyUSB0` dans le compose | Fallback documenté — le noyau WSL2 6.6 a bien `cp210x` (module), mais l'attache saute à chaque redémarrage de Docker Desktop et le conteneur refuse de démarrer si le device manque |

Séquence de mise en route :

1. ✅ Pilote CP210x installé (Silabs 11.6.0.420, Windows Update ne le proposait
   pas) → dongle sur **COM4** (le script le retrouve seul par VID:PID).
2. ✅ Pont validé de bout en bout : `SYS_VERSION` répond en direct sur COM4 et à
   travers le pont depuis le conteneur HA (CC2652, Z-Stack 2.7 rev 20210708).
   Tâche planifiée `portal6-zigbee-bridge` créée et en cours (Running).
3. ✅ HA → **Paramètres → Appareils et services → Ajouter → Zigbee Home Automation**,
   type de radio **ZNP (Texas Instruments)**, port `socket://host.docker.internal:6638`
   (vitesse et contrôle de flux indifférents en mode socket). Fait par François
   le 2026-09-07 sur l'instance recréée à neuf.
4. ✅ Réseau formé : coordinateur « Texas Instruments CC2652 » visible dans ZHA,
   IEEE `00:12:4B:00:38:A8:EB:B6`, `config/zigbee.db` créé (hors git). Le config
   flow a sondé le port plusieurs fois (connexions/déconnexions en rafale dans
   le journal du pont — normal), puis la connexion tient.
5. Appairer : multiprise en mode appairage (bouton 5 s en général) →
   « Ajouter un appareil » dans ZHA.

Détails électriques notés dans le script : sur le Dongle-P les lignes DTR/RTS
pilotent reset et bootloader, on les laisse basses à l'ouverture du port.

## Interrupteurs physiques ×4 — MOES « Zigbee Wireless 12 » ESZ-0ZAA-EU (reçus le 2026-09-07)

Rôle dans le plan : la commande **au mur / sur la table**, en complément de l'app.
Objectif final : un bouton = une prise (ou un groupe de lampes), avec l'app
remote et le TRMNL qui reflètent l'état quoi qu'il arrive.

Fiche (identifié le 2026-09-07) :

| | |
|---|---|
| Identité Zigbee | **TS0044**, fabricant `_TZ3000_wkai4ga5` (ou `_TZ3000_vp6clf9d`) — plateforme Tuya, produit Eardatek vendu sous MOES, Girier, Lonsonho, Aubess… |
| Type | **Bouton sans fil à pile** (CR2430) → *end device* : ne relaie pas le mesh, s'appaire près du coordinateur |
| Touches | 4 touches × 3 gestes (simple / double / long) = les « 12 scènes » du nom commercial |
| Dans ZHA | Supporté (quirk Tuya intégré). Pas d'entité `switch` : un capteur batterie + des **événements `zha_event`**, un par geste, avec `endpoint_id` 1–4 = la touche et `command` `remote_button_short_press` / `remote_button_double_press` / `remote_button_long_press` |
| Appairage | **Touche en bas à gauche, 10 s**, jusqu'à ce que les 4 LED clignotent |
| **Binding direct** | **Non** : le TS0044 envoie des commandes Tuya propriétaires (pas un On/Off standard) → une prise ne peut pas l'écouter en direct. Tout passe par HA |
| Point d'attention | Latence ~1 s signalée sous ZHA par certains utilisateurs (instantané sous Z2M) — voir question ouverte |

Conséquence sur l'arbitrage binding vs automatisation : **tranché, automatisation
HA obligatoire** pour ces boutons. Donc : PC/HA/pont éteints = boutons morts.
C'est acceptable (les boutons physiques des multiprises restent la roue de
secours), et ça renforce le « pont en tâche planifiée + HA `restart: unless-stopped` ».

Comment on câble dans HA (une fois les entités nommées) :

- **Blueprint** communautaire « MOES 12 scene switch — easy button mapping »
  (ZHA + Z2M) : une automatisation par bouton, 12 champs « geste → action ».
  Ou une automatisation maison par bouton : déclencheur *Événement* `zha_event`
  avec `device_ieee` + `endpoint_id` + `command`, action `switch.toggle`.
- Convention proposée : **simple** = toggle la prise associée à la touche,
  **long** = éteindre tout le groupe, **double** = libre (scène TV, etc.).

Séquence :

1. Appairer les **multiprises d'abord** (routers, elles solidifient le mesh) et
   nommer chaque prise (`switch.salon_lampe_bureau`…).
2. Appairer les 4 boutons **à moins de 2 m du dongle**, un par un (bas-gauche
   10 s), nommés par emplacement : `bouton_canape`, `bouton_entree`…
3. Tester : Outils de développement → Événements → écouter `zha_event` et
   appuyer → noter `device_ieee`, `endpoint_id`, `command`. Mesurer la latence
   à l'œil (appui → log).
4. Remplir le tableau « qui pilote quoi », puis une automatisation par bouton.

| Bouton (nom ZHA) | Emplacement | Touche 1 | Touche 2 | Touche 3 | Touche 4 |
|---|---|---|---|---|---|
| — | — | — | — | — | — |
| — | — | — | — | — | — |
| — | — | — | — | — | — |
| — | — | — | — | — | — |

Portée : un mesh de 2 multiprises (routers) + coordinateur couvre un appartement
sans souci ; si un bouton à pile à l'autre bout perd le lien, c'est une
multiprise mal placée, pas le dongle. Rallonge USB pour éloigner le dongle du
PC/USB 3 (interférences 2,4 GHz) = bon réflexe, ligne au BOM.

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

### 2026-09-07
Dongle **Sonoff Dongle-P** branché sur le PC Windows (celui qui fait tourner
toute la pile Docker). Détecté en `VID_10C4&PID_EA60` mais **sans pilote CP210x**
(code 28) → action François. Montage retenu : pont série→TCP
`zigbee_bridge.py` + ZHA en `socket://host.docker.internal:6638` ; joignabilité
conteneur→hôte vérifiée. `HA_TOKEN` toujours absent de `.env`.

### 2026-09-07 (bis)
Pilote CP210x Silabs installé (pnputil, UAC) → COM4. Z-Stack répond (rev
20210708) en direct et via le pont depuis le conteneur. Tâche planifiée
`portal6-zigbee-bridge` (à l'ouverture de session, `--log`) en cours. Reste :
ZHA à configurer dans HA (auth François), puis appairage des multiprises.

### 2026-09-07 (ter)
Instance HA recréée à neuf (mot de passe perdu par autofill Brave à l'onboarding).
François a configuré **ZHA sur le pont** : coordinateur CC2652 reconnu, réseau
formé. Pont : avertissement `WinError 10038` à la fermeture normale d'un client
rendu silencieux (prend effet au prochain redémarrage de la tâche). Reste :
jeton `HA_TOKEN`, appairage des multiprises.

### 2026-09-07 (quater)
**4 interrupteurs physiques Zigbee** reçus, intégrés au plan : section dédiée
(bouton à pile = événements `zha_event`, filaire = `switch.*`), arbitrage
binding direct vs automatisation HA (reco : binding quand supporté), ordre
d'appairage multiprises → boutons, tableau « qui pilote quoi » à remplir.
Modèle identifié dans la foulée : **MOES ESZ-0ZAA-EU = Tuya TS0044** (à pile,
4 × 3 gestes, appairage bas-gauche 10 s). Binding direct impossible (commandes
Tuya propriétaires) → automatisations HA, tranché. Risque à mesurer : latence
ZHA ~1 s rapportée, Z2M en plan B.
