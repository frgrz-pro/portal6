# Plex — migration de l'install native Windows vers Docker

**Statut :** **PRÊT À EXÉCUTER — rien n'a encore basculé.** Inventaire fait et vérifié,
plan écrit, outillage rédigé dans [`features/media/plex/`](../features/media/plex/README.md).
**Date :** 2026-09-06
**Voisins :** [musique.md](musique.md) (axe B : `M:\music\library` dans Plex),
[infra-reseau.md](infra-reseau.md) (IP fixe, accès distant),
[setup-dev-windows.md](setup-dev-windows.md) (Docker Desktop),
[hardware/design-serveur-azuracast.md](hardware/design-serveur-azuracast.md) (le même parc Docker)

> **À LIRE EN PREMIER.** Ce serveur n'est pas un bac à sable : il sert **2 comptes
> partagés** (walid.ih, GFC410) et porte **10 ans d'historique de visionnage**. La
> migration Windows → Linux est **cross-OS**, donc explicitement **hors du support
> officiel Plex** — la doc « Move an Install to Another System » ne couvre que les
> déplacements same-OS.
> Corollaire : **on ne bascule pas, on double.** L'install native reste intacte et
> arrêtée pendant toute la phase de validation, et on ne désinstalle rien avant d'avoir
> validé les 6 bibliothèques dans le conteneur. Le rollback (§7) doit rester à une
> commande près, tout du long.

---

## Questions ouvertes

**Les six questions d'ouverture ont toutes été tranchées le 2026-09-06 (bis) — voir §2.5.**

- [x] ~~Réservation DHCP `192.168.0.5`~~ → **levée le 2026-09-06** : la réservation
      `R2D2` ↔ `EC-3A-56-BD-04-5A` → `192.168.0.5` **existait déjà** sur le Mercusys
      (bail *Permanent*), vérifiée des deux côtés. `ADVERTISE_IP` est donc sûr.

**Plus aucun prérequis : la phase 0 peut être lancée.**

---

## 0. L'objectif en un paragraphe

Sortir Plex de l'install native `C:\Program Files\Plex` pour le faire tourner comme
service Docker, aux côtés de xTeVe / samba / dozzle déjà en place — **sans perdre
l'identité du serveur, l'historique de visionnage, ni les comptes partagés**, et sans
déplacer un seul octet de média.

---

## 1. État des lieux (relevé le 2026-09-06)

### L'install native

| | |
|---|---|
| Version | 1.43.3.10896, `C:\Program Files\Plex\Plex Media Server` |
| Processus | `Plex Media Server`, **`Plex Tuner Service`**, `Plex Update Service`, `PlexScriptHost` |
| Données | `%LOCALAPPDATA%\Plex Media Server` — **12,6 Go** |
| Préférences | ⚠️ **dans le registre** `HKCU\Software\Plex, Inc.\Plex Media Server` (38 valeurs), **pas** de `Preferences.xml` |
| Accès distant | publié sur plex.tv, `ManualPortMappingMode=1` |
| Comptes | `fc.grzy` (fc.grzy / franekpolak59@gmail.com) + **2 partagés** : `walid.ih`, `GFC410` |
| DLNA | désactivé (`DlnaEnabled=0`) — un souci de moins en mode bridge |
| Transcodage matériel | non activé |

Poids du dossier de données : `Metadata` 5,9 Go · `Plug-in Support` 2,9 Go (dont la base
138 Mo) · `Media` 2,2 Go · `Cache` 1,2 Go · le reste est négligeable.

### Les bibliothèques

| # | Nom | Type | Items | Racines |
|---|---|---|---|---|
| 19 | Series | séries | 5 117 | `G:\10/11/12/13/14-*`, `I:\10/11/12-*` |
| 20 | Animes | séries | 2 929 | `I:\20/21-*` |
| 21 | TV Programs | séries | 397 | `H:\30-TV Shows` |
| 22 | Films | films | 753 | `D:\0/1/2/3/4/6/7/9-*` |
| 23 | TV Recordings | films | 0 | `H:\31-TV Recordings` ← **cible d'écriture du DVR** |
| 27 | YouTube | films (agent `none`) | 889 | `H:\32-YouTube\playlists` |

**23 racines, 4 lecteurs** (`D:` Film 3,7 To · `G:` Series 3,7 To · `H:` Series-3 1,8 To ·
`I:` Series-2 3,7 To), **tous des disques internes** — c'est ce qui condamne l'option
« vrai serveur Linux séparé » sans déménagement physique des disques.

### Le parc Docker

`C:\docker\media\docker-compose.yml` → `xteve` (34400), `samba` (139/1445), `dozzle` (8080).
Plus `portal6-ha` (8123) lancé depuis le repo. Moteur 29.5.2, **backend WSL2**.
Le port **32400 est libre** une fois le Plex natif arrêté.

---

## 2. Décisions actées

### 2.1 On reste sur cette tour, sous Docker Desktop

Les 13 To sont sur 5 disques **internes**. Un mini-PC Linux dédié (le « Plan B' » de
[zigbee-multiprises.md](zigbee-multiprises.md)) imposerait soit de déménager les disques,
soit de servir les médias en SMB — mauvaise idée pour Plex. La tour reste l'hôte.

**Ce qu'on accepte en échange :**

- **Pas de `network_mode: host`** : il n'existe pas sous Docker Desktop Windows. Plex sera
  derrière un bridge NAT → la découverte automatique GDM est morte (**les clients
  s'ajoutent à la main en `192.168.0.5:32400`**) et, sans `ALLOWED_NETWORKS`, Plex prendrait
  les clients du salon pour des clients distants (et les transcoderait comme tels).
- **Pas de `/dev/dri`** → pas de transcodage QSV. Neutre aujourd'hui (non activé).

### 2.2 Le Live TV / DVR est migré aussi

Décision du 2026-09-06. C'est la partie la plus fragile : tuner xTeVe à re-déclarer, EPG à
re-mapper, enregistrements programmés à recréer côté Plex. Elle est donc traitée en
**phase 5, après** la validation des 6 bibliothèques — jamais en même temps.

Conséquence de montage : **`H:` doit être monté en écriture** (le DVR écrit dans
`H:\31-TV Recordings`). `D:`, `G:`, `I:` sont montés en **lecture seule**.

### 2.3 `/config` dans un volume Docker nommé, pas un bind mount Windows

C'est la décision la moins évidente et la plus importante pour la fiabilité.

Un bind mount `C:\...\config` traverse la couche **9p/virtiofs** de WSL2. Or `/config`
contient une **base SQLite en écriture permanente** (138 Mo, plus les WAL) : c'est
exactement le profil d'accès que 9p gère le plus mal, en performance comme en verrouillage.
Un volume nommé vit dans le **VHDX ext4 de WSL2** — système de fichiers natif.

- `/config` → volume nommé `plex_config`
- `/transcode` → volume nommé `plex_transcode` (fichiers temporaires, même raison)
- les médias → **bind mounts** : lectures séquentielles de gros fichiers, 9p s'en sort bien

Contrepartie assumée : la sauvegarde de `/config` n'est plus un simple copier-coller de
dossier Windows, elle passe par `docker run --rm -v plex_config:/c ... tar` (§7).

### 2.4 Mapping des chemins : mécanique, une lettre = un dossier

| Windows | Conteneur | Mode |
|---|---|---|
| `D:\` | `/data/d` | ro |
| `G:\` | `/data/g` | ro |
| `H:\` | `/data/h` | **rw** (DVR) |
| `I:\` | `/data/i` | ro |
| `M:\` | `/data/m` | ro (ajouté le 2026-09-06 bis, cf. §2.5) |

On **ne renomme pas** en `/data/movies`, `/data/series`… : une règle uniforme
`X:\reste` → `/data/x/reste` est vérifiable d'un coup d'œil, réversible, et ne demande
aucun cas particulier pour les 23 racines.

### 2.5 Arbitrages du 2026-09-06 (bis) — les six questions d'ouverture

**a. Ethernet : NON, on reste en Wi-Fi.** Le serveur est **peu utilisé** aujourd'hui ;
le câble serait un gain net mais ne justifie pas de retarder la bascule. Le lien actuel
est un TP-Link Wi-Fi 7 PCIe à 2,9 Gbit/s négociés — largement au-dessus du besoin réel.
À rouvrir si l'usage monte, ou au premier symptôme de saturation en lecture distante.

**b. IP fixe : ✅ réglée — la réservation existait déjà.** `192.168.0.5` ↔ MAC Wi-Fi
**`EC-3A-56-BD-04-5A`** (nom routeur `R2D2`), bail *Permanent* sur le Mercusys, vérifié
le 2026-09-06 côté routeur et côté tour. `ADVERTISE_IP` ne se périmera pas.
Détail et procédure : [infra-reseau.md](infra-reseau.md) § Réservations DHCP.

**c. NordVPN : on ne fait rien de préventif.** `NordLynx 10.5.0.2` + deux interfaces
OpenVPN tournent sur la machine. Décision : **on verra si ça arrive.** Reste consigné
comme **suspect n°1** si l'accès distant Plex tombe après bascule — c'est là qu'on
regardera en premier, avant de soupçonner le conteneur.

**d. Stack média dans le repo : NON, définitivement.** `C:\docker\media\` reste hors
repo. Le parallèle avec `features/home/ha/` ne tient pas : **HA est le projet domotique,
un autre domaine** — ce n'est pas un précédent applicable au stack média. Question
**fermée**, pas reportée. `features/media/plex/` garde uniquement l'outillage et le bloc
de service de référence.

**e. Bibliothèque Musique : le montage est posé dès la migration, la bibliothèque vient
après.** `M:\` → `/data/m` en lecture seule est **décommenté dans `plex.service.yml`
maintenant**. Raison : monter un volume ne crée aucune bibliothèque et n'ajoute donc
aucune variable à valider en phase 5 ; en revanche l'ajouter plus tard imposerait de
**recréer le conteneur**. La bibliothèque *Musique* sur `/data/m/music/library` se crée
en phase 5+, une fois les 6 bibliothèques historiques validées (axe B de
[musique.md](musique.md)).

**f. Transcodage matériel : acté, on s'en passe.** Pas de `/dev/dri` sous Docker Desktop
Windows, `HardwareAcceleratedCodecs` non activé aujourd'hui → **aucune perte**. Reste
noté comme argument de déménagement le jour où le transcodage devient un besoin réel.

---

## 3. Le point dur : les chemins dans la base

Plex stocke des **chemins Windows absolus**. Il n'existe aucune fonction de re-mapping :
il faut réécrire la base. Scan exhaustif de `com.plexapp.plugins.library.db` (toutes les
tables réelles, regex sur les lettres de lecteur réellement montées) :

| Table.colonne | Valeurs | Forme |
|---|---|---|
| `media_parts.file` | 9 151 | `D:\0-Movies\1917….mp4` |
| `media_streams.url` | 1 772 | `file:///G:/10-Finished%20Series/…` (**URL-encodé**, espaces en `%20`) |
| `section_locations.root_path` | 23 | `G:\10-Finished Series` |
| `metadata_items.guid` | 1 | `file:///I:/11-Returning%20Series/Fargo/…` (agent `none`) |
| **Total** | **10 947** | |

Deux formes distinctes, donc **deux transformations** :

- brut : `X:\a\b` → `/data/x/a/b` (lettre en minuscule, `\` → `/`)
- URL : `file:///X:/a%20b` → `file:///data/x/a%20b` (les séparateurs sont déjà des `/`,
  **l'encodage `%20` doit être laissé tel quel**)

Les 8 807 lignes de `media_parts.file` à chaîne vide sont ignorées (parts sans fichier).

### Pièges vérifiés sur place

- **Plex embarque un SQLite custom** : le scan a buté sur des tables virtuelles
  `spellfix1` et `fts4`, inconnues d'un SQLite standard. La réécriture doit passer par
  **`Plex SQLite.exe`** livré avec le serveur, pas par un `sqlite3` du système, sous peine
  de corrompre le fichier. (Les 4 colonnes à réécrire sont des tables ordinaires, donc
  lisibles par le `sqlite3` de Python **en lecture seule** — c'est ce qui a servi à
  l'inventaire — mais l'écriture se fait avec le binaire de Plex.)
- **Les préférences sont dans le registre**, pas dans un fichier. Sans conversion en
  `Preferences.xml`, le conteneur démarre en serveur **neuf** : nouvelle identité, les
  comptes partagés perdent le serveur, l'accès distant est à refaire. Valeurs vitales à
  reporter : `MachineIdentifier`, `ProcessedMachineIdentifier`, `AnonymousMachineIdentifier`,
  `CertificateUUID`/`CertificateVersion`, `PlexOnlineToken`, `PlexOnlineUsername`/`Mail`.
- Les valeurs `_<hexa>-TranscodeCountLimit` sont des réglages **par client** : sans intérêt
  à migrer, mais inoffensifs.

---

## 4. Procédure — 7 phases, on valide chaque phase avant la suivante

Commandes exactes dans le runbook :
[`features/media/plex/README.md`](../features/media/plex/README.md).

**Principe d'ordonnancement :** tout le travail lourd (réécriture, préférences) se fait sur
une **copie Windows** posée sur `M:\backup\`, *avant* l'import dans le volume Docker. C'est
là que `Plex SQLite.exe` et le registre sont accessibles nativement, et ça garde le volume
propre : il ne reçoit qu'une arborescence déjà correcte.

**Phase 0 — Arrêt et copie de travail.** Arrêter le service `PlexUpdateService` et les
processus (`Plex Media Server`, `Plex Tuner Service`, `PlexScriptHost`). `robocopy` de
`%LOCALAPPDATA%\Plex Media Server` vers `M:\backup\plex-migration-AAAA-MM-JJ\`, en excluant
`Cache`, `Updates`, `Crash Reports`, `Diagnostics` (régénérés par Plex, ~1,5 Go de moins).
**STOP** : code robocopy < 8, base à ~138 Mo dans la copie.

**Phase 1 — Préférences.** `export_preferences.ps1` : registre → `Preferences.xml` dans la
copie. **STOP** : le script doit confirmer `MachineIdentifier : present`. Il échoue
volontairement si une valeur vitale manque, plutôt que de produire un serveur neuf.

**Phase 2 — Réécrire la base.** `check_paths.py` (attendu 10 947 / 0), puis
`rewrite_paths.sql` via `Plex SQLite.exe`, puis `check_paths.py` à nouveau (attendu
0 / 10 947). **STOP** : `integrity_check = ok` et aucun état mixte.

**Phase 3 — Import dans le volume.** Créer `plex_config` / `plex_transcode`, y copier
l'arborescence sous `Library/Application Support/Plex Media Server/`.
**STOP** : `Preferences.xml`, `Metadata/`, `Media/`, `Plug-in Support/` présents dans le volume.

**Phase 4 — Démarrer le conteneur, Plex natif toujours arrêté.** Fusionner le bloc compose,
`docker compose up -d plex`. **STOP** : `http://192.168.0.5:32400/web` répond, le serveur
apparaît sous **son nom d'origine** (pas un serveur neuf à réclamer), les 6 bibliothèques
sont là avec leurs compteurs d'items.

**Phase 5 — Valider les médias.** Sur chaque bibliothèque : lecture directe d'un fichier,
poster présent, épisode marqué vu toujours marqué vu. Vérifier depuis un client **du LAN**
(qu'il ne soit pas vu comme distant) et depuis un **compte partagé**. **STOP** : c'est le
vrai point de décision ; en dessous, on rollback (§7).

**Phase 6 — Live TV / DVR.** Re-déclarer le tuner xTeVe (`http://xteve:34400`, le conteneur
Plex étant sur le même réseau compose), re-mapper l'EPG, recréer les enregistrements
programmés. Vérifier qu'un enregistrement atterrit bien dans `H:\31-TV Recordings`.

**Phase 7 — Débrancher le natif.** Seulement après plusieurs jours d'usage réel :
`PlexUpdateService` en `Disabled`, entrée `Run` du registre retirée, désinstallation
ensuite. `%LOCALAPPDATA%\Plex Media Server` reste en place tant qu'il n'y a pas une
sauvegarde du volume qui tourne.

### Ce qui est déjà validé (2026-09-06)

La phase 2 a été **répétée à blanc sur une copie jetable de la vraie base** : les
10 947 valeurs sont passées à `/data/…`, `integrity_check` répond `ok`, l'encodage `%20`
des `media_streams.url` est préservé, et `check_paths.py` confirme 0 chemin Windows
restant. Le mécanisme de réécriture n'est donc plus une hypothèse.

---

## 5. Ce qui change à l'usage (à annoncer aux comptes partagés)

- Les clients du LAN qui trouvaient le serveur tout seuls devront être **repointés à la
  main** sur `192.168.0.5:32400`. Les comptes distants, eux, passent par plex.tv et ne
  voient rien changer — **si** l'accès distant est resté fonctionnel.
- Une coupure le temps de la bascule (phases 0 à 3).

## 6. Ce qu'on ne fait pas

- Pas de changement de version de Plex pendant la migration : on veut une seule variable.
  Le conteneur est épinglé sur le tag correspondant à l'install actuelle, la mise à jour
  vient après.
- Pas de `docker compose down -v` sur ce stack — même garde-fou que pour AzuraCast : `-v`
  détruirait `plex_config`.
- Pas de rapatriement du stack média dans le repo (question ouverte).

## 7. Rollback

Tant que la phase 7 n'est pas faite, le retour arrière est : `docker compose stop plex`,
puis relancer le service Windows Plex. L'install native n'a **pas** été touchée — la base
réécrite est une copie posée sur `M:\backup\` puis importée dans le volume Docker. C'est
toute la raison de l'ordonnancement §4.

Sauvegarde du `/config` conteneurisé, une fois en régime :

```powershell
docker run --rm -v plex_config:/c -v M:/backup:/b alpine tar czf /b/plex_config-$(Get-Date -f yyyy-MM-dd).tgz -C /c .
```

---

## Journal

### 2026-09-06
Création du doc. Cible tranchée (**cette tour, Docker Desktop**) après avoir écarté le
mini-PC Linux : les 13 To sont sur 5 disques internes. Live TV **conservé**, mais isolé en
phase 5. Inventaire complet relevé : 6 bibliothèques / 10 100 items / 23 racines sur 4
lecteurs, 12,6 Go de données Plex, 2 comptes partagés.
Deux découvertes qui structurent le plan : (1) le scan exhaustif de la base donne
**10 947 valeurs à réécrire sur exactement 4 colonnes**, sous deux formes (brute et
URL-encodée) ; (2) **sous Windows les préférences sont dans le registre**, pas dans
`Preferences.xml` — sans conversion, le conteneur démarrerait en serveur neuf et les
comptes partagés perdraient le serveur. Décidé aussi de mettre `/config` dans un **volume
nommé** plutôt qu'un bind mount, la base SQLite supportant mal la couche 9p de WSL2.
Outillage écrit dans `features/media/plex/` et **phase 2 répétée à blanc avec succès** sur
une copie jetable de la vraie base (10 947 valeurs réécrites, `integrity_check = ok`).
L'install native n'a pas été touchée : la bascule attend le go.

### 2026-09-06 (bis) — les six questions d'ouverture sont tranchées
Arbitrages de François, consignés en §2.5 : (a) **on reste en Wi-Fi**, le serveur est peu
utilisé ; (b) réservation DHCP `192.168.0.5` ↔ MAC `EC-3A-56-BD-04-5A` à poser — seule
tâche bloquante restante, procédure écrite dans [infra-reseau.md](infra-reseau.md) ;
(c) NordVPN : aucune action préventive, consigné comme suspect n°1 si l'accès distant
tombe ; (d) stack média dans le repo → **non, question fermée** (HA relève du domaine
domotique, ce n'est pas un précédent) ; (e) le bind `M:` → `/data/m` est **décommenté dès
maintenant** dans `plex.service.yml`, la bibliothèque Musique n'étant créée qu'après la
phase 5 — monter ne coûte rien à la validation, ajouter plus tard imposerait de recréer
le conteneur ; (f) transcodage matériel : acté, on s'en passe.
Le tableau de mapping §2.4 gagne la ligne `M:\` → `/data/m` (ro).

### 2026-09-06 (ter) — dernier prérequis levé
La réservation DHCP `192.168.0.5` ↔ `EC-3A-56-BD-04-5A` **existait déjà** sur le Mercusys
(bail *Permanent*, nom routeur `R2D2`) : rien à poser. `ADVERTISE_IP=http://192.168.0.5:32400/`
est donc stable. **La phase 0 (arrêt du Plex natif + copie de travail) est lançable.**
