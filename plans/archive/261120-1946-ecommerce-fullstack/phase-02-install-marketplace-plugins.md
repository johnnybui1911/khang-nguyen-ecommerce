---
title: "Phase 2: Cài marketplace plugins + cấu hình shop VND"
status: todo
priority: P1
owner: Cyborg
effort: "2h"
dependencies: [1]
gate: "Johnny duyệt Q1"
---

# Phase 2: Cài marketplace plugins + cấu hình shop

## Overview
Cài `ecommerce` 1.5.8, `ezylogin` 0.4.7, `ezyaccount` 0.2.4, `ezymail` 0.3.3 (đều free) lên Docker local; cấu hình shop mặc định, quốc gia Việt Nam, tiền tệ VND (Johnny chốt Q4). `ezypayment` chỉ cài nếu Phase 4 cần extension point của nó.

## Requirements
- [ ] Backup Phase 1 còn mới (< 2h) hoặc dump lại ngay trước khi cài.
- [ ] Cài theo cách ezy CLI/admin UI chính thức; ghi lệnh + output. Không sửa `.env`, không xoá volume.
- [ ] Tạo shop mặc định, kích hoạt country VN; dùng currency VND (kiểm trước xem VND có trong 7 preset chưa; chưa có thì tạo bằng form "Thêm mới" mã ISO VND, 0 chữ số thập phân, ký hiệu ₫). Đặt VND làm tiền mặc định.
- [ ] Bật ezylogin cho web; xác nhận giỏ/đơn của ecommerce gắn với user ezylogin.

## Implementation Steps
1. Dump DB → cài từng plugin một → sau mỗi plugin: `curl :8080` = 200, `ezy check` OK, log không có ERROR mới.
2. Cấu hình shop/currency/country trong admin; chụp màn hình.
3. Liệt kê bảng mới (`SHOW TABLES LIKE 'ecommerce_%'` …) để Phase 4 dùng.

## Success Criteria
- 4 thư mục plugin có trong `web/plugins` và `admin/plugins`; admin menu có E-Commerce, EzyLogin, EzyAccount, EzyMail.
- `GET /api/v1/shopping-cart/product-count` khi chưa đăng nhập trả 401/redirect (không 404/500).
- Trang đăng ký/đăng nhập của ezylogin mở được (200) trên :8080.
- Rollback ghi sẵn: tắt/gỡ plugin + restore dump; dry-run lệnh restore (in lệnh, không chạy).
