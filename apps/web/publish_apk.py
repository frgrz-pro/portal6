"""Publie l'APK debug de ha-remote dans le portail : apps/web/dl/ (non versionné).

Produit :
  dl/ha-remote.apk   copie de apps/ha-remote/app/build/outputs/apk/debug/app-debug.apk
  dl/qr.svg          QR code de l'URL LAN de l'APK (à scanner avec le téléphone)
  dl/apk.js          window.PORTAL6_APK = { url, size_mb, built } pour la tuile de l'accueil

Usage (venv portal6-home, qui a segno) :
  & "$env:USERPROFILE\\.venvs\\portal6-home\\Scripts\\python.exe" apps\\web\\publish_apk.py

L'URL est celle de la tour sur le LAN (réservation DHCP 192.168.0.5), pas localhost :
c'est le téléphone qui la lit. Le portail doit être servi sur 8712 (`npm run web`).
"""
from __future__ import annotations

import datetime as dt
import json
import shutil
import sys
from pathlib import Path

import segno

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "apps" / "ha-remote" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
WEB = Path(__file__).resolve().parent
DL = WEB / "dl"
WWW = ROOT / "features" / "home" / "ha" / "config" / "www"
# Le QR pointe sur la page d'installation, qui détecte l'appareil : Android →
# téléchargement de l'APK ; iPhone → message (pas d'app iOS à ce jour). Voir install.html.
URL = "http://192.168.0.5:8123/local/install.html"
APK_URL = "http://192.168.0.5:8123/local/ha-remote.apk"


def main() -> None:
    if not SRC.exists():
        sys.exit(f"APK absent : {SRC}\n-> cd apps/ha-remote ; .\\gradlew.bat assembleDebug")
    DL.mkdir(exist_ok=True)
    fresh = not WWW.exists()
    WWW.mkdir(exist_ok=True)
    shutil.copy2(SRC, WWW / "ha-remote.apk")
    shutil.copy2(WEB / "install.html", WWW / "install.html")
    segno.make(URL, error="m").save(str(DL / "qr.svg"), scale=6, border=2, dark="#1a1d23", light=None)
    built = dt.datetime.fromtimestamp(SRC.stat().st_mtime).strftime("%Y-%m-%d %H:%M")
    info = {"url": URL, "apk_url": APK_URL, "size_mb": round(SRC.stat().st_size / 1e6, 1), "built": built}
    js = "window.PORTAL6_APK = " + json.dumps(info, ensure_ascii=False) + ";\n"
    # Un build iOS, s'il existe un jour : www/ios/manifest.plist (HTTPS obligatoire pour itms-services).
    ios = WWW / "ios" / "manifest.plist"
    if ios.exists():
        js += "window.PORTAL6_IOS = " + json.dumps({
            "manifest_url": "https://192.168.0.5:8123/local/ios/manifest.plist",
            "built": dt.datetime.fromtimestamp(ios.stat().st_mtime).strftime("%Y-%m-%d %H:%M"),
        }) + ";\n"
    for target in (DL, WWW):
        (target / "apk.js").write_text(js, encoding="utf-8")
    print(f"publie : {WWW / 'ha-remote.apk'} ({info['size_mb']} Mo, build {built})\nQR : {URL}")
    if fresh:
        print("config/www vient d'etre cree : redemarrer HA (docker compose restart) pour que /local soit servi")


if __name__ == "__main__":
    main()
