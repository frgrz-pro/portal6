# Portal6 HA Remote

Télécommande Android du foyer — design dans [`.docs/app-remote.md`](../../.docs/app-remote.md).

- Kotlin + Jetpack Compose (Material 3), single module `app/`.
- 2 tabs : **Lights** (configs + grille 2×4 + switch All + Turn off) et **TV**
  (bouton MUTE branché sur un mock ; l'envoi réel à la Shield est en phase 2).
- **Configs de pièce** : instantané nommé de l'état des 8 prises du salon
  (« Soirée », « Lecture »…), enregistré depuis l'onglet Lights et persisté
  localement (`ConfigStore`, SharedPreferences + JSON).
- **Deux tuiles dans le volet des réglages rapides** (`qs/`) : *Salon* fait
  défiler les configs en boucle, *Mute TV* coupe/rétablit le son. Un bouton de
  l'onglet Lights les ajoute au volet en un tap (Android 13+).
- Backend **mocké** (`MockLightsRepository`, `MockTvRepository`) derrière les
  interfaces `LightsRepository` / `TvRepository` — le client Home Assistant
  (REST + WebSocket) et le client Shield les remplaceront sans toucher l'UI.
- Dépôts en **singletons de process** (`Portal6App` + `AppContainer`) : les
  tuiles tournent hors Activity et partagent l'état de l'UI.

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
