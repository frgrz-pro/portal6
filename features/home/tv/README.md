# home/tv — piloter la Shield (mute pendant les pubs)

Scripts de **validation** avant d'écrire le tab TV de l'app remote. Design et
plan complet : [`.docs/tv-mute.md`](../../../.docs/tv-mute.md).

| Script | Angle | Prérequis |
|---|---|---|
| `shield_mute_adb.ps1 -Ip <ip>` | ADB réseau (fallback, trivial) | Débogage réseau activé sur la Shield, popup d'autorisation à accepter à l'écran |
| `shield_remote.py <ip> [KEY]` | Android TV Remote v2 (la cible de l'app) | Rien côté Shield ; code d'appairage affiché la première fois |

Lancer le second depuis le venv :

```powershell
& "$env:USERPROFILE\.venvs\portal6-home\Scripts\python.exe" home\tv\shield_remote.py 192.168.x.y
```

Le test qui tranche : **la barre TCL se coupe-t-elle ?** Si oui la chaîne CEC
Shield → télé → barre fonctionne et le tab TV de l'app n'a qu'à parler Remote v2.
