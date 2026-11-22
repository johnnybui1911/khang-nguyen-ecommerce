---
title: "Ecommerce clone phase 1: baseline, backup, reset"
date: 2026-11-20
summary: "Backed up my-site pages and fragments, archived regenerated v0 files, my-site clean for rebuild; server still holds v0."
---

# Ecommerce clone phase 1: baseline, backup, reset

## What happened
A hand-build session regenerated pages/ecommerce-* and page-fragments/ecommerce at 02:12:08 after the first archive move. Phase 1 ran via ak-cook then ak-test: backup with sha256 (36 files, checksum OK), read-only server baseline, moved the 32 ecommerce files to .artifacts/archive/ecommerce-clone-v0/my-site-reappeared-0215, re-checked after 70s: nothing regrew.

## Decision
No apply or restore with --yes. The server still serves 4 v0 pages and 4 fragments (posts 1-8); removal is a dry run only, pending approval from Johnny. The earlier backup 20261121-021057 holds only 4 v0 fragments and is not a clean rollback.

## Next steps
Johnny decides on removing the v0 content from the server; then Phase 2 (shared fragments). The dotenv checksum was skipped because the privacy hook blocked the read; environment.json size and mtime were unchanged.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
