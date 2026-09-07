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
DL = Path(__file__).resolve().parent / "dl"
URL = "http://192.168.0.5:8712/dl/ha-remote.apk"


def main() -> None:
    if not SRC.exists():
        sys.exit(f"APK absent : {SRC}\n-> cd apps/ha-remote ; .\\gradlew.bat assembleDebug")
    DL.mkdir(exist_ok=True)
    shutil.copy2(SRC, DL / "ha-remote.apk")
    segno.make(URL, error="m").save(str(DL / "qr.svg"), scale=6, border=2, dark="#1a1d23", light=None)
    built = dt.datetime.fromtimestamp(SRC.stat().st_mtime).strftime("%Y-%m-%d %H:%M")
    info = {"url": URL, "size_mb": round(SRC.stat().st_size / 1e6, 1), "built": built}
    (DL / "apk.js").write_text("window.PORTAL6_APK = " + json.dumps(info, ensure_ascii=False) + ";\n", encoding="utf-8")
    print(f"publie : {DL / 'ha-remote.apk'} ({info['size_mb']} Mo, build {built})\nQR : {URL}")


if __name__ == "__main__":
    main()
