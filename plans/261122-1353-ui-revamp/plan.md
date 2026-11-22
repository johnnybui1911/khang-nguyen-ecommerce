---
title: Revamp UI phía khách "Khang Nguyên · Ngọc Bảo"
status: in-progress
branch: feat/ui-redesign (worktree ezyplatform-ui), không merge vào main
owner: Batman (tech lead) · writer duy nhất: Plastic Man · QA: Martian Manhunter · rollback: Cyborg
created: 2026-11-22 13:53 (Asia/Saigon)
source: ui-revamp-brief.md (JohnnyBot), docs/design-brief-khang-nguyen-ngoc-bao.md §3, §7, §12–14
---

# Plan

## Outcome
Mọi trang phía khách dùng ngôn ngữ thiết kế `docs/design` (token/class `kn-*`, nền tối khu bán hàng,
nền giấy khu bài viết, Cormorant Garamond + Be Vietnam Pro), giao diện tiếng Việt, hành vi mua hàng giữ nguyên
và chạy thật trên Docker local.

## Quyết định đã chốt
- Johnny chốt (13:5x): team tự triển khai hết, không chờ duyệt plan; giữ hướng nền đen cho khu bán hàng.
- "Server EzyPlatform" trong lệnh cấm publish = mọi môi trường ngoài máy này. `my-site/environment.json` chỉ trỏ
  `http://127.0.0.1:9090` (Docker local `ezy-my-site-e29d88`), nên được phép apply page/fragment và chép theme
  lên Docker local để chạy luồng mua, với điều kiện: backup DB + volume trước, dry run trước mỗi lần apply,
  không tải media lên server. Không có production trong phạm vi.
- `environment.json` không đọc, không in, không commit (đã gitignore ở `my-site/.gitignore`). Nếu cần trong worktree
  thì chép file nguyên vẹn, không mở nội dung.
- Điểm chưa có quyết định (loại ngọc, thanh toán, chính sách, mã bưu chính, tiếng Anh): dùng dữ liệu mẫu gắn
  `data-demo="true"`, ghi vào báo cáo cuối. Mã bưu chính: giữ trường nhưng không bắt buộc nếu chỉ cần đổi ở UI;
  nếu logic kiểm tra nằm ở Java thì giữ nguyên và ghi lại.

## Non-goals
Không sửa Java/controller/ecommerce-ext, không đổi schema DB, không publish production, không tải media,
không merge main, không làm các tính năng trong `plans/BACKLOG.md`.

## Phase (mỗi phase một commit trên feat/ui-redesign)
| # | Nội dung | File chính |
|---|---|---|
| 1 | Token + font (hai nơi khớp), gộp khối CSS trùng, đổi `sw-` → `kn-` (class) đồng bộ CSS/JS/template; giữ id mà `ecommerce.js` đọc (§14.5) | `static/css/main.css`, `page-fragments/ecommerce/theme/head.html`, `static/js/ecommerce.js`, mọi template |
| 2 | Fragments header, footer, category-strip (DB + theme) | `page-fragments/ecommerce/{header,footer,category-strip}`, `templates/fragments/*` |
| 3 | Trang chủ (thay nội dung thương hiệu cũ) | `my-site/pages/home`, `templates/home.html` |
| 4 | Danh mục + chi tiết sản phẩm (ẩn số lượng khi tồn kho = 1) | `templates/products.html`, `templates/product.html`, `pages/vong-ngoc-demo` (sinh bằng inline-css.py) |
| 5 | Giỏ hàng + thanh toán (tiếng Việt, mặc định Việt Nam) | `templates/cart.html`, `templates/checkout.html`, phần JS liên quan |
| 6 | Khu bài viết về ngọc (nền giấy) | trang trong `my-site/pages` theo brief §7 |
| 7 | Tài khoản/đơn hàng/wishlist/đăng nhập/đăng ký/quên & đặt lại mật khẩu | các template tương ứng |
| 8 | Trang nội dung (customer-service, stores, contact, size-guide, appointment, newsletter, gift-card) + 404 | template + `pages/customer-service` |
| 9 | Deploy lên Docker local (sau backup), chạy luồng mua thật + trạng thái trống/lỗi, ảnh 1440/390 | `.artifacts/reports/ui-revamp/` |

## Acceptance criteria
- `python3 -I docs/design/check-design.py <file>` = 0 lỗi cho mọi file đã sửa (lưu output).
- `grep -rnE '\bsw[pd]?-|--sw-' ` trên phần UI = 0 (trừ id `sw*` được giữ theo hợp đồng JS).
- Luồng thêm vào giỏ → giỏ → thanh toán → đặt hàng chạy trên Docker local với sản phẩm thật; có ảnh/log; giỏ trống
  và lỗi form hiển thị đúng.
- 390px không tràn ngang; focus nhìn thấy; AA; tiếng Việt đủ dấu (QA Martian Manhunter).
- Rollback đã dry run (Cyborg); code review toàn bộ diff; `VERIFY-LOCAL.md` cho Johnny.

## Rollback
Quay về bản main: restore backup DB/volume đã tạo ở phase 9, hoặc chép lại theme + apply lại page/fragment từ
worktree main. Chi tiết trong `.artifacts/reports/ui-revamp/VERIFY-LOCAL.md`.
