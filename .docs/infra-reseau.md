# Infra réseau domestique

État des lieux du réseau sur lequel reposent l'app remote, Home Assistant et la
Shield. Doc de référence à enrichir au fil des découvertes (modèles, IPs).

## État connu

- **Modem/routeur Mercusys** = point d'entrée unique du réseau domestique.
- **OpenVPN configuré dessus, licence via NordVPN** — c'est un client VPN
  **sortant** : il chiffre le trafic du foyer vers Internet. ⚠️ Ça ne donne
  **aucun accès entrant** au LAN depuis l'extérieur — ne pas confondre avec un
  serveur VPN d'accès distant.
- Devices concernés par les projets : Shield TV Pro, future tour Docker
  (Home Assistant), les TRMNL, le téléphone.

## Questions ouvertes

- [ ] Modèle exact du Mercusys (détermine : réservations DHCP, mDNS, serveur VPN ?)
- [x] ~~IPs de la Shield et de la tour ; poser des réservations DHCP~~ → **tranché le
  2026-09-06** : tour `192.168.0.5`, Shield `192.168.0.52`, les deux **déjà réservées**
  en bail permanent (section Réservations DHCP).
- [ ] **Activer le débogage réseau (ADB) sur la Shield** : le port 5555 est fermé alors que
  6466/6467 et 8008/8009 répondent — prérequis de l'app remote ([app-remote.md](app-remote.md)).
- [ ] Le VPN NordVPN du routeur route-t-il TOUT le trafic ? Vérifier que le
  trafic LAN↔LAN n'est pas impacté (normalement non) et que les services locaux
  (HA, Shield) restent joignables.
- [ ] Accès distant un jour ? Options si besoin : NordVPN Meshnet (si supporté),
  ou Tailscale sur la tour Docker — largement suffisant et plus simple qu'un
  serveur OpenVPN sur le Mercusys. **Pas un sujet v1** : l'app remote est
  LAN-only.

## Réservations DHCP

Passerelle / admin du routeur : **`192.168.0.1`**, LAN en `192.168.0.0/24`.

Plage DHCP du routeur : `192.168.0.2` → `192.168.0.253`, bail 120 min, DNS servi par le
routeur lui-même.

| Device | Nom routeur | IP | MAC | Statut |
|---|---|---|---|---|
| **Tour DevLab** (Wi-Fi) | `R2D2` | `192.168.0.5` | `EC-3A-56-BD-04-5A` | ✅ **réservée** (bail *Permanent*) |
| **Shield TV Pro** | `Android` | `192.168.0.52` | `AC-3A-E2-E8-74-6A` | ✅ **réservée** (bail *Permanent*) |
| **Multiprise A** (Shelly Power Strip 4 Gen4) | `shellypstripg4-48f6eedd4148` | `192.168.0.78` | `48-F6-EE-DD-41-48` | ✅ **réservée** le 2026-09-07 — HA la joint par IP |
| **Multiprise B** (Shelly Power Strip 4 Gen4) | `shellypstripg4-d885aceb742c` | `192.168.0.98` | `D8-85-AC-EB-74-2C` | ✅ **réservée** le 2026-09-07 |

**Constaté le 2026-09-06 : les deux réservations étaient déjà posées.** Vérifié côté tour
(`192.168.0.5` effective) et côté routeur (bail *Permanent* dans la liste des clients DHCP).

- Tour : TP-Link Wi-Fi 7 PCIe. Porte HA `:8123`, Plex `:32400` après migration, et bientôt
  AzuraCast `:80` / `:8000+`. C'est cette réservation qui débloque `ADVERTISE_IP`
  ([plex-docker.md](plex-docker.md) §2.5b).
- Shield : identifiée par empreinte de ports — **6466/6467 ouverts** (Android TV Remote v2)
  et **8008/8009** (Google Cast). ⚠️ **ADB 5555 est fermé** : le débogage réseau n'est pas
  activé sur la Shield — à faire avant tout test de l'app remote
  ([app-remote.md](app-remote.md)).
- **TRMNL** : `TRMNL-OG-RT97Q4` → `192.168.0.117`, MAC `A4-CB-8F-2B-34-BC`, bail dynamique
  de 120 min. **Aucune réservation nécessaire** : le device est *pull-only*, il ouvre les
  connexions vers le cloud TRMNL et n'écoute rien en entrant — son IP n'est jamais
  composée par personne. Une réservation ne deviendrait utile qu'en passant en **BYOS**
  (le device pointant vers un serveur de la tour). Le nom du client confirme au passage
  le modèle : **OG**, cf. [trmnl-dashboard.md](trmnl-dashboard.md).
- Autres clients vus au passage, en bail dynamique : un Mac, un iPhone et un
  `R2D2s-Air` en MAC randomisées.

> ⚠️ La tour a **plusieurs interfaces** (Ethernet en APIPA car débranché, NordLynx,
> OpenVPN, vEthernet WSL). La seule MAC qui compte pour la réservation est celle de
> l'adaptateur **Wi-Fi**, ci-dessus — ne pas réserver sur une MAC virtuelle.

### Marche à suivre (Mercusys) — pour la prochaine réservation

Les deux réservations utiles sont déjà en place ; cette procédure est conservée comme
référence.

1. Ouvrir `http://192.168.0.1` (ou `mwlogin.net`) et se connecter à l'admin du routeur.
2. Aller dans **Advanced → Network → DHCP Server** (selon firmware : **LAN → DHCP**),
   section **Address Reservation** / *Réservation d'adresse*.
3. **Add** → soit choisir la tour dans la liste des clients connectés (plus sûr : la MAC
   est pré-remplie), soit saisir à la main :
   - MAC : `EC-3A-56-BD-04-5A`
   - IP : `192.168.0.5`
4. Vérifier que `192.168.0.5` est **dans** la plage DHCP du routeur (sinon élargir la
   plage, ou sortir l'IP de la plage ET la fixer côté Windows — ne pas faire les deux).
5. **Save**, puis côté tour :

```powershell
ipconfig /release "Wi-Fi"; ipconfig /renew "Wi-Fi"; ipconfig | Select-String -Context 0,4 'Wi-Fi'
```

> **Vérification :** l'IP doit revenir à `192.168.0.5` après le renouvellement, et le bail
> doit être long (la réservation le rend permanent en pratique).

**Alternative écartée** : fixer l'IP statiquement dans Windows. Ça marche, mais ça
dédouble la source de vérité et le routeur peut attribuer `.5` à un autre device — la
réservation côté routeur est la bonne place.

Modèle exact du Mercusys encore à relever : il détermine le libellé exact des menus
(et l'existence d'un serveur VPN entrant).

## Journal

### 2026-08-30
Création du doc. Clarification importante : NordVPN sur le routeur = client
sortant, pas d'accès distant entrant. v1 de l'app = LAN-only ; accès distant
éventuel via Tailscale sur la tour, plus tard.

### 2026-09-06
Relevé des interfaces de la tour : Wi-Fi `192.168.0.5` (TP-Link Wi-Fi 7 PCIe, MAC
`EC-3A-56-BD-04-5A`, 2,9 Gbit/s négociés), passerelle `192.168.0.1` ; l'Ethernet est
débranché (APIPA `169.254.x`), et NordLynx + deux interfaces OpenVPN tournent en plus
sur la machine. Décidé de **rester en Wi-Fi** pour la migration Plex (serveur peu
utilisé). Section **Réservations DHCP** créée avec la marche à suivre Mercusys : la
réservation `192.168.0.5` est désormais **bloquante** pour
[plex-docker.md](plex-docker.md) (`ADVERTISE_IP`).

### 2026-09-06 (bis) — réservations DHCP : elles étaient déjà là
Accès à l'admin Mercusys retrouvé. **Les deux réservations attendues existaient déjà** en
bail permanent : `R2D2` (la tour) → `192.168.0.5` et `Android` → `192.168.0.52`. Ce dernier
a été **identifié comme la Shield TV Pro** par empreinte de ports : 6466/6467 (Android TV
Remote v2) et 8008/8009 (Cast) répondent. La question ouverte « IPs Shield et tour +
réservations » est donc close sans rien avoir à modifier, et le dernier prérequis de la
migration Plex tombe. Plage DHCP relevée : `.2`–`.253`, bail 120 min.
Découverte annexe : **ADB 5555 est fermé sur la Shield** → nouvelle question ouverte,
prérequis de l'app remote.

### 2026-09-06 (ter) — TRMNL sur le LAN
Device relevé : `TRMNL-OG-RT97Q4` → `192.168.0.117`, MAC `A4-CB-8F-2B-34-BC`, bail
dynamique. Le nom du client **confirme le modèle OG** (indépendamment de la fiche
produit). Décidé : **pas de réservation DHCP** — le TRMNL est pull-only, son IP n'est
jamais composée. À rouvrir seulement le jour où on passera en BYOS.

### 2026-09-07 — Shelly sur le LAN
Deux Shelly Power Strip 4 Gen4 rejoignent le Wi-Fi : `.78` (A) et `.98` (B), bail
dynamique, puis **réservations posées dans la foulée** (menu *Serveur DHCP →
Réservation d'adresses*, choix dans la liste des clients) : Home Assistant les
adresse par IP, pas par mDNS (Docker Desktop). Plex n'a pas besoin de ligne : le
serveur tourne sur la tour `.5`, déjà réservée. AP ouvert, Bluetooth et Matter
désactivés sur les deux ([zigbee-multiprises.md](zigbee-multiprises.md)).
