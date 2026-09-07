"""Pont série <-> TCP pour le coordinateur Zigbee (Sonoff Dongle-P) sous Windows.

Docker Desktop ne passe pas l'USB aux conteneurs : ce script expose le port COM
du dongle sur un port TCP, et ZHA (dans le conteneur HA) s'y connecte via
`socket://host.docker.internal:6638` — exactement comme un coordinateur Ethernet.

Usage : python features/home/ha/zigbee_bridge.py [--port COM5] [--tcp-port 6638] [--log fichier]
Sans --port, le dongle est détecté par son VID:PID (CP2102N 10C4:EA60).
Un seul client à la fois (un coordinateur = un maître). Reconnexion automatique
si le dongle disparaît ou si HA se déconnecte.
"""
from __future__ import annotations

import argparse
import logging
import socket
import threading
import time

import serial
from serial.tools import list_ports

DONGLE_VID_PID = (0x10C4, 0xEA60)  # CP2102N du Sonoff Zigbee 3.0 USB Dongle Plus (P)
BAUD = 115200                      # Z-Stack sur CC2652P
log = logging.getLogger("zigbee_bridge")


def find_dongle() -> str | None:
    for p in list_ports.comports():
        if (p.vid, p.pid) == DONGLE_VID_PID:
            return p.device
    return None


def open_serial(port: str) -> serial.Serial:
    ser = serial.Serial()
    ser.port = port
    ser.baudrate = BAUD
    ser.timeout = 0.05
    ser.rtscts = False
    # DTR/RTS pilotent reset et bootloader sur le Dongle-P : les laisser bas
    # avant l'ouverture, sinon le CC2652P peut démarrer en mode bootloader.
    ser.dtr = False
    ser.rts = False
    ser.open()
    return ser


def pump(ser: serial.Serial, conn: socket.socket, stop: threading.Event) -> None:
    def ser_to_tcp() -> None:
        try:
            while not stop.is_set():
                data = ser.read(ser.in_waiting or 1)
                if data:
                    conn.sendall(data)
        except (serial.SerialException, OSError) as e:
            log.warning("série -> tcp interrompu : %s", e)
        finally:
            stop.set()

    def tcp_to_ser() -> None:
        try:
            while not stop.is_set():
                data = conn.recv(4096)
                if not data:
                    break
                ser.write(data)
        except (serial.SerialException, OSError) as e:
            log.warning("tcp -> série interrompu : %s", e)
        finally:
            stop.set()

    threads = [threading.Thread(target=ser_to_tcp, daemon=True),
               threading.Thread(target=tcp_to_ser, daemon=True)]
    for t in threads:
        t.start()
    stop.wait()
    conn.close()
    for t in threads:
        t.join(timeout=2)


def serve(port: str | None, bind: str, tcp_port: int) -> None:
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind((bind, tcp_port))
    srv.listen(1)
    log.info("écoute sur %s:%d", bind, tcp_port)

    while True:
        dev = port or find_dongle()
        if not dev:
            log.error("dongle introuvable (VID:PID %04x:%04x) — nouvel essai dans 10 s", *DONGLE_VID_PID)
            time.sleep(10)
            continue
        try:
            ser = open_serial(dev)
        except serial.SerialException as e:
            log.error("ouverture %s impossible : %s — nouvel essai dans 10 s", dev, e)
            time.sleep(10)
            continue
        log.info("dongle ouvert sur %s @ %d", dev, BAUD)

        try:
            while True:
                conn, addr = srv.accept()
                conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                log.info("client connecté : %s:%d", *addr)
                stop = threading.Event()
                pump(ser, conn, stop)
                log.info("client déconnecté")
                if not ser.is_open:
                    break
                # sonde : le dongle est-il toujours là ?
                try:
                    ser.in_waiting
                except (serial.SerialException, OSError):
                    break
        finally:
            try:
                ser.close()
            except Exception:
                pass
            log.warning("port série fermé, réouverture")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--port", help="port COM du dongle (défaut : détection par VID:PID)")
    ap.add_argument("--bind", default="0.0.0.0", help="adresse d'écoute (défaut 0.0.0.0)")
    ap.add_argument("--tcp-port", type=int, default=6638, help="port TCP (défaut 6638)")
    ap.add_argument("--log", help="fichier journal (sinon stderr) — utile sous pythonw/tâche planifiée")
    ap.add_argument("-v", "--verbose", action="store_true")
    args = ap.parse_args()
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO,
                        format="%(asctime)s %(levelname)s %(message)s",
                        filename=args.log, encoding="utf-8")
    serve(args.port, args.bind, args.tcp_port)


if __name__ == "__main__":
    main()
