---
title: Chuyển lớp giao diện sang design v2 (sáng: ngọc bích, vàng gold, kem nắng)
status: draft, CHỜ ANH JOHNNY DUYỆT (chưa sửa file UI nào)
branch: feat/ui-redesign (worktree ezyplatform-ui), không merge vào main
owner: Batman (tech lead) · writer duy nhất: Plastic Man · UX review (chỉ đọc): Mister Terrific · QA: Martian Manhunter · backup / deploy Docker local / rollback: Cyborg
created: 2026-11-22 16:58 (Asia/Saigon)
source: docs/design-handoff-prompt.md (phần "## Prompt"), docs/design-brief-khang-nguyen-ngoc-bao.md bản 2 (§0–5, §6.2, §13, §14.2, §14.4, §14.5), docs/design/kn.css v2 (commit b0b30a5)
supersedes: phần thị giác của plans/261122-1353-ui-revamp/plan.md (nền tối). Phần chức năng của plan đó (id §14.5, giỏ chặn tồn kho, mã bưu chính bắt buộc, newsletter gửi API) giữ nguyên.
---

# Plan: chuyển UI sang design v2 sáng

## 1. Outcome, ràng buộc, non-goals

**Outcome:** mọi trang phía khách khớp 5 trang mẫu v2 và ảnh chụp `docs/design/screenshots/` (1440px, 390px): nền kem nắng/trắng, mảng ngọc nhạt phẳng, nút viên thuốc ngọc, thẻ sản phẩm không khung, giá ngay dưới tên. Không còn nền tối ở đâu.

**Giữ nguyên (chỉ đổi lớp giao diện):** catalog 35 món, đơn hàng (#77 = 10.740.000₫, #85, #86), luồng mua, id/field §14.5, logic `ecommerce.js` (giỏ chặn tồn kho, ẩn số lượng khi tồn kho 1, mã bưu chính bắt buộc, newsletter gọi API, Esc đóng tìm kiếm…).

**Non-goals:** không đụng Java/controller/schema, không publish/upload media/ghi server EzyPlatform thật, không đọc/commit `my-site/environment.json` hay `.env`, không làm lại từ đầu, không bịa chính sách/số liệu/chứng nhận, không tô nền tối để "hợp" ảnh.

## 2. Kiểm kê (chỉ đọc, đo lúc 16:4x–16:58)

### 2.1 Số lỗi `check-design.py` v2 TRƯỚC khi sửa

Lệnh: `python3 -I docs/design/check-design.py <file>`. Output đầy đủ của 3 file chính: `baseline/`.

| File / thư mục | Lỗi | Cảnh báo | Ghi chú |
|---|---:|---:|---|
| `ecommerce-theme/.../static/css/main.css` | **123** | 1 | 91 token cũ không có trong kn.css v2, 21 hex lạ (tối), 9 `rgb()` tối, 2 bo góc 6px |
| `my-site/page-fragments/ecommerce/theme/head.html` | **123** | 1 | Bản sao y main.css (sync-theme.py) |
| `my-site/pages/home/content.html` | **123** | 1 | Nhúng nguyên CSS cũ (build-pages.py) |
| `my-site/pages/customer-service/content.html` | **123** | 1 | như trên |
| `my-site/pages/ngoc-hoa-dien-tan-cuong-thanh-hai/content.html` | **123** | 1 | như trên (trang chưa có trong DB) |
| `my-site/pages/vong-ngoc-demo/content.html` | **123** | 1 | như trên; file sinh tự động |
| 22 template `templates/*.html` + `fragments/header.html`, `fragments/footer.html` | 0 | 0 | Script không bắt được, xem 2.3 (class không còn trong kn.css v2) |
| `static/js/ecommerce.js` | 0 | 0 | Như trên, và vi phạm quy tắc giá (2.4) |
| `my-site/page-fragments/ecommerce/{header,footer,category-strip}/*.html`, `theme/foot.html` | 0 | 0 | Cấu trúc theo design cũ (footer giấy/nav tối), phải đổi |
| `docs/design/{article,order,order-status,customer-service}.html` (mockup team tự làm) | 0 | 0 | Dùng class không còn trong kn.css v2 |
| **Tổng** (`check-design.py` trên templates, fragments, my-site, docs/design + main.css) | **738** | 11 | = 6 file × 123 |

Token cũ đếm theo tên (main.css, mỗi bản sao giống hệt): `--kn-edge` 18, `--kn-paper-ink` 14, `--kn-rule` 10, `--kn-black` 9, `--kn-text` 8, `--kn-gold-light` 8, `--kn-panel` 6, `--kn-paper-muted` 5, `--kn-outline` 4, `--kn-danger-text` 4, `--kn-paper` 2, `--kn-horizon-mid` 1, `--kn-horizon-end` 1, `--kn-btn-hover` 1. Grep `#05070b|#bc9c5c|#f0dfae|--kn-panel|…|--kn-btn-hover`: **109 lần mỗi file**, đúng 6 file trên, 0 ở template/JS/fragment khác. Ngoài ra main.css còn 9 chỗ `color:var(--kn-gold)` (vàng làm chữ, sai vai trò v2; script không bắt).

`sw-*/swp-*/swd-*`: 0 (đã sạch từ đợt trước). "Black Friday", "Masters of Light", "Singapore", "+65": 0 trong repo và 0 trong DB local.

### 2.2 File đã sửa/tạo theo design cũ (so `main...HEAD`)

- Theme: `static/css/main.css`, `static/js/ecommerce.js`, 22 template + 2 fragment template, `messages*.properties` (chữ, không đổi), `static/data/stores.json`, 3 ảnh `static/images/*.jpg`.
- `my-site/page-fragments/ecommerce/`: `theme/head.html`, `theme/foot.html`, `header/content.html`, `footer/content.html`, `category-strip/content.html`.
- `my-site/pages/`: `home`, `customer-service`, `ngoc-hoa-dien-tan-cuong-thanh-hai` (mới), `vong-ngoc-demo` (sinh tự động) — đều dựng bằng `docs/design/build-pages.py` từ mockup trong `docs/design/`.
- Công cụ: `docs/design/build-pages.py`, `sync-theme.py` (kn.css → main.css + head.html → build-pages).
- Mockup team tự làm: `docs/design/article.html`, `customer-service.html`, `order.html`, `order-status.html`.

### 2.3 Class `kn-*` đang dùng nhưng KHÔNG còn trong kn.css v2

kn.css v2 ghi đè bản team mở rộng. main.css đang có **54 class ngoài kn.css v2** (tính cả biến thể):
`kn-alt kn-article* kn-auth kn-bag__ico/__txt kn-btn--outline kn-btn--wide kn-card kn-cell__code kn-cell__price kn-code kn-contact kn-count kn-dl kn-drawer* kn-faq kn-field--wide kn-filters kn-heart kn-hide-sm kn-hp kn-ico kn-lede kn-line__end kn-line__img kn-menu-btn kn-nav__tools kn-news* kn-ok kn-orders kn-page kn-qty kn-qty-row kn-rm kn-scrim kn-scroll kn-search kn-skip kn-spin kn-state kn-stock kn-stores kn-swatches kn-toast kn-tools kn-wish`.

Nơi dùng: header (drawer, search, skip link, menu), footer (newsletter, honeypot), product (qty, stock, swatches, wish), products (tools, count), account/order/size-guide (card, dl, orders), auth (login/register/forgot/reset/newsletter/404), stores, wishlist, JS (toast, spin, heart, state, rm, line__img).

**Quyết định:** các class có chức năng (drawer, search, qty, toast, state, auth, card/dl/orders, news, hp, skip, sr…) được **thêm lại vào kn.css v2 theo kiểu sáng**, đồng thời vào `style-guide.html`, trong một nhóm "Mở rộng cho trang động" kèm lý do (§14.3 bước 2). Class trùng vai trò với class v2 thì **bỏ và dùng class v2**: `kn-lede`→`kn-lead`, `kn-btn--outline`→`kn-btn--ghost`, `kn-cell__price`→`kn-cell p`, `kn-line__end`→bỏ (giá vào cột tên), `kn-filters`→`kn-toolbar/kn-chips`.

### 2.4 Vi phạm QUY TẮC GIÁ (giá phải ngay dưới tên)

| Nơi | Hiện trạng | Sửa |
|---|---|---|
| Thẻ sản phẩm `productCard()` (`ecommerce.js` ~347) → trang chủ, `/products`, gợi ý, wishlist | Thứ tự `h3` → `code.kn-cell__code` → giá; CSS cũ `.kn-cell div{justify-content:space-between}` đặt giá bên phải | `h3` → giá (`p`) → mã; dùng `.kn-cell` v2 (cột dọc) |
| Dòng giỏ `lineNode()` (~825), `/shopping-cart` | Giá trong `.kn-line__end` cột phải (grid `88px 1fr auto`) | Cột tên: `h3` → `p.kn-line__price` → `p.kn-line__meta` (mã) → số lượng (nếu tồn kho > 1) → nút Xóa `kn-link`; grid v2 `88px 1fr` |
| Tóm tắt thanh toán `#knMini` (~916) | `li > span(tên) + span(giá)`, CSS cũ hàng ngang | Giữ markup (giống mockup), CSS v2 `.kn-mini li{flex-direction:column}` xếp dọc |
| Đơn hàng `#knOrderLines` (~1647, `/order`) | Như `#knMini` | Như trên; kiểm #77 = 10.740.000₫ |
| Tra cứu đơn `/order-status` (~1256) | `span(số lượng × tên (đơn giá))` + `span(thành tiền)` | Tách tên / giá thành 2 dòng dọc |
| Chi tiết sản phẩm `product.html` | `code` → `h1` → **mô tả** → giá → tồn kho | nhãn/mã → `h1` → giá → "còn 1 chiếc" → mô tả → nút thêm vào giỏ |
| Bảng đơn của tài khoản `#knOrders` | Bảng (mã đơn, ngày, trạng thái, tổng) | Không phải dòng sản phẩm, giữ bảng `kn-table` |
| Tổng tiền (tạm tính, tổng cộng) | nhãn trái / số phải | Giữ đúng như quy tắc |

### 2.5 Nội dung trong DB local (Docker `127.0.0.1:9090`, schema `blog`) cần đổi

| Bản ghi | Hiện trạng | Đổi |
|---|---|---|
| `ezyarticle_post_meta` post 4 (fragment `ecommerce/theme`) `additionalHead` (29.821 ký tự) | CSS nền tối (`--kn-edge`, `#05070b`) | Nạp lại từ `theme/head.html` mới |
| `ezyarticle_post_meta` post 4 `additionalFoot` | JS dùng chung | Nạp lại nếu `theme/foot.html` đổi |
| `ezyarticle_posts` 1, 2, 3 (fragment `category-strip`, `footer`, `header`) | Markup design cũ | Nạp lại từ `my-site/page-fragments` mới |
| `ezyarticle_posts` 5 `home` (32.577 ký tự), 8 `customer-service` (33.563) | Nhúng CSS tối | Nạp lại từ `my-site/pages` mới |
| Trang `ngoc-hoa-dien-tan-cuong-thanh-hai`, `vong-ngoc-demo` | Chưa có trong DB | Không tạo (cần anh Johnny tạo bằng VS Code extension như đợt trước) |
| Template theme, main.css, ecommerce.js | Trong container (`/app/...`) | Chép vào container; template cần restart (cache) |

Các bản ghi trên đều trạng thái DRAFT trên Docker local; server EzyPlatform thật không bị đụng.

### 2.6 Ảnh sản phẩm (35 ảnh đang dùng, đo trong container)

Cả 35 ảnh **đều phải chụp lại** theo §6.2: tất cả dọc 9:16 hoặc 3:4 (không phải 1:1/4:5), độ sáng viền trung bình 23–121/255 (nền kem ≈ 245), không ảnh nào nền sáng. Ảnh xem nhanh: `baseline/product-images-contact-sheet.jpg`.

| Nhóm | Mã | Vấn đề | Ưu tiên |
|---|---|---|---|
| A. Nền đen studio | KN-003, 007, 009, 020, 023, 031 | Nền đen nửa trên; khay nhung kem nửa dưới → cắt `object-position` đáy dùng tạm được | 2 |
| A'. Nền đen, chuỗi trên đế đan chéo | KN-026, 032 | Nền đen quanh đế, viền đo được 23–38 (tối nhất) | 1 |
| B. Có chữ chèn sẵn (caption) | KN-001, 005, 010, 011, 013, 014, 015, 017, 019, 021, 024, 025, 027, 028, 030, 033 | Chữ trắng chèn trên ảnh + nền kệ tối + cầm tay; 13 món có số size viết tay/tem trên vòng (005, 010, 013, 014, 015, 017, 021, 024, 025, 027, 028, 030, 033) | 1 |
| C. Kệ tối, cầm tay, không chữ | KN-002, 004, 006, 008, 012, 016, 018, 022, 029, 034, 035 | Nền kệ tối, cây cảnh, tay cầm; KN-035 lộ tem treo | 2 |

Gợi ý chụp lại: một bộ đồng nhất, nền kem/trắng ngà, ánh sáng ban ngày, 1:1 hoặc 4:5, mỗi món thêm 1 ảnh cận chất ngọc + 1 ảnh đeo thật, không tem/số viết tay trong ảnh chính.

## 3. Thứ tự file và phase (theo bước 3a → 3b của prompt)

Mọi phase chạy **tuần tự** (đều đụng `kn.css`/`main.css`/`ecommerce.js`), commit riêng từng phase trên `feat/ui-redesign`. Writer: **Plastic Man**. Sau mỗi phase: `check-design.py` 0 lỗi trên file đã sửa + chụp 1440/390 so ảnh mẫu; Mister Terrific review UX (chỉ đọc).

| Phase | File (theo thứ tự) | Việc chính | Ước lượng |
|---|---|---|---|
| **P0** Backup (Cyborg) | không sửa repo | `mysqldump` toàn DB `blog` + riêng `ezyarticle_posts`, `ezyarticle_post_meta`; tar `main.css`, `ecommerce.js`, templates trong container; SHA256SUMS → `.artifacts/backups/ui-light-v2-<ts>/` | 15 ph |
| **P1** Token + font + thành phần mở rộng | `docs/design/kn.css` (chỉ THÊM nhóm "Mở rộng" ở cuối, không đổi phần v2), `docs/design/style-guide.html` (thêm mục minh họa), `docs/design/sync-theme.py` (URL font = URL của mockup v2), rồi sinh `static/css/main.css` + `page-fragments/ecommerce/theme/head.html` | Thay bộ CSS tối bằng kn.css v2; viết lại ~45 class mở rộng theo kiểu sáng (drawer, search, toast, qty, state, auth, card/dl/orders, news, hp, skip…); bỏ các class trùng (2.3). Kiểm `diff` hai nơi khớp | 2,5–3 h |
| **P2** Fragments | `templates/fragments/header.html`, `templates/fragments/footer.html`, `my-site/page-fragments/ecommerce/{header,footer,category-strip}/content.html`, `theme/foot.html` (nếu cần) | Nav kính kem 68px + con dấu vàng + nút giỏ viên thuốc trắng huy hiệu vàng; footer mảng `--kn-jade-mist`, con dấu 132px, 3 cột, khẩu hiệu vàng đậm; giữ drawer/search/newsletter đang chạy | 1,5 h |
| **P3** Trang chủ | `templates/home.html`; `docs/design/home.html` chỉ đọc → `build-pages.py` sinh `my-site/pages/home/content.html` | Hero kem + quầng nắng, ảnh khung trắng viền vàng; kệ sản phẩm (JS); khu bài viết hai nửa; dải bình minh + kim cương vàng. Mục chưa có dữ liệu: `data-demo` | 1,5 h |
| **P4** Danh mục + chi tiết | `templates/products.html`, `templates/product.html`, `ecommerce.js` (`productCard`, PDP), `vong-ngoc-demo` sinh lại | Thẻ không khung, gap 24, ảnh vuông bo 20 cắt đáy, tên → giá → mã; chip lọc; PDP: tên → giá → còn 1 chiếc → mô tả → nút | 2 h |
| **P5** Giỏ + thanh toán + đơn | `templates/cart.html`, `checkout.html`, `order.html`, `order-status.html`, `ecommerce.js` (`lineNode`, `#knMini`, `#knOrderLines`, order-status) | Dòng giỏ xếp dọc (2.4), khung tóm tắt trắng bo 20, ô nhập trắng viền `--kn-line-strong` focus ngọc, lỗi `--kn-crimson`/`--kn-danger`; `select#knPay` giữ dạng `kn-input` | 2 h |
| **P6** Khu bài viết | `docs/design/article.html` (mockup team, chuyển sang `kn-spread` hai nửa + bảng `kn-table`), sinh `my-site/pages/ngoc-hoa-dien-tan-cuong-thanh-hai`; `templates/page.html` | Một nửa ảnh, một nửa `--kn-jade-mist` phẳng; bảng so sánh trắng bo 10 | 1 h |
| **P7** Tài khoản | `account.html`, `wishlist.html`, `login.html`, `register.html`, `forgot-password.html`, `reset-password.html`, `newsletter.html` | Thẻ trắng bo 20, form v2, bảng đơn `kn-table`, wishlist dùng `productCard` | 1,5 h |
| **P8** Trang nội dung + 404 | `customer-service.html` (template + `docs/design/customer-service.html` → page), `contact.html`, `size-guide.html`, `stores.html`, `appointment.html`, `gift-card-balance.html`, `notfound.html` | Áp v2; FAQ dùng `kn-acc`; nội dung chưa có → `data-demo` | 1,5 h |
| **P9** Deploy local + QA | Cyborg: `sync`/apply dry run → apply fragment + page vào Docker local, chép main.css/js/templates, restart; Martian Manhunter QA (mục 5) | | 2 h |
| **P10** Rollback thật + review | Cyborg thử rollback thật rồi áp lại; `ak-code-review` toàn diff; Batman hậu kiểm, cập nhật `VERIFY-LOCAL.md`, báo cáo | | 1 h |

Tổng ước lượng: **~17 giờ** làm việc của team (P1 và P5 rủi ro nhất).

Mockup `docs/design/order.html`, `order-status.html`: **bỏ** (không trang CMS nào dựng từ chúng; template động là nguồn thật). `article.html`, `customer-service.html`: **chuyển** sang v2 (đang là nguồn của 2 trang CMS).

## 4. Backup và rollback

- **Trước khi đổi nội dung trong DB (P9):** Cyborg dump DB (P0) và giữ bản backup JSON tự động của công cụ publish (`my-site/.artifacts/backups/<ts>/`). SHA256 cho mọi file.
- **Rollback A (nhanh):** `git checkout <commit trước P1> -- <file UI>` → sync → apply lại fragment/page → chép main.css/js/templates → restart.
- **Rollback B (DB):** nạp lại `ezyarticle_posts` + `ezyarticle_post_meta` từ dump P0, chép lại theme từ tar, restart.
- P10 chạy thật một lần (B), kiểm site về bản nền tối, rồi áp lại bản v2 và kiểm lại.

## 5. QA (Martian Manhunter, độc lập)

1. `check-design.py` 0 lỗi trên mọi file đã sửa (bảng trước/sau trong báo cáo); grep token cũ = 0; `diff` main.css ↔ head.html khớp.
2. Chụp 1440 và 390 từng trang đã đổi, đặt cạnh ảnh mẫu: home, products, product (KN-034 còn 1, KN-035 hết hàng), cart, checkout, order #77, account, wishlist, login, register, contact, size-guide, 404.
3. Luồng mua thật với một món KN-xxx còn hàng: thêm vào giỏ → giỏ → thanh toán → đặt hàng; giỏ trống; form trống báo lỗi; đơn #77 hiện đúng 10.740.000₫.
4. Quy tắc giá ở mọi nơi (2.4). Không nền tối ở đâu (kể cả drawer, toast, scrim).
5. Bàn phím: Tab qua nav, drawer, search, form; focus 2px `--kn-jade-deep` lệch 3px. Tương phản AA. 390px không tràn ngang. Tiếng Việt đủ dấu.

## 6. Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Ghi đè kn.css làm mất ~45 class chức năng → drawer/search/toast/qty vỡ | Cao | P1 thêm lại đủ trước P2; QA kiểm từng hành vi JS đã sửa đợt trước |
| 35 ảnh nền tối/có chữ trông như khối tối trên nền kem | Cao (thẩm mỹ) | Khung trắng + `object-position` đáy (cách tạm của brief); nhóm B (có chữ) cắt vẫn lộ chữ ở thẻ dọc → cần chụp lại |
| Ảnh 9:16 cắt vuông mất phần vòng ở vài món cầm tay | Trung bình | Kiểm từng thẻ khi QA; ghi món bị cắt xấu |
| Đổi thứ tự DOM PDP/dòng giỏ làm hỏng JS đọc id | Trung bình | Giữ nguyên id §14.5, chỉ đổi vị trí phần tử; chạy lại luồng mua |
| Template cache: đổi không hiện nếu chưa restart | Thấp | P9 luôn restart container web |
| Mockup v2 tải Cormorant 400/500 + italic 400, brief ghi 500/600 + italic 500 | Thấp | Dùng đúng URL của mockup v2 (đã đạt 0 lỗi và khớp ảnh chụp); ghi lệch này vào báo cáo |
| Trang CMS chưa có trong DB (bài viết, vong-ngoc-demo) → link "Kiến thức về ngọc" vẫn 404 | Đã biết | Cần anh Johnny tạo bằng VS Code extension |

## 7. Quyết định chưa có (dùng dữ liệu mẫu có nhãn, liệt kê ở báo cáo cuối)

Loại ngọc chính xác từng món · phương thức thanh toán (DB đang trả "Demo payment") · chính sách giao hàng/đổi trả/bảo hành · mã bưu chính có bắt buộc (đang bắt buộc) · có tiếng Anh không (mặc định không) · số điện thoại shop · thông tin cửa hàng cho `/stores`, `/book-an-appointment` · lịch chụp lại 35 ảnh.

## 8. Tiêu chí xong

- [ ] `check-design.py` 0 lỗi trên mọi file đã sửa; grep token cũ và hex cũ = 0.
- [ ] Mọi trang trong mục 3 khớp ảnh mẫu ở 1440/390 (khác biệt có chủ đích được ghi lý do).
- [ ] Giá ngay dưới tên ở thẻ, dòng giỏ, tóm tắt thanh toán, đơn hàng, tra cứu đơn, PDP.
- [ ] Luồng mua thật chạy; trạng thái trống/lỗi đúng; #77 = 10.740.000₫.
- [ ] Không nền tối; không box-shadow; bo góc chỉ 0/10/20/24/viên thuốc.
- [ ] Thành phần mới có trong cả kn.css và style-guide.html, có lý do.
- [ ] Backup trước khi đổi DB; rollback thật đã chạy; không tiến trình nền ngoài Docker local.
