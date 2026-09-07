# Multiprises Shelly + boutons Zigbee — piloter les lampes depuis HA

*(fichier historiquement nommé `zigbee-multiprises.md` : les multiprises se sont
révélées être des Shelly Wi-Fi, le nom est resté pour ne pas casser les liens)*

2 multiprises **Shelly Power Strip 4 Gen4** (Wi-Fi, 4 prises + mesure de conso
par prise) et 4 boutons **MOES TS0044** (Zigbee, à pile), plusieurs lampes
branchées sur chaque prise. Objectif : tout piloter depuis Home Assistant, donc
depuis l'app remote ([app-remote.md](app-remote.md)) et les boutons.

Architecture retenue le 2026-09-07 :

```
boutons MOES ──Zigbee──▶ dongle Sonoff ──pont TCP──▶ ZHA ┐
                                                          ├─ Home Assistant ──▶ app remote / TRMNL
multiprises Shelly ──Wi-Fi (LAN)──▶ intégration Shelly ───┘         (automatisations bouton → prise)
```

## Questions ouvertes

- [ ] **Firmware Shelly** : 1.7.99 installé, **2.0.0 stable** proposé (entité
  `update.multiprise_x_firmware` dans HA). Majeure → faire une multiprise
  d'abord, vérifier que HA la retrouve, puis l'autre. Pas urgent.
- [ ] Mot de passe sur la web UI des Shelly (`auth`) : l'AP et le BLE sont coupés,
  il reste l'accès HTTP local sans auth depuis le LAN. Acceptable en LAN privé ;
  si on met un mot de passe, le renseigner aussi dans HA (option de l'entrée).
- [ ] **Qui pilote quoi** : quel bouton (et quel geste) commande quelle prise ?
  (tableau dans la section Boutons, à remplir une fois les prises nommées).
- [ ] Appairer les **Boutons 2, 3, 4** (même procédure) et créer leurs
  automatisations (Bouton 2 → Multiprise B ; 3 et 4 : à décider).
- [ ] **Sauvegarde ZHA** : faite le 2026-09-07 (`config/zha-backup-*.json`, hors
  git — contient la clé réseau). À refaire après chaque appairage.
- [ ] Portée Zigbee sans routeur : le réseau n'a que le coordinateur + 4 boutons à
  pile (les Shelly ne relaient pas puisqu'ils restent en Wi-Fi). Si un bouton
  décroche à l'autre bout de l'appartement : rallonge USB pour le dongle, puis
  une prise Zigbee routeur à ~10 € (ligne au BOM) — pas les Shelly en Zigbee.
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

## Multiprises : Shelly Power Strip 4 Gen4, en Wi-Fi (tranché le 2026-09-07)

Identifiées le 2026-09-07 : **Shelly Power Strip 4 Gen4** — 4 prises commutables
individuellement, mesure de conso par prise, 16 A total / 12 A par prise,
radios Wi-Fi + Bluetooth + Zigbee + Matter (profil Matter par défaut, Zigbee en
remplacement de Matter, Wi-Fi toujours actif).

| Mode | Ce qu'on a | Verdict |
|---|---|---|
| **Wi-Fi + intégration Shelly native de HA** | 4 `switch.*` + capteurs puissance/énergie par prise, MAJ firmware depuis HA, API RPC locale, webhooks, web UI embarquée ; indépendant du dongle et du pont | **Retenu** |
| Zigbee via ZHA | On/Off par prise, et le Shelly relaie le mesh | **Écarté** : bug connu — deux Power Strip 4 Gen4 en Zigbee inondent le réseau (0,5–1,5 msg/s au repos, coordinateur CC2652 qui décroche), aucune correction Shelly à janv. 2026, les utilisateurs touchés sont repassés en Wi-Fi. Perte du BTHome et pas de garantie sur la conso par prise |

Conséquence : **le Zigbee ne sert qu'aux boutons MOES**. Les Shelly ne sont
jamais mis en mode Zigbee (la combinaison Bouton 1 + 5 × Bouton 4 bascule
Matter ↔ Zigbee : ne pas y toucher).

Mise en route côté HA (Docker Desktop, sans mDNS → ajout manuel) — **fait le
2026-09-07** :

1. ✅ Shelly sur le Wi-Fi 2,4 GHz. Piège rencontré : le QR code sur la
   multiprise est le code **Matter** (Apple Home), pas la mise sur Wi-Fi.
   L'AP n'était pas actif d'usine → **Boutons 1 + 4 maintenus 5 s** (LED bleue),
   puis téléphone sur `ShellyPStripG4-xxxx` → http://192.168.33.1 → Wi-Fi.
2. ✅ Ajout dans HA par l'API (config flow `shelly`, hôte + port 80) :

   | | Multiprise A | Multiprise B |
   |---|---|---|
   | IP | `192.168.0.78` | `192.168.0.98` |
   | id Shelly | `shellypstripg4-48f6eedd4148` | `shellypstripg4-d885aceb742c` |
   | modèle / fw | S4PL-00416EU, 1.7.99 | idem |
   | RSSI | −65 dBm | −71 dBm |

3. ✅ **Verrouillage** (François : « quelqu'un qui passe peut se connecter à mes
   prises ») : AP Wi-Fi ouvert **désactivé**, Bluetooth (+ RPC BLE) **désactivé**,
   Matter **désactivé** (reboot), cloud déjà off. Il ne reste que l'API HTTP
   locale sur le LAN, celle que HA utilise. Pour récupérer l'AP un jour (nouveau
   Wi-Fi) : Boutons 1 + 4, 5 s.
4. ✅ Renommage dans le registre HA (WebSocket `config/entity_registry/update`) :
   devices « Multiprise A/B », entités `switch.multiprise_a_prise_1…4` et
   `switch.multiprise_b_prise_1…4` — **exactement les `entityId` déjà codés dans
   l'app** (`DefaultLights`). Capteurs : `sensor.multiprise_a_prise_1_power` /
   `_energy`, `update.multiprise_a_firmware`, etc.
5. ✅ Test réel : `switch.turn_on` puis `turn_off` sur A prise 1 par l'API HA,
   état renvoyé cohérent.
6. **Nommage générique, tranché par François le 2026-09-07** : on garde
   « Multiprise A / B » et « prise 1…4 » (A = `.78`, B = `.98`), et « Bouton 1…4 »
   pour les MOES — pas de noms par lampe. Les lampes changent de prise, pas les
   identifiants ; le sens (« lampe du canapé ») vit dans les configs de pièce de
   l'app, pas dans HA.

## Plan retenu (option B) — historique, coordinateur pour les boutons

1. ~~Identifier les multiprises~~ Fait : Shelly Wi-Fi, voir section précédente.
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
| Latence | **Mesurée le 2026-09-07 : 66–92 ms** entre `zha_event` et le changement d'état de la prise Shelly (appui long → 8 prises en ≤ 145 ms). La crainte communautaire (~1 s sous ZHA) ne se vérifie pas ici ; Z2M reste en réserve mais n'a plus de motif |

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

1. Multiprises Shelly intégrées en Wi-Fi et prises nommées
   (`switch.salon_lampe_bureau`…) — section Multiprises.
2. Appairer les 4 boutons **à moins de 2 m du dongle**, un par un. Pas de bouton
   reset sur le MOES : c'est la **touche en bas à gauche, ~10 s**, jusqu'au
   clignotement des LED (retirer d'abord la languette isolante de la pile si
   présente ; le QR code au dos est un code Tuya, inutile ici). Le réseau doit
   être ouvert : « Ajouter un appareil » dans ZHA, ou service `zha.permit` par
   l'API (240 s max par appel). Noms : `Bouton 1` … `Bouton 4`.
3. Tester : Outils de développement → Événements → écouter `zha_event` et
   appuyer → noter `device_ieee`, `endpoint_id`, `command`. Mesurer la latence
   à l'œil (appui → log).
4. Remplir le tableau « qui pilote quoi », puis une automatisation par bouton.

Disposition des touches (endpoint ZHA = touche) : **1 haut-gauche, 2 haut-droite,
3 bas-gauche, 4 bas-droite** (la touche d'appairage, bas-gauche, remonte en `ep=3`).

| Bouton (nom ZHA) | IEEE | Simple, touche n | Long (toute touche) | Double | Automatisations HA |
|---|---|---|---|---|---|
| **Bouton 1** (`_TZ3000_zgyzgdua`) | `a4:c1:38:a8:8d:be:30:90` | toggle `switch.multiprise_a_prise_n` | éteint les 8 prises | libre | `automation.bouton_1_appui_simple_multiprise_a_prise_n`, `automation.bouton_1_appui_long_tout_eteindre` |
| Bouton 2 | — | toggle `switch.multiprise_b_prise_n` (prévu) | idem | libre | à créer à l'appairage |
| Bouton 3 | — | à décider (doublon de A ou B dans une autre pièce ?) | idem | libre | — |
| Bouton 4 | — | idem | idem | libre | — |

Les automatisations sont créées **par l'API config de HA** (`POST /api/config/automation/config/<id>`),
donc visibles/éditables dans l'UI (Paramètres → Automatisations) et stockées dans
`config/automations.yaml` (hors git). Le déclencheur filtre sur `device_ieee` +
`command`, l'action cible `switch.multiprise_a_prise_{{ trigger.event.data.endpoint_id }}`
— une seule automatisation pour les 4 touches. Le script de création n'est pas
versionné : l'IEEE vit dans `.env` (`ZIGBEE_BOUTON_1_IEEE`) et la recette est
ci-dessus ; si on en vient à 4 boutons, on l'externalise dans `features/home/ha/`.

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

### 2026-09-07 (quinquies)
`HA_TOKEN` + `HA_URL` posés dans `.env` : l'API répond (2026.9.1, tz Paris, FR,
position Bretagne — l'onboarding recréé l'a bien prise). ZHA n'a encore que le
coordinateur (zéro entité). `ha_probe.py` corrigé : il cherchait `.env` un
niveau trop haut depuis le déplacement dans `features/`.

### 2026-09-07 (sexies)
**Multiprises identifiées : Shelly Power Strip 4 Gen4** — pas des devices
Zigbee-only mais du Wi-Fi avec Zigbee en option. **Tranché : Wi-Fi + intégration
Shelly native**, Zigbee écarté (bug d'inondation documenté avec exactement deux
Power Strip 4 Gen4, sans correctif). Le dongle/ZHA ne sert qu'aux boutons MOES.
Doc retitré, architecture posée, questions ouvertes réécrites (Wi-Fi + DHCP
réservé, ne pas activer le profil Zigbee des Shelly, portée sans routeur).

### 2026-09-07 (nonies)
**Bout en bout validé** : touche n → prise n de A en 66–92 ms, appui long → les 8
prises off en ≤ 145 ms, double-clic reçu (libre). Question latence close.
Sauvegarde réseau ZHA créée (hors git).

### 2026-09-07 (octies)
**Bouton 1 appairé** (TS0044 `_TZ3000_zgyzgdua`, quirk Tuya, LQI 123), entités
renommées `sensor.bouton_1_*` (3 batteries en doublon désactivées), IEEE dans
`.env`. `zha_event` reçu sur les 4 touches, latence faible. Deux automatisations
créées par l'API : simple → toggle prise n de A, long → tout éteindre.
Incident : l'ajout de l'IEEE dans `.env` s'est collé à la ligne `HA_TOKEN` (pas de
retour à la ligne final) → jeton invalide quelques minutes, réparé.

### 2026-09-07 (septies)
Les 2 Shelly sur le Wi-Fi (`.78`, `.98`), ajoutées dans HA par l'API, verrouillées
(AP/BLE/Matter off), entités renommées `switch.multiprise_{a,b}_prise_{1..4}`,
on/off réel testé. **Backend lampes opérationnel.** Reste : réservations DHCP,
A/B physique + noms des lampes, firmware 2.0.0, boutons MOES à appairer.
