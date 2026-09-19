#!/usr/bin/env bash
# p6 apps — inventaire des applications macOS et de ce qui tourne en arrière-plan,
# avec pour chacune la bonne façon de la virer. LECTURE SEULE.
#
#   p6 apps            inventaire complet
#   p6 apps --agents   seulement les LaunchAgents/Daemons (ce qui démarre tout seul)
#   p6 apps --leftovers <bundle-id|nom>   restes d'une app dans ~/Library (avant/après suppression)
#
# Catégories :
#   SYSTÈME   /System/Applications — protégé par SIP, INAMOVIBLE (Safari, Mail, Music…)
#   APPLE     /Applications signé Apple, installé via App Store (Keynote, iMovie…) — supprimable
#   STORE     /Applications, App Store tiers — supprimable (Finder ou app-store cli)
#   TIERS     /Applications hors store — supprimable ; les restes vivent dans ~/Library
#   BREW      cask Homebrew — brew uninstall --zap <cask>
# Doc : .docs/mac-maintenance.md

set -u
ONLY_AGENTS=0; [ "${1:-}" = "--agents" ] && ONLY_AGENTS=1
LEFTOVERS=0; [ "${1:-}" = "--leftovers" ] && LEFTOVERS=1

h() { awk -v k="$1" 'BEGIN{ if (k>=1048576) printf "%6.1f Go", k/1048576; else printf "%6.0f Mo", k/1024 }'; }
title() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
dim() { printf '\033[2m%s\033[0m' "$1"; }

if [ $ONLY_AGENTS = 0 ] && [ $LEFTOVERS = 0 ]; then
  # --- 0. Contexte -----------------------------------------------------------
  title "Contexte"
  echo "  SIP : $(csrutil status | sed 's/.*status: //')"
  echo "  MDM : $(profiles status -type enrollment 2>/dev/null | grep 'MDM enrollment' | sed 's/.*: //')  (No = aucune app imposée par une entreprise)"
  n=$(ls /System/Applications | grep -c '\.app$')
  echo "  $n apps système dans /System/Applications : inamovibles tant que SIP est actif (et à ne pas toucher)."
  echo "  → pour les faire disparaître : les retirer du Dock / Launchpad, ou les masquer via Temps d'écran."

  # --- 1. Casks Homebrew (pour croiser) --------------------------------------
  casks=$(brew list --cask 2>/dev/null | tr '\n' ' ')

  # --- 2. Inventaire /Applications -------------------------------------------
  title "/Applications — par taille"
  printf '  %-8s %-9s %-36s %s\n' "TYPE" "TAILLE" "APP" "POUR SUPPRIMER"
  du -skx /Applications/* 2>/dev/null | sort -rn | while IFS=$'\t' read -r k p; do
    name=$(basename "$p"); base="${name%.app}"
    plist="$p/Contents/Info.plist"
    bid=$(defaults read "$plist" CFBundleIdentifier 2>/dev/null)
    if [ -L "$p" ]; then type="SYSTÈME"; how="lien vers /System — inamovible"
    elif [ -d "$p/Contents/_MASReceipt" ] || xattr -p com.apple.appstore.metadata "$p" >/dev/null 2>&1; then
      case "$bid" in com.apple.*) type="APPLE";; *) type="STORE";; esac
      how="Launchpad (clic long → ✕) ou : sudo rm -rf \"$p\""
    elif c=$(echo "$casks" | tr ' ' '\n' | grep -ix "$(echo "$base" | tr 'A-Z ' 'a-z-')" | head -1); [ -n "$c" ]; then
      type="BREW"; how="brew uninstall --zap $c"
    elif [ ! -d "$p/Contents" ]; then type="DOSSIER"; how="regarder dedans (suite Adobe, WD…)"
    else type="TIERS"; how="Corbeille + restes : p6 apps --leftovers \"$bid\""
    fi
    printf '  %-8s %s  %-36s %s\n' "$type" "$(h "$k")" "${name:0:36}" "$(dim "$how")"
  done

  # --- 3. Apps Apple préinstallées mais supprimables -------------------------
  title "Apps Apple hors système (iWork, iMovie, GarageBand) — supprimables"
  for a in Keynote Pages Numbers iMovie GarageBand; do
    p="/Applications/$a.app"; [ -d "$p" ] || continue
    printf '  %s  %-12s sudo rm -rf "%s"\n' "$(h "$(du -skx "$p" | cut -f1)")" "$a" "$p"
  done
  for d in "/Library/Application Support/GarageBand" "/Library/Audio/Apple Loops"; do
    [ -d "$d" ] && printf '  %s  %-12s sudo rm -rf "%s"\n' "$(h "$(du -skx "$d" | cut -f1)")" "(loops)" "$d"
  done
  echo "  $(dim 'Ces apps sont des achats App Store : elles se réinstallent gratuitement depuis l App Store.')"
fi

# --- 4. Ce qui démarre tout seul ---------------------------------------------
if [ $LEFTOVERS = 0 ]; then
title "LaunchAgents / LaunchDaemons (démarrage auto) — ORPHELIN = cible absente"
printf '  %-8s %-58s %s\n' "ÉTAT" "PLIST" "PROGRAMME"
for dir in /Library/LaunchDaemons /Library/LaunchAgents "$HOME/Library/LaunchAgents"; do
  for p in "$dir"/*.plist; do
    [ -f "$p" ] || continue
    if [ ! -r "$p" ]; then prog=""; else
      prog=$(/usr/libexec/PlistBuddy -c "Print :Program" "$p" 2>/dev/null \
          || /usr/libexec/PlistBuddy -c "Print :ProgramArguments:0" "$p" 2>/dev/null)
    fi
    if [ -z "$prog" ]; then st="illisible"          # plist root-only (daemons) : voir avec sudo
    elif [ "$prog" = "open" ] || command -v "$prog" >/dev/null 2>&1 || [ -e "$prog" ]; then st="ok"
    else st="ORPHELIN"; fi
    printf '  %-8s %-58s %s\n' "$st" "$(dim "${p#$HOME/}")" "${prog:-(sudo pour lire)}"
  done
done
echo
echo "  Désactiver un agent : launchctl bootout gui/\$(id -u) <plist>   (user)  |  sudo launchctl bootout system <plist>  (daemon)"
echo "  Puis supprimer le .plist. Les orphelins sont sans effet mais polluent ; à virer."
echo "  Helpers privilégiés associés : /Library/PrivilegedHelperTools/"
fi

# --- 5. Restes d'apps désinstallées -----------------------------------------
if [ $LEFTOVERS = 1 ] && [ -n "${2:-}" ]; then
  title "Restes pour $2"
  for d in "$HOME/Library/Application Support" "$HOME/Library/Caches" "$HOME/Library/Containers" \
           "$HOME/Library/Group Containers" "$HOME/Library/Preferences" "$HOME/Library/Saved Application State" \
           "$HOME/Library/WebKit" "$HOME/Library/HTTPStorages" "$HOME/Library/Logs" "/Library/Application Support"; do
    find "$d" -maxdepth 1 -iname "*$2*" 2>/dev/null | while read -r f; do
      printf '  %s  %s\n' "$(h "$(du -skx "$f" | cut -f1)")" "$f"
    done
  done
fi
