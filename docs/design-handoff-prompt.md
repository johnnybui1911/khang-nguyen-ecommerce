# Prompt giao việc (bản 2): chuyển UI/UX sang hướng SÁNG cho Khang Nguyên · Ngọc Bảo

Dùng khi Agent Dev đã làm (hoặc làm một phần) theo design cũ (nền đen, vàng champagne). Gửi nguyên khối trong
phần "Prompt". Spec mới: `docs/design-brief-khang-nguyen-ngoc-bao.md`. Bộ tham chiếu chạy được: `docs/design/`.

Bản 1 (làm từ đầu theo hướng nền đen) đã bị thay thế và không còn dùng.

## Prompt

```text
Khách đã phản hồi: nền đen trông không ổn. Design được ĐỔI HOÀN TOÀN sang hướng sáng: phương Đông, xanh ngọc bích,
vàng gold, trắng, nhiều ánh nắng, tươi sáng. Phần UI/UX bạn đã làm theo design cũ (nền đen, vàng champagne, Slash)
phải được chuyển sang design mới. Đừng làm lại từ đầu và đừng đụng vào phần chức năng/dữ liệu đã làm đúng
(catalog 35 món, đơn hàng, luồng mua, tính năng đã hoàn thành). Chỉ đổi lớp giao diện.

Repo: /Users/johnny.bui/Desktop/build-with-ezyplatform/ezyplatform
Ngôn ngữ giao diện: tiếng Việt.

## Bước 0: đọc design mới (đã cập nhật, ĐỪNG sửa các file này trừ khi thêm thành phần mới)
1. docs/design-brief-khang-nguyen-ngoc-bao.md (bản 2: đọc hết mục 0 đến 5, 14.2, 14.5; mục 3 là reference lock)
2. docs/design/style-guide.html, docs/design/kn.css (nguồn duy nhất của token và class kn-*)
3. Năm trang mẫu: docs/design/home.html, category.html, product.html, cart.html, checkout.html
   và ảnh chụp trong docs/design/screenshots/ (desktop 1440px, mobile 390px). Đây là hình đích.
4. my-site/AGENTS.md (mục "Design spec"), ecommerce-theme/AGENTS.md
Xem các trang mẫu bằng server tĩnh trong docs/design/ (python3 -m http.server 8765 --bind 127.0.0.1).
Kiểm tra cổng trước khi chạy, dừng tiến trình cũ, và dừng server của bạn khi xong việc.

## Design cũ khác design mới ở đâu (dùng làm bảng chuyển đổi)
Nền: canvas gần đen #05070b -> kem nắng (--kn-canvas) và trắng (--kn-white); KHÔNG còn nền tối ở bất kỳ đâu.
Token cũ -> token mới (kn.css mới đã định nghĩa, hãy thay giá trị bằng token mới, không tự đặt màu):
  --kn-panel, --kn-edge, --kn-rule   -> --kn-white (thẻ), --kn-line (đường kẻ), --kn-line-strong (viền ô nhập/chip)
  --kn-text, --kn-white (chữ trên nền tối) -> --kn-ink (chữ), --kn-jade-deep (tiêu đề, giá)
  --kn-gold dùng làm chữ/giá -> giá = --kn-jade-deep; chữ vàng = --kn-gold-ink; --kn-gold chỉ cho họa tiết, đường kẻ, viền, huy hiệu
  --kn-gold-light, gradient vàng của giá -> bỏ (giá là màu đặc --kn-jade-deep)
  --kn-emerald -> --kn-jade (hành động) hoặc --kn-jade-mist (mảng nền nhạt)
  --kn-paper, --kn-paper-ink, --kn-paper-muted -> --kn-jade-mist (mảng khu bài viết), --kn-ink, --kn-muted
  --kn-horizon-mid, --kn-horizon-end -> dải bình minh (.kn-horizon: trong suốt -> --kn-sun -> --kn-jade-mist)
  --kn-outline, --kn-btn-hover, --kn-black, --kn-danger-text -> bỏ (--kn-line-strong, --kn-jade-deep hover, --kn-danger)
Thành phần:
  Nút chính: chữ nhật bo 6px nền trắng chữ đen -> viên thuốc 52px nền --kn-jade chữ trắng, hover --kn-jade-deep
  Khung ảnh: viền kính mờ trên nền tối -> khung trắng padding 12px, viền --kn-line, bo 20px (hero: viền --kn-gold)
  Ô sản phẩm: lưới 3 cột liền có đường kẻ đen -> thẻ KHÔNG khung, khoảng cách 24px, ảnh vuông bo 20px, tên rồi giá NGAY DƯỚI tên
  Hero: sân khấu đen tràn màn hình -> nền kem có quầng nắng, chữ trái, ảnh trong khung trắng viền vàng bên phải
  Khu bài viết: nền giấy/đen -> một nửa ảnh, một nửa mảng --kn-jade-mist PHẲNG; bảng so sánh nền trắng bo 10px
  Dải kêu gọi: chân trời ấm tối -> dải bình minh sáng + họa tiết kim cương vàng
  Chân trang: nền trắng/đen -> mảng --kn-jade-mist, con dấu vàng, chữ --kn-jade-deep
  Chữ: serif weight 400 màu trắng/vàng -> serif weight 500 màu --kn-jade-deep, một cụm italic màu --kn-gold-ink; đoạn văn sans weight 400 (không phải 300)
  Bo góc: chỉ còn 0, 10px, 20px, 24px (chỉ cặp nút mobile), viên thuốc; bỏ 6px
  Focus ring: vàng -> 2px --kn-jade-deep, lệch 3px

## QUY TẮC HIỂN THỊ GIÁ (bắt buộc, tôi đã nhấn mạnh)
Giá tiền LUÔN đặt ngay DƯỚI tên sản phẩm cho rõ ràng, ở mọi nơi hiển thị sản phẩm:
- Thẻ sản phẩm (trang chủ, danh mục, gợi ý, wishlist): ảnh, rồi tên, rồi giá ngay bên dưới tên. Không đặt giá bên phải cùng hàng với tên.
- Dòng trong giỏ hàng, đơn hàng, tóm tắt đơn ở trang thanh toán: bên cạnh ảnh nhỏ xếp dọc tên, giá, rồi thông tin phụ (kích thước, mã) và nút Xóa.
- Trang chi tiết sản phẩm: nhãn, tên, giá ngay dưới tên, dòng "còn 1 chiếc", rồi mới đến mô tả và nút thêm vào giỏ.
Trang mẫu và ảnh chụp trong docs/design đã làm đúng như vậy; template động và đoạn JS tự dựng DOM (ecommerce.js, dòng giỏ và thẻ sản phẩm)
cũng phải theo. Tổng tiền (tạm tính, tổng cộng) vẫn là hàng nhãn bên trái, số tiền bên phải.

## Quy tắc bắt buộc (giữ nguyên từ trước, vẫn hiệu lực)
- Chỉ dùng token --kn-* và class kn-* trong docs/design/kn.css. Cần thành phần mới: thêm vào kn.css VÀ style-guide.html
  trong cùng thay đổi, kèm lý do. Không tạo màu mới, không viết hex ngoài :root. Không box-shadow. Không nền đen/gần đen.
- Không font ngoài Cormorant Garamond và Be Vietnam Pro. Không còn dấu vết sw-*/swp-*/swd-* hay nội dung thương hiệu cũ.
- Trang trong my-site/pages dựng bằng docs/design/inline-css.py; không sửa tay content.html đã sinh.
- Giữ nguyên các id và tên trường mà ecommerce.js đọc (bảng mục 14.5 của brief). Khi đổi class, đổi đồng bộ cả template
  lẫn đoạn JS tự dựng DOM. Mỗi món ngọc là hàng độc nhất: ẩn bộ chọn số lượng khi tồn kho là 1.
- Dữ liệu mẫu phải gắn data-demo="true" hoặc chú thích; KHÔNG bịa số liệu, chứng nhận, chính sách. Chưa có giấy kiểm định.
- Không publish, không tải media, không ghi lên server EzyPlatform khi chưa chạy dry run và chưa được tôi duyệt.
  Trước khi đổi DB hay nội dung trang trong DB: sao lưu. Không đọc, in hay commit my-site/environment.json hoặc file .env.
- Không đụng backend Java (ecommerce-ext), controller, schema. Chỉ sửa UI: my-site/pages, my-site/page-fragments,
  ecommerce-theme/.../templates, static/css, static/js (chỉ phần liên quan UI), và docs/design khi thêm thành phần.
- Làm theo .claude/rules (process-management: không để lại tiến trình chạy nền).

## Cách làm
1. KIỂM KÊ trước (chỉ đọc), ghi kết quả vào plans/ (theo quy ước tên của repo). Xác định chính xác bạn đã sửa/tạo những
   file nào theo design cũ: theme (static/css/main.css, templates, fragments, ecommerce.js), my-site/pages,
   my-site/page-fragments, và NỘI DUNG ĐANG LƯU TRONG DB nếu đã đồng bộ lên site local (page, fragment `ecommerce/theme`, v.v.).
   Chạy script để đo mức lệch, ghi số lỗi TRƯỚC khi sửa:
      python3 -I docs/design/check-design.py <từng file hoặc thư mục đã sửa>
   Tìm thêm token cũ còn sót: grep các giá trị #05070b, #bc9c5c, #f0dfae, và các tên --kn-panel, --kn-edge, --kn-rule,
   --kn-paper*, --kn-horizon*, --kn-emerald, --kn-gold-light, --kn-black, --kn-text, --kn-outline, --kn-btn-hover.
   Kiểm kê trước của bạn (plans/261122-1400-ui-redesign-prep/inventory.md) có chỗ nhắc "nền tối/vàng": coi là lỗi thời.
2. Lập kế hoạch ngắn: danh sách file cần đổi theo thứ tự dưới đây, ước lượng, rủi ro; gửi tôi duyệt trước khi sửa.
3. Thứ tự chuyển đổi:
   a. Thay bộ token và component bằng docs/design/kn.css mới ở HAI nơi phải khớp: page-fragments/ecommerce/theme/head.html
      và static/css/main.css (nội dung kn.css là nguồn; không giữ lại khối CSS tối nào).
   b. Fragments header/footer/category-strip -> trang chủ -> danh mục -> chi tiết sản phẩm -> giỏ hàng -> thanh toán ->
      khu bài viết về ngọc -> tài khoản/đơn hàng/wishlist/đăng nhập -> trang nội dung -> 404.
   c. Mỗi trang phải khớp trang mẫu và ảnh chụp tương ứng (cấu trúc, thứ tự khu, nhịp, màu), không chỉ đổi màu.
   d. Trang chủ còn nội dung thương hiệu cũ ("Black Friday", "Masters of Light", ảnh banner trống): thay bằng nội dung
      của shop theo trang mẫu home.html. Mục nào chưa có dữ liệu thật thì dùng nhãn data-demo, không bịa.
4. Ảnh sản phẩm: các ảnh hiện có nền xanh đen/tối. Giữ cách xử lý tạm trong kn.css (khung trắng bo 20px, ảnh vuông cắt
   lấy phần khay nhung bằng object-position ở đáy). KHÔNG tô nền trang bằng màu tối để "hợp" với ảnh. Với 35 ảnh đã nhập
   từ fanpage: lập danh sách ảnh có nền tối hoặc có chữ chèn sẵn để chụp lại, ghi vào báo cáo (quy chuẩn ở mục 6.2 của brief).
5. Sau mỗi trang: chạy check-design.py (lỗi phải 0) rồi dựng thử ở 1440px và 390px và so với ảnh chụp trong
   docs/design/screenshots/. Sửa mọi chỗ lệch không có chủ đích.
6. Luồng mua hàng phải chạy thật sau khi đổi: thêm vào giỏ -> giỏ -> thanh toán -> đặt hàng (với sản phẩm thật KN-xxx),
   cùng các trạng thái trống và lỗi. Đơn #77 vẫn phải hiển thị đúng 10.740.000₫.
7. Kiểm tra: bàn phím và focus nhìn thấy, tương phản AA (token mới đã đạt), không tràn ngang ở 390px, tiếng Việt đủ dấu,
   không còn chỗ nào nền đen.

## Quyết định chưa có, đừng tự bịa
Loại ngọc chính xác của từng món, phương thức thanh toán, chính sách giao hàng/đổi trả/bảo hành, có cần mã bưu chính hay
không, có thêm tiếng Anh hay không, số điện thoại shop. Dùng dữ liệu mẫu có nhãn và liệt kê ở cuối báo cáo.

## Báo cáo cuối (bắt buộc)
- Số lỗi check-design.py trước và sau cho từng file/thư mục (sau phải 0).
- Danh sách file đã sửa/tạo; phần nội dung trong DB đã đổi (nếu có) và đường dẫn bản sao lưu.
- Kết quả thử luồng mua hàng và ảnh chụp 1440px/390px của từng trang đã đổi.
- Mọi chỗ lệch spec có chủ đích, kèm lý do. Lệch spec không có lý do là lỗi.
- Danh sách ảnh sản phẩm cần chụp lại trên nền sáng.
- Thành phần mới đã thêm vào kn.css/style-guide.html (nếu có), và việc còn dang dở hoặc cần tôi quyết định.
Kết thúc báo cáo bằng:
Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
```

## Ghi chú cho người giao việc

- Prompt buộc Agent Dev kiểm kê và gửi kế hoạch trước khi sửa. Muốn họ làm thẳng một mạch thì bỏ bước 2 ở mục "Cách làm".
- Em không kiểm tra được trạng thái site đang chạy (file `.env` bị chặn đọc), nên prompt yêu cầu Agent Dev tự xác định chính xác
  mình đã sửa những gì, gồm cả nội dung đã đồng bộ vào DB.
- Hai thứ bạn Dev cần từ a: đồng ý để họ sao lưu rồi đổi nội dung trang trong DB, và quyết định chụp lại ảnh sản phẩm nền sáng.
- Đường dẫn tuyệt đối ở dòng "Repo" là của máy hiện tại; đổi lại nếu Agent chạy ở máy khác.
