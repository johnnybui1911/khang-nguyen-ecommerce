#!/usr/bin/env python3
"""Rebuild the four publishable CMS pages from their reference pages and the current kn.css.

CMS pages must be self-contained, so kn.css is inlined by inline-css.py; this script then swaps the
reference header/footer for the shared fragments and rewrites the hrefs.
Usage (repo root):
    python3 -I docs/design/build-pages.py           rebuild my-site/pages/*/content.html
    python3 -I docs/design/build-pages.py --check   exit 1 if a page differs from a fresh build
"""
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))

LINKS = ["home.html=/", "category.html=/products", "product.html=/vong-ngoc-demo"]
# (reference page, page folder, href rewrites, extra literal rewrites)
PAGES = [
    ("home.html", "home", ["home.html=/", "category.html=/products", "product.html=/products"],
     [('href="/products#kien-thuc"', 'href="/ngoc-hoa-dien-tan-cuong-thanh-hai"')]),
    ("product.html", "vong-ngoc-demo", LINKS, []),
    ("customer-service.html", "customer-service", ["home.html=/"], []),
    ("article.html", "ngoc-hoa-dien-tan-cuong-thanh-hai", ["home.html=/", "category.html=/products"], []),
]
# the reference product page fakes "add to cart" with an inline script; a CMS page must not ship it
FAKE_CART = re.compile(r"<script>\s*\(function\(\)\{\s*var btn=document\.getElementById\('swAddBtn'\).*?</script>\n?", re.S)


def build(ref, pairs, literals):
    with tempfile.TemporaryDirectory() as tmp:
        raw = os.path.join(tmp, "raw.html")
        subprocess.check_call([sys.executable, "-I", os.path.join(HERE, "inline-css.py"),
                               os.path.join(HERE, ref), raw] + pairs, cwd=ROOT, stdout=subprocess.DEVNULL)
        with open(raw, encoding="utf8") as fh:
            s = fh.read()
    s = re.sub(r'<header class="kn-nav">.*?</header>', '<ezy:block ezy:replace="~{ecommerce/header :: content}" />', s, flags=re.S)
    s = re.sub(r'<footer class="kn-colophon">.*?</footer>', '<ezy:block ezy:replace="~{ecommerce/footer :: content}" />', s, flags=re.S)
    s = FAKE_CART.sub("", s)
    s = s.replace("</body>", '<ezy:block ezy:replace="~{ecommerce/theme :: foot}" />\n</body>')
    s = re.sub(r"<!-- Reference page:.*?-->\n?", "", s)
    for old, new in literals:
        s = s.replace(old, new)
    return s


def main():
    check = "--check" in sys.argv[1:]
    stale = []
    for ref, folder, pairs, literals in PAGES:
        path = os.path.join(ROOT, "my-site", "pages", folder, "content.html")
        fresh = build(ref, pairs, literals)
        with open(path, encoding="utf8") as fh:
            current = fh.read()
        if current == fresh:
            continue
        if check:
            stale.append(folder)
        else:
            with open(path, "w", encoding="utf8") as fh:
                fh.write(fresh)
            print("wrote", os.path.relpath(path, ROOT))
    if stale:
        sys.exit("out of date, run build-pages.py: " + ", ".join(stale))


if __name__ == "__main__":
    main()
