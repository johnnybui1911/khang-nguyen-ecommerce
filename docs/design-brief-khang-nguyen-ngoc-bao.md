# Design Brief: Khang Nguyên · Ngọc Bảo (storefront ngọc phong thủy)

Tài liệu này là nguồn thiết kế duy nhất để một agent khác thiết kế lại toàn bộ UI/UX của website.
Đọc hết mục 0 đến 3 trước khi làm bất cứ việc gì. Mọi quyết định thiết kế ở đây đều có nguồn
(yêu cầu của chủ shop, research Refero, hoặc quan sát từ repo). Nếu bạn muốn đi ngược lại một
quyết định, hãy nêu lý do và hỏi lại người giao việc thay vì tự đổi.

Ngày lập: 2026-11-22. Trạng thái: bản 2, hướng **sáng** (thay bản nền đen) theo phản hồi của khách:
"nền đen trông không ổn; muốn phong cách phương Đông, xanh ngọc bích, vàng gold, trắng, nhiều ánh nắng tươi sáng".

---

## 0. Tóm tắt một đoạn

Website bán trang sức ngọc (vòng, nhẫn, chuỗi) cho khách Việt, có giỏ hàng và thanh toán trực tiếp
trên web. Cảm giác cần đạt: **phương Đông, tươi sáng, nhiều ánh nắng**, lấy **xanh ngọc bích** làm màu
chủ đạo, **vàng gold** làm họa tiết và điểm nhấn, **trắng và kem nắng** làm nền; đỏ rượu dùng rất ít. Không
có nền đen ở bất kỳ đâu. Ảnh sản phẩm thật và các bài chia sẻ kiến thức về ngọc là hai thứ quan trọng nhất.
Thiết kế dựa trên ba reference Refero: **Sweetgreen** (nền kem nắng, mảng xanh phẳng, nút viên thuốc, thẻ không
khung) làm nền tảng, **Franco Maria Ricci** (vàng cổ, nút chữ ngoặc nhọn, con dấu) và **Zeus Jones** (kem ấm,
serif văn chương, cụm in nghiêng) cho chi tiết.

## 1. Brief

```text
Designing một storefront ngọc phong thủy cho khách Việt (mua online, thanh toán trên web), web desktop + mobile.
Goal: khách tin ngọc là thật, thấy rõ chất ngọc qua ảnh, thêm vào giỏ và thanh toán không vướng.
Tone: phương Đông, tươi sáng, nhiều nắng, sang nhưng gần gũi, ít chữ, nhiều ảnh. Không khuyến mãi ồn ào.
Main objection: "Ngọc này có thật không, giá có xứng không?" (khách Việt rất thận trọng với ngọc)
Must remember: nền kem nắng + xanh ngọc bích + vàng gold, ảnh vòng ngọc trong khung trắng bo 20px, bảng so sánh Tân Cương/Thanh Hải.
Constraints: chạy trên EzyPlatform (page + page fragment, Thymeleaf), font phải có đủ dấu tiếng Việt,
  chưa có giấy kiểm định riêng cho từng món, ảnh hiện có rất ít.
Research: styles (Sweetgreen, Franco Maria Ricci, Zeus Jones), screens (Farfetch, Saint Laurent product detail).
```

### 1.1 Người dùng và mục tiêu

| Mục | Nội dung | Nguồn |
|---|---|---|
| Khách hàng | Người Việt, mua ngọc để đeo, tặng, hoặc theo phong thủy | Chủ shop |
| Cảm giác mong muốn | Phương Đông; xanh ngọc bích, vàng gold, trắng; nhiều ánh nắng, tươi sáng; không nền đen | Chủ shop, khách (phản hồi 2026-11-22) |
| Ưu tiên | Ảnh sản phẩm, các bài chia sẻ về ngọc | Chủ shop |
| Mua hàng | Có giỏ hàng, thanh toán trực tiếp trên web | Chủ shop |
| Giấy kiểm định | Tạm thời không có. Không thiết kế khu vực "chứng nhận" cố định | Chủ shop |

### 1.2 Non-goals

- Không làm trang giống một thương hiệu quốc tế khác. Không dùng tên, logo, ảnh, câu chữ của họ.
- Không làm khuyến mãi dày đặc, đếm ngược, banner giảm giá lòe loẹt.
- Không dùng ảnh người mẫu AI làm ảnh chính của sản phẩm (xem mục 6).

## 2. Hiện trạng project (để biết cái gì phải thay)

Thư mục gốc: `ezyplatform/`. Các phần chính:

| Phần | Vai trò |
|---|---|
| `my-site/` | Nội dung site, đồng bộ với admin EzyPlatform. Pages ở `pages/<slug>/`, fragment ở `page-fragments/<page_name>/<fragment>/`. Mỗi thư mục gồm `content.html`, `head.html`, `foot.html`, `meta.json` |
| `ecommerce-theme/` | Theme Java: template Thymeleaf (`ecommerce-theme-theme/src/main/resources/templates/`), `static/css/main.css`, `static/js/ecommerce.js` |
| `ecommerce-ext/` | Plugin web: API giỏ hàng, thanh toán, đơn hàng, wishlist, sản phẩm, danh mục |
| `data/seed/` | Dữ liệu mẫu cục bộ (xem cảnh báo bản quyền ở mục 9) |
| `plans/`, `docs/` | Plan và tài liệu |

### 2.1 Các trang hiện có

Pages trong `my-site/pages/`: `home`, `necklaces` (trang danh mục), `product-detail-sample`,
`customer-service`, `example-page`, và **`vong-ngoc-demo`** (trang chi tiết sản phẩm mới, **sinh tự động** từ `docs/design/product.html`, không sửa tay).

Fragments trong `my-site/page-fragments/ecommerce/`: `theme` (chứa CSS dùng chung trong `head.html`),
`header`, `footer`, `category-strip`. Mỗi trang gọi fragment bằng
`<ezy:block ezy:replace="~{ecommerce/header :: content}" />`.

Templates theme (đây là các trang động do hệ thống render, cũng phải được thiết kế lại):
`home`, `products`, `product`, `cart`, `checkout`, `order`, `order-status`, `account`, `login`,
`register`, `forgot-password`, `reset-password`, `wishlist`, `stores`, `size-guide`,
`gift-card-balance`, `appointment`, `contact`, `customer-service`, `newsletter`, `notfound`, `page`,
cùng `fragments/header.html` và `fragments/footer.html`.

### 2.2 Giao diện hiện tại

Giao diện hiện tại là bản sao phong cách của một thương hiệu trang sức pha lê quốc tế: nền be
`#F0EEE4`, chữ Helvetica Neue và Georgia, nút vuông đen trắng, token đặt tên `--sw-*` (tiền tố
`sw-`/`swp-`/`swd-`). Token được khai báo **hai nơi giống hệt nhau**:
`page-fragments/ecommerce/theme/head.html` và `ecommerce-theme/.../static/css/main.css`.
Bản thiết kế lại phải thay cả hai, và nên đổi tiền tố sang một tiền tố của thương hiệu mới
(đề xuất `kn-`) để không còn dấu vết của thương hiệu cũ.

### 2.3 Hành vi mua hàng hiện có (đừng phá)

- Trang sản phẩm thật (`product.html`) lấy dữ liệu từ `/api/v1/products/{id}` rồi tự render bằng
  `ecommerce.js` (id như `swPdp`, `swAddBtn`, `swPdpPrice`). Giỏ dùng hàm `addToBag(product.id, qty)`.
- `cart.html` hiển thị `#swLines`, tổng tạm tính `#swSubtotal`, nút sang `/checkout`.
- Khi đổi tên class/id, phải sửa `ecommerce.js` tương ứng. Cách an toàn nhất: giữ id, chỉ đổi class
  và CSS, hoặc đổi đồng bộ cả hai và chạy lại luồng thêm vào giỏ, giỏ, thanh toán.
- Trang demo `vong-ngoc-demo` chưa nối giỏ thật (nút chỉ đổi giao diện). Khi làm thật, dùng
  product có `id` trong DB.

## 3. Hướng thiết kế (reference lock)

Phản hồi của khách (2026-11-22): nền đen trông không ổn; cần phương Đông, xanh ngọc bích, vàng gold, trắng,
nhiều ánh nắng, tươi sáng. Hướng nền đen (Slash) của bản trước đã bị loại bỏ hoàn toàn.

```text
Primary reference/direction: Sweetgreen — "sunlit appetite editorial" (style id d59df660-8f1a-438f-9c50-f210b2eeb229)
  cho toàn bộ site: nền kem nắng, mảng màu phẳng, nút viên thuốc, thẻ sản phẩm không khung, bố cục hai nửa
  (ảnh + mảng màu), chân trang dạng thư mục trên mảng nhạt.
Secondary (mỗi nguồn một vai trò):
  Franco Maria Ricci — "the collector's cabinet" (2c3b31a4-6e28-47a0-afbd-cb22e22e34fb): vàng cổ, nút chữ ngoặc nhọn { },
    con dấu vàng ở chân trang, đường kẻ vàng mảnh.
  Zeus Jones — "meaning in motion" (6151c9d4-5059-4ae0-9007-d9128207ec65): kem ấm, serif văn chương mảnh, một cụm in nghiêng
    có nghĩa trong tiêu đề, ảnh bo 20px, khoảng thở rộng.
Preserve: nền kem nắng và trắng, KHÔNG có nền tối; tiêu đề serif màu ngọc đậm, cụm italic màu vàng đậm;
  nút chính viên thuốc màu ngọc; mảng ngọc nhạt PHẲNG (không gradient); quầng nắng vàng nhạt ở hero và dải kêu gọi;
  ảnh bo 20px trong khung trắng viền mảnh; thẻ sản phẩm không khung.
Borrow only: nút chữ ngoặc nhọn; con dấu vàng; họa tiết kim cương mảnh ngăn cách khu.
Role rules: ngọc = hành động + tiêu đề + giá; vàng = họa tiết, đường kẻ, viền, huy hiệu (vàng đậm cho chữ);
  đỏ rượu chỉ cho viền lỗi và đường trái đoạn trích; nắng vàng nhạt chỉ làm quầng nền, không làm nút.
Media strategy: ảnh sản phẩm thật, ưu tiên chụp ban ngày trên nền sáng. Ảnh hiện có (nền xanh đen) được cắt lấy
  phần khay nhung và đặt trong khung trắng bo 20px cho đến khi có ảnh mới.
Reject: mọi nền đen hoặc gần đen; box-shadow; vàng làm nền nút; xanh chartreuse/neon của Sweetgreen (không hợp ngọc);
  gradient tím hoặc chàm; Bodoni; infographic ảnh thay cho nội dung HTML.
Token commitments: xem mục 4 và docs/design/kn.css.
```

### 3.1 Vì sao đổi sang sáng, và vấn đề ảnh nền tối

Khách thấy nền đen nặng và không hợp ngọc. Ngọc bích hợp nhất với nền kem nắng, trắng và xanh ngọc nhạt: màu
ngọc đọc rõ, gợi cảm giác tươi, thanh, hợp phong thủy. Vàng được giữ làm họa tiết để vẫn sang.

**Vấn đề còn lại:** ba ảnh sản phẩm hiện có chụp trên nền xanh đen sẫm, nên trên nền sáng chúng tạo ra các khối
tối. Cách xử lý tạm trong bản mẫu: cắt ảnh lấy phần khay nhung phía dưới (`object-position` ở đáy, hoặc phóng 1.3
lần ở khu bài viết) và đặt trong khung trắng bo 20px. Cách xử lý đúng là chụp lại bộ ảnh trên nền sáng (mục 6.2).

### 3.2 Decision ledger

| Quyết định | Nguồn | Vai trò cần giữ | Lý do |
|---|---|---|---|
| Nền kem nắng, trắng, mảng ngọc nhạt phẳng | Sweetgreen (cream canvas, sage band) | Nền trang, mảng khu | Sáng, tươi, màu ngọc nổi |
| Ngọc đậm cho nút chính, tiêu đề, giá | Sweetgreen (deep forest action ink), đổi sang bích ngọc theo yêu cầu khách | Hành động, tiêu đề | Màu chủ đạo của khách |
| Nút viên thuốc 52px | Sweetgreen (action capsule) | Một nút chính mỗi khu | Mềm, thân thiện, đọc rõ |
| Thẻ sản phẩm không khung, ảnh bo 20px | Sweetgreen, Zeus Jones | Lưới sản phẩm | Ảnh là trung tâm |
| Bố cục hai nửa: ảnh + mảng ngọc nhạt | Sweetgreen (editorial split) | Khu bài viết | Nhịp đọc rõ, giữ bảng so sánh |
| Vàng `#c29a3e` cho họa tiết, `#7d5f17` cho chữ | Franco (antique gold), kiểm tra tương phản | Đường kẻ, huy hiệu, nhãn, italic | Vàng sáng chỉ đạt 2.5:1 trên kem nên không làm chữ |
| Cụm in nghiêng màu vàng đậm trong tiêu đề | Zeus Jones (mixed serif/italic), yêu cầu khách có vàng | Tiêu đề | Thêm vàng mà vẫn đọc được |
| Nút chữ ngoặc nhọn `{ ... }` | Franco | Hành động phụ | Chi tiết riêng của thương hiệu |
| Cormorant Garamond weight 500 + Be Vietnam Pro weight 400 | Metadata Google Fonts (có Vietnamese); nền sáng cần nét đậm hơn bản tối | Chữ | Bodoni Moda không có glyph tiếng Việt |
| Quầng nắng vàng nhạt | Yêu cầu khách ("nhiều ánh nắng") | Nền hero và dải kêu gọi | Cảm giác nắng mà không cần ảnh |
| Không khu "chứng nhận" | Chủ shop (chưa có kiểm định) | Trang sản phẩm | Không hứa điều chưa có |

## 4. Design tokens

Nguồn duy nhất của token là khối `:root` trong **`docs/design/kn.css`**. Không sao chép giá trị
vào tài liệu khác và không tạo token mới ngoài file đó. Nhóm token:

| Nhóm | Token |
|---|---|
| Nền sáng | `--kn-canvas` (kem nắng), `--kn-white`, `--kn-sun` (nắng vàng nhạt), `--kn-jade-mist`, `--kn-jade-soft` |
| Ngọc | `--kn-jade` (hành động), `--kn-jade-deep` (tiêu đề, giá, hover) |
| Vàng và đỏ | `--kn-gold`, `--kn-gold-ink`, `--kn-crimson`, `--kn-danger` |
| Chữ và đường kẻ | `--kn-ink`, `--kn-body`, `--kn-muted`, `--kn-line`, `--kn-line-strong` |
| Font và bố cục | `--kn-serif`, `--kn-sans`, `--kn-pad`, `--kn-wrap` |

### 4.1 Vai trò của từng màu (không đổi nghĩa)

| Token | Dùng cho | KHÔNG dùng cho |
|---|---|---|
| `--kn-canvas` | Nền trang chính | Chữ |
| `--kn-white` | Thẻ, ô nhập, thanh điều hướng, khung ảnh, bảng | Chữ trên nền sáng |
| `--kn-sun` | Quầng nắng ở hero, giữa dải kêu gọi | Nút, chữ |
| `--kn-jade-mist` | Mảng khu bài viết, cuối dải kêu gọi, chân trang | Chữ |
| `--kn-jade-soft` | Đường kẻ trên mảng ngọc nhạt | Chữ, nền lớn |
| `--kn-jade` | Nền nút chính, chip đang chọn, viền nút phụ, viền ô nhập khi focus | Mảng nền lớn |
| `--kn-jade-deep` | Tiêu đề, giá, chữ nút phụ, hover nút chính, focus ring | Nền trang |
| `--kn-gold` | Họa tiết, đường kẻ, viền khung hero, viền nhãn, huy hiệu giỏ, con dấu | Chữ, nền nút |
| `--kn-gold-ink` | Chữ vàng: nhãn nhỏ viết hoa, cụm in nghiêng, dấu +/− | Nền |
| `--kn-crimson` | Viền lỗi, đường trái đoạn trích | Nền khối, nút |
| `--kn-danger` | Chữ báo lỗi | Nền |
| `--kn-ink`, `--kn-body`, `--kn-muted` | Chữ chính, chữ mô tả, chữ phụ | Nền (đặc biệt: không bao giờ làm nền) |
| `--kn-line`, `--kn-line-strong` | Đường kẻ mảnh, viền thẻ; viền ô nhập, chip, accordion | Chữ |

### 4.2 Tương phản (đã tính xấp xỉ)

| Cặp màu | Tỷ lệ | WCAG AA cho chữ thường |
|---|---|---|
| `--kn-jade-deep` trên `--kn-canvas` | khoảng 9.9 : 1 | Có |
| `--kn-white` trên `--kn-jade` (chữ nút chính) | khoảng 6.4 : 1 | Có |
| `--kn-gold-ink` trên `--kn-canvas` | khoảng 5.6 : 1 | Có |
| `--kn-gold-ink` trên `--kn-jade-mist` | khoảng 5.0 : 1 | Có |
| `--kn-muted` trên `--kn-canvas` | khoảng 5.4 : 1 | Có |
| `--kn-danger` trên `--kn-canvas` | khoảng 7 : 1 | Có |
| `--kn-gold` trên `--kn-canvas` | khoảng 2.5 : 1 | **Không**. Vàng sáng chỉ cho họa tiết, đường kẻ, viền, huy hiệu (chữ trong huy hiệu là `--kn-ink`) |

### 4.3 Typography

Font phải có Vietnamese subset (đã xác minh bằng metadata Google Fonts ngày 2026-11-22):

| Font | Vai trò | Vietnamese | Ghi chú |
|---|---|---|---|
| Cormorant Garamond 500, 600, italic 500 | Tiêu đề, tên sản phẩm, trích dẫn | Có | Nét mảnh; trên nền sáng dùng weight 500 để đủ đậm |
| Be Vietnam Pro 400, 500, 600 | Giao diện, nút, giá, bảng | Có | Thiết kế cho tiếng Việt. Weight 400 cho đoạn văn (300 quá mảnh trên nền sáng) |
| Bodoni Moda | KHÔNG dùng | Không | Thiếu glyph tiếng Việt |
| Playfair Display, Noto Serif | Phương án dự phòng cho serif | Có | Chỉ dùng nếu Cormorant không đạt |

Thang chữ (desktop; mobile giảm theo `kn.css`):

| Recipe | Font | Cỡ | Dùng cho |
|---|---|---|---|
| display | serif 500, màu ngọc đậm | 56 đến 64 | Tiêu đề hero, tên sản phẩm lớn |
| chapter | serif 500 | 60 | Lời kêu gọi cuối trang |
| heading | serif 500 | 48 | Tiêu đề khu bài viết |
| card-title | serif 500 | 26 | Tên sản phẩm trong thẻ |
| lead | sans 400 | 18 | Đoạn mô tả đầu |
| body | sans 400 | 15 đến 16 | Nội dung |
| price | sans 600, màu ngọc đậm | 32 | Giá |
| eyebrow | sans 600, viết hoa, màu vàng đậm | 12 | Nhãn phía trên tiêu đề |
| action | Arial 700, viết hoa | 13 | Nút chữ có ngoặc nhọn |

Quy tắc: tracking serif `+0.005em`. Mỗi tiêu đề có tối đa **một cụm in nghiêng có nghĩa**, tự động đổi sang màu vàng đậm
(`.kn h1 em, .kn h2 em`). Dùng `text-wrap: balance` cho tiêu đề.

### 4.4 Khoảng cách, bố cục, bo góc

| Giá trị | Dùng cho |
|---|---|
| 8, 12, 16 | Khoảng cách trong thành phần nhỏ |
| 24 | Khoảng cách lưới thẻ, padding thẻ |
| 32 | Padding hai bên khung nội dung |
| 48, 64 | Khoảng cách giữa nhóm, giữa ảnh và thông tin |
| 96 | Khoảng cách giữa các khu lớn |
| 120, 160 | Đầu và cuối dải kêu gọi cuối trang |

- Khung nội dung tối đa 1216px, padding 32px (mobile 16px). Khu hai nửa tối đa 1440px.
- Bo góc chỉ có: **0**, **10px** (ô nhập, bảng, dòng giỏ, ảnh nhỏ), **20px** (ảnh, thẻ, khung tóm tắt, hộp trống), **viên thuốc 9999px** (nút, chip, nhãn). `24px` chỉ cho cặp nút viên thuốc xếp dọc trên mobile.

### 4.5 Bề mặt và độ sâu (tất cả đều sáng)

| Bề mặt | Mô tả | Dùng cho |
|---|---|---|
| Kem nắng | `--kn-canvas` | Nền trang |
| Quầng nắng | `radial-gradient(--kn-sun → trong suốt)` phía trên bên phải, trên nền kem | Hero |
| Thẻ trắng | `--kn-white` + viền `--kn-line`, bo 20px | Khung ảnh, khung tóm tắt đơn, hộp trống |
| Mảng ngọc nhạt | `--kn-jade-mist` phẳng, không gradient | Khu bài viết, chân trang |
| Dải bình minh | `linear-gradient(trong suốt → --kn-sun → --kn-jade-mist)` | Dải kêu gọi cuối trang (nối liền vào chân trang) |
| Kính điều hướng | kem 90% + `backdrop-filter:blur(16px)` + viền dưới `--kn-line` | Thanh điều hướng dính |

Không dùng bóng đổ. Độ sâu đến từ ảnh, khung trắng và viền mảnh, không đến từ box-shadow.

## 5. Thành phần (components)

Mã chạy được: class `kn-*` trong `docs/design/kn.css`, minh họa đủ trạng thái ở `docs/design/style-guide.html`. Thành phần nào chưa có thì thêm vào `kn.css` và `style-guide.html` trước rồi mới dùng.

| Thành phần | Mô tả ngắn | Ghi chú |
|---|---|---|
| Thanh điều hướng | Cao 68px, dính trên đầu, nền kem mờ; con dấu vàng + tên serif, 4 liên kết giữa, nút giỏ viên thuốc nền trắng với huy hiệu vàng | Mobile: ẩn liên kết giữa |
| Nút chính | Viên thuốc, nền `--kn-jade`, chữ trắng, cao 52px, hover `--kn-jade-deep` | Một nút chính mỗi khu |
| Nút viền ngọc | Viền 1px `--kn-jade`, chữ `--kn-jade-deep`, nền trong suốt, hover `--kn-jade-mist` | Hành động phụ |
| Nút chữ ngoặc nhọn | `{ KIẾN THỨC VỀ NGỌC }`, Arial 700 viết hoa, màu `--kn-jade-deep` | Hành động phụ nhẹ, ngoặc là bắt buộc |
| Cặp nút viên thuốc | Hai nút trong khung trắng bo tròn | Chỉ ở dải kêu gọi cuối trang |
| Khung ảnh | Nền trắng, viền `--kn-line` (hero: viền `--kn-gold`), padding 12px, bo 20px; ảnh bên trong bo 10px | Ảnh nền tối được cắt lấy phần khay nhung |
| Giá | Sans 600, màu `--kn-jade-deep` | Không gradient |
| Accordion | Thẻ `details`, dấu +/− màu vàng đậm | Không cần JS |
| Bảng so sánh | Bảng HTML trắng bo 10px trên mảng ngọc nhạt; tiêu đề cột màu vàng đậm viết hoa; mobile xếp thành khối nhờ `data-k` | Thay hẳn infographic ảnh |
| Đoạn trích nhấn | Serif italic, viền trái 2px `--kn-crimson` | Câu lưu ý quan trọng |
| Thẻ sản phẩm | Ảnh vuông bo 20px, dưới là tên serif rồi **giá nằm ngay dưới tên**; hover gạch chân vàng dưới tên | Không có khung bao quanh thẻ |
| Dòng sản phẩm (giỏ hàng, đơn hàng, wishlist, tóm tắt đơn ở thanh toán) | Ảnh nhỏ bên trái; bên phải xếp dọc: tên, **giá ngay dưới tên**, rồi thông tin phụ (kích thước, mã) và nút Xóa | Không đặt giá ở cột phải cùng hàng với tên |
| Đầu kệ | Chữ serif italic viết hoa ở giữa, hai đường kẻ vàng mảnh hai bên | Tiêu đề nhóm sản phẩm |
| Dải kêu gọi cuối trang | Dải bình minh, họa tiết kim cương vàng, tiêu đề serif 60px, cặp nút viên thuốc | Có ở trang chủ và trang sản phẩm |
| Chân trang | Mảng ngọc nhạt, con dấu vàng 132px, tên serif italic màu ngọc đậm, khẩu hiệu vàng đậm, 3 cột liên kết, dòng pháp lý | Con dấu là SVG tự vẽ |
| Ô nhập | Nền trắng, viền `--kn-line-strong`, bo 10px, cao 48px; focus viền ngọc; lỗi viền đỏ rượu + chữ lỗi | Luôn có nhãn nhìn thấy |
| Chip lọc | Viên thuốc nền trắng, đang chọn: nền ngọc chữ trắng | Lọc theo loại ngọc |
| Lựa chọn (radio) | Hàng trắng bo 10px, đang chọn: viền ngọc + nền ngọc nhạt | Phương thức thanh toán |

Trạng thái bắt buộc cho mọi thành phần tương tác: hover, `:focus-visible` (viền 2px `--kn-jade-deep`, lệch 3px),
disabled, và trạng thái lỗi/trống cho giỏ hàng và thanh toán.

## 6. Hình ảnh

### 6.1 Ảnh hiện có (đã đánh giá)

| Ảnh | Điểm | Dùng |
|---|---|---|
| Vòng Hòa Điền trắng, khay nhung kem | 9/10 về ảnh, **6/10 cho giao diện sáng** | Ngọc và khay đẹp nhưng nền xanh đen tạo khối tối. Dùng tạm với cắt ảnh (mục 3.1); nên chụp lại trên nền sáng |
| Vòng xanh lam trên khay nhung | 8.5/10 ảnh, 6/10 cho giao diện sáng | Như trên. Tem mã số dán trong lòng vòng hơi lộ, nên che hoặc chụp lại |
| Vòng xanh vân hoa | 7.5/10 | Khay nhung xám nhạt hợp giao diện sáng hơn hai ảnh kia; khung gỗ lệch, nên chụp lại cùng bộ |
| Infographic Tân Cương/Thanh Hải | 6/10 | Giữ nội dung chữ, dựng lại bằng HTML. Không đăng nguyên ảnh |
| Infographic cách phối trang sức, người mẫu | 6/10 | Nền kem, hoa sen, vàng rất hợp hướng mới, nhưng người mẫu giống ảnh AI làm giảm niềm tin. Có thể lấy tinh thần (nắng, hoa sen) cho khu cảm hứng nếu dùng ảnh thật |
| Banner "Khang Nguyen - Ngoc Bao" | 5/10 | Họa tiết hồi văn vàng trên nền kem là cảm hứng tốt cho họa tiết trang trí. Không dùng nguyên: chữ trong ảnh bị lỗi, tagline tiếng Anh, sản phẩm là trang sức vàng chứ không phải ngọc |

Các ảnh đã nén nằm ở `docs/design/img/` (hoa-dien-trang.jpg, phi-thuy-xanh-lam.jpg, phi-thuy-xanh-hoa-van.jpg).
Ảnh gốc người dùng chỉ gửi qua chat, chưa được lưu vào repo ở độ phân giải gốc.

### 6.2 Quy chuẩn chụp ảnh mới (ưu tiên cao: thay bộ ảnh nền tối)

- Chụp **ban ngày hoặc ánh sáng mềm tự nhiên**, nền sáng (kem, trắng ngà, hoặc ngọc nhạt), khay nhung kem hoặc vải trắng, khung gỗ sáng. Không nền đen hay xanh đen.
- Ngọc đặt chéo hoặc thẳng, có khoảng thở quanh vật; để thấy độ nhuận, độ trong và vân ngọc dưới ánh sáng.
- Tỷ lệ 4:5 hoặc 1:1 cho ảnh chính; luôn có thêm một ảnh cận chất ngọc và một ảnh đeo thật trên cổ tay.
- Không để tem mã vạch hiện trong ảnh chính.
- Nơi cần ảnh nhưng chưa có: để khung giữ chỗ có kích thước và tỷ lệ cố định kèm ghi chú hướng ảnh. Không vẽ ngọc bằng CSS.
- Mọi ảnh phải có `alt` mô tả bằng tiếng Việt, ví dụ "Vòng ngọc phỉ thúy sắc xanh lam đặt trên khay nhung kem".

## 7. Sơ đồ trang và công thức từng trang

Thiết kế lại toàn bộ web nghĩa là xử lý đủ các trang dưới đây. Ưu tiên theo thứ tự bảng.

| Ưu tiên | Trang | Nơi chứa | Khu vực chính theo thứ tự |
|---|---|---|---|
| 1 | Chi tiết sản phẩm | `pages/product-detail-sample` (tĩnh, mẫu) và `templates/product.html` (động) | Breadcrumb, ảnh lớn + thông tin, accordion, bài kiến thức liên quan, kệ gợi ý, dải kêu gọi, chân trang |
| 1 | Danh mục | `pages/necklaces`, `templates/products.html` | Tiêu đề + bộ lọc, lưới sản phẩm 3 cột liền, đoạn mô tả SEO ngắn, chân trang |
| 1 | Trang chủ | `pages/home`, `templates/home.html` | Sân khấu mở đầu, kệ bộ sưu tập, bài kiến thức nổi bật (hai trang đối diện), dải kêu gọi, chân trang |
| 2 | Giỏ hàng | `templates/cart.html` | Danh sách dòng + tóm tắt đơn |
| 2 | Thanh toán | `templates/checkout.html` | Địa chỉ, giao hàng, thanh toán, xác nhận |
| 2 | Bài chia sẻ về ngọc | tạo trang `pages/` mới theo mẫu bài viết | Tiêu đề + đoạn mở, bảng so sánh, ảnh cận chất liệu, đoạn trích nhấn |
| 3 | Tài khoản, đơn hàng, wishlist, đăng nhập, đăng ký, quên mật khẩu | các template cùng tên | Tối giản, dùng thẻ trắng và thành phần ở mục 5 |
| 3 | Chăm sóc khách hàng, liên hệ, hướng dẫn size, cửa hàng | `pages/customer-service`, `size-guide`, `contact`, `stores` | Kiểu bài viết: ảnh + mảng ngọc nhạt, hoặc thẻ trắng trên nền kem |
| 3 | 404 | `notfound.html` | Một câu ngắn, kèm liên kết về bộ sưu tập |

### 7.1 Sân khấu mở đầu (trang chủ)

Nền kem nắng có quầng vàng nhạt ở góc trên bên phải, cao tối thiểu 700px. Hai cột: bên trái là nhãn nhỏ viết hoa
màu vàng đậm, tiêu đề serif 64px màu ngọc đậm có một cụm in nghiêng vàng, một câu mô tả, nút chính viên thuốc
màu ngọc và một nút chữ ngoặc nhọn. Bên phải là một ảnh ngọc trong khung trắng viền vàng mảnh, bo 20px, tỷ lệ 1:1
(cắt lấy phần khay nhung). Không dùng slider, không dùng nền tối hay ảnh tràn màn hình nền đen, không đặt
tiêu đề giữa trang khổng lồ.

### 7.2 Trang chi tiết sản phẩm (mẫu đã có)

Hai cột 7/5, thông tin bên phải dính khi cuộn. Thứ tự cột phải: nhãn, tên, **giá ngay dưới tên**, dòng "còn 1 chiếc", mô tả ngắn, nút thêm vào giỏ, accordion. Vì mỗi món ngọc là hàng độc nhất, **không có ô số lượng**;
hiển thị "Sản phẩm duy nhất · còn 1 chiếc" và đường kính trong cố định (không phải ô chọn size).
Mục "Cách đo size tay" giúp khách chọn đúng. Nội dung accordion: Thông số, Cách đo size tay, Giao hàng
và đổi trả, Bảo quản ngọc.

### 7.3 Trang bài chia sẻ về ngọc

Dùng khu hai nửa (Sweetgreen): một nửa ảnh, một nửa mảng ngọc nhạt phẳng. Hai nửa bằng nhau: ảnh một bên, chữ một bên, đổi bên qua từng đoạn. Bảng so sánh
HTML cho dữ liệu so sánh. Nội dung chữ của bài "Ngọc Hòa Điền: Tân Cương và Thanh Hải" đã có đủ trong
trang demo, có thể dùng làm bài đầu tiên.

## 8. Nội dung và giọng văn (tiếng Việt)

Trầm, rõ, có hiểu biết, không hô hào. Câu ngắn, danh từ cụ thể về chất liệu. Để tiêu đề mang chút
chất thơ, câu bên dưới nói cụ thể.

| Vai trò | Mẫu | Độ dài |
|---|---|---|
| Tiêu đề mở | "Vòng ngọc phỉ thúy *xanh lam*" | 3 đến 8 từ, một cụm in nghiêng |
| Mô tả | "Sắc xanh lam dịu, bề mặt trơn bóng, dáng vòng đầy đặn." | 1 câu, 10 đến 25 từ |
| Nút chính | "Thêm vào giỏ", "Thanh toán" | 2 đến 3 từ |
| Nút chữ ngoặc nhọn | "{ TƯ VẤN CHỌN VÒNG }" | 2 đến 4 từ |
| Lời kêu gọi cuối trang | "Chọn chiếc vòng *hợp với cổ tay* của bạn" | 6 đến 10 từ |
| Khẩu hiệu chân trang | "Trang sức ngọc phong thủy" | 3 đến 5 từ |

Nên dùng: ngọc, phỉ thúy, Hòa Điền, độ nhuận, độ trong, vân, chất ngọc, nhẫn, vòng, chuỗi.
Tránh: "siêu hot", "giá sốc", "đột phá", "cam kết 100%" và mọi lời hứa pháp lý (ví dụ "hàng thật 100%")
khi chưa có giấy tờ chứng minh. Mọi con số, chính sách, giá trên trang hiện tại là **dữ liệu mẫu**.

## 9. Cảnh báo về nội dung hiện có

`data/seed/SAMPLE-DATA.txt` ghi rõ tên sản phẩm, giá và ảnh trong dữ liệu mẫu được lấy từ các trang
công khai của một thương hiệu khác, chỉ dùng cho bản demo cục bộ, không dùng cho sản xuất. Khi thiết kế
lại:

- Không đưa tên sản phẩm, mô tả, giá, ảnh hay tên thương hiệu của thương hiệu đó vào bản thật.
- Trong code trang `home`, `necklaces`, `product-detail-sample` đang có tên và nội dung của dữ liệu đó. Thay hết bằng nội dung của shop.
- Tên thương hiệu hiển thị cho khách: **Khang Nguyên · Ngọc Bảo**. Có dấu tiếng Việt đầy đủ.
- Chủ shop có trang Facebook với nhiều bài về ngọc. Chưa đọc được nội dung (cần đăng nhập). Hỏi chủ shop xuất bài viết hoặc ảnh thay vì tự sao chép.

## 10. Chuyển động và responsive

### 10.1 Chuyển động

Tối giản. Chuyển màu nút 0.15s; hover mờ chữ 0.3s; không hiệu ứng nảy. Tôn trọng
`prefers-reduced-motion` (đã có trong demo: tắt `scroll-behavior` và mọi transition).

### 10.2 Breakpoint

| Ngưỡng | Thay đổi |
|---|---|
| ≤ 1023px | Cột sản phẩm xếp dọc; thông tin hết dính; tiêu đề giảm xuống 44px; hai nửa bài viết xếp dọc |
| ≤ 767px | Padding 16px; ẩn liên kết điều hướng giữa; kệ 1 cột; bảng so sánh xếp thành khối; nút viên thuốc xếp dọc |

Đã kiểm tra trang demo ở 390px: không tràn ngang. Các trang khác cần kiểm tra lại sau khi làm.

## 11. Truy cập (accessibility)

- Tất cả chữ thường đạt tương phản AA theo bảng 4.2.
- `:focus-visible` luôn hiện. Không bỏ outline.
- Dùng thẻ ngữ nghĩa: `nav`, `main`, `header`, `footer`, `details/summary`, bảng có `th scope`.
- Vùng thông báo thêm vào giỏ dùng `role="status"` và `aria-live="polite"`.
- Ảnh có `alt` tiếng Việt cụ thể. Con dấu SVG trang trí dùng `aria-hidden="true"`.
- Vùng bấm tối thiểu 44px chiều cao cho nút chính.

## 12. Cách làm trên EzyPlatform

1. Bắt đầu bằng skill `ezy-guide` (nằm ở plugin `ezyplatform`, định tuyến sang `ezy-author`, `ezy-media`,
   `ezy-publish`, `ezy-verify`). Đọc `my-site/AGENTS.md` trước khi sửa trang.
2. CSS dùng chung đặt ở `page-fragments/ecommerce/theme/head.html` **và** `ecommerce-theme/.../static/css/main.css`
   (hai nơi, phải khớp). Trang riêng có thể thêm `<style>` trong `content.html` như trang demo.
3. `content.html` của trang là tài liệu HTML đầy đủ, gọi fragment bằng
   `<ezy:block ezy:replace="~{ecommerce/header :: content}" />`. Tránh chuỗi `[[` và `${` trong văn bản vì Thymeleaf sẽ xử lý.
4. Ảnh phải tải lên bằng `ezy-media` trước khi publish; trong repo chỉ giữ bản xem thử ở `img/`.
5. Mọi thao tác ghi lên server (publish, tải media) phải chạy dry run trước và cần người duyệt rõ ràng. **Không publish khi chưa được duyệt.**
6. Trước khi chạy dev server hoặc preview, kiểm tra cổng đã bị chiếm chưa và dừng tiến trình cũ (xem `.claude/rules/process-management.md`).
7. Không đặt mã kế hoạch hay nhãn audit vào comment trong code.
8. Không in hay lưu `my-site/environment.json` (chứa token admin).

## 13. Phạm vi và thứ tự đề xuất cho agent thiết kế lại

1. Xác nhận hướng thiết kế sáng (mục 3) với chủ shop nếu có thay đổi; ghi nhận rằng ảnh sản phẩm nền tối hiện chỉ là giải pháp tạm.
2. Đổi token và font ở hai nơi (mục 2.2), đổi tiền tố sang `kn-`; cập nhật `ecommerce.js` nếu đổi class/id.
3. Làm fragments: `header`, `footer`, `category-strip`, rồi trang `home`.
4. Làm trang danh mục và trang chi tiết sản phẩm (cả bản tĩnh và template động).
5. Làm giỏ hàng, thanh toán; chạy luồng thêm vào giỏ → giỏ → thanh toán.
6. Làm khu bài viết về ngọc và các trang phụ.
7. Chạy kiểm tra: desktop 1440, tablet 768, mobile 390; bàn phím; tương phản; không tràn ngang.
8. Dry run publish, trình chủ shop duyệt, rồi mới publish.

### 13.1 Tiêu chí hoàn thành

- [ ] `python3 -I docs/design/check-design.py <các file đã sửa>` trả về 0 lỗi.
- [ ] Giao diện render ra khớp ảnh chụp trong `docs/design/screenshots/` ở cùng độ rộng (so bằng mắt từng khu vực, ghi lại điểm khác biệt có chủ đích).
- [ ] Không còn token `--sw-*`, font Helvetica/Georgia hay nội dung của thương hiệu cũ trong code.
- [ ] Mọi trang trong mục 7 dùng chung token, thành phần, ngôn ngữ thị giác ở tài liệu này.
- [ ] Tiếng Việt hiển thị đủ dấu ở mọi cỡ chữ.
- [ ] Luồng thêm vào giỏ, giỏ hàng, thanh toán chạy với sản phẩm thật trong DB.
- [ ] Không có số liệu, chính sách hay chứng nhận bịa; dữ liệu mẫu phải được gắn nhãn hoặc thay.
- [ ] Mobile 390px không tràn ngang; focus bàn phím nhìn thấy; tương phản đạt AA.
- [ ] Không có lời kêu gọi giảm giá ồn ào; màu vàng chỉ dùng đúng vai trò.

## 14. Bộ tham chiếu và cách bảo đảm làm đúng spec

Chỉ đọc tài liệu này là chưa đủ. Spec được thực thi bằng ba lớp: file mẫu chạy được, ảnh chụp, và script
kiểm tra tự động.

### 14.1 File trong `docs/design/`

| File | Vai trò |
|---|---|
| `kn.css` | Stylesheet chuẩn: token trong `:root` và mọi thành phần `kn-*`. **Nguồn duy nhất.** |
| `style-guide.html` | Mọi token, thang chữ, nút (4 trạng thái), ô nhập (mặc định, focus, lỗi, vô hiệu), chip, accordion, bảng, ô lưới, lỗi, trống, dòng giỏ hàng, bo góc, khoảng cách, nên/không nên |
| `home.html`, `category.html`, `product.html`, `cart.html`, `checkout.html` | Năm trang mẫu đầy đủ (trang chủ, danh mục, chi tiết sản phẩm, giỏ hàng, thanh toán) |
| `screenshots/*.jpg` | Ảnh chụp năm trang mẫu ở desktop (1440px) và mobile (390px) để đối chiếu mắt |
| `check-design.py` | Script kiểm tra tự động (xem 14.2) |
| `inline-css.py` | Đóng gói trang tham chiếu thành `content.html` tự chứa CSS để đăng lên EzyPlatform |
| `img/` | Ảnh sản phẩm dùng cho các trang mẫu |

Xem các trang mẫu cục bộ: chạy một server tĩnh trong `docs/design/` (ví dụ `python3 -m http.server 8765 --bind 127.0.0.1`),
mở `style-guide.html`. Trước khi chạy hãy kiểm tra cổng 8765 và dừng tiến trình cũ; dừng server khi xong việc.

### 14.2 Script kiểm tra

```bash
python3 -I docs/design/check-design.py                       # kiểm tra bộ tham chiếu
python3 -I docs/design/check-design.py my-site/pages/<slug>  # kiểm tra trang đã làm
```

Script trả về mã thoát 1 nếu có lỗi. Các lỗi nó bắt:

| Quy tắc | Ví dụ bị chặn |
|---|---|
| Mã hex chỉ được nằm trong `:root`, và phải là màu đã có trong `kn.css` | `color:#6366f1`, `#bc9c5c` viết thẳng |
| Màu `rgb()` chỉ là trắng, đen hoặc màu token; cấm `hsl()` | `rgba(99,102,241,.4)` |
| Không `box-shadow` | `box-shadow:0 4px 12px ...` |
| Bo góc chỉ trong 0, 10px, 20px, 24px, 50%, 9999px | `border-radius:16px`, `6px` |
| Không nền tối (`background` là `black`, `--kn-ink`, `--kn-body`) | `background:black` |
| Cấm font ngoài hai font đã chọn | Bodoni, Helvetica, Georgia, Inter, Roboto, Playfair |
| Không bỏ `outline` | `outline:none` |
| Mọi custom property dùng tiền tố `--kn-` và phải có trong `kn.css` | `--brand`, `--sw-pad` |
| Không còn tên cũ `sw-`, `swp-`, `swd-` | `class="sw-btn"` |
| `<html lang="vi">`, có viewport, mọi `<img>` có `alt` | thiếu `alt` |
| Cấm cụm từ khuyến mãi ồn ào và lời hứa pháp lý | "siêu hot", "giá sốc", "cam kết 100%" |
| Không `style=""` trong `my-site/` (ở nơi khác chỉ cảnh báo) | `style="color:red"` |
| Không hex trong thuộc tính SVG | `stroke="#bc9c5c"`; dùng `currentColor` |
| Cảnh báo khi dùng vàng hoặc ngọc đậm làm nền | `background:var(--kn-gold)`, `background:var(--kn-jade-deep)` |

Script đã được thử: bộ tham chiếu đạt 0 lỗi; một file cố ý vi phạm bị bắt 15 lỗi; fragment cũ
`page-fragments/ecommerce/theme/head.html` bị bắt 113 lỗi (đúng, vì nó chính là thứ cần thay).
Script không kiểm tra được bố cục, nhịp, hay "cảm giác"; phần đó phải đối chiếu mắt với ảnh chụp.

### 14.3 Quy trình bắt buộc cho Agent

1. Mở `style-guide.html` và năm trang mẫu, xem ảnh chụp, rồi mới sửa mã.
2. Chỉ dùng token và class `kn-*` có sẵn. Cần cái mới thì thêm vào `kn.css` **và** `style-guide.html` trong cùng thay đổi, kèm lý do.
3. Với mỗi trang: dựng trang tham chiếu (hoặc sửa template) rồi chạy `check-design.py`. Lỗi phải bằng 0.
4. Trang trong `my-site/pages/`: dựng bằng `inline-css.py`, ví dụ  
   `python3 -I docs/design/inline-css.py docs/design/product.html my-site/pages/vong-ngoc-demo/content.html home.html=/home category.html=/necklaces product.html=/vong-ngoc-demo`  
   Không sửa tay `content.html` đã sinh; sửa trang tham chiếu rồi sinh lại.
5. Template động của theme (`ecommerce-theme/.../templates/*.html`) dùng cùng class `kn-*` và `kn.css` (đưa nội dung `kn.css` vào `static/css/main.css` và `page-fragments/ecommerce/theme/head.html`).
6. Render ở 1440px và 390px, so với ảnh chụp, sửa điểm khác biệt không có chủ đích.
7. Báo cáo cuối: liệt kê file đã sửa, kết quả `check-design.py`, và mọi chỗ lệch spec có chủ đích kèm lý do. Lệch spec mà không có lý do là lỗi.

### 14.4 Điều script KHÔNG chặn được (Agent tự chịu trách nhiệm)

- **Giá luôn nằm ngay dưới tên sản phẩm**, ở mọi nơi hiển thị sản phẩm (thẻ, giỏ, đơn, wishlist, tóm tắt thanh toán, trang chi tiết). Không đặt giá ở cột bên phải cùng hàng với tên, không để mô tả chen giữa tên và giá (trang chi tiết: tên, giá, số lượng còn, rồi mới đến mô tả).
- Chỉ dùng một nút chính mỗi khu; vàng chỉ cho họa tiết, đường kẻ, viền, huy hiệu và chữ vàng đậm; mỗi tiêu đề chỉ một cụm in nghiêng có nghĩa.
- Không đưa lại nền tối: kể cả khi ảnh sản phẩm có nền tối, đặt trong khung trắng và cắt ảnh, không tô nền trang bằng màu tối.
- Ảnh phải là ảnh sản phẩm thật theo quy chuẩn mục 6; không vẽ ngọc bằng CSS, không dùng ảnh AI làm ảnh chính.
- Không bịa số liệu, chính sách, chứng nhận. Dữ liệu mẫu phải có `data-demo="true"` hoặc dòng chú thích.
- Luồng giỏ hàng, thanh toán phải chạy thật với sản phẩm trong DB sau khi đổi class/id (xem 14.5).

### 14.5 Hợp đồng với `ecommerce.js` (không được làm hỏng)

`ecommerce-theme/.../static/js/ecommerce.js` đọc các id và tên trường dưới đây, và tự dựng thêm DOM với class `sw-*`.
Trang mẫu `cart.html`, `checkout.html`, `product.html` đã giữ nguyên các id và tên này. Khi đổi sang `kn-*`:
đổi class trong template **và** trong đoạn JS tạo DOM, hoặc giữ id và chỉ đổi class; sau đó chạy lại cả luồng.

| Trang | Phải giữ nguyên | JS tự dựng (phải đổi class sang `kn-*`) |
|---|---|---|
| Chi tiết sản phẩm (`product.html`) | `swMain`, `swBcCur`, `swPdp`, `swPdpImg`, `swPdpCode`, `swPdpName`, `swPdpPrice`, `swPdpStock`, `swPdpSwatches`, `swPdpSwatchList`, `swPdpColor`, `swPdpQty`, `swAddBtn`, `swWishBtn`, `swAddNote`, `swPdpDesc`, `swPdpDescText`, thuộc tính `data-qty-dec`, `data-qty-inc`, `data-sw-state` | Khối trạng thái trống/lỗi, ô chọn màu |
| Giỏ hàng (`cart.html`) | `swMain`, `swCart`, `swLines`, `swSubtotal`, `data-sw-state` | Mỗi dòng giỏ: `li.sw-line`, `.sw-img`, `.sw-qty`, `.sw-rm`, `.sw-line-end`, `.sw-pcode` |
| Thanh toán (`checkout.html`) | `swCheckout`, `swCheckoutForm`, các tên trường `deliveryFullName`, `deliveryEmail`, `deliveryPhoneNumber`, `deliveryAddress`, `deliveryCityName`, `deliveryPostalCode`, `deliveryCountryId` (id `swCountry`), `paymentServiceCode` (id `swPay`), `swFormErr`, `swPlace`, `swMini`, `swTotal` | Các `option` của quốc gia và thanh toán, các dòng `swMini` |

Lưu ý thiết kế liên quan:

- Mỗi món ngọc là hàng độc nhất. Giao diện mẫu **không có ô số lượng**. Mã hiện tại có nút cộng trừ số lượng và đã chặn theo tồn kho (`product.amount`); khi tồn kho là 1, ẩn bộ chọn số lượng thay vì hiện cộng trừ.
- Form thanh toán đòi `deliveryPostalCode`; giá trị mặc định là của Việt Nam (số điện thoại `+84`, quốc gia Việt Nam đứng đầu và chọn sẵn). Xác nhận với chủ shop xem mã bưu chính có cần bắt buộc không trước khi đổi logic kiểm tra.
- Mẫu hiển thị phương thức thanh toán dạng radio. Mã hiện tại dùng `select#swPay` do API đổ vào. Có thể giữ `select` (kiểu `kn-input`) hoặc đổi sang radio `kn-choice`; nếu đổi, sửa đoạn JS đọc `swPay`.
- Nhóm accordion hiện tại do JS bật tắt qua `aria-expanded`; mẫu dùng thẻ `details`. Khi chuyển, xóa trình xử lý cũ trong `ecommerce.js` để không xung đột.
- Dữ liệu trong mẫu (giá, tên, phương thức thanh toán) là mẫu. Đừng giữ lại trong sản phẩm thật.

## 15. Câu hỏi còn mở (cần chủ shop trả lời)

1. Các vòng thuộc loại ngọc nào chính xác (phỉ thúy, Hòa Điền Tân Cương, Hòa Điền Thanh Hải)? Hiện tên sản phẩm trong demo đặt theo màu sắc.
2. Phương thức thanh toán nào được hỗ trợ (chuyển khoản, thẻ, COD)? Ảnh hưởng thiết kế trang thanh toán.
3. Chính sách giao hàng, đổi trả, bảo hành thật là gì? Nội dung trong demo là chỗ giữ chỗ.
4. Có đồng ý bỏ hẳn người mẫu AI khỏi khu bán hàng, và có sẵn ảnh đeo thật không?
5. Có muốn thêm tiếng Anh không? Theme đã có `messages_vi.properties` và `messages.properties`.
6. Chủ shop có thể cung cấp nội dung các bài chia sẻ về ngọc (xuất từ Facebook) để làm khu kiến thức không?

## 16. Nguồn tham chiếu

| Nguồn | Dùng cho |
|---|---|
| Refero style Sweetgreen, id `d59df660-8f1a-438f-9c50-f210b2eeb229` | Nền tảng: nền kem nắng, mảng phẳng, nút viên thuốc, thẻ không khung, hai nửa ảnh + mảng màu, chân trang thư mục |
| Refero style Franco Maria Ricci, id `2c3b31a4-6e28-47a0-afbd-cb22e22e34fb` | Vàng cổ, nút chữ ngoặc nhọn, con dấu, đường kẻ mảnh |
| Refero style Zeus Jones, id `6151c9d4-5059-4ae0-9007-d9128207ec65` | Kem ấm, serif văn chương, cụm in nghiêng, ảnh bo 20px |
| Refero style Slash, id `350e0391-9d87-4830-8d2b-826b3e9fc795` | **Đã loại bỏ** (hướng nền đen, khách không chấp nhận). Chỉ giữ để tham khảo lịch sử |
| Refero screens: Farfetch (trang đồng hồ), Saint Laurent (nhiều trang sản phẩm) | Bố cục trang chi tiết sản phẩm |
| `docs/design/` | Bộ tham chiếu chạy được: `kn.css`, `style-guide.html`, `home.html`, `category.html`, `product.html`, ảnh chụp trong `screenshots/` (mở trước khi làm) |
| `.claude/skills/refero-design/` | Phương pháp nghiên cứu và reference lock |

Có thể lấy lại toàn bộ chi tiết hai style bằng `refero_get_style` với id ở trên khi cần kiểm tra token gốc.
