---
title: Đổi tên dự án sang ecommerce, bỏ dấu vết nhãn hiệu gốc
status: in-progress
created: 2026-11-22
scope: LOCAL only
---

# Plan: đổi tên dự án sang ecommerce

Outcome: không còn tên nhãn hiệu gốc trong mã nguồn, tài liệu, tên file/thư mục, plugin/theme và bản ghi page/fragment trong DB; chỉ còn các file dữ liệu sản phẩm (tên sản phẩm, SKU, ảnh) sẽ được thay bằng catalog Khang Nguyen. Hiển thị trên UI là "Khang Nguyen".

Không đổi: tên sản phẩm/SKU/ảnh trong data/seed và DB, tiền tố CSS `.sw-`, `.artifacts/`, `my-site/.artifacts/`, `.env`, CORS. Không production, không gửi mail, không đặt đơn thủ công.

## Danh sách đổi

| Từ | Sang |
|---|---|
| thư mục `<old>-ext` (-sdk, -web-plugin, -admin-plugin) | ecommerce-ext (-sdk, -web-plugin, -admin-plugin) |
| thư mục `<old>-theme` (-theme) | ecommerce-theme (-theme) |
| Maven groupId cũ (tiền tố 3 chữ cái của dự án) | com.ecommerce |
| package `<groupId cũ>.<old>_ext` / `..._theme` | com.ecommerce.ext / com.ecommerce.theme |
| class `<Old>`*, và lớp tiền tố 3 chữ cái cũ | Ecommerce*, Ecm* |
| bảng tiền tố 3 chữ cái cũ | ecm_* (RENAME TABLE, giữ dòng) |
| đường dẫn API có đoạn tiền tố cũ | /api/v1/ecm (cả JS và test) |
| mã thanh toán demo (tiền tố viết hoa cũ) | ECM_DEMO và ECM_DEMO_SG (code, DB, khoá cấu hình) |
| chữ hiển thị tên thương hiệu gốc | Khang Nguyen |
| my-site/page-fragments/`<old>` | page-fragments/ecommerce |
| 4 pages `<old>-home`, `-necklaces`, `-customer-service`, `-mesmera-necklace` | home / necklaces / customer-service / product-detail-sample |
| link tới site gốc, cột source_url | bỏ |
| plans/ tài liệu, scripts, README, export.*, SKILL.md | đổi tên file và nội dung |

## Thứ tự (phase)

1. Plan (file này).
2. Backup: DB đầy đủ + sha256, tar mã nguồn (trừ target), danh sách và file plugin/theme đang cài trong container. Nằm trong `.artifacts/backups/rename-ecommerce-261122-1215/`.
3. Đổi tên mã nguồn và tài liệu bằng một script có thể chạy lại, rồi `mvn clean install`.
4. Deploy local: chuẩn bị DB (RENAME TABLE, cập nhật lịch sử script plugin, mã thanh toán), copy plugin/theme mới, restart, apply page/fragment mới, gỡ plugin/theme cũ.
5. Kiểm bằng curl, log, tests/api/run.sh.
6. Grep kiểm cuối và báo cáo.

Rủi ro đã thấy: script drop tables của admin-plugin nằm trong resources của admin-plugin và đã từng chạy tự động cùng create (xem ezy_run_script_histories). Plugin mới sẽ chạy lại script theo tên module mới nên phải bỏ script drop khỏi resources và ghi lịch sử trước khi boot, nếu không bảng ecm_* sẽ bị drop.

## Rollback

- Mã nguồn: giải nén `project-src.tar.gz` vào thư mục trống.
- DB: nạp lại `db-blog-full.sql` (đối chiếu sha256).
- Container: file plugin/theme cũ có trong `container/` của thư mục backup; lệnh cụ thể ghi trong report.md.
