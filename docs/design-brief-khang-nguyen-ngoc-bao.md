# Design Brief: Khang Nguyên · Ngọc Bảo (storefront ngọc phong thủy)

Tài liệu này là nguồn thiết kế duy nhất để một agent khác thiết kế lại toàn bộ UI/UX của website.
Đọc hết mục 0 đến 3 trước khi làm bất cứ việc gì. Mọi quyết định thiết kế ở đây đều có nguồn
(yêu cầu của chủ shop, research Refero, hoặc quan sát từ repo). Nếu bạn muốn đi ngược lại một
quyết định, hãy nêu lý do và hỏi lại người giao việc thay vì tự đổi.

Ngày lập: 2026-11-22. Trạng thái: bản nháp đầu, đã có 1 trang demo chạy được.

---

## 0. Tóm tắt một đoạn

Website bán trang sức ngọc (vòng, nhẫn, chuỗi) cho khách Việt, có giỏ hàng và thanh toán trực tiếp
trên web. Cảm giác cần đạt: cổ điển phương Đông, sang trọng, tin cậy, lấy ngọc xanh lá làm trung
tâm, vàng cổ làm điểm nhấn, đỏ rượu dùng rất ít. Ảnh sản phẩm thật và các bài chia sẻ kiến thức về
ngọc là hai thứ quan trọng nhất. Thiết kế dựa trên hai reference Refero: **Slash** (sân khấu đen,
serif, ánh vàng champagne) cho khu bán hàng, và **Franco Maria Ricci** (giấy, mực đen, vàng cổ,
đường kẻ mảnh) cho khu bài viết.

## 1. Brief

```text
Designing một storefront ngọc phong thủy cho khách Việt (mua online, thanh toán trên web), web desktop + mobile.
Goal: khách tin ngọc là thật, thấy rõ chất ngọc qua ảnh, thêm vào giỏ và thanh toán không vướng.
Tone: cổ điển phương Đông, trầm, sang, ít chữ, nhiều ảnh. Không rực rỡ, không khuyến mãi ồn ào.
Main objection: "Ngọc này có thật không, giá có xứng không?" (khách Việt rất thận trọng với ngọc)
Must remember: ảnh vòng ngọc trên nền đen + khay nhung kem, và bảng so sánh Tân Cương/Thanh Hải.
Constraints: chạy trên EzyPlatform (page + page fragment, Thymeleaf), font phải có đủ dấu tiếng Việt,
  chưa có giấy kiểm định riêng cho từng món, ảnh hiện có rất ít.
Research: styles (Slash, Franco Maria Ricci), screens (Farfetch, Saint Laurent product detail).
```

### 1.1 Người dùng và mục tiêu

| Mục | Nội dung | Nguồn |
|---|---|---|
| Khách hàng | Người Việt, mua ngọc để đeo, tặng, hoặc theo phong thủy | Chủ shop |
| Cảm giác mong muốn | Cổ điển phương Đông; màu ngọc xanh lá, vàng, đỏ | Chủ shop |
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

```text
Primary reference/direction: Slash — "serif in the shadows" (style id 350e0391-9d87-4830-8d2b-826b3e9fc795)
  cho toàn bộ khu bán hàng: trang chủ, danh mục, chi tiết sản phẩm, giỏ, thanh toán.
Secondary (có vai trò riêng): Franco Maria Ricci — "the collector's cabinet" (style id 2c3b31a4-6e28-47a0-afbd-cb22e22e34fb)
  chỉ cho khu nội dung: bài chia sẻ về ngọc, trang "câu chuyện", chân trang (colophon), ô lưới có đường kẻ.
Preserve: canvas gần đen; serif tương phản cao weight 400 cho tiêu đề, italic thật cho 1 cụm từ có nghĩa;
  vàng chỉ dùng cho điểm số/giá và chi tiết kim loại; viền kính (masked rim) quanh khung ảnh;
  nút chính nền trắng trên nền tối; "dải chân trời ấm" ở cuối trang.
Borrow only: (1) nút dạng chữ có ngoặc nhọn { ... } từ Franco; (2) con dấu vàng vẽ tay ở chân trang.
Role rules: vàng KHÔNG làm nền khối hay nút chính; đỏ rượu chỉ là đường kẻ nhấn/dấu ấn nhỏ;
  xanh lục đậm chỉ ở dải nền chuyển màu và con dấu, KHÔNG phải màu nút.
Media strategy: ảnh sản phẩm thật trên nền đen + khay nhung kem. Không dùng ảnh minh họa AI cho sản phẩm.
Reject: nền be/kem làm nền chính toàn site; serif Bodoni (không có dấu tiếng Việt);
  gradient tím/indigo; thẻ bo tròn có bóng; banner giảm giá nhiều màu.
```

### 3.1 Vì sao dùng nền đen cho sản phẩm

Ảnh sản phẩm hiện có đều chụp trên nền xanh đen sẫm (khay nhung kem, khung gỗ). Nền đen của site
hòa vào nền ảnh nên ngọc nổi hẳn lên. Đây là lợi thế sẵn có, hãy giữ. Khi chụp ảnh mới, giữ đúng
bộ cục này (xem mục 6).

### 3.2 Decision ledger

| Quyết định | Nguồn | Vai trò cần giữ | Lý do |
|---|---|---|---|
| Canvas `#05070b` | Slash (canvas `#040406`), chỉnh nhẹ sang xanh đen theo nền ảnh | Nền trang | Hòa với nền ảnh thật |
| Serif tiêu đề, italic cho 1 cụm | Slash typography | Tiêu đề lớn | Cảm giác tạp chí, sang |
| Cormorant Garamond thay IvyPresto/Bodoni | Kiểm tra metadata Google Fonts | Font tiêu đề | Có Vietnamese subset; Bodoni Moda thì không |
| Vàng `#bc9c5c` | Franco (antique gold) + Slash (brushed gold) | Giá, nhãn nhỏ, con dấu | Giữ vàng hiếm để quý |
| Nền giấy cho khu bài viết | Franco | Trang đọc dài | Dễ đọc, tạo nhịp tương phản |
| Bảng so sánh bằng HTML | Chủ shop cần bài chia sẻ | Nội dung kiến thức | Infographic ảnh nhỏ, khó đọc trên mobile |
| Bố cục chi tiết sản phẩm: ảnh lớn bên trái, thông tin bên phải, accordion | Farfetch, Saint Laurent | Trang sản phẩm | Mẫu luxury đã được kiểm chứng |
| Không có khu "chứng nhận" | Chủ shop (chưa có kiểm định) | Trang sản phẩm | Không hứa điều chưa có |

## 4. Design tokens

Đặt trong `:root`. Đây là bộ token đã dùng trong trang demo, đã kiểm tra tương phản.

Nguồn duy nhất của token là khối `:root` trong **`docs/design/kn.css`**. Không sao chép giá trị
vào tài liệu khác và không tạo token mới ngoài file đó. Nhóm token:

| Nhóm | Token |
|---|---|
| Nền tối | `--kn-canvas`, `--kn-panel`, `--kn-edge`, `--kn-rule` |
| Chữ và điều khiển trên nền tối | `--kn-black`, `--kn-white`, `--kn-text`, `--kn-body`, `--kn-muted`, `--kn-outline`, `--kn-btn-hover` |
| Vàng, ngọc, đỏ rượu | `--kn-gold`, `--kn-gold-light`, `--kn-gold-ink`, `--kn-emerald`, `--kn-crimson`, `--kn-danger-text` |
| Dải chân trời ấm | `--kn-horizon-mid`, `--kn-horizon-end` |
| Khu bài viết | `--kn-paper`, `--kn-paper-ink`, `--kn-paper-muted` |
| Font và bố cục | `--kn-serif`, `--kn-sans`, `--kn-pad`, `--kn-wrap` |


### 4.1 Vai trò của từng màu (không đổi nghĩa)

| Token | Dùng cho | KHÔNG dùng cho |
|---|---|---|
| `--kn-canvas` | Nền trang | Nền nút |
| `--kn-white` | Tiêu đề quan trọng, nút chính (nền trắng chữ đen) | Chữ đoạn dài |
| `--kn-body` | Đoạn mô tả | Tiêu đề |
| `--kn-muted` | Metadata, nhãn phụ | Thông tin bắt buộc phải đọc |
| `--kn-gold` | Giá, nhãn nhỏ viết hoa, con dấu, huy hiệu số lượng giỏ | Nền khối lớn, nút chính |
| `--kn-gold-light` | Điểm sáng trong giá gradient, thông báo ngắn | Chữ dài |
| `--kn-gold-ink` | Chữ vàng trên nền giấy (đậm hơn để đủ tương phản) | Trên nền tối |
| `--kn-emerald` | Dải chuyển màu dưới cùng, mảng nền nhỏ gắn với ngọc | Nút, viền |
| `--kn-crimson` | Đường kẻ nhấn trái của đoạn trích; con dấu | Nền khối, nút |
| `--kn-paper` / `--kn-paper-ink` | Khu bài viết và chân trang | Khu bán hàng |

### 4.2 Tương phản (đã tính xấp xỉ)

| Cặp màu | Tỷ lệ | Đạt WCAG AA cho chữ thường |
|---|---|---|
| `--kn-gold` trên `--kn-canvas` | khoảng 7.7 : 1 | Có |
| `--kn-muted` trên `--kn-canvas` | khoảng 6.1 : 1 | Có |
| `--kn-gold-ink` trên `--kn-paper` | khoảng 5.3 : 1 | Có |
| `#bc9c5c` trên `#f5f2ea` | khoảng 2.4 : 1 | **Không**. Không dùng vàng sáng làm chữ trên nền giấy |

### 4.3 Typography

Font phải có Vietnamese subset (đã xác minh bằng metadata Google Fonts ngày 2026-11-22):

| Font | Vai trò | Vietnamese | Ghi chú |
|---|---|---|---|
| Cormorant Garamond 400, 500, italic 400 | Tiêu đề, trích dẫn, tên sản phẩm | Có | Nét mảnh, hiển thị nhỏ hơn font khác ở cùng cỡ, nên tăng khoảng 10% |
| Be Vietnam Pro 300, 400, 500, 600 | Giao diện, nút, giá, bảng | Có | Thiết kế cho tiếng Việt |
| Bodoni Moda | KHÔNG dùng | Không | Thiếu glyph tiếng Việt |
| Playfair Display, Noto Serif | Phương án dự phòng cho serif | Có | Chỉ dùng nếu Cormorant không đạt |

Thang chữ (desktop, mobile ghi trong ngoặc):

| Recipe | Font | Cỡ | Line-height | Dùng cho |
|---|---|---|---|---|
| display | serif 400 | 56 (36) | 1.05 | Tên sản phẩm lớn, mở đầu |
| chapter | serif 400 | 64 (36) | 1.12 | Lời kêu gọi cuối trang |
| heading | serif 400 | 48 (36) | 1.1 | Tiêu đề khu bài viết |
| card-title | serif 400 | 24 | 1.15 | Tên sản phẩm trong ô lưới |
| lead | sans 300 | 17 đến 19 | 1.55 | Đoạn mô tả đầu |
| body | sans 300 hoặc 400 | 15 đến 16 | 1.6 | Nội dung |
| price | sans 500 | 34 | 1 | Giá (gradient vàng) |
| eyebrow | sans 600 | 12, viết hoa | 1 | Nhãn nhỏ phía trên tiêu đề |
| action | Arial 700 | 13 đến 14, viết hoa | 1.25 | Nút chữ có ngoặc nhọn |

Quy tắc: tracking của serif là `+0.01em` (không âm). Dùng italic thật cho đúng một cụm có nghĩa trong
tiêu đề (ví dụ "xanh lam", "hợp với cổ tay"), không đổi màu, không dùng chữ thảo. Dùng
`text-wrap: balance` cho tiêu đề.

### 4.4 Khoảng cách, bố cục, bo góc

| Giá trị | Dùng cho |
|---|---|
| 8, 12, 16 | Khoảng cách trong thành phần nhỏ |
| 24 | Padding trong ô, khoảng cách lưới 3 cột |
| 32 | Padding hai bên khung nội dung |
| 48, 64 | Khoảng cách giữa các nhóm, khoảng cách giữa ảnh và thông tin |
| 96 | Khoảng cách giữa các khu lớn |
| 160, 224 | Đầu và cuối dải kêu gọi (chân trời ấm) |

- Khung nội dung tối đa 1216px, padding 32px (mobile 16px).
- Khu bài viết kiểu hai trang đối diện: hai nửa bằng nhau, tối đa 1440px, ảnh một nửa, chữ một nửa.
- Bo góc: khung ảnh/ô trưng bày 10px; nút chính 6px; nút kêu gọi dạng viên thuốc 9999px; ô lưới có
  đường kẻ 0px; không bo thêm chỗ khác.

### 4.5 Bề mặt và độ sâu

| Bề mặt | CSS | Dùng cho |
|---|---|---|
| Canvas | `background:#05070b` | Nền trang |
| Ô trưng bày | `linear-gradient(201deg,#0c0f15 14%,#0b0c0e)`, bo 10px | Khung chứa ảnh |
| Viền kính | lớp phủ `border:1px solid rgba(255,255,255,.6)`, `mix-blend-mode:overlay`, mask mờ dần xuống dưới | Viền sáng mảnh quanh ảnh |
| Kính điều hướng | `rgba(5,7,11,.72)` + `backdrop-filter:blur(24px)` + viền dưới `--kn-edge` | Thanh điều hướng dính trên đầu |
| Chân trời ấm | `linear-gradient(transparent, #1d2a22 70%, #2e2a26)` | Dải kêu gọi cuối trang |
| Giấy | `background:#f5f2ea` | Khu bài viết |
| Chân trang trắng | `background:#fff` | Colophon |

Không dùng bóng đổ cho thẻ. Độ sâu đến từ ảnh và viền sáng, không đến từ box-shadow.

## 5. Thành phần (components)

Mã chạy được: class `kn-*` trong `docs/design/kn.css`, minh họa đủ trạng thái ở `docs/design/style-guide.html`. Thành phần nào chưa có thì thêm vào `kn.css` và `style-guide.html` trước rồi mới dùng.

| Thành phần | Mô tả ngắn | Ghi chú |
|---|---|---|
| Thanh điều hướng | Cao 64px, dính trên đầu, logo con dấu nhỏ + tên, 4 liên kết giữa, nút giỏ dạng viên thuốc với huy hiệu số lượng vàng | Mobile: ẩn liên kết giữa, thêm nút menu |
| Nút chính | Nền trắng, chữ đen, cao 52px, bo 6px, hover `#cfd1d8` | Một nút chính mỗi khu |
| Nút chữ ngoặc nhọn | `{ TƯ VẤN CHỌN VÒNG }`, Arial 700 viết hoa, hover opacity .7 | Hành động phụ, ngoặc là bắt buộc |
| Nút viên thuốc | Cặp nút lồng trong khung kính mờ | Chỉ ở dải kêu gọi cuối trang |
| Khung ảnh sản phẩm | Ô trưng bày + viền kính, tỷ lệ 3:4 | Ảnh `object-fit:cover` |
| Giá | Sans 500, gradient vàng sáng-tối-sáng | Chỉ ở giá, không dùng ở tiêu đề |
| Accordion thông tin | Dùng thẻ `details/summary` gốc, dấu + và − | Không cần JS |
| Bảng so sánh | Bảng HTML thật trên nền giấy, đường kẻ 1px mực đen; mobile xếp thành khối, nhãn cột hiện bằng `data-k` | Thay hẳn infographic ảnh |
| Đoạn trích nhấn | Serif italic, viền trái 2px `--kn-crimson` | Câu lưu ý quan trọng |
| Kệ sản phẩm | Lưới 3 cột liền nhau, đường kẻ 1px `--kn-edge`, ô vuông, tên serif + giá vàng | Mobile: 1 cột |
| Dải kêu gọi cuối trang | Chân trời ấm, tiêu đề serif 64px, cặp nút viên thuốc | Luôn có ở trang chủ và trang sản phẩm |
| Chân trang (colophon) | Con dấu vàng 132px, tên serif italic vàng, câu khẩu hiệu, rồi 3 cột liên kết, rồi dòng pháp lý nhỏ | Con dấu là SVG tự vẽ, không dùng logo thương hiệu khác |

Trạng thái bắt buộc cho mọi thành phần tương tác: hover, `:focus-visible` (viền 2px `--kn-gold`, lệch 3px),
disabled, và trạng thái lỗi/trống cho giỏ hàng và thanh toán.

## 6. Hình ảnh

### 6.1 Ảnh hiện có (đã đánh giá)

| Ảnh | Điểm | Dùng |
|---|---|---|
| Vòng Hòa Điền trắng, khay nhung kem, nền đen | 9/10 | Ảnh chuẩn. Dùng ở trang sản phẩm, bài kiến thức, kệ sản phẩm |
| Vòng xanh lam trên khay nhung | 8.5/10 | Dùng. Tem mã số dán trong lòng vòng hơi lộ, nên che hoặc chụp lại |
| Vòng xanh vân hoa | 7.5/10 | Màu đẹp nhất nhưng khay xám nhạt, khung gỗ lệch, không đồng bộ. Chụp lại theo khay kem |
| Infographic Tân Cương/Thanh Hải | 6/10 | Giữ nội dung chữ, dựng lại bằng HTML. Không đăng nguyên ảnh |
| Infographic cách phối trang sức, người mẫu | 6/10 | Ảnh người mẫu giống ảnh AI, làm giảm niềm tin. Chỉ dùng ở khu cảm hứng, nếu giữ |
| Banner "Khang Nguyen - Ngoc Bao" | 5/10 | Lấy bảng màu. Không dùng nguyên: chữ trong ảnh bị lỗi, tagline tiếng Anh, sản phẩm là trang sức vàng chứ không phải ngọc |

Các ảnh đã nén nằm ở `my-site/pages/vong-ngoc-demo/img/` (hoa-dien-trang.jpg, phi-thuy-xanh-lam.jpg,
phi-thuy-xanh-hoa-van.jpg). Ảnh gốc người dùng chỉ gửi qua chat, chưa được lưu vào repo ở độ phân giải gốc.

### 6.2 Quy chuẩn chụp ảnh mới

- Nền đen xanh, khay nhung kem có khung gỗ, ngọc đặt chéo, ánh sáng mềm từ trên xuống. Giữ đúng bộ này cho cả bộ sưu tập.
- Tỷ lệ 3:4 cho ảnh chính; luôn có thêm một ảnh cận chất ngọc và một ảnh đeo thật trên cổ tay.
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
| 3 | Tài khoản, đơn hàng, wishlist, đăng nhập, đăng ký, quên mật khẩu | các template cùng tên | Tối giản, dùng chung bề mặt tối + thành phần ở mục 5 |
| 3 | Chăm sóc khách hàng, liên hệ, hướng dẫn size, cửa hàng | `pages/customer-service`, `size-guide`, `contact`, `stores` | Kiểu bài viết trên nền giấy |
| 3 | 404 | `notfound.html` | Một câu ngắn, kèm liên kết về bộ sưu tập |

### 7.1 Sân khấu mở đầu (trang chủ)

Khung đen cao khoảng 900px. Cột chữ trái chiếm khoảng 35%: nhãn nhỏ viết hoa, tiêu đề serif 56 đến 64px
có một cụm in nghiêng, một câu mô tả ngắn, một nút chính. Bên phải và phía dưới là một ảnh ngọc lớn
đặt chéo, sáng mềm, rìa mờ vào nền đen. Không dùng slider, không đặt ảnh nhỏ nổi trên nền trống,
không đặt tiêu đề giữa trang khổng lồ.

### 7.2 Trang chi tiết sản phẩm (mẫu đã có)

Hai cột 7/5, thông tin bên phải dính khi cuộn. Vì mỗi món ngọc là hàng độc nhất, **không có ô số lượng**;
hiển thị "Sản phẩm duy nhất · còn 1 chiếc" và đường kính trong cố định (không phải ô chọn size).
Mục "Cách đo size tay" giúp khách chọn đúng. Nội dung accordion: Thông số, Cách đo size tay, Giao hàng
và đổi trả, Bảo quản ngọc.

### 7.3 Trang bài chia sẻ về ngọc

Dùng khu giấy (Franco). Hai nửa bằng nhau: ảnh một bên, chữ một bên, đổi bên qua từng đoạn. Bảng so sánh
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

1. Xác nhận hướng thiết kế với chủ shop (mục 3), đặc biệt: nền đen cho khu bán hàng.
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
| Bo góc chỉ trong 0, 6px, 10px, 24px, 50%, 9999px | `border-radius:16px` |
| Cấm font ngoài hai font đã chọn | Bodoni, Helvetica, Georgia, Inter, Roboto, Playfair |
| Không bỏ `outline` | `outline:none` |
| Mọi custom property dùng tiền tố `--kn-` và phải có trong `kn.css` | `--brand`, `--sw-pad` |
| Không còn tên cũ `sw-`, `swp-`, `swd-` | `class="sw-btn"` |
| `<html lang="vi">`, có viewport, mọi `<img>` có `alt` | thiếu `alt` |
| Cấm cụm từ khuyến mãi ồn ào và lời hứa pháp lý | "siêu hot", "giá sốc", "cam kết 100%" |
| Không `style=""` trong `my-site/` (ở nơi khác chỉ cảnh báo) | `style="color:red"` |
| Không hex trong thuộc tính SVG | `stroke="#bc9c5c"`; dùng `currentColor` |
| Cảnh báo khi dùng vàng làm nền | `background:var(--kn-gold)` |

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

- Chỉ dùng một nút chính mỗi khu; vàng không làm nền khối; mỗi tiêu đề chỉ một cụm in nghiêng có nghĩa.
- Ảnh phải là ảnh sản phẩm thật theo quy chuẩn mục 6; không vẽ ngọc bằng CSS, không dùng ảnh AI làm ảnh chính.
- Không bịa số liệu, chính sách, chứng nhận. Dữ liệu mẫu phải có `data-demo="true"` hoặc dòng chú thích.
- Luồng giỏ hàng, thanh toán phải chạy thật với sản phẩm trong DB sau khi đổi class/id (xem 14.5).

### 14.5 Hợp đồng với `ecommerce.js` (không được làm hỏng)

`ecommerce-theme/.../static/js/ecommerce.js` đọc các id và tên trường dưới đây, và tự dựng thêm DOM với class `sw-*`.
Trang mẫu `cart.html`, `checkout.html`, `product.html` đã giữ nguyên các id và tên này. Khi đổi sang `kn-*`:
đổi class trong template **và** trong đoạn JS tạo DOM, hoặc giữ id và chỉ đổi class; sau đó chạy lại cả luồng.

| Trang | Phải giữ nguyên | JS tự dựng (phải đổi class sang `kn-*`) |
|---|---|---|
| Chi tiết sản phẩm (`product.html`) | `swMain`, `swBcCur`, `swPdp`, `swPdpImg`, `swPdpCode`, `swPdpName`, `swPdpPrice`, `swPdpStock`, `swPdpSwatches`, `swPdpSwatchList`, `swPdpColor`, `swPdpQty`, `swAddBtn`, `swWishBtn`, `swAddNote`, `swPdpDescText`, thuộc tính `data-qty-dec`, `data-qty-inc`, `data-kn-state` | Khối trạng thái trống/lỗi, ô chọn màu |
| Giỏ hàng (`cart.html`) | `swMain`, `swCart`, `swLines`, `swSubtotal`, `data-kn-state` | Mỗi dòng giỏ: `li.sw-line`, `.sw-img`, `.sw-qty`, `.sw-rm`, `.sw-line-end`, `.sw-pcode` |
| Thanh toán (`checkout.html`) | `swCheckout`, `swCheckoutForm`, các tên trường `deliveryFullName`, `deliveryEmail`, `deliveryPhoneNumber`, `deliveryAddress`, `deliveryCityName`, `deliveryPostalCode`, `deliveryCountryId` (id `swCountry`), `paymentServiceCode` (id `swPay`), `swFormErr`, `swPlace`, `swMini`, `swTotal` | Các `option` của quốc gia và thanh toán, các dòng `swMini` |

Lưu ý thiết kế liên quan:

- Mỗi món ngọc là hàng độc nhất. Giao diện mẫu **không có ô số lượng**. Mã hiện tại có nút cộng trừ số lượng và đã chặn theo tồn kho (`product.amount`); khi tồn kho là 1, ẩn bộ chọn số lượng thay vì hiện cộng trừ.
- Form thanh toán hiện tại đòi `deliveryPostalCode` và mặc định theo Singapore (placeholder `+65`, thành phố "Singapore"). Bản mẫu đã đổi sang giá trị cho Việt Nam; mã bưu chính vẫn **bắt buộc** như plan. Câu hỏi mở cho chủ shop: có nên bỏ bắt buộc không (backend nhận giá trị rỗng).
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
| Refero style Slash, id `350e0391-9d87-4830-8d2b-826b3e9fc795` | Khu bán hàng: canvas, serif, viền kính, chân trời ấm |
| Refero style Franco Maria Ricci, id `2c3b31a4-6e28-47a0-afbd-cb22e22e34fb` | Khu bài viết: giấy, vàng cổ, ô lưới kẻ, nút ngoặc nhọn, colophon |
| Refero screens: Farfetch (trang đồng hồ), Saint Laurent (nhiều trang sản phẩm) | Bố cục trang chi tiết sản phẩm |
| `docs/design/` | Bộ tham chiếu chạy được: `kn.css`, `style-guide.html`, `home.html`, `category.html`, `product.html`, ảnh chụp trong `screenshots/` (mở trước khi làm) |
| `.claude/skills/refero-design/` | Phương pháp nghiên cứu và reference lock |

Có thể lấy lại toàn bộ chi tiết hai style bằng `refero_get_style` với id ở trên khi cần kiểm tra token gốc.
