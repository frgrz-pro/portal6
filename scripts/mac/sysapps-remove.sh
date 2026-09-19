#!/usr/bin/env bash
# sysapps-remove — supprime des apps de /System/Applications (volume système scellé).
# ⚠️ DESTRUCTIF et hors des clous Apple. Procédure et risques : .docs/mac-maintenance.md § 2bis.
#
#   sysapps-remove.sh          DRY-RUN : vérifie les prérequis, liste ce qui serait supprimé
#   p6 sysapps --apply    supprime + crée le snapshot bootable (sudo)
#
# Prérequis (en Recovery : power maintenu → Options → Terminal) :
#   csrutil disable && csrutil authenticated-root disable
# Liste arbitrée par François le 2026-09-19. Volontairement ABSENTS (jamais ici) :
# System Settings, App Store, Preview, Safari, Terminal, Disk Utility, Activity Monitor,
# Console, Keychain Access, System Information, Screenshot, Passwords, Apps (ex-Launchpad).

set -euo pipefail
APPLY=0; [ "${1:-}" = "--apply" ] && APPLY=1

APPS=(
  "News" "Stocks" "Chess" "Podcasts" "TV" "Freeform" "Maps" "Home" "Photo Booth"
  "Stickies" "VoiceMemos" "Dictionary" "Siri" "FaceTime" "Messages" "Calendar"
  "Reminders" "FindMy" "Shortcuts" "Automator" "TextEdit" "QuickTime Player"
  "Tips" "Games" "Journal" "Image Playground" "Siri AI" "Phone" "iPhone Mirroring"   # macOS 27
  "Utilities/Boot Camp Assistant" "Utilities/Grapher" "Utilities/VoiceOver Utility"
  "Utilities/Migration Assistant"
)

die() { printf '🔴 %s\n' "$1" >&2; exit 1; }
ok()  { printf '🟢 %s\n' "$1"; }

# --- 1. Prérequis ------------------------------------------------------------
ready=1
csrutil status | grep -q disabled && ok "SIP désactivé" || { echo "🔴 SIP actif"; ready=0; }
csrutil authenticated-root status | grep -q disabled && ok "authenticated-root désactivé" \
  || { echo "🔴 authenticated-root actif (volume scellé)"; ready=0; }
if tmutil listlocalsnapshots / 2>/dev/null | grep -q MSUPrepareUpdate; then
  echo "🟡 mise à jour macOS préparée : l'installer AVANT, sinon elle remettra toutes les apps"
fi

# volume système = le snapshot monté sur / sans son suffixe (disk3s1s1 → disk3s1)
snap=$(diskutil info / | awk '/Device Node/{print $3}')
vol=$(echo "$snap" | sed -E 's/s[0-9]+$//')
role=$(diskutil info "$vol" | awk -F': *' '/APFS Volume Group|Volume Name/{print $2}' | head -1)
echo "   volume système : $vol ($role) — snapshot courant : $snap"
[[ "$vol" =~ ^/dev/disk[0-9]+s[0-9]+$ ]] || die "volume système introuvable"

# --- 2. Ce qui sera supprimé ------------------------------------------------
echo; echo "== Apps visées =="
total=0
for a in "${APPS[@]}"; do
  p="/System/Applications/$a.app"
  if [ -d "$p" ]; then
    k=$(du -sk "$p" | cut -f1); total=$((total + k))
    printf '   %4d Mo  %s\n' $((k / 1024)) "$a"
  else
    printf '   absent   %s\n' "$a"
  fi
done
printf '   ---- total ~%d Mo\n' $((total / 1024))

if [ $APPLY = 0 ]; then
  echo; echo "DRY-RUN : rien n'a été touché."
  [ $ready = 1 ] && echo "Prérequis OK → scripts/mac/sysapps-remove.sh --apply" \
                 || echo "Prérequis manquants → passer par la Recovery (voir en-tête)."
  exit 0
fi

# --- 3. Application ----------------------------------------------------------
[ $ready = 1 ] || die "prérequis manquants, abandon"
printf '\nDernière sauvegarde Time Machine / clone OK ? Taper SUPPRIMER pour continuer : '
read -r answer; [ "$answer" = "SUPPRIMER" ] || die "annulé"

mnt="$HOME/.sysmnt"; mkdir -p "$mnt"
sudo mount -o nobrowse -t apfs "$vol" "$mnt" || die "montage de $vol impossible"
trap 'sudo umount "$mnt" 2>/dev/null || true' EXIT
[ -d "$mnt/System/Applications" ] || die "$mnt ne ressemble pas au volume système"
[ -w "$mnt/System/Applications" ] || sudo test -w "$mnt/System/Applications" || die "volume monté en lecture seule"

for a in "${APPS[@]}"; do
  p="$mnt/System/Applications/$a.app"
  case "$p" in "$mnt"/System/Applications/*.app) ;; *) die "chemin suspect : $p";; esac
  [ -d "$p" ] && sudo rm -rf "$p" && echo "   supprimé : $a"
done

# tout ou rien : sans bless réussi, le Mac reboote sur l'ancien snapshot intact
sudo bless --folder "$mnt/System/Library/CoreServices" --bootefi --create-snapshot \
  || die "bless a échoué — NE PAS s'inquiéter : l'ancien snapshot reste celui de boot"
ok "snapshot créé. Redémarrer pour voir le résultat."
echo "   Retour arrière : réinstaller macOS depuis la Recovery (garde les données) puis csrutil enable."
