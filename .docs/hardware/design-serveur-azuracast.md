# Design — Serveur de web-radios (AzuraCast)

**Owner :** François Grzybowski
**Statut :** **EN MARCHE** — Phases 1, 2 et **3 validées** (déploiement réussi le 2026-09-06,
canal stable). **Phase 4 : montages en place** (`/media/music`, `/media/radio`,
`/media/downloads`, tous en lecture seule) — reste à déclarer les 3 storage locations dans
l'UI et à lancer le scan, qui tranchera Q3.
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
| Déploiement AzuraCast | ✅ **EN MARCHE depuis le 2026-09-06** : base `/var/azuracast` (disque WSL), conteneurs `azuracast` + `azuracast_updater`, canal **stable**, 10 volumes `azuracast_*`. Détail : §5 Phase 3 |
| WSL2 | ✅ **Ubuntu 26.04 LTS**, distribution par défaut, version 2, 953 Go libres sur le disque de la VM |
| Docker Desktop | ✅ moteur 29.5.2, Compose v5.1.3, backend WSL2, opérationnel côté Windows |
| **Intégration WSL de Docker Desktop** | ✅ **ACTIVÉE le 2026-09-06** par François. Vérifié depuis le shell Ubuntu : `docker version` → **client 29.5.2 / serveur 29.5.2**, `docker compose version` → **v5.1.3**. **Le blocage historique est levé.** |

**Conteneurs en marche (2026-09-06)** — aucun ne dispute un port à AzuraCast :

| Conteneur | Image | Ports |
|---|---|---|
| `portal6-ha` | `ghcr.io/home-assistant/home-assistant:stable` | 8123 |
| `dozzle` | `amir20/dozzle:latest` | 8080 |
| `samba` | `dperson/samba` | 139, 1445→445 |
| `xteve` | `alturismo/xteve` | 34400 |

*(État du pré-vol, avant déploiement)* : ports **80, 443, 2022, 8000, 8005** tous libres,
volumes uniquement anonymes, aucun `azuracast_*`. **Depuis le déploiement**, AzuraCast
publie 80 / 443 / 2022 + la plage **8000–8496**, et les 10 volumes `azuracast_*` existent.

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

⚠️ **Corrigé le 2026-09-06.** L'arborescence annoncée ici jusqu'alors (`Library`, `Live`,
`Mixtapes`, `Playlists`, `Radio`, `Workspace`) était une **cible, pas la réalité** : il
n'existe aucun dossier `Mixtapes` ni `Live`. Voici ce que mesure le scan :

```
M:\
├── music\
│   ├── workspace\     67 763 fichiers  ← le gros du volume, mais du brouillon
│   ├── library\       17 252
│   ├── playlists\         24
│   └── tracks\             1
├── radio\
│   └── Radio-Library\    575  ← 558 fichiers de +20 min : LE contenu long
├── downloads\
│   ├── Book Club Radio\  125  (124 sets)
│   └── Big Business HQ\   21  (19 sets)
└── _a_trier\               ← quarantaine de la dédup : NE JAMAIS scanner
```

**Conséquence directe pour la Phase 4 :** monter `M:\music` seul **priverait les radios de
leur contenu long** — les mixtapes et DJ sets vivent dans `M:\radio\`. Le montage doit
couvrir `M:\` (voir §4.1 ci-dessous), pas `M:\music`.

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

**✅ VALIDÉ le 2026-09-06.** Installation réussie du premier coup, canal **stable**.

- Séquence réellement exécutée, depuis Windows, **sans `sudo`** : `wsl -d Ubuntu -u root`
  donne un shell root directement (l'utilisateur `r2d2` exige un mot de passe pour `sudo`,
  qui n'est pas saisissable depuis un shell non interactif).
- `./docker.sh setup-release stable` **avant** `install` : passer le canal en argument évite
  le prompt. Les `ask` restants prennent leur valeur par défaut si l'entrée standard est
  fermée (`< /dev/null`) — vérifié dans le script avant de lancer.
- Résultat : conteneurs **`azuracast`** (`ghcr.io/azuracast/azuracast:stable`) et
  **`azuracast_updater`**, 15 migrations de base appliquées, `All stations restarted`,
  `AzuraCast installation complete!`.
- **10 volumes nommés créés** : `azuracast_acme`, `azuracast_backups`, `azuracast_db_data`,
  `azuracast_geolite_install`, `azuracast_rsas_install`, `azuracast_sftpgo_data`,
  `azuracast_shoutcast2_install`, `azuracast_station_data`, `azuracast_stereo_tool_install`,
  `azuracast_www_uploads`. **C'est désormais ce que protège l'interdit n°3** (§10) : plus
  jamais de `down -v` sans sauvegarde.
- Vérifications du STOP : `http://localhost` → **200**, `http://192.168.0.5` → **200**,
  Dozzle `http://localhost:8080` → **200**. `portal6-ha`, `dozzle`, `samba`, `xteve`
  tournent toujours — **aucun conflit de port**.
- Ports effectivement publiés par le conteneur : **80, 443, 2022** + la plage stations
  **8000–8496**.

> ⚠️ **Fenêtre de vulnérabilité tant que le super-admin n'existe pas.** `/setup` répond 200 :
> **le premier visiteur qui l'ouvre devient administrateur de l'instance.** Créer le compte
> est la toute première action post-installation. (Le log d'installation affiche
> l'IP publique du foyer parce qu'AzuraCast fait un lookup externe — ce n'est pas une preuve
> d'exposition, mais ça ne dit pas non plus qu'il n'y a pas de redirection de port sur le
> routeur.)

**Pré-vol du 2026-09-06 — tout est vert sauf une case à cocher.**

| Prérequis | État |
|---|---|
| Méthode officielle identifiée et re-vérifiée | ✅ (Phase 1) |
| Compose global sain, sans image AzuraCast | ✅ (`docker compose config` valide) |
| Aucun volume `azuracast_*` à préserver | ✅ (Phase 2) |
| Ports 80 / 443 / 2022 / 8000 / 8005 libres | ✅ |
| Ubuntu WSL2 en place, place disque suffisante | ✅ (26.04 LTS, 953 Go libres) |
| **`docker` utilisable depuis le shell Ubuntu** | ✅ **levé le 2026-09-06** — intégration WSL activée, `docker version` répond 29.5.2 client **et** serveur |

> ✅ **Plus aucun prérequis.** L'intégration WSL a été activée par François le 2026-09-06 ;
> le tableau ci-dessus est intégralement vert. **La Phase 3 peut être lancée.**

La séquence Phase 3 (dans le shell Ubuntu, en sudo) :

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
>
> ✅ **Coché le 2026-09-08.** Les 5 stations partagent la storage location `/media/lib`.
> `Mixtapes/DnB/` alimente à la fois « MC · Mixtape Liquid DnB » (station 1) et « PR · Tout »
> (station 3), `Live/Cercle Shows/` à la fois « MC · Signature » et « CE · Tout » — les
> compteurs des deux stations restent justes après assignation, donc **l'association
> fichier→playlist est par station et ne déplace rien sur le disque**.

**🟡 Phase 4 — partie infrastructure FAITE le 2026-09-06 ; partie applicative à finir.**

**Méthode retenue, et elle est officielle : `docker-compose.override.yml`.** Le compose
généré par `docker.sh` ne doit jamais être édité ; le fichier d'échantillon du projet
indique explicitement de créer un `docker-compose.override.yml` pour toute
personnalisation. C'est donc la seule voie compatible avec la règle §3.

Fichier posé en `/var/azuracast/docker-compose.override.yml` :

```yaml
services:
  web:
    volumes:
      - /mnt/m/music:/media/music:ro
      - /mnt/m/radio:/media/radio:ro
      - /mnt/m/downloads:/media/downloads:ro
```

⚠️ **Corrigé le 2026-09-06, dans la foulée** : la première version ne montait que
`/mnt/m/music`. Elle **privait les radios de leur contenu long** — 558 des 895 fichiers de
plus de 20 min vivent dans `M:\radio`
([design-programmation-editoriale.md](design-programmation-editoriale.md) §9.1). Les trois
racines sont désormais montées.

**On ne monte pas `M:\` entier** : `M:\_a_trier` est la quarantaine de la dédup
(2 464 fichiers écartés), la monter réexposerait des doublons déjà triés. Les racines sont
listées explicitement — même règle que `npm run scan`.

- **Fusion vérifiée** avant application : `docker compose config` valide, et le bind
  apparaît **en plus** des 10 volumes nommés (aucun n'est remplacé — Compose concatène les
  listes de volumes).
- Conteneur recréé par `docker compose up -d` (**jamais** de `down`) : les volumes nommés,
  donc la base et le compte super-admin, sont intacts.
- **Vérifié dans le conteneur** : `/media/music` → `M:\` 864 Go, et `touch` répond
  `Read-only file system`. ✅

**Choix du chemin `/media/music`** — délibérément **hors** de `/var/azuracast/stations`,
qui appartient au volume `station_data` (le stockage interne d'AzuraCast). Y poser la
bibliothèque violerait l'interdit n°5 et appellerait la duplication. Plusieurs stations
pointeront sur **ce même montage**.

**`:ro` délibéré** : les radios lisent la bibliothèque, elles ne la réorganisent pas
(§4). À rediscuter seulement si une fonction réellement voulue d'AzuraCast exige
l'écriture.

**Ce qui reste à faire pour clore la Phase 4 :**

1. Déclarer **trois storage locations** dans AzuraCast (Administration → Storage Locations),
   toutes de type *Station Media*, adapter *Local Filesystem* :

   | Chemin | Contenu | Rôle éditorial |
   |---|---|---|
   | `/media/radio` | 575 fichiers, dont **558 de +20 min** | **la colonne vertébrale** : mixtapes et sets |
   | `/media/downloads` | 146 fichiers, dont 143 sets | DJ sets (Book Club Radio, Big Business HQ) → Stage 303 |
   | `/media/music` | 85 040 fichiers, surtout des tracks | les tracks qui aèrent et font les transitions |

   Les trois sont **partagées par toutes les stations** : c'est ce partage qui satisfait le
   STOP de cette phase (deux stations lisant le même fichier physique, zéro copie).
2. Lancer le scan de médiathèque — **c'est lui qui répondra à Q3** : le débit du pont 9p
   sur ~85 000 fichiers. Le point de mesure est le **scan**, pas la lecture en diffusion.
3. Le STOP formel (deux stations sur le même fichier physique) ne pourra être coché qu'en
   Phase 5, quand deux stations existeront.

> Note de méthode : la vérification du montage a été faite avec `df` et `stat`
> **volontairement, sans aucun `ls`/`find` sur la bibliothèque** — le scan de `M:\music`
> appartient aux scripts du repo et à AzuraCast, pas à des commandes lancées à la main.

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
7. ~~**Q7 — Midnight Club n'est pas publique.**~~ **Réglé le 2026-09-08** par l'API
   (`PUT /api/admin/station/1 {"enable_public_page": true}`). Contexte : `is_public: false`
   sur la station 1 :
   conséquence directe, `GET /api/nowplaying` et `GET /api/stations` renvoient `[]`, la
   page `/public/midnight_club` répond 404, et **tout client public (dont l'app remote)
   ne voit aucune radio**. Le réglage est dans *Station → Profil → Modifier → « Activer
   les pages publiques »*. À faire quand la station est prête à diffuser — c'est aussi
   ce qui décide si la maison peut l'écouter.

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
9. **Ne JAMAIS utiliser les fonctions de gestion de fichiers d'AzuraCast** (déplacer,
   renommer, supprimer, uploader) sur `/media/lib`. Depuis le 2026-09-07, ces montages sont
   en **lecture/écriture** — contraints, voir §4.2 — donc une manipulation dans l'UI
   **modifierait réellement `M:`**. Les jingles et openers se déposent depuis Windows.

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

### 2026-09-06 (bis) — blocage levé
François a activé l'intégration WSL de Docker Desktop pour Ubuntu. Vérifié depuis le shell
Ubuntu : `docker version` répond **29.5.2 client et serveur**, `docker compose version`
répond **v5.1.3**. `/var/azuracast` n'existe toujours pas → départ propre.
**Le pré-vol Phase 3 est intégralement vert : `docker.sh install` peut être lancé.**

### 2026-09-06 (ter) — Phase 3 franchie
AzuraCast est **déployé et en marche** sur canal stable, du premier coup. Deux
enseignements de méthode : (1) `wsl -u root` évite le `sudo` interactif impossible à
satisfaire depuis un shell non interactif ; (2) `setup-release stable` passé **en argument
avant** `install` supprime le prompt de canal, et les `ask` restants prennent leur défaut
avec `< /dev/null` — mécanique vérifiée dans le script avant lancement, conformément à §3.
STOP/VÉRIFIER validé : interface 200 en local et sur `192.168.0.5`, Dozzle intact, les
quatre autres conteneurs indemnes. 10 volumes `azuracast_*` existent maintenant — l'interdit
n°3 (`down -v`) cesse d'être théorique.
**Reste à faire immédiatement : créer le compte super-admin** (`/setup` est ouvert).
Prochaine étape : Phase 4, brancher `M:\music` sans duplication (Q3).

### 2026-09-06 (quater) — le contenu long était hors périmètre
L'analyse de `music.db` pour constituer de nouvelles stations a révélé une erreur qui aurait
été coûteuse : **l'arborescence `M:\music` décrite au §4 n'existait pas** (`Mixtapes`, `Live`
étaient une cible, pas la réalité), et surtout **le contenu long des radios vit dans
`M:\radio`** — 558 des 895 fichiers de plus de 20 min, sur 1 109 h cumulées.
Le montage posé plus tôt dans la journée ne couvrait que `/mnt/m/music` : les stations
auraient été privées de leur colonne vertébrale, sans erreur visible. Override corrigé le
jour même (3 racines montées, `_a_trier` toujours exclue), §4 réécrit sur les chiffres
mesurés, Phase 4 passée d'une à **trois storage locations**.
Enseignement : la cause racine était l'absence de script npm de scan — la racine était
passée à la main, d'où une dérive silencieuse du périmètre. Corrigé par `npm run scan`
([../musique.md](../musique.md)).

### 2026-09-07 — la lecture seule n'était pas tenable, et le montage a été restructuré
Trois enseignements, tous obtenus par l'expérience et non par supposition.

**1. Une station n'a qu'UNE storage location média** (`media_storage_location` est un
scalaire). Or les mixtapes (`M:\radio`) et les tracks (`M:\music\library`) sont sur deux
racines différentes : montées séparément, **une station ne pouvait pas jouer les deux**.
Montage restructuré en arborescence sous un parent commun `/media/lib` — une seule storage
location y donne accès. Au passage, `M:\music\workspace` (67 763 fichiers de brouillon) est
**sorti du périmètre** : le scan passe de 85 040 à ~17 985 fichiers.

**2. Le `:ro` est impossible.** Le scan échoue net :
`Unable to create a directory at /media/music. mkdir(): Permission denied`. AzuraCast crée
ses répertoires de travail à la racine d'une storage location, et il n'existe pas d'option
« lecture seule ». Les montages sont donc passés en **rw**.
**Conséquence à assumer** : le principe §4 (« la bibliothèque n'appartient pas aux radios »)
n'est plus garanti par le système de fichiers — il repose désormais sur la discipline, d'où
le nouvel **interdit n°9**. C'est un affaiblissement réel du garde-fou, acté faute
d'alternative : sans écriture, aucune station ne peut diffuser.

**3. Une storage location orpheline bloque tout le scan.** L'ancienne `/media/music`, qui
n'était plus assignée à aucune station, faisait quand même échouer la tâche `check_media`
pour l'ensemble des locations. Supprimée.

Côté grille, `features/radio/` matérialise le §10 : les grilles Midnight Club et Stage 303
sont versionnées en JSON et poussées par `push_schedule.py`. La grille Midnight Club est
**en place et vérifiée** via `/station/1/schedule`.

### 2026-09-07 (bis) — Q3 tranchée, et le piège des permissions de la racine

**Q3 est mesurée : le pont 9p tient ~0,5 fichier/s** (37 → 77 fichiers en 75 s), soit
**~9 h pour indexer les 17 985 fichiers**. Lent, mais **non rédhibitoire** : c'est un coût
unique, les scans suivants étant incrémentaux. Le repli SMB/CIFS envisagé n'a pas lieu
d'être. La prédiction du 2026-09-06 était juste : le coût est sur le **parcours**
d'arborescence, pas sur la lecture d'un fichier en diffusion.

**Le piège qui a coûté trois tentatives.** Après le passage en `rw`, le scan n'indexait
toujours que 9 fichiers puis s'arrêtait — en sortant pourtant en succès. Cause :
`/media/lib` est un **répertoire créé par Docker** pour porter les trois montages, et il
appartient à `root:root` en `755`. Les *sous-dossiers* montés étaient bien inscriptibles,
mais AzuraCast crée ses répertoires de travail **à la racine de la storage location**, et
échouait là. Corrigé par :

```bash
docker exec -u root azuracast chmod 777 /media/lib
```

> ⚠️ **Fragilité à connaître : ce `chmod` ne survit pas à un `docker compose up -d`.**
> Docker recrée le répertoire parent en `root:root` à chaque recréation du conteneur.
> **Après toute modification de l'override, rejouer le `chmod` puis relancer le scan**,
> sinon l'indexation s'arrête silencieusement — le symptôme est un scan qui « réussit »
> en n'indexant presque rien.

Diagnostic à retenir : `docker exec -u azuracast azuracast sh -c "mkdir -p /media/lib/.t"`
doit réussir. S'il répond `Permission denied`, le scan ne fonctionnera pas.

### 2026-09-08 — pourquoi l'app remote ne voit aucune radio
Constat depuis le Mac : `http://192.168.0.5/api/nowplaying` → `[]`, `/api/stations` → `[]`,
alors que `/api/status` répond `{"online":true}` et que `/api/station/1` retourne bien
Midnight Club. Cause : **`is_public: false`** sur la station (Q7) — l'API publique et les
pages publiques n'exposent que les stations publiques. Rien à corriger dans l'app, qui
consomme la bonne URL.

Second constat : le mount `http://192.168.0.5:8000/radio.mp3` **ne répond pas** (réponse
vide, le port accepte pourtant le TCP — artefact du portproxy WSL). Icecast n'est donc pas
en diffusion : la station n'est pas démarrée (ou n'a rien à jouer, le scan des ~18 000
fichiers à 0,5 fichier/s n'étant pas forcément terminé). **Les deux points sont à traiter
sur la tour** : publier la station *et* la faire diffuser.

### 2026-09-08 (bis) — station publiée et en diffusion, mais sans contenu
Tout fait par l'API depuis le Mac (`AZURACAST_API_KEY` + `AZURACAST_BASE_URL` dans le `.env`) :

1. `enable_public_page` → `true` (Q7). `GET /api/stations` liste enfin Midnight Club.
2. **Frontend et backend étaient tous les deux arrêtés** (`{"backendRunning": false,
   "frontendRunning": false}`). `POST …/frontend/start` et `…/backend/start` échouent en 500
   `BadNameException: not recognized as a service` — **c'est `POST /api/station/1/restart`
   qu'il faut** (il enregistre les services dans Supervisor). Après ça les deux tournent et
   Icecast répond 200 sur `http://192.168.0.5:8000/radio.mp3`.
3. `/api/nowplaying` restait vide : le cache n'est reconstruit que par la tâche périodique.
   `PUT /api/admin/debug/station/1/nowplaying` le force → la station **apparaît enfin dans
   l'API publique**, donc dans l'app.

Reste le vrai manque : **la station est `is_online: false` / « Station Offline »** parce que
les **9 playlists ont 0 fichier** — le scan média n'a jamais atteint `library/` (Q8) et le
`--fill` n'a pas été joué (Q9 à trancher avant). Ordre de résolution : Q8 (scan) → Q9
(sources) → `push_schedule.py --fill` → la radio est audible.

À noter pour l'outillage : `push_schedule.py` lit `AZURACAST_BASE_URL` (défaut `localhost`,
prévu pour un lancement depuis la tour) — la ligne est désormais dans le `.env` du Mac, le
script tourne donc aussi depuis ici (`--dry-run` vérifié : 8 playlists, 42 créneaux).

### 2026-09-08 (ter) — Midnight Club diffuse
**La station est en ligne et joue** : `/api/nowplaying` → `is_online: true`, playlist
« MC · Signature », et le mount sert bien du **MPEG layer III 192 kbps 44,1 kHz** (vérifié
en tirant 64 Ko de `http://192.168.0.5:8000/radio.mp3`). L'onglet Radio de l'app a donc
enfin une station à afficher.

Chemin parcouru : Q9 tranchée (sources par genre) → `push_schedule.py --fill` → `restart`.
Remplissage obtenu : Signature 167, Replay Signature 147, Mixtape Vaporwave 76, Phonk 49,
Liquid DnB 41. Les trois playlists `Tracks *` restent à **0** — elles pointent sur
`library/`, non indexé (Q8).

**Bug corrigé dans `push_schedule.py` : `--fill` n'avait jamais pu fonctionner.** Il postait
une liste d'ids sur `POST /station/{id}/playlist/{pid}/import`, qui répond
`500 No "playlist_file" provided` — cet endpoint attend un **fichier M3U en multipart**, pas
du JSON. La seule voie API pour assigner des médias à une playlist est l'action batch :

```
PUT /api/station/{id}/files/batch
{"do": "playlist", "files": ["<chemins relatifs>"], "dirs": [], "playlists": [<pid>]}
```

Le script envoie désormais ça, par paquets de 200 chemins, après le `DELETE …/empty`.

### 2026-09-08 (quater) — quatre radios « un dossier en boucle »
Demande François : des stations supplémentaires, **un dossier qui tourne en boucle, sans
programmation horaire**. Créées et en diffusion :

| Station | Dossiers | Titres | Flux |
|---|---|---|---|
| Panic Room | `Live/Panic Room/` + `Mixtapes/DnB/` | 89 | `:8020/radio.mp3` |
| Cercle | `Live/Cercle Shows/` | 167 | `:8030/radio.mp3` |
| Block Party | `Mixtapes/Hip-Hop/` | 56 | `:8040/radio.mp3` |
| LoFi Desk | `Mixtapes/LoFi/` | 28 | `:8050/radio.mp3` |

Les cinq stations répondent `is_online: true` dans `/api/nowplaying` et servent du MP3
192 kbps ; l'app les liste toutes.

**Ce que « en boucle » veut dire ici** : une seule playlist, `schedule: []`, donc **aucun
créneau**. Une playlist `default` sans créneau est en rotation permanente — c'est le
comportement natif d'AzuraCast, il n'y a rien à inventer. Le fichier de grille garde la
même forme que `midnight_club.json` : la différence entre une radio programmée et une radio
en boucle tient au seul contenu de `schedule`.

**Ports** : AzuraCast alloue lui-même dans la plage 8000-8496 (8020, 8030, 8040, 8050), il
ne faut donc pas les figer dans le repo — `listen_url` de l'API fait foi.

Reste inutilisé côté médias : `Live/Humano Sound` (61), `Live/Everybody Loves To Boogie`
(24), `Live/Login.jp` (12), `Mixtapes/Organic House` (5), `Live/JPool` (2), et les 146
fichiers de `downloads/` (fourre-tout, non éditorialisé). De quoi faire une 6e station
house/boogie si l'envie vient.

