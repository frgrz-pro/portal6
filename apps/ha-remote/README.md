# Portal6 HA Remote

Télécommande Android du foyer — design dans [`.docs/app-remote.md`](../../.docs/app-remote.md).

- Kotlin + Jetpack Compose (Material 3), single module `app/`.
- 3 tabs : **Lights** (4 modes + grille 2×4 + switch All + Turn off), **TV**
  (bouton MUTE branché sur un mock ; l'envoi réel à la Shield est en phase 2)
  et **Réglages** (URL + jeton Home Assistant, bouton « Tester »).
- **Modes** (`data/Mode.kt`) : 4 modes = les 4 touches d'un bouton MOES.
  Mode 1 = tout on/off ; modes 2-4 = scènes définies dans l'app (appui long sur
  le mode). Avec HA, les modes 2-4 sont les scènes `scene.mode_2..4` de HA —
  les boutons physiques et l'app jouent la même chose.
- **Client Home Assistant** (`data/ha/`) : `HaClient` (OkHttp, REST +
  WebSocket avec reconnexion), `HaLightsRepository`, `HaModesRepository`.
  Sans réglages → **mode démo** (`MockLightsRepository`, `MockModesRepository`,
  état en SharedPreferences). `BackendHolder` bascule entre les deux quand les
  réglages changent ; l'UI parle à des dépôts délégués stables.
- **Deux tuiles dans le volet des réglages rapides** (`qs/`) : *Modes* passe au
  mode suivant parmi 2-4, *Mute TV* coupe/rétablit le son. Un bouton de
  l'onglet Lights les ajoute au volet en un tap (Android 13+).
- Dépôts en **singletons de process** (`Portal6App` + `AppContainer`) : les
  tuiles tournent hors Activity et partagent l'état de l'UI.
- Le jeton HA saisi dans Réglages reste dans le stockage privé de l'app
  (jamais dans le code ni dans le repo). HA en `http` sur le LAN →
  `usesCleartextTraffic` dans le manifest.

## Build

Outillage installé sur le PC Windows le 2026-09-06 (détail et dépannage dans
[`.docs/setup-dev-windows.md`](../../.docs/setup-dev-windows.md)) : Android Studio,
SDK 35 dans `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME`), JDK Temurin 21.

- **Android Studio** : *Open* → `apps/ha-remote/` ; le SDK est détecté via `local.properties`.
- **Ligne de commande** (le wrapper Gradle 8.10.2 est commité) :

```powershell
cd apps\ha-remote
.\gradlew.bat assembleDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`. Installer sur le téléphone
(débogage USB activé) : `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
