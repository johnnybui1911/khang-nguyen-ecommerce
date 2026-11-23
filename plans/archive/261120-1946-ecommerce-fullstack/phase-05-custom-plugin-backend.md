---
title: "Phase 5: Plugin ecommerce-ext backend + test API"
status: todo
priority: P1
owner: Plastic Man
effort: "3d"  # tăng từ 1.5d: thêm lớp controller bù cho ecommerce 1.5.8 / ezyaccount 0.2.4
dependencies: [3, 4]
---

# Phase 5: Plugin `ecommerce-ext` backend

## Overview
Viết plugin riêng theo contract frozen: wishlist, contact, order-status, mock payment, (search/facet nếu cần), admin list. Test-first.

## Cập nhật 2026-11-21 10:40 (Batman): hướng 2, bù controller thiếu
Bối cảnh: `ecommerce` 1.5.8 và `ezyaccount` 0.2.4 là bản mới nhất trên marketplace (xem `.artifacts/reports/ecommerce-fullstack/phase-02-plugins.md` mục "Marketplace versions"). Web chỉ tạo bean cho class có `@Controller`. 25/26 controller của ecommerce và `AccountController` của ezyaccount thiếu annotation này nên không có route (mục "Follow-up web routes").
Quyết định: **không vá jar gốc**. `ecommerce-ext` thêm một lớp controller mỏng (`@Controller` + `@Api`/`@Authenticated`). Lớp này giữ đúng path và response của controller gốc và chỉ gọi lại service sẵn có của ecommerce/ezyaccount (`WebShoppingCartService`, `WebShoppingCartValidator`, `WebProductCurrencyService`, `WebShopService`, `*ControllerService`, `*ModelDecorator`…). Các bean này đã được tạo (log DEBUG Phase 2). Không viết lại nghiệp vụ, không ghi trực tiếp bảng `ecommerce_*`.

Phạm vi bù (tham chiếu class gốc trong jar để giữ path/tham số/response):
- Giỏ hàng: `WebApiShoppingCartController` (gồm `/api/v1/shopping-cart/product-count`).
- Checkout: `WebApiCheckoutController`, `WebApiProductCheckoutController`, view `WebCheckoutController`, `WebProductCheckoutController`.
- Đơn hàng: `WebApiOrderController`, `WebApiProductOrderController`, view `WebOrderController`.
- Catalog phụ trợ cho theme: `WebApiProductController`, `WebApiCategoryController`, `WebApiCollectionController`, `WebApiProductPriceController`, `WebApiProductCurrencyController`, `WebApiCountryController`.
- Thanh toán mock: `WebPaymentCallbackController` nếu luồng mock pay cần callback.
- Tài khoản: trang `/account` (thay `AccountController` của ezyaccount). API `/ezyaccount/api/v1/account*` đã chạy nên không bù.
- **Ngoài phạm vi** (không bù): Affiliate, Commission, UserBalance, Voucher, QrCode, Delivery/DeliveryCallback, ShopBranch.

Quy tắc chống trùng route: mỗi controller bù có cờ cấu hình `ecm.compat.<tên>.enabled` (mặc định bật). Khi Young Monkeys ra bản sửa, tắt cờ hoặc gỡ lớp bù để route gốc dùng lại. Phase 4 phải đọc chữ ký method của class gốc (javap) rồi ghi vào api-design §3 trước khi viết.

## Files
- Create: `ecommerce-ext/` (project tạo bằng `ezydev create`), migration bảng `ecm_*`, `ecommerce-ext/tests/api/` (script test API chạy được bằng một lệnh).

## Requirements
- [ ] Test tái hiện (đỏ trước khi có lớp bù): guest `GET :8080/api/v1/shopping-cart/product-count` → 401 hoặc redirect `/login` (hiện ra 200 "Not found"); user đăng nhập → 200 JSON; `GET /account` guest → redirect login, user → 200.
- [ ] Mỗi route bù trả cùng path, mã lỗi và dạng JSON như controller gốc (so với chữ ký đã ghi ở Phase 4).
- [ ] Test API viết trước (đỏ), gồm: happy path từng endpoint, validation 400, 401 guest, IDOR user B → 403/404, idempotent wishlist/mock-pay, rate limit 429, order-status không lộ đơn.
- [ ] Không ghi trực tiếp bảng `ecommerce_*`; mock pay đi qua service/extension của ecommerce.
- [ ] Không log email đầy đủ + nội dung form ở mức INFO; không log token.
- [ ] Build trên native dev site, test xanh ở đó trước; sau đó dump DB Docker → cài artifact lên Docker → chạy lại test.

## Success Criteria
- `mvn -q verify` xanh; script test API xanh trên Docker :8080 (log output lưu báo cáo).
- `SHOW CREATE TABLE ecm_*` khớp api-design §4.
- Log khởi động web có mapping `/api/v1/shopping-cart*`, `/api/v1/checkout*`, `/api/v1/orders*`, `/account`; luồng thêm giỏ → checkout → mock pay → xem đơn chạy được trên :8080.
- Gỡ plugin không làm hỏng ecommerce (dry-run gỡ trên native dev site).
