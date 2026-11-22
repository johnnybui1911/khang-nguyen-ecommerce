---
phase: 4
title: "Category và Product detail"
status: pending
priority: P1
effort: "6h"
dependencies: [2]
---

# Phase 4: Category và Product detail

## Goal
Hai trang `necklaces` (lưới sản phẩm) và `product-detail-sample` (chi tiết).

## Owner (một writer)
- Writer: Plastic Man, chỉ `my-site/pages/necklaces/` và `my-site/pages/product-detail-sample/`.
- Chạy song song với Phase 3 và 5 chỉ khi mỗi phase có writer khác và thư mục riêng (cô lập bằng ak-worktree); mặc định tuần tự.

## Steps
1. Category: breadcrumb, H1, nút Filters (chỉ giao diện), lưới 4 cột desktop / 2 cột mobile (24 sản phẩm đầu), nhãn giảm giá, 2 banner chèn, "Load more" không hoạt động, khối SEO.
2. Product: gallery, tên, giá, nhãn, chọn tuỳ chọn (giao diện), thông tin, category-strip.
3. Link qua lại giữa các trang local.

## Done when
- Hai slug trả 200; bố cục đạt tiêu chí 1280/390; không ảnh vỡ.

## Rollback
Như Phase 3.
