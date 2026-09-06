# Portal6 Remote

Télécommande Android du foyer — design dans [`.docs/app-remote.md`](../../.docs/app-remote.md).

- Kotlin + Jetpack Compose (Material 3), single module `app/`.
- 2 tabs : **Lights** (grille 2×4 + switch All + Turn off) et **TV** (maquette MUTE, phase 2).
- Backend **mocké** (`MockLightsRepository`, état en mémoire) derrière l'interface
  `LightsRepository` — le client Home Assistant (REST + WebSocket) la remplacera
  sans toucher l'UI.

## Build

Ouvrir `apps/remote/` dans Android Studio (le Mac n'a ni JDK ni SDK au 2026-08-30 —
installer Android Studio d'abord). Si le sync réclame le wrapper Gradle manquant :
`gradle wrapper` à la racine du projet (via `brew install gradle`).
