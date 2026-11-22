#!/usr/bin/env python3
"""Import the Khang Nguyen catalog (categories, images, products) through the admin API.

Idempotent: categories match by name (= CSV slug), products by productCode (= CSV sku),
images by exact original file name. Running it twice creates nothing new.

Source data (read only): data/seed/khang-nguyen/{categories,products}.csv and images/.
Prices are VND only (column gia_vnd); the column gia_can_xac_nhan stays in the data and is
never sent to the site. Each product gets the stock in column ton_kho.

Environment:
  ADMIN_BASE        admin origin, e.g. http://127.0.0.1:9090   (required)
  ADMIN_USERNAME    default "admin"
  ADMIN_PASSWORD    required; read from the environment only, never logged
  SHOP_ID           default 1          WAREHOUSE_ID   default 1
  VND_CURRENCY_ID   default 6          SGD_CURRENCY_ID default 8 (only for the create call)

Usage:
  ADMIN_BASE=http://127.0.0.1:9090 ADMIN_PASSWORD=... python3 -I ecommerce-ext/scripts/import-catalog.py [--dry-run]
"""
import csv
import http.cookiejar
import json
import mimetypes
import os
import re
import sys
import uuid
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "data" / "seed" / "khang-nguyen"
DRY = "--dry-run" in sys.argv

BASE = os.environ.get("ADMIN_BASE", "").rstrip("/")
USER = os.environ.get("ADMIN_USERNAME", "admin")
PASSWORD = os.environ.get("ADMIN_PASSWORD", "")
SHOP_ID = int(os.environ.get("SHOP_ID", "1"))
WAREHOUSE_ID = int(os.environ.get("WAREHOUSE_ID", "1"))
VND = int(os.environ.get("VND_CURRENCY_ID", "6"))
SGD = int(os.environ.get("SGD_CURRENCY_ID", "8"))

ECOM = BASE + "/ecommerce/api/v1"
CORE = BASE + "/api/v1"

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
stats = {}


def count(key):
    stats[key] = stats.get(key, 0) + 1


def log(msg):
    print(msg, flush=True)


def call(method, url, body=None, headers=None, raw=None):
    data = raw
    hdrs = dict(headers or {})
    if body is not None:
        data = json.dumps(body).encode()
        hdrs["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
    try:
        with opener.open(req, timeout=60) as resp:
            text = resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as err:
        text = err.read().decode("utf-8", "replace")
        raise RuntimeError("%s %s -> HTTP %d %s" % (method, url, err.code, text[:300]))
    if not text.strip():
        return None
    try:
        return json.loads(text)
    except ValueError:
        return text


def login():
    form = urllib.parse.urlencode({"username": USER, "password": PASSWORD}).encode()
    call("POST", BASE + "/login", raw=form,
         headers={"Content-Type": "application/x-www-form-urlencoded"})
    try:
        call("GET", ECOM + "/products?limit=1")
    except RuntimeError:
        sys.exit("login failed for %s on %s" % (USER, BASE))


def read_csv(name):
    with open(SEED / name, newline="", encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))


def money(value):
    return None if value in (None, "") else float(value)


# ---------------------------------------------------------------- categories
def sync_categories(rows):
    existing = {}
    for item in call("GET", ECOM + "/product-categories?limit=200")["items"]:
        existing[item["name"]] = item
    ids = {}
    for row in sorted(rows, key=lambda r: int(r["sort_order"])):
        slug = row["slug"]
        item = existing.get(slug)
        if item:
            ids[slug] = item["id"]
            count("categories_existing")
        elif DRY:
            ids[slug] = -1
            count("categories_would_create")
            log("[dry-run] create category " + slug)
        else:
            created = call("POST", ECOM + "/product-categories/add", {
                "shopId": SHOP_ID, "name": slug, "displayName": row["name"],
                "displayOrder": int(row["sort_order"]), "type": "CATEGORY",
                "status": "SHOW_ON_MENU"})
            ids[slug] = created["createdId"]
            count("categories_created")
            log("created category %s id=%s" % (slug, ids[slug]))
    return ids


# -------------------------------------------------------------------- media
def find_media(filename):
    result = call("GET", CORE + "/media/list?limit=100&keyword=" + urllib.parse.quote(filename))
    # The server prefixes uploaded names with a timestamp, e.g. 20261121062046486Z-1-name.jpg.
    prefixed = re.compile(r"^(\d{17}Z-\d+-)?" + re.escape(filename) + "$")
    for item in sorted(result["items"], key=lambda m: m["id"]):
        if prefixed.match(item["originalName"]):
            return item["id"]
    return None


def upload_media(path):
    boundary = uuid.uuid4().hex
    ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    payload = (
        ("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n"
         "Content-Type: %s\r\n\r\n" % (boundary, path.name, ctype)).encode()
        + path.read_bytes() + ("\r\n--%s--\r\n" % boundary).encode())
    result = call("POST", CORE + "/media/add", raw=payload,
                  headers={"Content-Type": "multipart/form-data; boundary=" + boundary})
    return result["id"]


def media_id_for(filename):
    found = find_media(filename)
    if found:
        count("images_existing")
        return found
    path = SEED / "images" / filename
    if not path.is_file():
        raise RuntimeError("image file missing: " + filename)
    if DRY:
        count("images_would_upload")
        log("[dry-run] upload " + filename)
        return -1
    new_id = upload_media(path)
    count("images_uploaded")
    log("uploaded %s id=%s" % (filename, new_id))
    return new_id


# ----------------------------------------------------------------- products
def find_product(code):
    result = call("GET", ECOM + "/products?limit=20&keyword=" + urllib.parse.quote(code))
    for item in result["items"]:
        if item["productCode"] == code:
            return item
    return None


def product_body(item, currency_id, price, original, icon_id, amount, sold):
    return {
        "productName": item["productName"], "productCode": item["productCode"],
        "shopId": item["shop"]["id"], "displayOrder": item.get("displayOrder", 0),
        "amount": amount, "soldAmount": sold, "importPrice": 0,
        "originalPrice": original, "price": price, "currencyId": currency_id,
        "productType": item["productType"], "warehouseIds": [WAREHOUSE_ID],
        "deliveryRequired": True, "status": item["status"], "iconImageId": icon_id}


def has_category(cat_id, product_id):
    result = call("GET", "%s/product-categories/%d/products?limit=200" % (ECOM, cat_id))
    return any(p["id"] == product_id for p in result["items"])


def link_category(cat_id, product_id):
    if cat_id < 0 or has_category(cat_id, product_id):
        return
    call("POST", "%s/product-categories/%d/products/add?productIds=%d" % (ECOM, cat_id, product_id), {})
    count("category_links_created")


def sync_product(row, cats, cat_by_name):
    sku = row["sku"]
    item = find_product(sku)
    icon = media_id_for(Path(row["anh"]).name) if (item is None or not item.get("iconImage")) else 0
    price = money(row["gia_vnd"])
    stock = int(row["ton_kho"] or 1)

    if item is None:
        if DRY:
            count("products_would_create")
            log("[dry-run] create product " + sku)
            return
        skeleton = {"productName": row["ten"], "productCode": sku, "shop": {"id": SHOP_ID},
                    "displayOrder": 0, "productType": "SINGLE", "status": "PUBLISHED"}
        added = call("POST", ECOM + "/products/add", product_body(
            skeleton, SGD, round(price / 20000, 2), None, icon, stock, 0))
        pid = added.get("addedId") or added.get("id")
        call("PUT", "%s/products/%d" % (ECOM, pid), product_body(
            skeleton, VND, price, None, icon, stock, 0))
        call("POST", ECOM + "/warehouse-products/save",
             {"warehouseId": WAREHOUSE_ID, "productId": pid, "remainingAmount": stock})
        count("products_created")
        log("created product %s id=%s" % (sku, pid))
    else:
        pid = item["id"]
        count("products_existing")
        needs_vnd = str(int(float(item["price"]))) != str(int(price))
        if (icon > 0 or needs_vnd) and not DRY:
            fresh = find_product(sku)
            call("PUT", "%s/products/%d" % (ECOM, pid), product_body(
                fresh, VND, price if needs_vnd else money(fresh["price"]),
                money(fresh.get("originalPrice")),
                icon if icon > 0 else (fresh.get("iconImage") or {}).get("id", 0),
                int(fresh["amount"]), int(fresh["soldAmount"])))
            count("products_updated")
            log("updated %s (image=%s vnd=%s)" % (sku, icon > 0, needs_vnd))

    if not DRY:
        link_category(cats[cat_by_name[row["danh_muc"]]], pid)


def main():
    if not BASE or not PASSWORD:
        sys.exit("set ADMIN_BASE and ADMIN_PASSWORD")
    log("target %s dry_run=%s" % (BASE, DRY))
    login()
    cat_rows = read_csv("categories.csv")
    cats = sync_categories(cat_rows)
    cat_by_name = {r["name"]: r["slug"] for r in cat_rows}
    for row in read_csv("products.csv"):
        sync_product(row, cats, cat_by_name)
    log("summary " + json.dumps(stats, sort_keys=True))


if __name__ == "__main__":
    main()
