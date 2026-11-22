---
title: Revamp UI phía khách "Khang Nguyên · Ngọc Bảo"
status: approved-to-execute (Johnny 13:5x: team tự triển khai, giữ nền đen khu bán hàng)
superseded_by: plans/261122-1658-ui-light-v2/plan.md (phần thị giác nền tối đã bị loại theo design v2 sáng; phần chức năng vẫn giữ)
branch: feat/ui-redesign (worktree ezyplatform-ui), không merge vào main
owner: Batman (tech lead) · writer duy nhất: Plastic Man · UX review (đọc): Mister Terrific · QA: Martian Manhunter · vận hành Docker local / backup / rollback: Cyborg
created: 2026-11-22 13:53, sửa 14:1x (Asia/Saigon)
source: ui-revamp-brief.md (cùng thư mục), docs/design-brief-khang-nguyen-ngoc-bao.md §3, §4, §7, §12–15
---

# Plan: revamp UI phía khách

## 1. Outcome
Mọi trang phía khách dùng ngôn ngữ thiết kế `docs/design`: token/class `kn-*` từ `docs/design/kn.css`,
nền tối cho khu bán hàng (Slash), nền giấy cho khu bài viết (Franco Maria Ricci), Cormorant Garamond + Be Vietnam Pro,
vàng đúng vai trò, ảnh sản phẩm thật là trung tâm, giao diện tiếng Việt. Hành vi mua hàng giữ nguyên và chạy thật
trên Docker local.

## 2. Hiện trạng đã kiểm (14:1x, worktree @ fe8f065)
- `my-site/pages/`: `home`, `customer-service`, `example-page`, `vong-ngoc-demo` (sinh tự động). Hai trang
  `necklaces`, `product-detail-sample` và `data/seed/*` **đã bị xoá** ở task catalog (c674c91), nên phần "thay nội dung
  thương hiệu cũ" chỉ còn ở `home`, `customer-service`, fragments, templates, CSS/JS.
- Templates (23 + 2 fragment) ở `ecommerce-theme/ecommerce-theme-theme/src/main/resources/templates/`.
- DB local đã có 35 sản phẩm Khang Nguyên thật (task catalog) → dùng một món có sẵn cho luồng mua, không cần tạo SP mới.
- Docker local: `ezy-my-site-e29d88-ezyplatform-1` (127.0.0.1:8080 web, :9090 admin) + `ezy-my-site-e29d88-mysql-1`.
  Static (`main.css`, `ecommerce.js`) phục vụ ngay sau `docker cp`; **template Thymeleaf bị cache, cần restart container
  web** (xác nhận ở `.artifacts/reports/logo-fix/report.md`).
- `check-design.py` bắt `\bsw-[a-z]`, `swp-`, `swd-`, `--sw-`, **không** bắt id camelCase `swMain`… → giữ id theo §14.5
  vẫn đạt 0 lỗi.
- `ecommerce.js` (1716 dòng): accordion bật tắt bằng `aria-expanded` (~dòng 1051), số lượng `#swPdpQty` (~671–720),
  gợi ý điện thoại `+65` (~1041). Có `messages/messages_vi.properties` + `messages.properties`.

## 3. Quyết định đã chốt (Batman)
1. **Giữ toàn bộ id/field §14.5 nguyên tên** (`swMain`, `swLines`, `deliveryPostalCode`…), chỉ đổi class `sw-*` → `kn-*`
   trong template + đoạn JS dựng DOM + CSS **trong cùng một commit**. Lý do: ít rủi ro vỡ luồng mua nhất; id camelCase
   không vi phạm check-design. Ghi là lệch có chủ đích ("còn id `sw*` theo hợp đồng JS").
2. `select#swPay` giữ dạng `select` style `kn-input` (không đổi sang radio) → không phải sửa logic JS đọc `swPay`.
3. Accordion: chuyển sang `<details>` như mẫu, **xoá trình xử lý `aria-expanded` cũ của accordion** trong `ecommerce.js`
   (giữ handler menu/search).
4. Số lượng: khi `product.amount <= 1` ẩn `#swPdpQty` cùng nút cộng/trừ (vẫn giữ phần tử trong DOM, qty = 1), hiện
   "Sản phẩm duy nhất · còn 1 chiếc".
5. Mã bưu chính: **giữ trường, giữ bắt buộc** như hiện tại (không biết kiểm tra nằm ở JS hay Java; không đụng Java).
   Đổi placeholder/mặc định sang Việt Nam. Ghi vào câu hỏi mở.
6. "Server EzyPlatform" trong lệnh cấm = mọi môi trường ngoài máy này. Docker local 127.0.0.1 được coi là môi trường dev:
   được `ezy publish apply` page/fragment, `docker cp` static/template, restart container web — **chỉ sau** backup DB +
   dry run, và không tải media. Không có production trong phạm vi. (Nếu Johnny không đồng ý cách hiểu này → xem Q1.)
7. `my-site/environment.json`: không đọc, không in, không commit. Worktree không có file này; khi cần apply lên Docker
   local, Cyborg chạy lệnh từ thư mục main (không chép nội dung) hoặc chép nguyên file không mở, và xoá sau khi xong.
8. Chưa có quyết định thật (loại ngọc, thanh toán, chính sách, bưu chính, tiếng Anh): dùng dữ liệu mẫu gắn
   `data-demo="true"`, không dừng việc, gom vào báo cáo cuối.

## 4. Non-goals
Không sửa Java/controller/`ecommerce-ext`, không đổi schema DB, không publish ngoài máy, không tải media, không merge main,
không thêm tiếng Anh, không làm khu "chứng nhận", không làm các tính năng trong `plans/BACKLOG.md`.

## 5. File sẽ sửa, theo thứ tự §13 của brief
Viết tắt: `T/` = `ecommerce-theme/ecommerce-theme-theme/src/main/resources/templates/`,
`S/` = `.../resources/static/`, `F/` = `my-site/page-fragments/ecommerce/`, `P/` = `my-site/pages/`.

| Bước §13 | File |
|---|---|
| 2. Token + font + tiền tố | `S/css/main.css` (thay `:root --sw-*` bằng nội dung `docs/design/kn.css`, gộp 2 khối trùng `.sw`/`body.sw`), `F/theme/head.html` (cùng nội dung CSS, phải khớp main.css), `F/theme/foot.html`, `S/js/ecommerce.js` (class DOM tự dựng → `kn-*`), `T/page.html` (link font, `lang="vi"`, viewport) |
| 3. Fragments + home | `F/header/`, `F/footer/`, `F/category-strip/`, `T/fragments/header.html`, `T/fragments/footer.html`, `P/home/content.html` (dựng bằng inline-css.py từ `docs/design/home.html`), `T/home.html` |
| 4. Danh mục + chi tiết | `T/products.html`, `T/product.html`, `P/vong-ngoc-demo/content.html` (chỉ sinh lại bằng inline-css.py, không sửa tay) |
| 5. Giỏ + thanh toán | `T/cart.html`, `T/checkout.html`, `T/order.html`, `T/order-status.html`, phần JS cart/checkout trong `S/js/ecommerce.js` |
| 6a. Bài viết về ngọc | `docs/design/article.html` (trang tham chiếu mới, nền giấy, nội dung "Ngọc Hòa Điền: Tân Cương và Thanh Hải" lấy từ demo) → `P/ngoc-hoa-dien-tan-cuong-thanh-hai/` (content/head/foot/meta, sinh bằng inline-css.py) |
| 6b. Tài khoản & xác thực | `T/account.html`, `T/wishlist.html`, `T/login.html`, `T/register.html`, `T/forgot-password.html`, `T/reset-password.html` |
| 6c. Trang nội dung + 404 | `T/customer-service.html`, `P/customer-service/content.html`, `T/contact.html`, `T/size-guide.html`, `T/stores.html`, `T/appointment.html`, `T/newsletter.html`, `T/gift-card-balance.html`, `T/notfound.html` |
| i18n | `.../resources/messages/messages_vi.properties` (chỉ chuỗi UI hiển thị nếu template dùng `#{…}`) |
| Thành phần mới (nếu có) | `docs/design/kn.css` + `docs/design/style-guide.html` cùng commit, kèm lý do |

`P/example-page` không phải trang khách → không sửa, ghi vào báo cáo.

## 6. Cách dựng trang `my-site/pages` (brief §14.3)
1. Sửa/viết trang tham chiếu trong `docs/design/<x>.html` (dùng `kn.css`, link nội bộ tương đối).
2. Sinh: `python3 -I docs/design/inline-css.py docs/design/<x>.html my-site/pages/<slug>/content.html home.html=/home category.html=/products product.html=/vong-ngoc-demo`
   (map link theo URL thật; danh mục thật là `/products` vì `necklaces` đã xoá).
3. `python3 -I docs/design/check-design.py my-site/pages/<slug>` = 0 lỗi.
4. Không có `[[` / `${` trong văn bản; fragment gọi bằng `<ezy:block ezy:replace="~{ecommerce/header :: content}" />`.

## 7. Hợp đồng `ecommerce.js` phải giữ (brief §14.5)
| Trang | Giữ nguyên id / field | JS tự dựng (đổi class → `kn-*`) |
|---|---|---|
| `product.html` | `swMain`, `swBcCur`, `swPdp`, `swPdpImg`, `swPdpCode`, `swPdpName`, `swPdpPrice`, `swPdpStock`, `swPdpSwatches`, `swPdpSwatchList`, `swPdpColor`, `swPdpQty`, `swAddBtn`, `swWishBtn`, `swAddNote`, `swPdpDesc`, `swPdpDescText`, `data-qty-dec`, `data-qty-inc`, `data-sw-state` | khối trống/lỗi, ô chọn màu |
| `cart.html` | `swMain`, `swCart`, `swLines`, `swSubtotal`, `data-sw-state` | `li.sw-line`, `.sw-img`, `.sw-qty`, `.sw-rm`, `.sw-line-end`, `.sw-pcode` |
| `checkout.html` | `swCheckout`, `swCheckoutForm`, `deliveryFullName`, `deliveryEmail`, `deliveryPhoneNumber`, `deliveryAddress`, `deliveryCityName`, `deliveryPostalCode`, `deliveryCountryId` (`#swCountry`), `paymentServiceCode` (`#swPay`), `swFormErr`, `swPlace`, `swMini`, `swTotal` | `option` quốc gia/thanh toán, dòng `swMini` |

Kiểm sau Phase 1: `grep -rnE '\bsw[pd]?-[a-z]|--sw-' <phạm vi UI>` = 0; mỗi id trong bảng còn xuất hiện ở cả template và JS
(script nhỏ đếm, lưu output vào report).

## 8. Phase (mỗi phase một commit trên `feat/ui-redesign`)
Writer mọi file code/UI: **Plastic Man**. Mister Terrific review UX song song sau mỗi phase (chỉ đọc, so ảnh 1440/390).
Cyborg chỉ làm thao tác Docker local (backup, apply, `docker cp`, restart, restore), không sửa file trong repo.

| # | Nội dung | File-owner | Kiểm khi xong phase |
|---|---|---|---|
| P0 | Cyborg backup DB + theme resources trong container (mục 9), ghi SHA256 | Cyborg | file backup + checksum |
| P1 | Token + font hai nơi khớp, `sw-*`→`kn-*` class đồng bộ CSS/JS/24 template, giữ id §7 | Plastic Man | check-design trên `main.css`, `head.html`, `foot.html` = 0; diff `:root` main.css vs head.html = trùng; grep `sw-` = 0; id còn đủ |
| P2 | Fragments header/footer/category-strip (DB + theme) | Plastic Man | check-design 0; logo đủ chữ |
| P3 | Trang chủ: `docs/design/home.html` → `P/home` + `T/home.html`; bỏ hết "Black Friday", "Since 1895", copy cũ | Plastic Man | check-design 0; so `screenshots/home-*` |
| P4 | Danh mục + chi tiết (template động + sinh lại `vong-ngoc-demo`), ẩn số lượng khi tồn kho ≤ 1, accordion `details` | Plastic Man | check-design 0; so `category-*`, `product-*` |
| P5 | Giỏ + thanh toán + đơn hàng/tra cứu đơn; mặc định Việt Nam | Plastic Man | check-design 0; so `cart-*`, `checkout-*` |
| P6 | Bài viết về ngọc (nền giấy) | Plastic Man | check-design 0; style-guide cập nhật nếu thêm thành phần |
| P7 | Tài khoản/wishlist/đăng nhập/đăng ký/quên & đặt lại mật khẩu | Plastic Man | check-design 0 |
| P8 | Trang nội dung + 404 | Plastic Man | check-design 0 |
| P9 | Cyborg deploy lên Docker local: dry run → apply page/fragment → `docker cp` static + templates → restart web. Martian Manhunter QA toàn bộ (mục 10) | Cyborg (deploy), Martian Manhunter (QA) | report QA + ảnh |
| P10 | Cyborg dry run rollback (mục 11); `ak-code-review` toàn diff; Batman hậu kiểm + `VERIFY-LOCAL.md` + báo cáo cuối | Cyborg, Batman | rollback đã thử thật |

P1 bắt buộc đi trước và đơn lẻ (đụng mọi file). P2–P8 tuần tự vì cùng `ecommerce.js`/`main.css`; không chạy song song.

## 9. Backup DB local trước khi đổi dữ liệu (P0, Cyborg)
- `docker exec ezy-my-site-e29d88-mysql-1 mysqldump --single-transaction --routines …` toàn bộ DB của site
  → `ezyplatform/.artifacts/backups/ui-revamp-261122/db-before.sql` + `SHA256SUMS`. Mật khẩu lấy từ biến môi trường
  container, không in ra log.
- `docker cp` thư mục `/app/ezyplatform/web/themes/ecommerce-theme/resources` → `theme-resources-before.tgz`.
- `ezy publish` dry run để lưu bản page/fragment hiện tại (backup tự động của apply) trước lần apply đầu.
- Đặt đơn thử sẽ trừ tồn kho món độc nhất → sau QA, restore tồn kho món đó (hoặc restore cả DB về `db-before.sql`
  rồi apply lại UI). Ghi rõ đã làm cách nào.

## 10. QA (Martian Manhunter, P9)
1. `python3 -I docs/design/check-design.py <mọi file đã sửa>` = 0 lỗi, lưu output từng file.
2. Render 1440 / 768 / 390 mọi trang; so ảnh `docs/design/screenshots/`; liệt kê khác biệt (có chủ đích hay lỗi).
3. 390px: `document.documentElement.scrollWidth <= 390` mọi trang.
4. Bàn phím: Tab qua header, PDP, giỏ, form thanh toán; `:focus-visible` luôn thấy; vùng bấm nút chính ≥ 44px.
5. Tương phản AA theo §4.2 (đo thật vài cặp chữ quan trọng: giá, muted, gold-ink trên giấy).
6. Tiếng Việt đủ dấu ở mọi cỡ chữ (tiêu đề serif lớn, nhãn viết hoa nhỏ).
7. Luồng mua thật với 1 sản phẩm trong DB: PDP → thêm vào giỏ (thông báo `role=status`) → `/shopping-cart` → đổi/xoá
   dòng → `/checkout` (đăng nhập tài khoản test) → đặt hàng → trang đơn. Trạng thái: giỏ trống, SP hết hàng, lỗi form
   (thiếu trường, email/điện thoại sai), API lỗi. Ảnh + log mạng lưu ở `.artifacts/reports/ui-revamp/qa/`.
8. Không còn `sw-`, Helvetica/Georgia, nội dung thương hiệu cũ (grep + xem tay), không box-shadow.

## 11. Rollback (Cyborg, P10 dry run thật)
- Code: nhánh riêng, `main` không đổi → rollback repo = không merge / `git revert` theo phase.
- Docker local: restore `db-before.sql` (gồm page/fragment trong DB), giải nén `theme-resources-before.tgz` vào container,
  restart web. Thử thật một lần trên local, đo lại trang chủ về giao diện cũ, rồi apply lại bản mới.
- Hướng dẫn cho Johnny: `.artifacts/reports/ui-revamp/VERIFY-LOCAL.md`.

## 12. Acceptance criteria
Lấy nguyên §13.1 của brief, cộng:
- [ ] check-design 0 lỗi cho từng file đã sửa (output lưu lại).
- [ ] So ảnh 1440/390 với screenshots, khác biệt có chủ đích được ghi lý do.
- [ ] Không `--sw-*`/class `sw-*`, Helvetica/Georgia, nội dung thương hiệu cũ; chỉ còn id `sw*` theo §7 (lệch có chủ đích).
- [ ] Mọi trang §7 dùng chung token/thành phần; thành phần mới có trong `kn.css` + `style-guide.html`.
- [ ] Tiếng Việt đủ dấu; 390px không tràn ngang; focus thấy; AA.
- [ ] Luồng thêm giỏ → giỏ → thanh toán → đặt hàng chạy thật trên Docker local với SP trong DB; trạng thái trống/lỗi đúng.
- [ ] Không số liệu/chính sách/chứng nhận bịa; dữ liệu mẫu có `data-demo="true"`.
- [ ] Không còn tiến trình nền (server 8765 tắt); không commit/in `environment.json`.
- [ ] Rollback đã thử thật; code review toàn diff; `VERIFY-LOCAL.md` có.

## 13. Rủi ro hướng "nền đen cho khu bán hàng"
| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Ảnh mới chụp nền sáng/khác nền sẽ thành "ô sáng" trên canvas đen, mất hiệu ứng hòa nền | Cao | Quy chuẩn chụp §6.2; ảnh catalog hiện có (nguồn fanpage, có chữ chèn) cần chủ shop duyệt từng ảnh |
| Ảnh có chữ chèn sẵn (từ fanpage) nổi bật hơn trên nền đen | Trung bình | Liệt kê ảnh có chữ trong báo cáo, đề nghị thay |
| Đoạn dài trên nền tối mỏi mắt, nhất là mobile ngoài trời | Trung bình | Đọc dài chuyển sang khu giấy; mô tả SP ngắn, `--kn-body` |
| Màu ngọc xanh/vàng trên màn hình rẻ tiền lệch tông, khách nghi ngọc "không thật" | Trung bình | Không chỉnh màu ảnh bằng CSS; ghi chú cho chủ shop chụp ánh sáng thật |
| Form thanh toán nền tối: ô nhập, trạng thái lỗi dễ thiếu tương phản | Trung bình | Dùng đúng `kn-input`/trạng thái lỗi trong style-guide; QA AA trên form |
| Khách Việt quen sàn TMĐT nền trắng → cảm giác "lạ/đắt" có thể giảm chuyển đổi | Thấp–TB, chưa đo | Chỉ chủ shop/số liệu thật trả lời; đề xuất đo sau khi chạy |
| Đổi `sw-`→`kn-` hàng loạt làm vỡ JS | Cao (kỹ thuật) | Giữ id, đổi class một commit, grep + luồng mua thật |

Johnny đã chốt giữ hướng nền đen (13:5x) → không chặn, chỉ trình rủi ro để xác nhận với chủ shop.

## 14. Câu hỏi cần Johnny / chủ shop quyết (đang dùng mặc định)
| # | Câu hỏi | Mặc định đang dùng |
|---|---|---|
| Q1 | Được apply page/fragment + chép theme + restart container lên **Docker local 127.0.0.1** (sau backup + dry run) để chạy luồng mua thật không? Brief cấm "ghi lên server EzyPlatform" | Có, coi Docker local là dev; không có gì ra ngoài máy |
| Q2 | Loại ngọc chính xác từng món (phỉ thúy / Hòa Điền Tân Cương / Thanh Hải) | Giữ tên theo màu như catalog, không gán loại |
| Q3 | Phương thức thanh toán thật (chuyển khoản, COD, thẻ) | Giữ danh sách API trả về, nhãn mẫu `data-demo` |
| Q4 | Chính sách giao hàng, đổi trả, bảo hành | Chỗ giữ chỗ gắn `data-demo="true"` |
| Q5 | Mã bưu chính có bắt buộc không | Giữ bắt buộc như hiện tại (không đụng logic) |
| Q6 | Có thêm tiếng Anh không | Không; chỉ tiếng Việt |
| Q7 | Bỏ hẳn ảnh người mẫu AI khỏi khu bán hàng; có ảnh đeo thật không | Không dùng ảnh AI làm ảnh chính |
| Q8 | Nội dung bài chia sẻ (xuất từ Facebook) | Chỉ làm 1 bài từ nội dung demo đã có |
