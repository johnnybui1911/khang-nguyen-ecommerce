---
phase: 3
title: "Homepage home"
status: pending
priority: P1
effort: "4h"
dependencies: [2]
---

# Phase 3: Homepage

## Goal
Trang `home` giống `/en-SG/` ở desktop 1280px và mobile 390px.

## Owner (một writer)
- Writer: Plastic Man, chỉ `my-site/pages/home/`.

## Files
- Create: `meta.json` (slug `home`), `head.html`, `content.html`, `foot.html`.

## Steps
1. Import fragment header, footer, category-strip, theme qua `<ezy:block ezy:replace="~{page/fragment :: content|head|foot}" />` đúng vùng.
2. Hero carousel 2 slide, H1 "Masters of Light Since 1895", Shop by Category, 3 banner, dải product tiles, editorial 3 thẻ (theo quan sát nguồn; dữ liệu plan cũ chỉ để tham khảo).
3. Ảnh dùng tài nguyên local; không video.

## Done when
- `http://127.0.0.1:8080/home` trả 200 (sau Phase 6).
- Không có request 4xx; link nội bộ đúng.

## Rollback
Xoá thư mục page hoặc khôi phục từ backup Phase 1.
