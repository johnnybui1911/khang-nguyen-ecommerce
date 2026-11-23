---
title: Ecommerce clone phase 3 homepage
date: 2026-11-20
summary: "home page built, 4 fragments set to DRAFT, theme wrapper fix, local preview only"
---

# Ecommerce clone phase 3 homepage

## What happened
- Fragments theme/header/footer/category-strip meta.json status PUBLISHED -> DRAFT (before/after grep in .artifacts/reports/ecommerce-clone/phase-03-status-change.txt).
- Built my-site/pages/home (meta.json DRAFT, head/content/foot) importing fragments with ezy:block ezy:replace per region.
- Local preview (no server write) showed two phase-2 defects: theme CSS was scoped to body.sw (a page cannot set body class), and .sw a color:inherit beat .sw-btn colors (invisible button text). Fixed in theme/head.html: .sw wrapper selector and .sw a.sw-btn specificity. Pages wrap content in div.sw (home: div.sw.sw-home).
## Decision
- Imagery is CSS gradient placeholders, no media created; real images deferred.
- No apply/restore; dry run lists 5 updates.
## Next steps
- Phases 4-5 must wrap content in div.sw. Rendered check on real server only after Johnny approves apply (Phase 6).

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
