---
phase: 2
title: "Fragment dùng chung (theme, header, footer, category-strip)"
status: pending
priority: P1
effort: "4h"
dependencies: [1]
---

# Phase 2: Fragment dùng chung

## Goal
Dựng theme/header/footer/category-strip một lần để cả 4 trang tái dùng, không nhân bản.

## Owner (một writer)
- Writer: Plastic Man, chỉ trong `my-site/page-fragments/ecommerce/{theme,header,footer,category-strip}/`.
- Reviewer (chỉ đọc): Minh Lý (UX/BA).

## Files
- Create: mỗi fragment gồm `meta.json` (không có `id`), `head.html`, `content.html`, `foot.html`.
- Media: tên khớp `^[a-zA-Z0-9_-]+(.[a-zA-Z0-9_-]+)*$`.

## Steps
1. Theme: biến CSS (màu, serif/sans), reset, grid, JS nhỏ cho drawer/carousel/accordion.
2. Header: promo bar, utility bar, thanh chính (menu, logo, search/wishlist/bag chỉ giao diện), menu drawer; biến thể trong suốt (homepage) và nền trắng (trang khác).
3. Footer: dải 3 dịch vụ, 4 cột link, chọn quốc gia/ngôn ngữ, icon MXH.
4. Category-strip: 4 ô tròn Jewelry, Watches, Decorations, Accessories.
5. Link ngoài 4 trang trỏ `#`; link nội bộ trỏ đúng slug trang local.

## Done when
- Mỗi fragment có đủ 4 file, import đúng vùng (content, head, foot).
- Dry run publish chỉ fragment (kết quả lưu report) không lỗi.
- Không còn trùng lặp CSS/JS giữa fragment.

## Rollback
Khôi phục `page-fragments/ecommerce/` từ backup Phase 1.

## Risks
Tài nguyên nguồn gọi API Ecommerce, vỡ khi chạy local. Dùng tài nguyên tự lưu/tự dựng; ảnh chỉ demo local.
