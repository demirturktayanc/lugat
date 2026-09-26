#!/usr/bin/env python3
"""DM Serif Display (SIL OFL) yazı tipini app/src/main/assets/fonts altına indirir.
İndirme başarısız olursa uygulama sistem serif fontunu kullanır; bu yüzden hata derlemeyi durdurmaz."""
import os, sys, urllib.request

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
DST = os.path.join(ROOT, "app", "src", "main", "assets", "fonts")
BASE = "https://github.com/google/fonts/raw/main/ofl/dmserifdisplay/"
FILES = ["DMSerifDisplay-Regular.ttf", "DMSerifDisplay-Italic.ttf"]

os.makedirs(DST, exist_ok=True)
for name in FILES:
    try:
        req = urllib.request.Request(BASE + name, headers={"User-Agent": "lugat-font-fetcher"})
        data = urllib.request.urlopen(req, timeout=60).read()
        if data[:4] not in (b"\x00\x01\x00\x00", b"OTTO", b"true"):
            raise ValueError("geçerli bir TTF değil")
        with open(os.path.join(DST, name), "wb") as f:
            f.write(data)
        print(f"✓ {name} ({len(data) // 1024} KB)")
    except Exception as e:  # noqa: BLE001
        print(f"! {name} indirilemedi ({e}); sistem serif fontu kullanılacak", file=sys.stderr)
