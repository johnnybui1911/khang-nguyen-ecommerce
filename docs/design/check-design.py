#!/usr/bin/env python3
"""Design spec checker for the Khang Nguyen - Ngoc Bao storefront.

Usage (run from the repo root):
    python3 -I docs/design/check-design.py                 # checks docs/design reference files
    python3 -I docs/design/check-design.py <file-or-dir>…  # checks HTML/CSS files you changed

Exit code 0 = no errors, 1 = at least one error. Warnings never fail the run.
Source of truth for allowed colors is the :root block of docs/design/kn.css.
Spec: docs/design-brief-khang-nguyen-ngoc-bao.md
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
KN_CSS = os.path.join(HERE, "kn.css")

FORBIDDEN_FONTS = ["bodoni", "helvetica", "georgia", "inter", "roboto", "poppins",
                   "montserrat", "open sans", "lato", "system-ui", "segoe", "sf pro",
                   "playfair", "noto serif"]
BANNED_PHRASES = ["siêu hot", "giá sốc", "đột phá", "cam kết 100%", "hàng thật 100%",
                  "100% chính hãng", "flash sale", "hot deal", "giảm sốc"]
ALLOWED_RADII = {"0", "6px", "10px", "50%", "9999px", "24px"}
LEGACY = re.compile(r"(--sw-|\bsw-[a-z]|\bswp-[a-z]|\bswd-[a-z])")

HEX = re.compile(r"#[0-9a-fA-F]{3,8}\b")
RGB = re.compile(r"rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)")


def read(path):
    with open(path, encoding="utf8") as fh:
        return fh.read()


def root_blocks(css):
    """Return list of (start, end, body) for every :root{...} block."""
    out = []
    for m in re.finditer(r":root\s*\{", css):
        depth, i = 1, m.end()
        while i < len(css) and depth:
            depth += {"{": 1, "}": -1}.get(css[i], 0)
            i += 1
        out.append((m.start(), i, css[m.end():i - 1]))
    return out


def hex_to_rgb(h):
    h = h.lstrip("#")
    if len(h) in (3, 4):
        h = "".join(c * 2 for c in h[:3])
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def allowed_tokens():
    css = read(KN_CSS)
    hexes, props = set(), set()
    for _, _, body in root_blocks(css):
        for name, val in re.findall(r"(--kn-[a-z0-9-]+)\s*:\s*([^;]+);", body):
            props.add(name)
            for h in HEX.findall(val):
                hexes.add(h.lower())
    rgbs = {hex_to_rgb(h) for h in hexes} | {(255, 255, 255), (0, 0, 0)}
    return hexes, rgbs, props


def css_regions(path, text):
    """Yield (css_text, line_offset) chunks from a .css file or from <style> blocks."""
    if path.endswith(".css"):
        yield text, 0
        return
    for m in re.finditer(r"<style[^>]*>(.*?)</style>", text, re.S | re.I):
        yield m.group(1), text[:m.start(1)].count("\n")


def strip_comments(css):
    return re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), css, flags=re.S)


def check_file(path, hexes, rgbs, props, is_kn_css):
    errors, warnings = [], []
    text = read(path)
    is_html = path.endswith((".html", ".htm"))
    in_site = os.sep + "my-site" + os.sep in os.path.abspath(path)

    def err(msg, line=None):
        errors.append((line, msg))

    def warn(msg, line=None):
        warnings.append((line, msg))

    # legacy tokens, classes and the old brand
    for i, line in enumerate(text.splitlines(), 1):
        if LEGACY.search(line):
            err("legacy sw-/swp-/swd- name found; use kn- tokens and classes", i)

    # CSS rules
    for css, off in css_regions(path, text):
        css = strip_comments(css)
        roots = root_blocks(css)

        def in_root(pos):
            return any(s <= pos < e for s, e, _ in roots)

        for m in HEX.finditer(css):
            line = off + css[:m.start()].count("\n") + 1
            h = m.group(0).lower()
            if in_root(m.start()):
                if h not in hexes:
                    err("hex %s is not a kn.css token color; do not invent colors" % h, line)
            else:
                err("hex %s outside :root; use var(--kn-*)" % h, line)
        for m in RGB.finditer(css):
            trip = tuple(int(x) for x in m.groups())
            if trip not in rgbs:
                line = off + css[:m.start()].count("\n") + 1
                err("rgb%s is not a token color, white or black" % (trip,), line)
        for m in re.finditer(r"hsla?\(", css):
            err("hsl() colors are not allowed; use var(--kn-*)", off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"box-shadow\s*:\s*([^;}]+)", css):
            if m.group(1).strip() != "none":
                err("box-shadow is not allowed (depth comes from imagery and rims)", off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"border-radius\s*:\s*([^;}]+)", css):
            for tok in m.group(1).split():
                tok = tok.strip().rstrip("!important")
                if tok.startswith("var(") or tok in ALLOWED_RADII or tok == "inherit":
                    continue
                err("border-radius %s not in spec (0, 6px, 10px, 24px, 50%%, 9999px)" % tok, off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"font(?:-family)?\s*:\s*([^;}]+)", css):
            val = m.group(1).lower()
            for bad in FORBIDDEN_FONTS:
                if re.search(r"(?<![a-z])%s(?![a-z])" % re.escape(bad), val):
                    err("font '%s' is not allowed (only Cormorant Garamond and Be Vietnam Pro, plus Arial for actions)" % bad, off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"outline\s*:\s*(none|0)\b", css):
            err("outline removed; keep visible :focus-visible styling", off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"(?<![\w-])(--[a-z0-9-]+)\s*:", css):
            if not m.group(1).startswith("--kn-"):
                err("custom property %s must use the --kn- prefix" % m.group(1), off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"var\((--[a-z0-9-]+)", css):
            name = m.group(1)
            if name.startswith("--kn-") and name not in props:
                err("%s is not defined in kn.css :root" % name, off + css[:m.start()].count("\n") + 1)
        for m in re.finditer(r"background(?:-color)?\s*:\s*var\(--kn-gold(?:-light|-ink)?\)", css):
            warn("gold used as a background; allowed only for the cart badge and tags", off + css[:m.start()].count("\n") + 1)

    if is_html:
        low = text.lower()
        if "<html" in low:
            if not re.search(r"<html[^>]*\blang=\"vi\"", low):
                err('missing <html lang="vi">')
            if 'name="viewport"' not in low:
                err("missing viewport meta")
        for m in re.finditer(r"<img\b[^>]*>", text, re.I):
            if not re.search(r"\balt=\"[^\"]+\"", m.group(0)):
                err("<img> without a descriptive alt", text[:m.start()].count("\n") + 1)
        body_text = re.sub(r"<(script|style)\b.*?</\1>", " ", text, flags=re.S | re.I)
        body_text = re.sub(r"<!--.*?-->", " ", body_text, flags=re.S)
        plain = re.sub(r"<[^>]+>", " ", body_text).lower()
        for phrase in BANNED_PHRASES:
            if phrase in plain:
                err("banned phrase in copy: '%s'" % phrase)
        for m in re.finditer(r"\sstyle=\"", text):
            msg = "inline style attribute; use a kn- class (add it to kn.css if missing)"
            line = text[:m.start()].count("\n") + 1
            (err if in_site else warn)(msg, line)
        for m in re.finditer(r"(stroke|fill)=\"#[0-9a-fA-F]{3,8}\"", text):
            err("hex color in SVG attribute; use currentColor and set color with a token", text[:m.start()].count("\n") + 1)
    return errors, warnings


def collect(args):
    if not args:
        args = [HERE]
    files = []
    for a in args:
        if os.path.isdir(a):
            for dp, dn, fn in os.walk(a):
                dn[:] = [d for d in dn if d not in ("node_modules", "target", ".git", "screenshots")]
                files += [os.path.join(dp, f) for f in fn if f.endswith((".html", ".htm", ".css"))]
        elif os.path.isfile(a):
            files.append(a)
        else:
            print("not found: %s" % a)
            sys.exit(2)
    return sorted(files)


def main():
    hexes, rgbs, props = allowed_tokens()
    total_err = total_warn = 0
    for f in collect(sys.argv[1:]):
        errors, warnings = check_file(f, hexes, rgbs, props, os.path.abspath(f) == KN_CSS)
        rel = os.path.relpath(f)
        for line, msg in errors:
            print("ERROR   %s%s  %s" % (rel, ":%d" % line if line else "", msg))
        for line, msg in warnings:
            print("warning %s%s  %s" % (rel, ":%d" % line if line else "", msg))
        total_err += len(errors)
        total_warn += len(warnings)
    print("\n%d error(s), %d warning(s)" % (total_err, total_warn))
    sys.exit(1 if total_err else 0)


if __name__ == "__main__":
    main()
