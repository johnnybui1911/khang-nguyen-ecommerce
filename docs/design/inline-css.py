#!/usr/bin/env python3
"""Build a publishable EzyPlatform page from a reference page.

EzyPlatform pages must be self-contained, so kn.css is inlined into the page.
Usage (repo root):
    python3 -I docs/design/inline-css.py <reference.html> <out content.html> [old=new ...]
Each old=new pair rewrites an href, for example: home.html=/home category.html=/necklaces
The reference page must contain the markers <!--kn:css--> ... <!--/kn:css-->.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    src, dst, pairs = sys.argv[1], sys.argv[2], sys.argv[3:]
    with open(src, encoding="utf8") as fh:
        page = fh.read()
    with open(os.path.join(HERE, "kn.css"), encoding="utf8") as fh:
        css = fh.read().strip()
    marker = re.compile(r"<!--kn:css-->.*?<!--/kn:css-->", re.S)
    if not marker.search(page):
        sys.exit("missing <!--kn:css--> markers in " + src)
    page = marker.sub(lambda _: "<style>\n" + css + "\n</style>", page)
    for pair in pairs:
        old, new = pair.split("=", 1)
        page = page.replace('href="%s' % old, 'href="%s' % new)
    with open(dst, "w", encoding="utf8") as fh:
        fh.write(page)
    print("wrote", dst)


if __name__ == "__main__":
    main()
