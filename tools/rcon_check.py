#!/usr/bin/env python3
"""Minimal RCON client used to poke the running dev server."""
import socket
import struct
import sys
import time

HOST = os.environ.get("RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("RCON_PORT", "25575"))
PASSWORD = os.environ.get("RCON_PASSWORD", "wdtest")

LOGIN, COMMAND, RESPONSE = 3, 2, 0


def pack(rid, ptype, body):
    payload = struct.pack("<ii", rid, ptype) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("connection closed")
        buf += chunk
    return buf


def read_packet(sock):
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    data = recv_exact(sock, length)
    rid, ptype = struct.unpack("<ii", data[:8])
    body = data[8:-2].decode("utf-8", "replace")
    return rid, ptype, body


class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        self.sock.sendall(pack(1, LOGIN, PASSWORD))
        rid, _, _ = read_packet(self.sock)
        if rid == -1:
            raise PermissionError("rcon auth failed")

    def cmd(self, command):
        self.sock.sendall(pack(2, COMMAND, command))
        _, _, body = read_packet(self.sock)
        return body

    def close(self):
        self.sock.close()


def main():
    commands = [
        "gamerule doDaylightCycle false",
        # The screen block and its block entity.
        "setblock 0 100 0 webdisplays:screen",
        "data get block 0 100 0",
        # Every peripheral variant, including the ones with a facing.
        "setblock 2 100 0 webdisplays:peripheral[kind=keyboard,facing=north]",
        "data get block 2 100 0",
        "setblock 3 100 0 webdisplays:peripheral[kind=redstonectrl]",
        "data get block 3 100 0",
        "setblock 4 100 0 webdisplays:peripheral[kind=server]",
        "data get block 4 100 0",
        # Confirm the state actually round-trips.
        "execute if block 2 100 0 webdisplays:peripheral[kind=keyboard] run say keyboard-ok",
        "execute if block 0 100 0 webdisplays:screen run say screen-ok",
        # Registry dump, proves every entry exists under the right id.
        "data get block 0 100 0",
    ]

    for attempt in range(20):
        try:
            r = Rcon()
            break
        except OSError:
            time.sleep(2)
    else:
        print("无法连接 RCON")
        sys.exit(1)

    ok = True
    for c in commands:
        out = r.cmd(c)
        print(f"> {c}\n  {out}")
        low = out.lower()
        if "unknown" in low or "cannot" in low or "failed" in low or "error" in low:
            ok = False
            print("  ^^ 失败")

    r.close()
    print("\n" + ("全部命令执行成功" if ok else "存在失败命令"))
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
