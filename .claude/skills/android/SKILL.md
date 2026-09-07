---
name: android
description: Piloter le téléphone Android de François (Galaxy S20 Ultra, USB) et l'app apps/ha-remote via adb — détecter le device, builder/installer l'APK, lancer l'app, capturer l'écran, taper, et surtout POUSSER DU TEXTE dans un champ pour éviter la saisie au clavier (jeton HA, URL longue). Invoquer quand on parle du téléphone, d'adb, d'installer/tester l'app, ou de « coller » quelque chose sur le phone.
---

# android — le téléphone et l'app `ha-remote` via adb

Le simulateur gèle sur le PC → on teste **sur le vrai téléphone** : Galaxy S20 Ultra
(`SM-G988`, Android 13 / SDK 33, arm64), serial adb **`R5CN404Q81Z`**, branché en USB.
L'app demande Android 8.0 minimum (`minSdk 26`).

```powershell
$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

## 1. Voir le téléphone

```powershell
& $ADB devices -l
```

| État | Cause | Action |
|---|---|---|
| absent | débogage USB off, câble « charge seule », mode USB | Options développeur → Débogage USB ; mode **Transfert de fichiers** ; rebrancher |
| `unauthorized` | popup pas accepté | déverrouiller, cocher « Toujours autoriser », Autoriser. Si pas de popup : `& $ADB kill-server; & $ADB start-server`, attendre ~5 s, relister |
| `device` | OK | continuer |

Toujours passer `-s R5CN404Q81Z` dès qu'un émulateur peut être listé en même temps.

## 2. Builder, installer, lancer

```powershell
Set-Location apps\ha-remote
.\gradlew.bat assembleDebug --console=plain -q        # ~1-4 min, APK ≈ 17 Mo
& $ADB -s R5CN404Q81Z install -r app\build\outputs\apk\debug\app-debug.apk
& $ADB -s R5CN404Q81Z shell am force-stop com.portal6.haremote
& $ADB -s R5CN404Q81Z shell monkey -p com.portal6.haremote -c android.intent.category.LAUNCHER 1
```

Crash ? `& $ADB -s R5CN404Q81Z logcat -d -t 300 | Select-String "FATAL|AndroidRuntime"`.

## 3. Vérifier à l'écran (Claude le fait tout seul)

```powershell
& $ADB -s R5CN404Q81Z exec-out screencap -p > "$SCRATCH\phone.png"   # puis Read
& $ADB -s R5CN404Q81Z shell input tap 412 272                          # coordonnées en pixels écran 1080×2400
```

L'image lue est affichée réduite (900×2000) : **multiplier les coordonnées par 1,2**.
Deux taps à la suite → mettre ~0,7 s entre, sinon le second est avalé.

## 4. Pousser du texte dans un champ (éviter la saisie clavier)

`input text` frappe la chaîne dans le **champ qui a le focus** sur le téléphone.
Ça marche pour tout texte sans espace (URL, jeton JWT `.`/`-`/`_` acceptés).

Procédure :
1. Sur le téléphone, taper **dans le champ** pour lui donner le focus (clavier ouvert).
2. Envoyer le texte :

```powershell
& $ADB -s R5CN404Q81Z shell input text "http://192.168.0.5:8123"
```

Si le texte arrive tronqué : vider le champ, désactiver la saisie prédictive du clavier
Samsung, recommencer. Un espace se tape `%s`.

### Cas secret (jeton HA, mot de passe) — règle absolue

**Claude ne lit pas, n'affiche pas et ne tape pas un secret.** Il donne à François la
commande à lancer lui-même, qui lit la valeur dans `.env` sans jamais la faire
transiter par la conversation :

```powershell
$t = ((Get-Content .env | Where-Object { $_ -match '^HA_TOKEN=' }) -replace '^HA_TOKEN=','').Trim(); & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s R5CN404Q81Z shell input text $t
```

Prérequis vérifiables sans afficher la valeur : pas de guillemets, pas d'espace
(`awk -F= '/^HA_TOKEN=/{print length($2)}' .env`), et **`.env` terminé par un retour
à la ligne** (sinon le prochain ajout se colle au jeton — déjà arrivé le 2026-09-07).

## 5. Réglages de l'app sur le téléphone

Onglet **Réglages** : URL `http://192.168.0.5:8123` (jamais `localhost` — le téléphone
n'est pas la tour), jeton longue durée HA (Profil → Sécurité → Jetons d'accès longue
durée), **Tester** puis **Enregistrer**. La ligne du bas de l'onglet Lights passe de
« Mode démo » à « connecté ». Tant qu'elle dit « Mode démo », les modes définis dans
l'app restent locaux (pas écrits dans les scènes HA, donc pas vus par les MOES).

## Quick Settings tiles

Les « actions de la barre du haut » = **Quick Settings tiles** (`TileService`). L'app en
a deux (Modes, Mute TV). Elles n'apparaissent qu'après ajout : bouton « + Tuile … » dans
l'app (Android 13+) ou crayon Modifier du volet.
