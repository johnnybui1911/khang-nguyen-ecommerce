# Thiết kế API, schema, auth (v1, Phase 4, 2026-11-21)

Trạng thái: **frozen** (Batman duyệt 2026-11-21 11:00; số lượng 70 endpoint đối chiếu lại ở Phase 5). Nguồn xác minh: curl thật vào `:8080` bằng user test tự đăng ký + đọc chữ ký controller trong jar; chi tiết ở `.artifacts/reports/ecommerce-fullstack/phase-04-api-contract.md`. Bản nháp v0 backup ở `.artifacts/backups/api-design.261121-1048.md`.

> **Sửa nền tảng so với v0:** hầu hết controller `ecommerce` + `ezyaccount.AccountController` **không được đăng ký** (thiếu `@Controller` cấp class) nên mọi GET `/api/v1/...` của chúng trả **HTTP 200 trang HTML "Not found" (soft-404)**, còn POST trả `404 {"error":"404"}`. Các controller `ezylogin` và `ezyaccount` mang `@Controller` chạy tốt. Xem mục 3b.

## 1. Nguyên tắc
- Dùng lại trước, viết sau: catalog, giỏ, checkout, đơn, kho, tiền tệ = `ecommerce`; user/session = `ezylogin`; hồ sơ/địa chỉ = `ezyaccount`. `ecommerce-ext` **không** ghi trực tiếp vào bảng `ecommerce_*`, chỉ gọi service/SDK của ecommerce; đọc bảng `ecommerce_*` chỉ khi không có service (ghi rõ trong contract).
- Không sửa file plugin đã cài; mở rộng bằng plugin riêng hoặc page fragment.
- Prefix: bảng `ecm_`, route web `/api/v1/ecm/...`, route admin `/api/v1/ecm-admin/...`.
- JSON lỗi thống nhất: `{"code": "<UPPER_SNAKE>", "message": "...", "fields": {..}}`; HTTP 400 dữ liệu sai, 401 chưa đăng nhập, 403 không phải của mình, 404, 409 trùng, 429 quá nhiều request.

> 2026-11-21: Johnny bỏ newsletter/club (Q8) → đã xoá A4, B1, bảng `ecm_newsletter_subscribers`; số hiệu còn lại giữ nguyên để không lệch tham chiếu. Tiền tệ VND (Q4): `amount` lưu số nguyên đồng, `currency`='VND'.

## 2. Auth
- Web: session do `ezylogin` cấp (cookie). Mọi endpoint của user lấy `userId` từ session phía server, **không bao giờ** nhận `userId` từ body/query.
- Admin: auth admin sẵn có của Ezyplatform (`:9090`), không dùng token admin trong frontend.
- Guest: chỉ được search, xem catalog, contact, order-status. Wishlist/giỏ/checkout yêu cầu đăng nhập (Default Q6).
- **CSRF (đã xác minh, kết quả xấu):** không có CSRF token. Cookie `accessToken` + `accessTokenExpiredAt` (Path=/, 7 ngày, KHÔNG HttpOnly, không thấy Secure/SameSite). `PUT /ezyaccount/api/v1/account/update` với cookie + `Origin: https://evil.example` → **204** (được chấp nhận). Mọi endpoint `ecm` có ghi dữ liệu phải tự chặn: kiểm `Origin`/`Referer` cùng host, bắt buộc header `X-Requested-With: ecm`, chỉ nhận `application/json`. Phần của plugin gốc để Phase 8 đánh giá.
- Lấy `userId` trong plugin: tham số `@UserId long userId` (framework giải mã token từ cookie/header); chưa đăng nhập + `@Authenticated` → `401 {"token":"invalid"}`. Không đọc cookie thủ công.

## 3. Endpoint của `ecommerce-ext`
| # | Method + path | Auth | Request | Response | Ghi chú |
|---|---|---|---|---|---|
| A1 | `GET /api/v1/ecm/wishlist` | user | `?page=&size=` | `{items:[{productId,slug,name,price,image,addedAt}],total}` | **GIỮ.** ecommerce không có wishlist (`EcommerceUserItemActionType` chỉ có `ADD_TO_CART`). `curl -b jar localhost:8080/api/v1/ecm/wishlist` → 200 text/html (chưa có route) |
| A2 | `PUT /api/v1/ecm/wishlist/{productId}` | user | – | `204` | **GIỮ.** Idempotent; 404 nếu product không tồn tại. Cùng lý do A1 |
| A3 | `DELETE /api/v1/ecm/wishlist/{productId}` | user | – | `204` | **GIỮ.** Idempotent |
| A5 | `POST /api/v1/ecm/contact` | guest | `{name,email,topic,orderCode?,message≤2000}` | `201 {id}` | **GIỮ.** Không có contact trong ecommerce/ezylogin/ezyaccount (`curl localhost:8080/api/v1/contact` → 200 text/html soft-404). Cần CSRF-guard + rate limit |
| A6 | `GET /api/v1/ecm/order-status` | guest | `?orderCode=&email=` | `{orderCode,status,updatedAt}` | **GIỮ.** `GET /api/v1/orders/{id}/status` của ecommerce có `@Authenticated` + theo id, không dùng được cho guest. Ecm-ext gọi `WebOrderControllerService`/service đơn rồi so email |
| A7 | ~~`POST /api/v1/ecm/payments/mock/{orderCode}/pay`~~ | – | – | – | **BỎ endpoint, dùng extension point.** Cài `PaymentService` (SDK) mã `ECM_DEMO` làm payment method; luồng chuẩn `POST /api/v1/orders/create-payment-url` → `GET/POST /verify/{code}`, `GET /callback/{code}`. Xem 3c. Bảng `ecm_mock_payments` giữ để audit |
| A8 | `GET /api/v1/ecm/products` | guest | cùng tham số, thêm facet | `{items,total,page,facets}` | **BỎ phần lớn.** `GET /api/v1/products` (ecommerce) đã có `keyword, categoryId, categoryName, tagId, productType(s), currencyId, priceGte, priceLte, discountPercentGt, sizeName, sortOrder, nextPageToken/prevPageToken, limit(30)` + `GET /products/min|max-published-price`. Chỉ giữ `GET /api/v1/ecm/products/facets?categoryId=` (đếm color/material) nếu Phase 6 thấy cần; không có màu/chất liệu facet sẵn (chỉ `sizeName`). Hiện endpoint gốc trả 200 text/html (soft-404) → phải dùng bản thay thế 3b |
| A9 | `POST /api/v1/ecm/appointments` | guest | `{storeId,name,email,date,slot}` | `201 {id}` | **GIỮ** (stretch). Không có trong plugin |
| B2 | `GET /api/v1/ecm-admin/contacts` | admin | `?page=` | list | Admin xem contact |

## 4. Schema (migration qua cơ chế plugin chuẩn)
```
ecm_wishlist_items      id PK, user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at
                        UNIQUE(user_id, product_id), INDEX(user_id, created_at)
ecm_contact_messages    id PK, name VARCHAR(120), email VARCHAR(320), topic VARCHAR(50), order_code VARCHAR(64) NULL,
                        message TEXT, status ENUM(NEW,READ), created_at
ecm_store_appointments  id PK, store_id VARCHAR(50), name, email, date DATE, slot VARCHAR(20), created_at   (stretch)
ecm_mock_payments       id PK, order_id BIGINT UNIQUE, user_id BIGINT, amount DECIMAL(18,2), currency CHAR(3),
                        status ENUM(PAID), created_at                 (log để audit, không chứa dữ liệu thẻ)
```
Không có cột mật khẩu, thẻ hay token trong bảng `ecm_*`. Danh sách cửa hàng Singapore là file tĩnh trong theme.

## 5. Validation & bảo mật (đầu vào ak-security ở Phase 8)
- Email: chuẩn hoá lowercase, tối đa 320 ký tự, regex cơ bản. Text: trim, giới hạn độ dài, escape khi render (Thymeleaf `th:text`, không `th:utext`).
- Rate limit theo IP cho A5, A6, A9 (ví dụ 10 req/phút); A6 chống dò đơn.
- IDOR: A1-A3, A7 kiểm chủ sở hữu từ session. Test bắt buộc: user B không thao tác được dữ liệu user A.
- Mock payment tắt mặc định ngoài local; không có trường số thẻ ở bất cứ form nào.

## 6. Extension trace (một biến thể kế tiếp)
Biến thể đáng tính nhất: thay mock payment bằng cổng thật của `ezypayment` (VNPay/PayPal) sau này. Hợp đồng giữ nguyên: checkout ecommerce gọi một payment method → đơn chuyển PAID. Thứ thay đổi: chỉ cài/cấu hình method khác, gỡ A7. Phase 4 phải chứng minh mock đi qua đúng extension point payment của ecommerce (nếu có) để việc đổi sau này không chạm checkout. Không dựng registry/factory riêng.

## 3b. Endpoint thay thế cho Phase 5 (controller thiếu `@Controller`)
**Căn cứ:** 18 controller `WebApi*` của `ecommerce` 1.5.8 (70 endpoint JSON) chỉ có `@Api`/`@Authenticated` (marker), không có `@Controller` → bean context không tạo → 200 text/html soft-404 (GET) / `404 {"error":"404"}` (POST). Trong đó `WebApiOrderController`, `WebApiCheckoutController`, `WebApiShoppingCartController`, `WebApiUserBalanceController`, `WebApiVoucherController`, `WebApiCommissionController`, `WebApiAffiliateController` là `@Authenticated`. `ezyaccount` `WebApiAccountController` **không** thuộc nhóm lỗi (có `@Controller("/ezyaccount/api/v1")`, curl authed → 200 JSON). Các controller `WebApiCommonAccount/Letter/Media/Notification` có `@Controller("/api/v1")`; chỉ `GET /api/v1/media/settings` được thử và trả 200 JSON, phần còn lại chưa thử.
**Cách làm (Phase 5):** `ecommerce-ext` thêm lớp `@Controller("/api/v1")` mỏng, **giữ nguyên path + response model gốc**, gọi bean `*ControllerService`/`WebShoppingCartService`... có sẵn; bật/tắt từng nhóm bằng `ecm.compat.<nhóm>.enabled`. Không sửa jar. Path gốc có tiền tố `/api/v1` (mô tả trong jar tự dẫn "GET /api/v1/checkout/receive-at-shop-branches").
Ưu tiên cho luồng chính (Phase 6/7): Product (3), Category, Collection, ProductPrice, ProductCurrency, ShoppingCart (3), Checkout (14), Order (9), Country (cho địa chỉ), Delivery, Voucher; phần Affiliate/Commission/UserBalance/QrCode để sau (không thuộc phạm vi clone).

| Nhóm | Method | Path gốc `/api/v1`+ | Auth |
|---|---|---|---|
| Affiliate | GET | `/user-affiliate-tokens` | user |
| Affiliate | POST | `/affiliate-tokens/{token}/create-affiliate-url` | user |
| Category | GET | `/product-categories` | guest* |
| Checkout | GET | `/checkout/context` | user |
| Checkout | GET | `/checkout/selected-delivery-information` | user |
| Checkout | GET | `/checkout/allow-user-received-stock-branch-ids` | user |
| Checkout | GET | `/checkout/receive-at-shop-branches` | user |
| Checkout | GET | `/available-payment-services` | user |
| Checkout | GET | `/default-shop-bank-accounts` | user |
| Checkout | POST | `/checkout` | user |
| Checkout | GET | `/checkout/shopping-cart/qr-codes` | user |
| Checkout | GET | `/checkout/orders/{id}/qr-codes` | user |
| Checkout | DELETE | `/checkout/shopping-carts/{shoppingCartId}/products/{productId}` | user |
| Checkout | POST | `/checkout/shopping-cart-products/add` | user |
| Checkout | PUT | `/checkout/shopping-cart-products/{id}` | user |
| Checkout | DELETE | `/checkout/shopping-cart-products/{id}` | user |
| Checkout | GET | `/user-affiliate-tokens/voucher` | user |
| Collection | GET | `/product-collections` | guest* |
| Collection | GET | `/product-collections/{id}/product-medias` | guest* |
| Commission | GET | `/commissions/{year}` | user |
| Commission | GET | `/commission-histories/{year}/{month}` | user |
| Country | GET | `/countries` | guest* |
| Country | GET | `/countries/{countryId}/provinces` | guest* |
| Country | GET | `/countries/{countryId}/province-and-children-list` | guest* |
| Country | GET | `/provinces/{provinceId}/districts` | guest* |
| Country | GET | `/provinces/{provinceId}/wards` | guest* |
| Country | GET | `/districts/{districtId}/wards` | guest* |
| Country | GET | `/countries/search` | guest* |
| Country | GET | `/provinces/search` | guest* |
| Country | GET | `/districts/search` | guest* |
| Country | GET | `/wards/search` | guest* |
| Delivery | GET | `/delivery/min-fee` | guest* |
| Delivery | GET | `/delivery/calculate-fee` | guest* |
| Delivery | GET | `/orders/{id}/delivery-orders` | guest* |
| Delivery | GET | `/delivery-orders/{id}` | guest* |
| Delivery | GET | `/delivery-orders/{id}/histories` | guest* |
| Order | GET | `/orders` | user |
| Order | POST | `/orders/create` | user |
| Order | POST | `/orders/create-waiting-to-confirm` | user |
| Order | PUT | `/orders/{id}/waiting-to-confirm` | user |
| Order | PUT | `/orders/{id}/waiting-to-confirm-in-duration` | user |
| Order | POST | `/orders/create-payment-url` | user |
| Order | POST | `/orders/{id}/create-payment-url` | user |
| Order | GET | `/orders/{id}` | user |
| Order | GET | `/orders/{id}/status` | user |
| ProductCheckout | POST | `/products/{code}/checkout` | guest* |
| ProductCheckout | POST | `/products/checkout` | guest* |
| Product | GET | `/products` | guest* |
| Product | GET | `/products/{id}` | guest* |
| Product | GET | `/products/{code}/details` | guest* |
| ProductCurrency | GET | `/currencies/default` | guest* |
| ProductCurrency | GET | `/currencies/{id}/format-amount` | guest* |
| ProductCurrency | GET | `/currencies/exchange-amount` | guest* |
| ProductOrder | POST | `/products/{productCode}/orders/create` | guest* |
| ProductPrice | GET | `/products/min-published-price` | guest* |
| ProductPrice | GET | `/products/max-published-price` | guest* |
| ProductPrice | GET | `/products/{productId}/size-color-price` | guest* |
| QrCode | POST | `/qr-code` | guest* |
| QrCode | GET | `/users/{userId}/orders/{orderId}/qr-code` | guest* |
| QrCode | GET | `/qr-images/{name}` | guest* |
| ShopBranch | GET | `/shop-branch-addresses` | guest* |
| ShopBranch | GET | `/shops/{shopId}/branches` | guest* |
| ShopBranch | GET | `/default-shop/branches` | guest* |
| ShoppingCart | GET | `/shopping-cart` | user |
| ShoppingCart | GET | `/shopping-cart/product-count` | user |
| ShoppingCart | GET | `/shopping-cart-products/{id}` | user |
| UserBalance | GET | `/me/balance` | user |
| Voucher | GET | `/user-vouchers` | user |
| Voucher | GET | `/user-vouchers/{id}/used` | user |
| Voucher | GET | `/vouchers/{code}/discount-amount` | user |
(`guest*` = không có `@Authenticated` ở cấp class; method có thể vẫn yêu cầu `@UserId`/`@Authenticatable`, Phase 5 kiểm từng method.)
Ghi chú: view route `WebProductController` (`/ecommerce/products/{code}`), `WebCheckoutController`, `WebPaymentCallbackController` (`/callback/{code}`, `/verify/{code}`), `WebProductCheckoutController` cũng **chưa** khớp route khi curl (trang 200 "Not found"); xử lý như nhóm trên.

## 3c. Quyết định: payment extension point
- **A7 là method, không phải endpoint.** Extension point thật: lớp trừu tượng `PaymentService` trong `ecommerce-sdk`, quản lý bởi `PaymentServiceManager` (`EzySingletonFactory`), loại `PaymentServiceType`, bật trong bảng `ecommerce_payment_service_settings` (admin :9090). Plugin `ecommerce-ext` khai báo `@EzySingleton class EcmDemoPaymentService extends PaymentService` mã `ECM_DEMO`.
- Luồng: `POST /api/v1/orders/create-payment-url` (hoặc `/orders/{id}/create-payment-url`) → service trả URL nội bộ trang "Demo payment" → xác nhận gọi `GET/POST /verify/{code}` / `GET /callback/{code}` → đơn PAID. Đổi cổng thật sau này = thay service, checkout không đổi (khớp mục 6).
- Chưa chứng minh chạy thật (cần mã Phase 5 + bật setting admin, thuộc quyền Johnny). `GET /api/v1/available-payment-services` hiện 200 text/html (soft-404).

## 3d. Quyết định: nguồn địa chỉ và theme
- **Một nguồn địa chỉ = `ezyaccount`** (hồ sơ user): `GET /ezyaccount/api/v1/account` trả `addressCountryId, addressProvinceId, addressDistrictId, addressWardId, addressNumber, address, companyAddress, metadata, metadataText`; `PUT /ezyaccount/api/v1/account/update`. Lý do: ecommerce không có sổ địa chỉ, chỉ có `checkout/selected-delivery-information` theo giỏ/đơn (snapshot). Checkout đọc địa chỉ ezyaccount để điền sẵn, đơn lưu bản chụp. Nhiều địa chỉ: **không hỗ trợ trong v1** (một địa chỉ mặc định; nếu Johnny cần sổ địa chỉ → bảng `ecm_addresses`, quyết định mới). Danh mục quốc gia/tỉnh/huyện/xã lấy từ `Country` (nhóm 3b).
- **Theme:** theme Freestyle layout `ezytheme` nhận `commonFragments` (`header, footer, styles, scripts, alert, loading, prompt, toast`) và `pageFragments` theo từng trang (ví dụ `content, container, shopping_cart, shopping_cart_empty, orders, order_details, payment_error, voucher_details, not_found...`; ezylogin: `login_modal, register_modal, forgot_password_modal`). Override template của ecommerce/ezylogin/ezyaccount bằng cách tạo fragment (post type FRAGMENT của ezyarticle) cho đúng key; không sửa template trong jar. Chưa xác minh quy ước slug→key (đường đi từ tên fragment tới key); Phase 7 thử bằng một fragment trên local.

## 7. Thay đổi so với v0
1. Trạng thái nháp → frozen-candidate; thêm nhận định soft-404 và mục 3b–3d.
2. A1, A2, A3, A5, A6, A9: giữ. A7: bỏ endpoint, thành method `PaymentService`. A8: bỏ phần lớn (dùng `GET /api/v1/products`), chỉ còn facet màu/chất liệu nếu cần.
3. CSRF: từ "theo cơ chế Ezyplatform" → **không có**, ecm tự chặn bằng Origin + header.
4. Thêm quyết định nguồn địa chỉ (ezyaccount) và không hỗ trợ nhiều địa chỉ.
