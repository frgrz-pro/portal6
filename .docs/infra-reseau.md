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
- [ ] IPs actuelles de la Shield et de la tour ; poser des **réservations DHCP**
  pour les deux (l'app remote a besoin d'adresses stables).
- [ ] Le VPN NordVPN du routeur route-t-il TOUT le trafic ? Vérifier que le
  trafic LAN↔LAN n'est pas impacté (normalement non) et que les services locaux
  (HA, Shield) restent joignables.
- [ ] Accès distant un jour ? Options si besoin : NordVPN Meshnet (si supporté),
  ou Tailscale sur la tour Docker — largement suffisant et plus simple qu'un
  serveur OpenVPN sur le Mercusys. **Pas un sujet v1** : l'app remote est
  LAN-only.

## Conventions cibles

- Réservations DHCP (à remplir) :

| Device | IP | Notes |
|---|---|---|
| Shield TV Pro | à fixer | ADB 5555 / Remote 6466-6467 |
| Tour Docker (HA) | à fixer | HA :8123 |

## Journal

### 2026-08-30
Création du doc. Clarification importante : NordVPN sur le routeur = client
sortant, pas d'accès distant entrant. v1 de l'app = LAN-only ; accès distant
éventuel via Tailscale sur la tour, plus tard.
