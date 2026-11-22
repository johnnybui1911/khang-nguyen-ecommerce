#!/usr/bin/env python3
"""Build static/data/product-attributes.json from the public product API (GET only).

The ecommerce API has no colour or material for these products, so both are read from the
segments of the product name after the first comma ("Swan pendant, Pink, Rose gold-tone plated").
The part before the first comma is the product family; products of one family are the colour
siblings shown on the product page.

Usage: build-product-attributes.py [BASE_URL] [OUTPUT]
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080").rstrip("/")
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else Path(__file__).resolve().parent.parent
           / "ecommerce-theme-theme/src/main/resources/static/data/product-attributes.json")

COLORS = {
    "white": "White", "blue": "Blue", "pink": "Pink", "black": "Black", "green": "Green",
    "yellow": "Yellow", "multicolored": "Multicolored", "multi-colored": "Multicolored",
    "gold tone": "Gold tone", "rose gold tone": "Rose gold tone", "silver tone": "Silver tone",
}
MATERIALS = [
    (re.compile(r"rhodium plated", re.I), "Rhodium plated"),
    (re.compile(r"rose gold-tone (plated|finish)", re.I), "Rose gold-tone"),
    (re.compile(r"(?<!rose )gold-tone (plated|finish)", re.I), "Gold-tone"),
    (re.compile(r"crystal pearl", re.I), "Crystal pearl"),
    (re.compile(r"stainless steel", re.I), "Stainless steel"),
]


def get(path):
    req = urllib.request.Request(BASE + path, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as res:
        return json.load(res)


def all_products():
    items, token = [], ""
    while True:
        page = get("/api/v1/products?productTypes=SINGLE&limit=100&nextPageToken=" + token)
        items += page.get("items", [])
        if not (page.get("continuation") or {}).get("hasNext"):
            return items
        token = page["pageToken"]["next"]


def attributes(name):
    parts = [p.strip() for p in name.split(",")]
    colors = [COLORS[p.lower()] for p in parts[1:] if p.lower() in COLORS]
    materials = [label for rx, label in MATERIALS if rx.search(", ".join(parts[1:]))]
    return {"family": parts[0], "colors": colors, "materials": materials}


def main():
    result = {p["code"]: attributes(p["name"]) for p in sorted(all_products(), key=lambda p: p["code"])}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, indent=1, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
    colored = sum(1 for v in result.values() if v["colors"])
    materialed = sum(1 for v in result.values() if v["materials"])
    print(f"{len(result)} products, {colored} with colour, {materialed} with material -> {OUT}")


if __name__ == "__main__":
    main()
