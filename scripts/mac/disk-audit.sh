#!/usr/bin/env bash
# p6 disk — état des lieux de l'espace disque macOS : qui occupe quoi, et ce qui est
# droppable. LECTURE SEULE : rien n'est effacé, chaque ligne donne la commande à lancer
# soi-même.
#
#   p6 disk          état des lieux rapide (~30 s)
#   p6 disk --full   + mesure de l'espace « inaccessible » (du complet, ~3 min)
#
# Niveaux :  🟢 cache régénéré tout seul   🟡 à vérifier avant   🔵 app/outil entier
# Doc : .docs/mac-maintenance.md

set -u
FULL=0; [ "${1:-}" = "--full" ] && FULL=1
MIN_KB=$((100 * 1024))          # on n'affiche rien sous 100 Mo
GREEN_KB=0

h() { awk -v k="$1" 'BEGIN{ if (k>=1048576) printf "%6.1f Go", k/1048576; else printf "%6.0f Mo", k/1024 }'; }
kb() { du -skx "$1" 2>/dev/null | awk '{print $1+0}'; }
title() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }

# item <niveau> <chemin> <libellé> <commande de nettoyage>
item() {
  local lvl=$1 path=$2 label=$3 cmd=$4 k
  [ -e "$path" ] || return 0
  k=$(kb "$path"); [ "${k:-0}" -ge $MIN_KB ] || return 0
  [ "$lvl" = "🟢" ] && GREEN_KB=$((GREEN_KB + k))
  printf '%s %s  %-38s \033[2m%s\033[0m\n' "$lvl" "$(h "$k")" "$label" "$cmd"
}

# top <niveau> <dossier> <n> <suffixe commande> : les n plus gros sous-dossiers
top() {
  local lvl=$1 dir=$2 n=$3
  [ -d "$dir" ] || return 0
  du -skx "$dir"/* 2>/dev/null | sort -rn | head -"$n" | while IFS=$'\t' read -r k p; do
    [ "$k" -ge $MIN_KB ] || continue
    printf '%s %s  %-38s \033[2m%s\033[0m\n' "$lvl" "$(h "$k")" "${p#$HOME/}" "rm -rf \"$p\""
  done
}

# --- 1. Conteneur APFS -------------------------------------------------------
title "Conteneur APFS (disque interne)"
diskutil apfs list 2>/dev/null | awk '
  /Size \(Capacity Ceiling\)/ { match($0,/\(([0-9.]+ GB)\)/); tot=substr($0,RSTART+1,RLENGTH-2) }
  /Capacity In Use By Volumes/ { match($0,/\(([0-9.]+ GB)\)/); use=substr($0,RSTART+1,RLENGTH-2) }
  /Capacity Not Allocated/ { match($0,/\(([0-9.]+ GB)\)/); fr=substr($0,RSTART+1,RLENGTH-2);
                             printf "  total %s · utilisé %s · libre %s\n", tot, use, fr; exit }'
diskutil apfs list 2>/dev/null | awk '
  /Name:/ { sub(/.*Name: +/,""); sub(/ \(Case.*/,""); n=$0 }
  /Capacity Consumed/ { match($0,/\(([0-9.]+ GB)\)/); printf "  %-14s %s\n", n, substr($0,RSTART+1,RLENGTH-2) }
  /^\+-- Container/ && ++c>1 { exit }'
echo "  ↳ System/Preboot/Recovery/VM ne sont PAS scannables par un outil type Disk Inspector"
echo "    (volumes système, scellés) → ils finissent dans « Inaccessible Disk Space »."

# --- 2. Snapshots & mises à jour ---------------------------------------------
title "Snapshots locaux & mises à jour macOS"
snaps=$(tmutil listlocalsnapshots / 2>/dev/null | grep -c 'com.apple')
echo "  $snaps snapshot(s) local(aux) :"
tmutil listlocalsnapshots / 2>/dev/null | grep 'com.apple' | sed 's/^/    /'
if tmutil listlocalsnapshots / 2>/dev/null | grep -q MSUPrepareUpdate; then
  echo "  ⚠️  MSUPrepareUpdate = une mise à jour macOS est téléchargée/préparée et gonfle Preboot."
  echo "     → l'installer (Réglages > Mise à jour) ou la laisser expirer ; pas de suppression manuelle."
fi
tm=$(tmutil listlocalsnapshots / 2>/dev/null | grep -c 'TimeMachine')
[ "$tm" -gt 0 ] && echo "  🟢 $tm snapshot(s) Time Machine purgeables : tmutil thinlocalsnapshots / 999999999999 4"

# --- 3. Caches régénérables ---------------------------------------------------
title "🟢 Caches — se reconstruisent seuls"
item 🟢 "$HOME/Library/Caches/Homebrew"      "Homebrew (téléchargements)"     "brew cleanup --prune=all"
item 🟢 "$HOME/.npm"                          "npm cache"                       "npm cache clean --force"
item 🟢 "$HOME/.cache"                        "~/.cache (pip, uv, outils CLI)"  "rm -rf ~/.cache/*"
item 🟢 "$HOME/.gradle/caches"                "Gradle caches"                   "rm -rf ~/.gradle/caches"
item 🟢 "$HOME/Library/Developer/Xcode/DerivedData" "Xcode DerivedData"         "rm -rf ~/Library/Developer/Xcode/DerivedData"
item 🟢 "$HOME/Library/Developer/Xcode/iOS DeviceSupport" "Xcode iOS DeviceSupport" "rm -rf ~/Library/Developer/Xcode/iOS\\ DeviceSupport"
item 🟢 "$HOME/Library/Logs"                  "Logs utilisateur"                "rm -rf ~/Library/Logs/*"
item 🟢 "$HOME/Library/Application Support/Claude/Cache" "Claude (cache Electron)" "quitter Claude puis rm -rf"
echo "  -- plus gros caches d'apps (~/Library/Caches) :"
top 🟢 "$HOME/Library/Caches" 10
GREEN_KB=$((GREEN_KB + $(du -skx "$HOME/Library/Caches" 2>/dev/null | awk '{print $1+0}') - $(kb "$HOME/Library/Caches/Homebrew")))

# --- 4. À vérifier ------------------------------------------------------------
title "🟡 À vérifier avant de supprimer"
item 🟡 "$HOME/.Trash"                        "Corbeille"                       "vider depuis le Finder"
item 🟡 "$HOME/Downloads"                     "Téléchargements"                 "trier à la main"
item 🟡 "$HOME/Library/Application Support/Claude/vm_bundles" "Claude — VM Cowork" "se retélécharge si Cowork est relancé"
item 🟡 "$HOME/Library/Developer/CoreSimulator" "Simulateurs iOS"              "xcrun simctl delete unavailable"
item 🟡 "$HOME/.android/avd"                  "Émulateurs Android (AVD)"        "avdmanager delete avd -n <nom>"
item 🟡 "$HOME/Library/Application Support/MobileSync/Backup" "Sauvegardes iPhone" "Finder > iPhone > Gérer les sauvegardes"
item 🟡 "$HOME/Library/ScreenRecordings"      "Enregistrements d'écran"         "trier à la main"
item 🟡 "$HOME/Library/Containers/com.apple.BKAgentService" "Livres (Books) téléchargés" "retirer les livres dans l'app"
item 🟡 "$HOME/Library/Containers/com.apple.mail" "Mail (pièces jointes)"      "réglages Mail"
item 🟡 "/Library/Application Support/GarageBand" "Boucles GarageBand"          "sudo rm -rf (si GarageBand supprimé)"
item 🟡 "/Library/Audio/Apple Loops"          "Apple Loops"                     "sudo rm -rf (si GarageBand supprimé)"
echo "  -- plus gros dossiers de données d'apps (~/Library/Application Support) :"
top 🟡 "$HOME/Library/Application Support" 8
echo "  -- fichiers > 1 Go dans ~ (hors Library) :"
find "$HOME" -xdev -path "$HOME/Library" -prune -o -type f -size +1G -print 2>/dev/null | while read -r f; do
  printf '🟡 %s  %s\n' "$(h "$(kb "$f")")" "${f#$HOME/}"
done

# --- 5. Apps entières ---------------------------------------------------------
title "🔵 Applications (les plus grosses) — p6 apps pour le détail"
du -skx /Applications/* 2>/dev/null | sort -rn | head -12 | while IFS=$'\t' read -r k p; do
  [ "$k" -ge $MIN_KB ] && printf '🔵 %s  %s\n' "$(h "$k")" "${p#/Applications/}"
done
item 🔵 "/Library/Developer/CommandLineTools" "Xcode Command Line Tools"     "requis par brew/git — garder"
item 🔵 "/opt/homebrew"                       "Homebrew (formules)"           "brew autoremove ; brew leaves pour lister"

# --- 6. Inaccessible (option --full) -----------------------------------------
if [ $FULL = 1 ]; then
  title "Espace « inaccessible » (mesure complète)"
  data_b=$(diskutil info /System/Volumes/Data | awk -F'[()]' '/Volume Used Space/{print $2}' | awk '{print $1}')
  read_k=$(du -xsk /System/Volumes/Data 2>/dev/null | awk '{print $1}')
  awk -v d="$data_b" -v r="$read_k" 'BEGIN{
    printf "  Volume Data : %.1f Go consommés, %.1f Go lisibles sans droits → %.1f Go illisibles\n", d/1e9, r*1024/1e9, (d-r*1024)/1e9 }'
  echo "  Illisible = dossiers protégés (TCC/SIP : /private/var/db, /private/var/folders, ~/Library/Mail…)"
  echo "  → donner « Accès complet au disque » à Disk Inspector (Réglages > Confidentialité) réduit ce bloc."
fi

title "Bilan"
printf '  🟢 récupérable sans risque : ~%s\n' "$(h $GREEN_KB)"
echo "  (rien n'a été supprimé — lancer les commandes affichées à la main)"
