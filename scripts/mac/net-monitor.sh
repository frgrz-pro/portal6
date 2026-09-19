#!/usr/bin/env bash
# p6 net — qui parle à qui depuis ce Mac (l'esprit Little Snitch, en ligne de commande,
# avec les outils livrés avec macOS : nettop, lsof, tcpdump). LECTURE SEULE.
#
#   p6 net            photo instantanée : connexions établies, par process, avec le pays/ASN si dispo
#   p6 net live       tableau qui se rafraîchit (nettop) — q pour sortir
#   p6 net listen     ce qui ÉCOUTE (ports ouverts par des process locaux = surface d'attaque)
#   p6 net watch [s]  échantillonne pendant s secondes (défaut 30) et liste les destinations
#                     nouvelles/inattendues, par process (tcpdump → nécessite sudo)
#   p6 net lan        scan ARP du LAN : qui est sur le réseau (ping sweep + arp)
#
# Périmètre : ce script voit le trafic DU MAC. Pour voir tout le foyer il faut se placer
# sur le routeur (voir .docs/mac-maintenance.md § réseau).

set -u
mode="${1:-snap}"
title() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }

case "$mode" in
  snap)
    title "Connexions établies (TCP) — par process"
    # lsof : pid, process, socket → on agrège par (process, IP distante:port)
    lsof -nP -iTCP -sTCP:ESTABLISHED 2>/dev/null | awk 'NR>1 {
        split($9, a, "->"); dst=a[2]; if (dst=="") next; gsub(/\\x20/," ",$1);
        key=$1 "\t" dst; c[key]++ }
      END { for (k in c) print c[k] "\t" k }' | sort -t$'\t' -k2,2 -k1,1rn |
    awk -F'\t' 'BEGIN{printf "  %-4s %-22s %s\n","N","PROCESS","DESTINATION"} {printf "  %-4s %-22s %s\n",$1,substr($2,1,22),$3}'
    echo
    title "UDP actifs (QUIC/DNS/mDNS…)"
    lsof -nP -iUDP 2>/dev/null | awk 'NR>1 && $9 !~ /\*:\*/ {print $1}' | sort | uniq -c | sort -rn | head -15 | sed 's/^/  /'
    echo
    echo "  → p6 net live pour le débit en temps réel, p6 net listen pour les ports ouverts."
    ;;

  live)
    # nettop : débit in/out par process et par socket, mode delta pour voir ce qui bouge maintenant
    exec nettop -m tcp -d -c -s 2 -j bytes_in,bytes_out,rx_dupe,rtt_avg
    ;;

  listen)
    title "Ports en écoute (LISTEN) — ce que d'autres machines peuvent joindre"
    lsof -nP -iTCP -sTCP:LISTEN 2>/dev/null | awk 'NR>1 {printf "  %-22s %-6s %s\n", $1, $2, $9}' | sort -u
    echo
    echo "  *:port  = ouvert sur toutes les interfaces (LAN inclus)   127.0.0.1:port = local seulement"
    echo "  Pare-feu : $(/usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null | sed 's/.*is //')"
    echo "  Mode furtif : $(/usr/libexec/ApplicationFirewall/socketfilterfw --getstealthmode 2>/dev/null | sed 's/.*mode //')"
    ;;

  watch)
    secs="${2:-30}"
    iface=$(route -n get default 2>/dev/null | awk '/interface:/{print $2}')
    title "Capture $secs s sur $iface (sudo) — destinations sortantes, hors LAN"
    tmp=$(mktemp)
    sudo tcpdump -i "$iface" -nn -q -l "tcp[tcpflags] & tcp-syn != 0 and tcp[tcpflags] & tcp-ack == 0 and not dst net 192.168.0.0/16 and not dst net 10.0.0.0/8" \
      -G "$secs" -W 1 -w "$tmp" 2>/dev/null
    tcpdump -nn -q -r "$tmp" 2>/dev/null | awk '{split($5,a,":"); sub(/\.[0-9]+$/,"",a[1]); print a[1]}' | sort | uniq -c | sort -rn |
      while read -r n ip; do
        rev=$(host -W 1 "$ip" 2>/dev/null | awk '/pointer/{print $NF; exit}')
        printf '  %4s  %-16s %s\n' "$n" "$ip" "${rev:-?}"
      done
    rm -f "$tmp"
    echo
    echo "  (nouvelles connexions TCP sortantes seulement ; pour attribuer à un process : p6 net snap en parallèle)"
    ;;

  lan)
    title "Machines vues sur le LAN (ARP après ping sweep)"
    net=$(ipconfig getifaddr "$(route -n get default | awk '/interface:/{print $2}')" | cut -d. -f1-3)
    for i in $(seq 1 254); do ping -c1 -W 200 "$net.$i" >/dev/null 2>&1 & done; wait
    arp -an | grep -v incomplete | awk '{gsub(/[()]/,"",$2); printf "  %-16s %-18s %s\n", $2, $4, $6}' | sort -t. -k4 -n
    echo
    echo "  Nommer les MAC connues : .docs/infra-reseau.md (réservations DHCP)"
    ;;

  *) sed -n '2,15p' "$0"; exit 1;;
esac
