---
title: "Phase 9: Ship local, rollback dry-run, journal"
status: todo
priority: P2
owner: "Cyborg (ship/rollback), Batman (báo cáo cuối)"
effort: "2h"
dependencies: [8]
---

# Phase 9: Ship local + journal

## Requirements
- [ ] ak-ship ở chế độ local: đóng gói artifact plugin + theme, ghi version, backup DB cuối.
- [ ] Dry-run rollback đầy đủ: gỡ `ecommerce-ext`, kích hoạt theme cũ, restore dump (in lệnh, chạy thử trên native dev site).
- [ ] Không publish ra ngoài, không domain thật.
- [ ] ak-journal ghi quyết định kiến trúc và bài học (lần trước gắn mác ak-* mà làm tay).
- [ ] Batman tự đo lại các số chính (số sản phẩm, E2E, test API) trước khi báo DONE.

## Success Criteria
- Báo cáo cuối `.artifacts/reports/ecommerce-fullstack/final.md`: Status, bằng chứng, phần chưa kiểm chứng, tồn đọng.
