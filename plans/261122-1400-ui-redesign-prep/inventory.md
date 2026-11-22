# Kiểm kê UI/UX trước khi thiết kế lại (READ-ONLY, LOCAL)

Không sửa code/CSS trong bước này. Theme: `ecommerce-theme/ecommerce-theme-theme/src/main/resources`. Ảnh chụp thật (Chrome, 1280px, desktop) ở `screenshots/`. Mobile: **chưa chụp** (công cụ trình duyệt không đổi được kích thước cửa sổ); CSS có 1 breakpoint mobile (cuối phần đầu `main.css`, `--sw-pad:16px`, lưới 2 cột).

## 1. Trang / template (24 file) — đếm `sw-` (class) và `swXxx` (id/hook JS)
| Template | URL | `sw-` | id `sw*` | Ghi chú |
|---|---|---|---|---|
| page.html (layout chung, 96 dòng) | mọi trang | 0 | 0 | nạp `/css/main.css`, `/js/jquery.min.js`, `/js/ecommerce.js`; chèn fragment `head`/`scripts` từ DB |
| fragments/header.html | mọi trang | 36 | 10 | promo bar, util bar, logo SVG chữ, menu, search, wishlist, account, bag |
| fragments/footer.html | mọi trang | 15 | 7 | 3 ô dịch vụ, 3 cột link, newsletter, chọn nước/ngôn ngữ, logo |
| home.html | `/` | 17 | 5 | nội dung thật nằm ở page `home` trong DB |
| products.html | `/products` | 12 | 17 | bộ lọc + lưới, "Load more" (JS gọi API) |
| product.html | `/products/{code}` | 38 | 18 | ảnh, giá, số lượng, accordion mô tả/giao hàng |
| cart.html | `/shopping-cart` (`/cart` = 404) | 12 | 4 | |
| checkout.html | `/checkout` (khách bị đẩy sang `/login`) | 12 | 9 | |
| order.html / order-status.html | `/orders/{id}`, `/order-status` | 13 / 11 | 8 / 14 | |
| account.html, login, register, forgot-password, reset-password | `/account`, `/login`… | 21 / 12 / 10 / 10 / 11 | 11 / 4 / 4 / 5 / 5 | |
| wishlist, newsletter, stores, gift-card-balance, size-guide, appointment, contact, customer-service, notfound | tương ứng | 8, 9, 6, 11, 18, 19, 15, 33, 6 | 4, 6, 3, 4, 1, 5, 5, 1, 1 | |
| Page DB: `home` (20 `sw-`), `customer-service` (9) | `/`, `/customer-service` | 29 | | nguồn: `my-site/pages/*/content.html` |
| Fragment DB: header 27, footer 8, category-strip 9, theme/head 138 (CSS nhúng), theme/foot 13 | | 195 | | nguồn: `my-site/page-fragments/ecommerce/*` |

## 2. CSS / JS
| File | Dòng | Lượt `sw-` | Id `sw*` |
|---|---|---|---|
| static/css/main.css | 287 | 366 | 2 |
| static/js/ecommerce.js | 1716 | 54 | 138 |
| templates + fragments (24 file) | | ~420 | |
| my-site pages + fragments (DB) | | ~224 | |
| static/js/jquery.min.js, static/data/stores.json, product-attributes.json (nay `{}`), images/favicon.svg | | 0 | |
Class phổ biến nhất: `sw-line` 22, `sw-btn` 22, `sw-pad` 16, `sw-tools` 14, `sw-form` 14, `sw-bar` 11, `sw-drawer` 10.
Ngoài ra `my-site/AGENTS.md` nhắc `sw-`. `main.css` có hai khối trùng ý (`.sw …` và `body.sw …` "storefront theme additions") cần gộp khi làm lại.

## 3. Thành phần dùng chung
Header (promo, util, logo, menu drawer, search, wishlist, account, bag) · Footer (service strip, link, newsletter, locale) · Category strip (4 nút danh mục, ở home) · Product card + lưới (`sw-grid`, dựng bằng JS trong `ecommerce.js`, ~dòng 330) · Product detail (`sw-pdp`) · Cart · Checkout · Account · Accordion (`sw-acc`) · Nút (`sw-btn`, `sw-btn--dark`) · Form (`sw-form`).

## 4. Biến màu / chữ / logo ở đâu
- Màu & chữ: `:root` ở dòng 2 `static/css/main.css` — `--sw-bg #F0EEE4`, `--sw-ink #000`, `--sw-white #fff`, `--sw-badge #383735`, `--sw-line #d9d6c9`, `--sw-sans "Helvetica Neue",Arial`, `--sw-serif Georgia`, `--sw-pad 24px` (16px mobile). Một số màu còn cứng (`#000`, `#fff`) ngoài biến.
- Không có web font (chỉ font hệ thống) → dark/gold của brief cần thêm font + token mới.
- Logo: SVG chữ "KHANG NGUYEN" viết thẳng trong `templates/fragments/header.html:26` (và bản trong footer); favicon `static/images/favicon.svg`. **Lỗi thấy ở ảnh chụp:** chữ logo bị cắt thành "HANG NGUYE" (viewBox 200 rộng, chữ tràn).
- Brief thiết kế: `docs/design-brief-khang-nguyen-ngoc-bao.md` (nền tối/vàng, tiền tố `kn-`).

## 5. Quan sát từ ảnh chụp thật (cần xử lý khi làm lại)
1. Trang chủ vẫn còn nội dung thương hiệu cũ: banner "Black Friday — Up to 40% off", "Masters of Light Since 1895", "Gifts for every occasion" (ảnh trống); promo bar "Black Friday". Nằm trong page `home`/fragment header (DB), không phải catalog.
2. Logo bị cắt chữ (mục 4).
3. `/products`: ảnh từ thẻ thứ 17 trở đi trống trong ảnh chụp full-page (ảnh lazy-load; cả 35 URL ảnh đều trả 200 — chưa xác nhận trong cuộn thật).
4. `/cart` 404, đường đúng `/shopping-cart`; `/checkout` đẩy khách sang `/login` (không đặt đơn).
5. Văn bản giao diện toàn tiếng Anh (Customer Service, Shop now…) trong khi catalog tiếng Việt; địa chỉ footer "Singapore"; copy "demo storefront".
6. Thẻ sản phẩm: một số ảnh có chữ chèn sẵn trong ảnh gốc (do nguồn fanpage), không phải lỗi giao diện.

## 6. Cách nhận file thiết kế (đề xuất)
Ưu tiên: **Figma** (link + quyền xem, kèm tên frame cho 8 màn: Home, Products, Product, Cart, Checkout, Account, Customer service, 404) hoặc **PNG/JPG 1440 + 390px** mỗi màn. HTML tĩnh cũng nhận được. Đặt vào `design/incoming/` (đã có thư mục `design/`), kèm token màu/chữ (hex, font, cỡ) nếu có; nếu không thì trích từ brief. Mỗi màn cần đủ trạng thái: loading, trống, lỗi, hover/focus.

## 7. Kế hoạch đổi `.sw-` → `kn-` (đề xuất, chưa làm)
Phạm vi: ~640 lượt class + ~140 id/hook JS + 195 lượt trong fragment DB. Cách an toàn:
1. Chốt tiền tố `kn-` (id `knXxx`) theo brief; viết map 1-1 (script đổi chuỗi có ranh giới `\bsw-` và `\bsw[A-Z]`, không đụng `swiper`/`switch`…).
2. Đổi đồng thời theo một "writer": `main.css` + `ecommerce.js` + 24 template trong cùng một commit (JS tìm phần tử bằng id/class, lệch một chỗ là vỡ).
3. Đổi fragment/page trong `my-site/` rồi `ezy publish apply` (có backup tự động) — DB giữ cùng slug.
4. Giữ alias tạm `.sw-*` → không cần; vì cùng một lần deploy nên đổi thẳng, kiểm bằng `grep -r '\bsw-'` = 0.
5. Kiểm: chụp lại 8 màn trước/sau, so ảnh; chạy `tests/api` (m1) + duyệt tay luồng mua (không đặt đơn tay).
6. Triển khai theme cần deploy jar/static; static trong `/app/ezyplatform/web/themes/ecommerce-theme/resources` đã chứng minh phục vụ ngay không restart (đã thử với `product-attributes.json`), còn template Thymeleaf có thể cần restart — hỏi Batman trước.
Rủi ro: đổi sót id làm hỏng JS (giỏ hàng, bộ lọc); trộn khối CSS trùng; cache `?v=` của trình duyệt.
