---
title: "Phase 6: Seed catalog"
status: todo
priority: P1
owner: "Plastic Man (import), Mister Terrific (dữ liệu nguồn)"
effort: "4h"
dependencies: [2, 4]
---

# Phase 6: Seed catalog 40 sản phẩm

## Overview
≥ 40 sản phẩm mẫu (24 necklaces từ category page + home + vài watches/decorations), category tree như menu gốc, màu/chất liệu/giá/giá gốc/discount VND (quy đổi từ giá SGD gốc theo 1 SGD = 20.000 VND (tỷ giá demo cố định, làm tròn đến 1.000 ₫); ghi cả giá SGD gốc trong file seed để đối chiếu), ảnh lưu local media. Ghi rõ dữ liệu mẫu.

## Requirements
- [ ] Mister Terrific chuẩn bị `data/seed/source/products.csv` + `categories.csv` từ trang public (không crawl ngoài phạm vi trang đã xem).
- [ ] Import bằng Excel import hoặc API admin của ecommerce (Phase 4 chọn), script lặp lại được và idempotent (chạy 2 lần không nhân đôi).
- [ ] Ảnh tải về và upload qua media API; không hotlink ecommerce.com.

## Success Criteria
- Admin: số sản phẩm published ≥ 40, mỗi sản phẩm có giá, category, ≥ 1 ảnh.
- Mọi URL ảnh trả 200 từ `:8080/api/v1/media/...`.
- Sản phẩm 5517117: giá 135, giá gốc 169, -20%.
