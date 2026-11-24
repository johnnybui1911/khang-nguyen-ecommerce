#!/usr/bin/env python3
"""Copy the mockup stylesheet kn.css into the theme stylesheet (the one real source of CSS).

Target (repo root):
  ecommerce-theme/.../static/css/main.css   (theme stylesheet, loads fonts with @import)
Usage: python3 -I docs/design/sync-theme.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
FONTS = ("https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600"
         "&family=Cormorant+Garamond:ital,wght@0,400;0,500;1,400&display=swap")
MAIN = os.path.join(ROOT, "ecommerce-theme/ecommerce-theme-theme/src/main/resources/static/css/main.css")


def write(path, text):
    with open(path, "w", encoding="utf8") as fh:
        fh.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    with open(os.path.join(HERE, "kn.css"), encoding="utf8") as fh:
        css = fh.read().strip()
    write(MAIN, '@import url("%s");\n%s\n' % (FONTS, css))


if __name__ == "__main__":
    main()
