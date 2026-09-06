# Validation n°1 du use case MUTE (cf. .docs/tv-mute.md, angle 2 — ADB réseau).
# Prérequis sur la Shield : Paramètres → Préférences de l'appareil → À propos →
# taper 7× sur « Build » ; puis Options pour les développeurs → Débogage réseau ON.
#
# Usage : .\shield_mute_adb.ps1 -Ip 192.168.x.y
param([Parameter(Mandatory = $true)][string]$Ip, [int]$Port = 5555)

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb introuvable ($adb) — SDK Android non installé ?" }

& $adb connect "${Ip}:${Port}"
& $adb -s "${Ip}:${Port}" wait-for-device
Write-Host "Envoi KEYCODE_VOLUME_MUTE (164) — regarde si la BARRE TCL se coupe..."
& $adb -s "${Ip}:${Port}" shell input keyevent 164
Write-Host "Si oui : la chaîne CEC Shield→télé→barre marche, on implémente l'angle 1 (Remote v2)."
Write-Host "Si non : vérifier HDMI-CEC sur la télé et la barre (cf. .docs/tv-mute.md)."
