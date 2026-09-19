# Maintenance du Mac — disque, apps imposées, réseau

Trois besoins de la vie courante sur le Mac (M1, macOS 14 Sonoma, 245 Go) : savoir **ce qui
mange le disque** et ce qu'on peut jeter, **virer les apps qu'on n'a pas choisies**, et
**voir qui parle à qui** sur le réseau (l'esprit Little Snitch). Chaque besoin a sa
sous-commande dans le shell `p6` : `p6 disk`, `p6 apps`, `p6 net` (scripts dans
`scripts/mac/`, **lecture seule** — ils montrent, ils n'effacent rien).

## Questions ouvertes

- [ ] **Ménage à faire** (🔴 François, tout est listé par `p6 disk`) : ~26 Go 🟢 sans
  risque (caches Spotify 3,4 Go, Xcode DeviceSupport 3,7 Go, Homebrew 2,1 Go, npm 1,9 Go,
  Gradle 1,8 Go, Google/Brave/Firefox…). Puis 🟡 : VM Cowork Claude 10 Go, simulateurs
  iOS 4,4 Go (jamais utilisés ?), Books 1,5 Go.
- [ ] **iMovie (2,4 Go) + iWork (1,3 Go)** : garder ? Ce sont les seules apps Apple
  supprimables ; Keynote sert peut-être encore.
- [ ] **Agents orphelins** à supprimer (`p6 apps --agents`) : BlueJeans, GOG Galaxy,
  JetBrains AppCode, Acrobat Update Helper, GoogleUpdater, Teams updater daemon.
- [ ] **Pare-feu macOS désactivé** et `ControlCenter` écoute sur `*:5000/*:7000`
  (AirPlay Receiver). L'activer + mode furtif ? À arbitrer — sans impact sur le LAN
  sortant.
- [ ] **Vue réseau côté routeur** : le Mercusys n'offre ni NetFlow ni SSH. Trois pistes
  (§ Réseau) — la seule vraiment « Little Snitch pour le foyer » est un **Pi-hole /
  AdGuard Home sur la tour Docker** (voit toutes les requêtes DNS du foyer).
- [ ] Faut-il un vrai pare-feu applicatif ? **LuLu** (Objective-See, gratuit, open
  source) est l'équivalent libre de Little Snitch — à tester avant de payer LS (~45 €).

## 1. Disque — les 26 Go « inaccessibles »

Constat du 2026-09-19 (Disk Inspector : 26 Go *Inaccessible Disk Space*). Ce n'est
**pas de l'espace perdu**, c'est ce qu'un scanner sans droits ne peut pas lire :

| Bloc | Taille | Pourquoi invisible |
|---|---|---|
| Volumes système du conteneur APFS (Macintosh HD 14,5 + Preboot 11 + Recovery 1,8 + VM 3,2) | ~30 Go | volumes scellés/séparés, pas dans `Data` |
| Dossiers protégés TCC/SIP dans `Data` (`/private/var/db`, `/private/var/folders`, Mail…) | ~6,5 Go | mesuré par `p6 disk --full` |
| Snapshot `com.apple.os.update-MSUPrepareUpdate` | inclus dans Preboot | **une mise à jour macOS (14.8.9 ou 27) est téléchargée et prête** — c'est elle qui gonfle Preboot à 11 Go |

**Décisions**
- Ne pas chercher à « récupérer » ce bloc : Preboot redescend après installation (ou
  expiration) de la mise à jour ; le reste est du système.
- Donner *Accès complet au disque* à Disk Inspector (Réglages > Confidentialité) si on
  veut qu'il voie les 6,5 Go de `Data` protégés — purement cosmétique.
- Le vrai levier est dans `~/Library` (55 Go sur 68 Go du home) : caches et données
  d'apps, cf. Questions ouvertes.

**`p6 disk`** classe en trois niveaux : 🟢 caches régénérés seuls (commande de purge
affichée), 🟡 à regarder avant (VM Claude, simulateurs, AVD, Books, gros fichiers > 1 Go),
🔵 apps/outils entiers. `--full` ajoute la mesure de l'illisible (~3 min).

## 2. Apps « installées de force »

Vérifié : **pas de MDM** (`profiles status` → No), donc aucune app imposée par une
entreprise. Ce qui semble forcé, c'est Apple :

| Où | Exemples | Supprimable ? |
|---|---|---|
| `/System/Applications` (39 apps) | Safari, Mail, Music, TV, News, Stocks, Chess… | **Non** : volume système scellé + SIP. Désactiver SIP pour les effacer casse les mises à jour et le sceau — on ne le fait pas. On les cache (Dock, Launchpad, Temps d'écran) |
| `/Applications` signées Apple, achats App Store | Keynote, Pages, Numbers, iMovie, GarageBand | **Oui** : Launchpad clic long → ✕, ou `sudo rm -rf`. Réinstallables gratuitement |
| `/Applications` tiers | tout le reste | Oui : Corbeille, puis `p6 apps --leftovers <bundle-id>` pour les restes dans `~/Library` |
| Casks Homebrew | android-studio, drawio, claude-code | `brew uninstall --zap <cask>` (nettoie aussi les restes) |

Ce qui « revient tout seul » n'est en général pas une app mais un **LaunchAgent/Daemon** :
`p6 apps --agents` liste `/Library/LaunchDaemons`, `/Library/LaunchAgents`,
`~/Library/LaunchAgents` et marque **ORPHELIN** ceux dont le binaire n'existe plus.
Désactiver : `launchctl bootout gui/$(id -u) <plist>` (user) ou `sudo launchctl bootout
system <plist>` (daemon), puis supprimer le `.plist` et le helper dans
`/Library/PrivilegedHelperTools/`.

## 3. Réseau — voir qui parle à qui

### Sur le Mac (fait) — `p6 net`

Outils macOS natifs, rien à installer :

| Commande | Ce qu'elle montre |
|---|---|
| `p6 net` | connexions TCP établies **par process** + UDP actifs (`lsof`) |
| `p6 net live` | débit in/out temps réel par process et socket (`nettop -d`) |
| `p6 net listen` | ports en écoute = ce que le LAN peut joindre, état du pare-feu |
| `p6 net watch [s]` | capture `tcpdump` (sudo) des nouvelles connexions sortantes hors LAN, avec reverse DNS |
| `p6 net lan` | ping sweep + table ARP : qui est sur le réseau |

Premier `p6 net` du 2026-09-19 : Brave, Claude, Notion, NordPass, NordVPN (port 8884),
Steam (dont **192.168.0.52:8009** = Cast vers la Shield), rien d'inattendu.

**Limite** : ça voit le Mac, pas le foyer. Et ça observe sans bloquer.

### Bloquer par app (le vrai Little Snitch)

- **LuLu** (Objective-See) : gratuit, open source, popup à chaque nouvelle connexion
  sortante, règles par app. À tester en premier.
- **Little Snitch** : payant, plus fin (règles par domaine, Network Monitor avec carte).
  À acheter seulement si LuLu ne suffit pas → ligne dans [bom.md](bom.md).
- Le pare-feu macOS (`socketfilterfw`) ne filtre que l'**entrant** — utile mais pas le sujet.

### Au niveau du routeur (à faire)

Le Mercusys grand public ne remonte ni NetFlow, ni SNMP exploitable, ni SSH. Options,
de la plus simple à la plus complète :

1. **Page admin du routeur** (`192.168.0.1`) : liste des clients + trafic cumulé par
   device. Pas de détail par destination.
2. **Pi-hole ou AdGuard Home sur la tour Docker** (`192.168.0.5`), déclaré comme DNS du
   routeur : **journal de toutes les requêtes DNS du foyer** par device, blocage par
   liste/domaine. C'est ce qui ressemble le plus à un Little Snitch de maison, et ça
   s'installe en un `docker compose` à côté de HA et Plex. ⚠️ Vérifier que le VPN
   NordVPN du routeur ne force pas ses propres DNS ([infra-reseau.md](infra-reseau.md)).
3. **Miroir de port / OpenWrt** : voir les paquets eux-mêmes. Le Mercusys ne fait pas de
   miroir ; il faudrait un switch managé ou un routeur OpenWrt devant. Pas un sujet v1.

## Journal

### 2026-09-19
Diagnostic des 26 Go « inaccessibles » : volumes système + snapshot de mise à jour macOS
en attente + 6,5 Go protégés — rien à récupérer là, le gras est dans `~/Library` (26 Go de
caches purgeables). Écrit `scripts/mac/{disk-audit,apps-audit,net-monitor}.sh` et les
sous-commandes `p6 disk|apps|net` (zshrc + `setup/bootstrap.sh`). Vérifié : pas de MDM,
SIP actif → apps `/System/Applications` inamovibles, iWork/iMovie supprimables. Pistes
réseau foyer : Pi-hole/AdGuard sur la tour ; LuLu avant Little Snitch.
