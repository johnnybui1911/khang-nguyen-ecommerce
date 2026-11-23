# Task: Revamp UI/UX "Khang Nguyên · Ngọc Bảo" (yêu cầu gốc của Johnny Bui, 2026-11-22 13:52)

WORKTREE: /Users/johnny.bui/Desktop/build-with-ezyplatform/ezyplatform-ui  (branch feat/ui-redesign, dựa trên main c674c91 + commit 770b7a6 chứa design docs)
KHÔNG sửa trong /Users/johnny.bui/Desktop/build-with-ezyplatform/ezyplatform (main). Mọi đường dẫn bên dưới tính từ worktree.

Phạm vi: toàn bộ giao diện phía khách (trang chủ, danh mục, chi tiết sản phẩm, giỏ hàng, thanh toán,
tài khoản/đơn hàng/wishlist/đăng nhập, trang nội dung, 404). Ngôn ngữ giao diện: tiếng Việt.

## Bước 0: đọc trước khi làm bất cứ việc gì (theo thứ tự)
1. docs/design-brief-khang-nguyen-ngoc-bao.md  (spec đầy đủ; đọc hết, đặc biệt mục 3, 4, 7, 12, 13, 14)
2. my-site/AGENTS.md  (có mục "Design spec" là bắt buộc) và ecommerce-theme/AGENTS.md
3. docs/design/style-guide.html  (mọi token, thành phần, trạng thái)
4. Năm trang mẫu: docs/design/home.html, category.html, product.html, cart.html, checkout.html
   và ảnh chụp tương ứng trong docs/design/screenshots/ (desktop 1440px, mobile 390px)
5. docs/design/kn.css  (nguồn duy nhất của token và class kn-*)
Mở các trang mẫu bằng một server tĩnh trong docs/design/ (python3 -m http.server 8765 --bind 127.0.0.1).
Kiểm tra cổng trước khi chạy, dừng tiến trình cũ, và dừng server của bạn khi xong việc.

## Mục tiêu
Mọi trang phía khách đạt đúng ngôn ngữ thiết kế trong docs/design: nền tối cho khu bán hàng (Slash),
nền giấy cho khu bài viết (Franco Maria Ricci), serif Cormorant Garamond + sans Be Vietnam Pro,
vàng chỉ dùng đúng vai trò, ảnh sản phẩm thật là trung tâm. Giữ nguyên toàn bộ hành vi mua hàng.

## Quy tắc bắt buộc (không thương lượng)
- Chỉ dùng token --kn-* và class kn-* trong docs/design/kn.css. Cần thành phần mới: thêm vào kn.css
  VÀ style-guide.html trong cùng thay đổi, kèm lý do. Không tạo màu mới, không viết hex ngoài :root.
- Không box-shadow, không font ngoài hai font đã chọn (Bodoni không có dấu tiếng Việt), không còn
  bất kỳ dấu vết sw-*/swp-*/swd-* hay nội dung của thương hiệu cũ trong code.
- Không sửa tay my-site/pages/vong-ngoc-demo/content.html (file sinh tự động). Trang trong my-site/pages
  dựng bằng docs/design/inline-css.py (xem mục 14.3 của brief).
- Giữ nguyên các id và tên trường mà ecommerce.js đọc (bảng ở mục 14.5 của brief). Khi đổi class,
  đổi đồng bộ cả template lẫn đoạn JS tự dựng DOM. Mỗi món ngọc là hàng độc nhất: ẩn bộ chọn số lượng khi tồn kho là 1.
- Dữ liệu mẫu (giá, tên sản phẩm, phương thức thanh toán, chính sách) phải gắn data-demo="true" hoặc chú thích rõ,
  và KHÔNG được bịa số liệu, chứng nhận, lời hứa pháp lý. Chưa có giấy kiểm định: không thiết kế khu "chứng nhận".
- Dữ liệu seed trong data/seed và nội dung các trang cũ (home, necklaces, product-detail-sample) lấy từ một
  thương hiệu khác, chỉ để demo. Thay hết bằng nội dung của shop; không đưa tên, ảnh, mô tả của họ vào bản thật.
- Không publish, không tải media, không ghi lên server EzyPlatform nếu chưa chạy dry run và chưa được Johnny duyệt rõ ràng.
- Không in, đọc ra log hay commit my-site/environment.json (chứa token admin).
- Không đụng backend Java (ecommerce-ext), DB, hay controller. Chỉ sửa UI: my-site/pages, my-site/page-fragments,
  ecommerce-theme/.../templates, static/css, static/js (chỉ phần liên quan UI), và docs/design khi thêm thành phần.
- Làm theo .claude/rules (đặc biệt process-management: không để lại tiến trình chạy nền).

## Cách làm
1. Trước khi viết code: lập kế hoạch ngắn trong plans/ (theo quy ước tên của repo), liệt kê file sẽ sửa theo thứ tự
   ở mục 13 của brief, và gửi Johnny duyệt. Hỏi Johnny xác nhận hướng "nền đen cho khu bán hàng" nếu thấy có rủi ro.
2. Thứ tự: token + font (hai nơi phải khớp: page-fragments/ecommerce/theme/head.html và static/css/main.css) →
   fragments header/footer/category-strip → home → danh mục → chi tiết sản phẩm → giỏ hàng → thanh toán →
   khu bài viết về ngọc → trang tài khoản/đơn hàng/wishlist/đăng nhập → trang nội dung → 404.
3. Mỗi trang xong: chạy  python3 -I docs/design/check-design.py <file đã sửa>
   Lỗi phải bằng 0. Script chỉ bắt lỗi kỹ thuật; bố cục và cảm giác phải tự so với ảnh chụp ở 1440px và 390px.
4. Giỏ hàng và thanh toán phải chạy thật: cần một sản phẩm thật trong DB (tạo local, sao lưu DB trước khi đổi dữ liệu),
   thử đủ luồng thêm vào giỏ → giỏ → thanh toán → đặt hàng, và các trạng thái trống, lỗi.
5. Kiểm tra: bàn phím và focus nhìn thấy, tương phản AA, không tràn ngang ở 390px, tiếng Việt đủ dấu.

## Những quyết định chưa có, đừng tự bịa
Loại ngọc chính xác của từng món, phương thức thanh toán, chính sách giao hàng/đổi trả/bảo hành, có cần mã bưu chính hay không,
có thêm tiếng Anh hay không. Dùng dữ liệu mẫu có gắn nhãn, và liệt kê các điểm này ở cuối báo cáo để Johnny xác nhận với chủ shop.

## Báo cáo cuối (bắt buộc)
- Danh sách file đã sửa/tạo.
- Kết quả check-design.py cho từng file (phải 0 lỗi) và kết quả thử luồng mua hàng.
- Mọi chỗ lệch spec có chủ đích, kèm lý do. Lệch spec không có lý do là lỗi.
- Thành phần mới đã thêm vào kn.css/style-guide.html (nếu có).
- Việc còn dang dở hoặc cần Johnny quyết định.
Kết thúc báo cáo bằng:
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
