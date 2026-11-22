# Báo cáo: thay 47 món mẫu bằng 35 món Khang Nguyên (LOCAL)

**Status: DONE_WITH_CONCERNS.** 35 sản phẩm Khang Nguyên đã lên web local, 47 món cũ + ảnh cũ đã xoá khỏi DB/repo; đơn #77 vẫn 10.740.000₫. Không restart web.

## Bằng chứng (chạy thật)
| Tiêu chí | Kết quả |
|---|---|
| Danh sách `GET /api/v1/products?productTypes=SINGLE` (nguồn của trang `/products`) | count 35, total 35 |
| Ảnh | 35/35 `GET /api/v1/media/<tên>` → 200 |
| Giá | bảng giá: 35 dòng VND, min 1.200.000, max 4.800.000 (đã xoá 35 dòng SGD tạm do importer tạo) |
| Chi tiết SP | `/products/KN-001`, `KN-018`, `KN-035` → 200; `/api/v1/products/<mã>/details` → 200 |
| Đơn #77 | `/api/v1/ecm/order-status?orderId=77&email=…` → PAID, total `10,740,000₫`, 2 dòng tên trung tính "Sản phẩm đã ngừng bán A/B" (2×4.380.000 + 1×1.980.000); email sai → 404 |
| Chữ cũ trong repo | `grep -rIil` với 3 từ khoá cũ (Johnny đã nêu), loại `target` và `.artifacts` → 0 file |
| Chữ cũ trong DB | quét 626 cột text: còn 0 ở sản phẩm, đơn, trang, fragment, chỉ mục; chỉ còn bảng mail sandbox (xem Concerns) |
| Cờ "CẦN XÁC NHẬN" | giữ trong `products.csv`; web (API + trang SP) 0 lần xuất hiện |
| Importer chạy lại | lần 2: `categories_existing 4, products_existing 35`, không tạo thêm |
| Danh mục | 4: set-nhan-vong (22), vong-ngoc (7), chuoi-hat (4), nhan-ngoc (2) |
| Admin | hash mật khẩu admin#1 đã khôi phục; file mật khẩu tạm đã xoá |

## Đã làm
- Backup `.artifacts/backups/khang-nguyen-catalog-261122/`: `db-blog-before-catalog.sql`, `db-blog-after-import-before-cleanup.sql` (sha256 `565397250c18…`, đủ trong `SHA256SUMS`), `cleanup.sql` (lệnh đã chạy), `old-media-files.tgz` (79 ảnh cũ), `product-attributes.json.old`, `admin1-hash.old`.
- `ecommerce-ext/scripts/import-catalog.py` viết lại: đọc `data/seed/khang-nguyen/`, idempotent, chỉ giá VND, kho theo `ton_kho`, có `--dry-run`.
- DB (1 transaction): xoá sản phẩm 1–47 (trừ 33, 35 giữ cho đơn #77, đổi tên trung tính + ARCHIVED, bỏ ảnh), 13 danh mục cũ, mọi dòng phụ thuộc, 79 bản ghi media + 79 file ảnh trong container, 2 trang mẫu (id 6, 7), toàn bộ lịch sử bài viết (`ezyarticle_post_histories`, vì chứa tên cũ) và chỉ mục tìm kiếm có chữ cũ.
- Repo: xoá `data/seed/{source,images,scripts,SAMPLE-DATA.txt}`, `my-site/pages/{necklaces,product-detail-sample}`; `product-attributes.json` → `{}` (cả bản live trong container, phục vụ ngay không restart); sửa 2 dòng trong 2 file plan cũ.

## Concerns / chưa kiểm chứng
1. **Mail sandbox** (`ezymail_mails` 118 dòng, `ezymail_mail_participants` 160) vẫn chứa tên cũ trong người gửi/nội dung — để nguyên theo chỉ đạo; muốn xoá cần Batman duyệt.
2. **Trang chủ còn nội dung thương hiệu cũ không phải catalog**: "Black Friday – Up to 40% off", "Masters of Light Since 1895" (ảnh chụp trong `ui-redesign-prep`). Nằm trong page `home`/header (DB) — để bước UI/UX.
3. Ảnh banner/danh mục cũ (media 1–21, 60–63) đã xoá; các vùng ảnh trên home hiện trống/nền trơn.
4. SP 33 và 35 vẫn tồn tại trạng thái ARCHIVED (không hiện trên web) vì đơn #77 tham chiếu; kho/giá cũ của chúng đã xoá.
5. Chưa chụp trình duyệt cuộn thật cho ảnh lazy-load từ thẻ 17 trở đi (API trả 200 cả 35).
6. Tên file ảnh seed lấy từ fanpage; một số ảnh có chữ chèn sẵn từ nguồn.
Rollback: nạp `db-blog-before-catalog.sql` (hoặc bản `after-import`) + giải nén `old-media-files.tgz` vào `/app/ezyplatform/upload/images`.
