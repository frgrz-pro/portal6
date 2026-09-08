# Poste de dev macOS — builder l'app Android depuis le Mac

Le pendant de [setup-dev-windows.md](setup-dev-windows.md) : ce que le Mac (Apple Silicon,
macOS 14) exige pour builder `apps/ha-remote/` et faire tourner les scripts Python du repo.
Installé le 2026-09-08, **sans Android Studio** — les outils en ligne de commande suffisent.

## Questions ouvertes

- [ ] **Deux postes de build, un seul repo** : le PC Windows et le Mac ont chacun leur
  outillage. Rien ne les synchronise à part git. Faut-il désigner un poste de référence
  (ex. le Mac pour l'app, la tour pour HA/AzuraCast qui y tournent de toute façon) ?
- [ ] Émulateur : rien d'installé (`system-images` non téléchargées). Le test se fait sur
  le S20 Ultra en USB (skill `android`), comme sous Windows.

## Ce qui est installé

| Brique | Version | Emplacement | Comment |
|---|---|---|---|
| JDK | OpenJDK 21.0.12.1 | `/opt/homebrew/opt/openjdk@21` | `brew install openjdk@21` |
| Android cmdline-tools | — | `/opt/homebrew/share/android-commandlinetools` | `brew install --cask android-commandlinetools` |
| SDK platform | android-35 | idem | `sdkmanager "platforms;android-35"` |
| Build-tools | 35.0.0 | idem | `sdkmanager "build-tools;35.0.0"` |
| platform-tools (`adb`) | 37.0.1 | idem | `sdkmanager "platform-tools"` |
| venv Python | 3.14.4 | `~/.venvs/portal6` | `setup/bootstrap.sh` |

`~/.zshrc` porte trois blocs : le raccourci `p6` (repo + venv), `JAVA_HOME`, et
`ANDROID_HOME` + `platform-tools` dans le `PATH`.

## Décisions

- **`brew install openjdk@21` et non le cask Temurin** : le cask demande un `sudo` pour
  écrire dans `/Library/Java/JavaVirtualMachines`. La formule est *keg-only* (rien n'est
  lié dans `/opt/homebrew/bin`), ce qui est sans importance : Gradle ne veut qu'un
  `JAVA_HOME` correct.
- **Pas d'Android Studio.** Le build est un `./gradlew assembleDebug` ; l'IDE n'apporte
  rien qu'un `adb` et un éditeur ne donnent pas, pour ~10 Go de plus.
- **`local.properties` reste local** (gitignoré) : `sdk.dir=/opt/homebrew/share/android-commandlinetools`.
  Le chemin diffère du PC Windows, c'est précisément pourquoi ce fichier n'est pas versionné.

## Pièges rencontrés

- **`./gradlew` : `permission denied`.** Le wrapper avait été commité depuis Windows sans
  le bit exécutable. Corrigé une fois pour toutes dans le repo (`git update-index --chmod=+x`).
- **`sdkmanager` affiche un avertissement de dépréciation** (« use Android CLI instead ») :
  cosmétique, l'outil fonctionne. Les licences s'acceptent en pipant `yes |`.
- **Premier build = 12 min** (téléchargement des dépendances Gradle/AGP compris), APK debug
  de 19 Mo. Les suivants sont incrémentaux.

## Journal

### 2026-09-08
Reprise de l'app sur le Mac. Installé JDK 21, cmdline-tools, SDK 35, build-tools 35.0.0,
platform-tools ; `JAVA_HOME`/`ANDROID_HOME` dans `~/.zshrc` ; `local.properties` écrit.
**`gradlew assembleDebug` : BUILD SUCCESSFUL en 12 min**, `app-debug.apk` (19 Mo). Bit
exécutable de `gradlew` corrigé dans le repo. Venv `~/.venvs/portal6` créé et raccourci
`p6` posé (voir `setup/bootstrap.sh`, qui pose désormais `p6` au lieu de l'alias `portal6`).
