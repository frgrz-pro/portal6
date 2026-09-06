"""Validation n°2 du MUTE : protocole Android TV Remote v2 (angle 1 de .docs/tv-mute.md).

Pas besoin du mode développeur : appairage par code affiché sur la télé (une fois),
certificat stocké dans home/tv/.certs/ (ignoré par git).

Usage (venv portal6-home) :
    python home/tv/shield_remote.py <ip-shield>            # appaire si besoin, puis MUTE
    python home/tv/shield_remote.py <ip-shield> VOLUME_UP  # autre keycode
"""
from __future__ import annotations

import asyncio
import sys
from pathlib import Path

from androidtvremote2 import AndroidTVRemote, CannotConnect, InvalidAuth

HERE = Path(__file__).resolve().parent
CERTS = HERE / ".certs"


async def main(host: str, key: str) -> None:
    CERTS.mkdir(exist_ok=True)
    remote = AndroidTVRemote(
        client_name="portal6-remote",
        certfile=str(CERTS / "cert.pem"),
        keyfile=str(CERTS / "key.pem"),
        host=host,
    )
    if await remote.async_generate_cert_if_missing():
        print("Certificat client généré.")

    try:
        await remote.async_connect()
    except InvalidAuth:
        print("Pas encore appairé : un code s'affiche sur la télé.")
        await remote.async_start_pairing()
        code = input("Code affiché à l'écran : ").strip()
        await remote.async_finish_pairing(code)
        await remote.async_connect()
    except CannotConnect as e:
        sys.exit(f"Connexion impossible à {host}:6466 — Shield allumée ? IP correcte ? ({e})")

    remote.keep_reconnecting()
    print("Connecté :", remote.device_info)
    print(f"Envoi {key} — regarde si la barre TCL réagit.")
    remote.send_key_command(key)
    await asyncio.sleep(1)
    remote.disconnect()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    asyncio.run(main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "MUTE"))
