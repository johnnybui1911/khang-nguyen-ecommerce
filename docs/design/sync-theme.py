#!/usr/bin/env python3
"""Copy kn.css into the two places that serve it, so they never drift.

Targets (repo root):
  ecommerce-theme/.../static/css/main.css            (theme stylesheet, loads fonts with @import)
  my-site/page-fragments/ecommerce/theme/head.html   (shared <head> fragment: meta, font links, <style>)
Usage: python3 -I docs/design/sync-theme.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
FONTS = ("https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600"
         "&family=Cormorant+Garamond:ital,wght@0,400;0,500;1,400&display=swap")
MAIN = os.path.join(ROOT, "ecommerce-theme/ecommerce-theme-theme/src/main/resources/static/css/main.css")
HEAD = os.path.join(ROOT, "my-site/page-fragments/ecommerce/theme/head.html")


def write(path, text):
    with open(path, "w", encoding="utf8") as fh:
        fh.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    with open(os.path.join(HERE, "kn.css"), encoding="utf8") as fh:
        css = fh.read().strip()
    write(MAIN, '@import url("%s");\n%s\n' % (FONTS, css))
    write(HEAD, (
        '<meta charset="utf-8"/>\n'
        '<meta name="viewport" content="width=device-width, initial-scale=1"/>\n'
        '<link rel="preconnect" href="https://fonts.googleapis.com"/>\n'
        '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>\n'
        '<link href="%s" rel="stylesheet"/>\n'
        '<style>\n%s\n</style>\n' % (FONTS.replace("&", "&amp;"), css)))


if __name__ == "__main__":
    main()
