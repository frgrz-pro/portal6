# Design — Serveur de web-radios (AzuraCast)

**Owner :** François Grzybowski
**Statut :** **EN REPRISE** — Phases 1 et 2 validées le 2026-08-20, re-vérifiées le 2026-09-06 ;
Phase 3 (déploiement) **prête à lancer**, bloquée par une seule action utilisateur (§5, Phase 3)
**Date :** 2026-08-12, mises à jour 2026-08-20 et 2026-09-06
**Piste :** SERVEUR — le poste qui consomme ces flux est décrit dans
[design-brandt-rk711s.md](design-brandt-rk711s.md)

> **À LIRE EN PREMIER — instructions d'exécution.**
> Ce chantier a déjà produit une impasse, et cette note existe d'abord pour ne pas la
> reproduire : **plusieurs `docker-compose.yml` ont été inventés dans des sessions
> précédentes, avec des images qui n'existent pas.** La règle qui prime sur tout le reste :
> **on ne rédige pas de compose, on prend celui du projet.** Voir §3.
> **On vérifie sur la source officielle avant de proposer**, on avance une étape à la fois, et
> on valide chaque étape avant la suivante.
> ⚠️ **Ne jamais exécuter `docker compose down -v`** tant que le contenu des volumes n'a pas
> été inventorié (§6).

---

## 0. L'objectif en un paragraphe

Faire tourner **plusieurs web-radios personnelles H24** depuis le DevLab, à partir d'une
**bibliothèque musicale centrale unique**, avec playlists et programmation horaire, via
**AzuraCast** (AutoDJ + Liquidsoap + Icecast). Le contenu éditorial — stations, grilles,
échelle d'énergie — est décrit dans
[design-programmation-editoriale.md](design-programmation-editoriale.md). Cette note-ci ne
traite que **l'infrastructure**.

---

## 1. État exact aujourd'hui

Relevé du **2026-09-06** (inventaire en lecture seule : aucun conteneur créé, aucun volume
touché, aucun `down`).

| Élément | État |
|---|---|
| Machine | Windows 11 + Docker Desktop + Docker Compose + Git (Git Bash disponible) |
| Bibliothèque musicale | `M:\music` — vue depuis WSL en `/mnt/m/music` (9p/drvfs, 864 Go, 382 Go libres) |
| Racine Docker | `C:\docker\media` |
| Dépôt AzuraCast cloné | `C:\docker\media\azuracast` — **inutile** : la procédure officielle ne s'en sert pas (§5, Phase 1). Vestige à supprimer. |
| `docker.sh` téléchargé | dans ce clone — **non utilisable** : il doit être retéléchargé dans `/var/azuracast` côté WSL |
| Compose global | `C:\docker\media\docker-compose.yml` — **re-vérifié sain le 2026-09-06** : xteve + samba + dozzle uniquement, `docker compose config` valide, `--images` ne sort que `alturismo/xteve`, `dperson/samba`, `amir20/dozzle:latest` |
| Déploiement AzuraCast | ❌ **rien n'existe** : `/var/azuracast` absent côté WSL, aucun conteneur, aucun volume nommé |
| WSL2 | ✅ **Ubuntu 26.04 LTS**, distribution par défaut, version 2, 953 Go libres sur le disque de la VM |
| Docker Desktop | ✅ moteur 29.5.2, Compose v5.1.3, backend WSL2, opérationnel côté Windows |
| **Intégration WSL de Docker Desktop** | ❌ **DÉSACTIVÉE** — c'est le blocage unique. `docker` dans Ubuntu répond *« The command 'docker' could not be found in this WSL 2 distro »* ; `settings-store.json` ne contient aucune clé d'intégration WSL (= valeur par défaut, désactivée). |

**Conteneurs en marche (2026-09-06)** — aucun ne dispute un port à AzuraCast :

| Conteneur | Image | Ports |
|---|---|---|
| `portal6-ha` | `ghcr.io/home-assistant/home-assistant:stable` | 8123 |
| `dozzle` | `amir20/dozzle:latest` | 8080 |
| `samba` | `dperson/samba` | 139, 1445→445 |
| `xteve` | `alturismo/xteve` | 34400 |

Ports **80, 443, 2022, 8000, 8005 : tous libres** (aucun listener Windows).
Volumes : uniquement des volumes anonymes (hashes), **aucun `azuracast_*`**.

**Historique.** Une installation antérieure tournait dans un **LXC Proxmox** (`LXC-Radio`,
`/media/music`, `/srv/services/azuracast`) et avait validé le fonctionnement d'AzuraCast et de
la programmation, avec une station **Midnight Club** créée et configurée. Le projet a été
volontairement redémarré sur Windows 11 + Docker Desktop.

---

## 2. Les deux blocages constatés

### 2.1 Images Docker inexistantes

Les compose proposés référençaient :

```
azuracast/azuracast_web:latest
azuracast/azuracast_stations:latest
```

Docker répond :

```
pull access denied ... repository does not exist
```

**Diagnostic :** ce sont les images de l'**architecture pré-consolidation** d'AzuraCast, qui
séparait le conteneur web et le conteneur stations. Cette architecture est retirée ; les
versions actuelles s'appuient sur une image consolidée. **Les compose qui les référencent sont
définitivement invalides.**

### 2.2 `docker.sh` refuse de s'exécuter sous Git Bash

```
[FAIL] Operating System: MINGW64_NT-10.0-26200
You are running an unsupported operating system.
```

**Diagnostic :** Git Bash n'est pas l'environnement attendu par le script officiel. Le script
est pensé pour un hôte Linux — ce qui, sur Windows, désigne **WSL2**, pas MINGW.

> Note de méthode : la bonne commande de téléchargement sous PowerShell est
> `Invoke-WebRequest ... -OutFile`, car `curl` y est un alias d'`Invoke-WebRequest` et
> n'accepte pas `-L`. Ce point-là est réglé.

---

## 3. La règle qui prime sur tout

> **On ne rédige pas de `docker-compose.yml` pour AzuraCast.**
> On prend celui que le projet fournit, on ne remplace pas une image par une autre « qui
> ressemble », et on ne devine pas un nom de service.
> Si une information manque, on la lit dans la **documentation officielle ou le dépôt**, pas
> dans une mémoire.

Corollaire : la **première action** du prochain chantier n'est pas d'écrire du YAML, c'est de
**vérifier la méthode d'installation actuellement supportée** (§5, Phase 1).

---

## 4. Architecture cible

```
                         ┌─────────────────────┐
                         │      M:\music       │
                         │  Bibliothèque MP3   │
                         │  (source unique)    │
                         └──────────┬──────────┘
                                    │  montée en lecture (et écriture maîtrisée)
                     ┌──────────────▼──────────────┐
                     │          AzuraCast          │
                     │  ┌────────┐ ┌────────┐      │
                     │  │Station1│ │Station2│ ...  │
                     │  └────────┘ └────────┘      │
                     │  playlists / scheduling     │
                     │  AutoDJ / Icecast / API     │
                     └──────────────┬──────────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
             Stream             Metadata               API
                │                   │                   │
                ▼                   ▼                   ▼
        ┌───────────────┐      ┌─────────┐        ┌──────────┐
        │ LE POSTE      │      │  XMLTV  │        │ Scripts  │
        │ Brandt RK711S │      │  / M3U  │        │ énergie, │
        └───────────────┘      └────┬────┘        │ likes    │
                                    ▼             └──────────┘
                                  xTeVe → Plex
```

**Organisation Docker visée :**

```
C:\docker\media
├── docker-compose.yml       ← compose global
├── azuracast\               ← dépôt + fichiers du projet
├── dozzle\                  ← visualisation des logs, port 8888:8080
└── autres services
```

### Principe non négociable : la bibliothèque n'appartient pas aux radios

```
M:\music
├── Library
├── Live
├── Mixtapes
├── Playlists
├── Radio
└── Workspace
```

**Les radios ne doivent pas imposer leur organisation au stockage, ni dupliquer les fichiers.**
Plusieurs stations doivent pouvoir pointer sur les **mêmes fichiers physiques** et n'en
sélectionner que des sous-ensembles via playlists.

C'est pour cette raison que le répertoire créé automatiquement par AzuraCast — du type
`/var/azuracast/stations/<slug>/media` — **n'est pas** la bibliothèque du projet : il ne doit
pas devenir le lieu où vivent les MP3.

> **Point à instruire (Q3) :** comment exactement monter `M:\music` dans AzuraCast pour que
> plusieurs stations le partagent sans copie ? Selon les versions, AzuraCast propose des
> chemins média partagés entre stations. **À vérifier dans la doc, pas à supposer.**

---

## 5. Plan de reprise

### PHASE 1 — Vérifier la méthode officielle (avant tout YAML)

Lire la documentation et le dépôt AzuraCast sur : Docker Desktop, Windows 11, **WSL2**,
installation manuelle par Docker Compose, **fichiers compose réellement fournis**, **images
réellement publiées**, et support officiel — ou non — de Windows.

> **STOP / VÉRIFIER (Phase 1) :** la méthode supportée est identifiée et **écrite dans cette
> note**, avec le nom exact des images et le chemin du compose fourni. Aucun YAML n'a été
> rédigé.

**✅ VALIDÉ le 2026-08-20.** Sources : `azuracast.com/docs/getting-started/installation/windows/`
et `/docker/`, plus lecture directe de `docker.sh` et `docker-compose.sample.yml` sur le dépôt
`AzuraCast/AzuraCast` (branche `main`).

- **Windows est officiellement supporté, via WSL2** : Docker Desktop + une distribution Linux
  (Ubuntu LTS recommandé, définie comme distribution par défaut), puis on suit **la procédure
  Linux standard à l'intérieur du shell WSL**, sans aucune adaptation. Ceci répond à Q1 :
  pas besoin de retourner sur Proxmox.
- **Méthode d'installation officielle** (dans le shell Ubuntu WSL, en sudo) :
  ```bash
  mkdir -p /var/azuracast && cd /var/azuracast
  curl -fsSL https://raw.githubusercontent.com/AzuraCast/AzuraCast/main/docker.sh > docker.sh
  chmod a+x docker.sh
  ./docker.sh install
  ```
  Le répertoire de base vit **dans le disque de la VM WSL** (`/var/azuracast`), pas sous
  `/mnt/c` — cohérent avec la leçon venv du repo. L'ancien clone `C:\docker\media\azuracast`
  ne sert pas à cette procédure.
- **Le compose n'est jamais écrit à la main** : `docker.sh` télécharge
  `docker-compose.sample.yml` depuis le dépôt (branche `stable` ou `main` selon le canal
  choisi via `setup-release`), l'installeur génère `docker-compose.new.yml` puis le script le
  promeut en `/var/azuracast/docker-compose.yml`.
- **Images réellement publiées** (architecture consolidée, 2 services) :
  `ghcr.io/azuracast/azuracast:${AZURACAST_VERSION:-latest}` (service `web`, tout-en-un) et
  `ghcr.io/azuracast/updater:latest`. Les images `azuracast/azuracast_web` /
  `azuracast_stations` sont bien mortes (§2.1 confirmé).
- **Ports du compose officiel** : 80, 443, 2022, plus la plage stations **8000–8496**.
- **Explication du blocage §2.2 confirmée** : `docker.sh` n'accepte que `uname` = `Linux` ou
  `Darwin` ; MINGW64 (Git Bash) est rejeté, Ubuntu WSL passe.
- Accès final depuis Windows : `http://localhost` une fois l'installation terminée.

**🔁 Re-vérifié le 2026-09-06** sur les sources brutes (`raw.githubusercontent.com`, branche
`main`) — **rien n'a changé**, les conclusions ci-dessus tiennent :

- `docker-compose.sample.yml` : toujours **2 services** (`web`, `updater`), images
  `ghcr.io/azuracast/azuracast:${AZURACAST_VERSION:-latest}` et
  `ghcr.io/azuracast/updater:latest` ; ports 80 / 443 / 2022 paramétrables + 47 ports fixes
  dans la plage 8000–8496 ; **volumes nommés** : `db_data`, `acme`, `shoutcast2_install`,
  `stereo_tool_install`, `rsas_install`, `geolite_install`, `sftpgo_data`, `station_data`,
  `www_uploads`, `backups`.
- `docker.sh` : n'accepte toujours que `uname -s` ∈ {`Linux`, `Darwin`} (arch x86_64 /
  aarch64), attend toujours `/var/azuracast` comme répertoire de base, et télécharge toujours
  `docker-compose.sample.yml` depuis la branche du canal choisi. Sous-commandes utiles :
  `install`, `setup-release`, `setup-ports`, `update`, `backup`, `restore`, `rollback`,
  `uninstall`, `up` / `down` / `restart`, `cli`, `bash`, `db`.
- ⚠️ `azuracast.com/docs/...` répond **403** aux fetchs automatisés — la source à interroger
  est le dépôt brut, pas le site.

### PHASE 2 — Nettoyer l'existant

Inventorier ce qui traîne avant de lancer quoi que ce soit :

```powershell
docker version
docker compose version
docker ps -a
docker volume ls
docker network ls
```

```powershell
cd C:\docker\media
docker compose config
```

Corriger ou remplacer le compose global fautif (§2.1).

> **STOP / VÉRIFIER (Phase 2) :** `docker compose config` sort une configuration valide, sans
> aucune image inexistante · l'inventaire des volumes est écrit (§6) · **aucun `down -v`**.

**✅ VALIDÉ le 2026-08-20** (inventaire en lecture seule, aucun `down`, aucun volume touché) :

- Conteneurs actifs : `dozzle` (8080:8080 — la cible §7 disait 8888, la réalité est 8080),
  `samba` (139, 1445:445), `xteve` (34400). Réseau `media_default`.
- `docker compose config` sur `C:\docker\media` : **valide**, aucune image AzuraCast — le
  compose global fautif a déjà été purgé, rien à corriger.
- Volumes : uniquement des volumes anonymes (hash), **aucun volume nommé `azuracast_*`** sur ce
  Docker Desktop. Réponse à Q2 : rien de l'ancienne installation LXC n'est récupérable ici —
  Midnight Club sera recréée de zéro en Phase 5.
- Ports 80, 443, 2022, 8000 : **tous libres** (aucun listener Windows).

### PHASE 3 — Déployer AzuraCast

Selon la méthode retenue en Phase 1. Vérifier les **conflits de ports** avant lancement :
AzuraCast utilise notamment **80**, **443**, **2022**, plus une plage de ports par station.

> **STOP / VÉRIFIER (Phase 3) :** interface AzuraCast accessible · aucun conflit de port avec
> les autres services · Dozzle toujours joignable.

**Pré-vol du 2026-09-06 — tout est vert sauf une case à cocher.**

| Prérequis | État |
|---|---|
| Méthode officielle identifiée et re-vérifiée | ✅ (Phase 1) |
| Compose global sain, sans image AzuraCast | ✅ (`docker compose config` valide) |
| Aucun volume `azuracast_*` à préserver | ✅ (Phase 2) |
| Ports 80 / 443 / 2022 / 8000 / 8005 libres | ✅ |
| Ubuntu WSL2 en place, place disque suffisante | ✅ (26.04 LTS, 953 Go libres) |
| **`docker` utilisable depuis le shell Ubuntu** | ❌ **BLOQUANT** |

> **🔴 Action utilisateur — la seule qui manque.**
> Docker Desktop → **Settings → Resources → WSL integration** → activer l'intégration pour la
> distribution **Ubuntu** (ou cocher *« Enable integration with my default WSL distro »*) →
> **Apply & Restart**.
> Vérification attendue, dans un shell Ubuntu : `docker version` doit répondre côté serveur
> (aujourd'hui : *« The command 'docker' could not be found in this WSL 2 distro »*).

Une fois cette case cochée, la séquence Phase 3 est (dans le shell Ubuntu, en sudo) :

```bash
sudo mkdir -p /var/azuracast && cd /var/azuracast
curl -fsSL https://raw.githubusercontent.com/AzuraCast/AzuraCast/main/docker.sh > docker.sh
chmod a+x docker.sh
sudo ./docker.sh install      # canal Stable
```

Puis : `http://localhost` depuis Windows, et création du compte super-admin.

⚠️ Pendant l'installeur, ne **pas** changer les ports par défaut sans raison : 80/443/2022 sont
libres sur cette machine, et déplacer les ports complique la Phase 6 (exposition).

### PHASE 4 — Brancher la bibliothèque

Monter `M:\music` **sans que les stations n'y imposent leur arborescence**, et sans copie.

> **STOP / VÉRIFIER (Phase 4) :** deux stations différentes lisent le **même fichier
> physique** · aucun MP3 dupliqué dans les répertoires d'AzuraCast · `M:\music` inchangé.

### PHASE 5 — Recréer Midnight Club

Station de test et de référence (slug probable `midnight_club`), avec sa grille
([design-programmation-editoriale.md](design-programmation-editoriale.md)).

> **STOP / VÉRIFIER (Phase 5) :** la station diffuse en continu 24 h sans intervention · la
> grille horaire est respectée · le flux est lisible depuis un lecteur externe.

### PHASE 6 — Exposer pour le poste

Rendre les flux et l'API joignables depuis Internet, et vérifier ce dont le poste a besoin.

> **STOP / VÉRIFIER (Phase 6) :** flux joignable hors du LAN · API stations et now-playing
> répondent · **CORS** vérifié si la route web est retenue côté poste
> ([design-visualiseur.md](design-visualiseur.md)).

### PHASE 7 — Le reste

Stations 2 et 3, tagging énergie, système de likes, XMLTV/M3U → xTeVe → Plex. Voir
[design-programmation-editoriale.md](design-programmation-editoriale.md).

---

## 6. Volumes — prudence

Une installation antérieure (LXC) avait créé des volumes nommés du type :

```
azuracast_acme            azuracast_backups         azuracast_db_data
azuracast_geolite_install azuracast_rsas_install    azuracast_sftpgo_data
azuracast_station_data    azuracast_www_uploads     …
```

Ils **peuvent** contenir des données utiles de l'ancienne installation, mais **ne doivent pas
être supposés présents ni réutilisables** sous Windows. Inventorier avant toute action
destructive.

---

## 7. Configuration cible

```
TZ   = Europe/Paris
PUID = 1000
PGID = 1000

Bibliothèque   : M:\music
Racine Docker  : C:\docker\media
AzuraCast      : C:\docker\media\azuracast
Station 1      : Midnight Club  (slug probable : midnight_club)
Dozzle         : 8080:8080     ← réalité constatée (la cible historique disait 8888, on garde 8080)
```

Côté WSL, le répertoire de base d'AzuraCast est **`/var/azuracast`** (disque de la VM), pas
`C:\docker\media\azuracast` : ce dernier est un clone du dépôt source, sans rôle dans la
procédure officielle.

---

## 8. Ce que le poste attend de ce serveur

Contrat côté poste : [design-brandt-rk711s.md](design-brandt-rk711s.md) §4.5 et §11.

| Le poste consomme | Statut |
|---|---|
| Flux Icecast stables, joignables depuis Internet | Phase 6 |
| API liste des stations | Native AzuraCast |
| API now-playing (titre, artiste, pochette) | Native AzuraCast |
| Énergie **E1–E5** du créneau en cours (optionnel, pilote le visualiseur) | Concept défini, **non exposé** — à concevoir |
| En-têtes **CORS** (si route web côté poste) | À configurer |

**Ce serveur est sur le chemin critique de la Phase 2 du poste.** En attendant, le poste se
teste sur des flux publics.

---

## 9. Questions ouvertes

1. ~~**Q1 — Windows est-il le bon hôte ?**~~ **Tranché (Phase 1, 2026-08-20)** : oui, via
   WSL2 — chemin officiellement documenté par AzuraCast (Docker Desktop + Ubuntu + procédure
   Linux standard dans le shell WSL). Pas de retour Proxmox.
2. ~~**Q2 — Données de l'ancienne installation LXC ?**~~ **Tranché (Phase 2, 2026-08-20)** :
   aucun volume `azuracast_*` sur ce Docker Desktop — repartir propre, Midnight Club recréée
   en Phase 5.
3. **Q3 — Montage partagé de `M:\music`** entre plusieurs stations, sans duplication (§4).
   Piste identifiée : depuis WSL, `M:` est visible en `/mnt/m` ; AzuraCast permet des
   storage locations « local filesystem » par station pointant sur un même montage. ⚠️ À
   vérifier en Phase 4, y compris la **performance du pont 9P** (`/mnt/m` traverse
   Windows→WSL) sur une bibliothèque de ~85 000 fichiers.
   **Constaté le 2026-09-06 :** le montage est bien là et vivant —
   `M:\ on /mnt/m type 9p (rw,noatime,aname=drvfs;path=M:\;uid=1000;gid=1000;msize=65536,…)`,
   864 Go dont 382 Go libres. Le `msize=65536` (64 Ko par message) est exactement le
   paramètre qui plombe les gros parcours d'arborescence : **le risque de lenteur est sur le
   scan de la médiathèque par AzuraCast**, pas sur la lecture d'un MP3 en cours de diffusion.
   À mesurer en Phase 4 avant de conclure ; repli possible si c'est inutilisable : exposer
   `M:\music` en SMB et le monter en CIFS dans WSL, ou déplacer la bibliothèque sur le disque
   de la VM (contraire au principe §4 — dernier recours).
   Côté AzuraCast, le volume nommé `station_data` reste le propriétaire de
   `/var/azuracast/stations/<slug>/` : **c'est un chemin d'AzuraCast, pas la bibliothèque** —
   les storage locations doivent pointer ailleurs (§4, interdit n°5).
4. **Q4 — Exposition Internet** : nom de domaine, TLS, reverse proxy déjà présent dans le
   DevLab ?
5. **Q5 — Sauvegardes** : que sauvegarde-t-on, et où ? (la bibliothèque, la configuration
   AzuraCast, les grilles) — noter que `docker.sh backup` existe et sauvegarde les volumes.
6. **Q6 — Que fait-on du clone `C:\docker\media\azuracast` ?** Il ne sert à rien dans la
   procédure officielle et entretient la confusion qui a produit l'impasse initiale.
   Proposition : le supprimer une fois la Phase 3 passée (pas avant, par prudence).

---

## 10. Liste des interdits (DO-NOT)

1. **Ne pas** rédiger un `docker-compose.yml` AzuraCast à la main ou de mémoire (§3).
2. **Ne pas** réutiliser les images `azuracast_web` / `azuracast_stations` : elles n'existent
   plus (§2.1).
3. **Ne pas** exécuter `docker compose down -v` avant l'inventaire des volumes (§6).
4. **Ne pas** exécuter `docker.sh` depuis Git Bash (§2.2).
5. **Ne pas** laisser AzuraCast devenir le propriétaire de la bibliothèque : `M:\music` reste
   la source unique, non réorganisée par les radios (§4).
6. **Ne pas** dupliquer les MP3 par station.
7. **Ne pas** lancer un déploiement sans avoir vérifié les conflits de ports (Phase 3).
8. **Ne pas** considérer une étape comme acquise sans sa vérification.

---

## 11. Journal

### 2026-09-06 — point de setup avant Phase 3
Inventaire complet en lecture seule (aucun conteneur créé, aucun volume touché) et
re-vérification des sources officielles sur le dépôt brut. **Rien n'a bougé depuis
le 2026-08-20 côté AzuraCast** : mêmes 2 services, mêmes images `ghcr.io/azuracast/*`,
même exigence `uname` ∈ {Linux, Darwin}, même `/var/azuracast`.

Constats nouveaux : Ubuntu WSL est en **26.04 LTS** avec 953 Go libres ; un conteneur
`portal6-ha` (port 8123) s'est ajouté au parc sans gêner AzuraCast ; les ports
80/443/2022/8000/8005 sont **tous libres** ; `/mnt/m` est monté en 9p `msize=65536`
(864 Go, 382 libres) — le point de perf à surveiller en Phase 4 est le **scan** de la
médiathèque, pas la lecture en diffusion.

Conclusion : la Phase 3 est intégralement prête et tient à **une seule case à cocher** —
l'intégration WSL de Docker Desktop pour Ubuntu (`settings-store.json` ne contient aucune
clé d'intégration = désactivée par défaut). Séquence d'installation écrite dans la Phase 3.
Deux points ouverts ajoutés : Q6 (sort du clone inutile `C:\docker\media\azuracast`) et
la piste de repli SMB/CIFS si le pont 9p s'avère trop lent (Q3).
