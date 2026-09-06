# TV — couper le son (phase 2)

Use case principal : **mute instantané pendant les pubs**. Les télécommandes
physiques ont des gros boutons Netflix qui rendent fou, impossible de skip les
pubs, et pas de mute accessible. L'app remote doit offrir un gros bouton MUTE
qui marche à tous les coups.

## La chaîne audio (3 angles possibles)

Trois devices peuvent théoriquement couper le son :

1. **La barre de son TCL** — c'est elle qui sort le son au final.
2. **La télé** — relaie vers la barre (probablement HDMI ARC).
3. **La Shield TV Pro** — la source ; Android TV relaie volume/mute en **HDMI-CEC**
   vers la télé/barre.

## Questions ouvertes

- [ ] Modèle exact de la télé et de la barre TCL ; branchement (barre en HDMI ARC
  sur la télé ? optique ?).
- [ ] **Test clé** : sur la télécommande Shield, le bouton volume/mute agit-il
  réellement sur la barre ? Si oui → l'angle Shield suffit, on ignore les 2 autres.
- [ ] La Shield a-t-elle une IP fixe / réservation DHCP ? (cf. [infra-reseau.md](infra-reseau.md))
- [ ] Le réseau "Options développeur → Débogage réseau" est-il activable sur la
  Shield ? (nécessaire pour l'angle ADB)

## Angles techniques (du plus prometteur au moins)

### 1. Shield via Android TV Remote protocol v2 — recommandé
Le protocole officiel des apps télécommande Google TV : TCP **6466** (commandes) /
**6467** (pairing), TLS avec pairing par code affiché à l'écran (une fois).
Ensuite on envoie des keycodes : `KEYCODE_VOLUME_MUTE` (164), `KEYCODE_VOLUME_UP/DOWN`,
d-pad, etc. La Shield relaie en CEC vers la barre.
- Implémentations de référence à étudier : projets « androidtv-remote » (Node/Python)
  et lib Kotlin éventuelle — le protocole est documenté par rétro-ingénierie, stable.
- Avantage : pas de mode développeur, réveille la Shield (wake-on-lan intégré au flux).

### 2. Shield via ADB réseau — le fallback simple
`adb connect <ip>:5555` puis `input keyevent 164`. Trivial à prototyper (même
depuis le Mac pour valider la chaîne CEC avant d'écrire l'app), mais : mode
développeur requis, popup d'autorisation ADB, la connexion saute après reboot,
et libadb en Kotlin/Android c'est lourd. Bon pour **valider le use case**, pas
pour la v finale.

### 3. Télé ou barre en direct — seulement si 1 et 2 échouent
Dépend totalement des modèles (API TCL/Roku ? IR ?). On n'investigue que si le
mute via Shield ne pilote pas la barre.

## Plan de validation (avant tout code)

1. Activer le débogage réseau sur la Shield, depuis le Mac :
   `adb connect <ip-shield>:5555 && adb shell input keyevent 164`
2. Si le son de la **barre** se coupe → la chaîne CEC marche, on implémente
   l'angle 1 (protocole Remote v2) dans le tab TV de l'app.
3. Sinon → creuser le branchement ARC/CEC (réglages télé) avant de se rabattre
   sur l'angle 3.

## Tab TV de l'app (spec cible)

- Gros bouton **MUTE** central (le use case n°1, accessible pouce).
- Volume +/− , et éventuellement d-pad + back/home ensuite.
- Rien d'autre en v1 du tab — surtout pas de bouton Netflix.

## Journal

### 2026-08-30
Création du doc. Chaîne audio décrite, 3 angles identifiés, stratégie : valider
CEC via ADB depuis le Mac, puis implémenter Android TV Remote protocol v2.

### 2026-09-06
Outils de validation prêts dans `features/home/tv/` : `shield_mute_adb.ps1` (angle 2, ADB
réseau) et `shield_remote.py` (angle 1, lib Python `androidtvremote2` — appairage
par code + `send_key_command("MUTE")`). Reste à connaître l'IP de la Shield et
faire le test qui tranche : la barre TCL se coupe-t-elle ?
