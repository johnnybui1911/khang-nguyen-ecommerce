---
title: "Ecommerce clone phase 2: shared fragments"
date: 2026-11-20
summary: "Built theme, header, footer and category-strip fragments from scratch; static checks pass, publish dry run shows 4 fragment updates, nothing applied."
---

# Ecommerce clone phase 2: shared fragments

## What happened
Phase 2 ran through ak-cook then ak-test. Re-checked the en-SG source in the browser (menu, utility bar, footer columns, social links) and rebuilt four fragments in my-site/page-fragments/ecommerce/ without reusing the archived v0 files. CSS lives only in theme/head.html and JS only in theme/foot.html; header, footer and category-strip have content only. Logo and icons are inline SVG, so no media was created.

## Decision
Fragments use contentType THYMELEAF and status PUBLISHED to match the existing server records. Carousel and gallery styles are left to the page phases. Internal links point to the three planned slugs; everything else is a hash link. The publish dry run lists 4 pending updates and nothing was applied.

## Next steps
Phase 3 homepage. A rendered check of the fragments needs a page that imports them plus an apply, which waits for Johnny's approval. My first static-check regex flagged "width:" as a Thymeleaf token; that was a false positive, confirmed with a word-bounded search.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
