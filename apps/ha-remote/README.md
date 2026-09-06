# Portal6 HA Remote

Télécommande Android du foyer — design dans [`.docs/app-remote.md`](../../.docs/app-remote.md).

- Kotlin + Jetpack Compose (Material 3), single module `app/`.
- 2 tabs : **Lights** (grille 2×4 + switch All + Turn off) et **TV** (maquette MUTE, phase 2).
- Backend **mocké** (`MockLightsRepository`, état en mémoire) derrière l'interface
  `LightsRepository` — le client Home Assistant (REST + WebSocket) la remplacera
  sans toucher l'UI.

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
