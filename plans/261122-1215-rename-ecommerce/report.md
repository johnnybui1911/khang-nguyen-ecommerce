# Báo cáo: đổi tên dự án sang ecommerce (LOCAL)

**Status: DONE_WITH_CONCERNS.** Đổi tên xong; sau restart #3 (13:01, 22/11) `/account` khách → 302 /login, trang storefront dùng layout ecommerce-theme có "Khang Nguyen", log 0 NPE. Concern: nguyên nhân gốc runtime của lỗi 500 `/account` chưa chứng minh được tuyệt đối (xem mục "Cập nhật sau restart #3"). Đã dùng đủ 3 lần restart.

## Đã làm (có bằng chứng chạy thật)
| Bước | Kết quả |
|---|---|
| Backup | `.artifacts/backups/rename-ecommerce-261122-1215/`: `db-blog-full.sql` sha256 `b943f77c…9751e`; `db-blog-pre-db-change.sql` (ngay trước khi đổi DB) sha256 `51e27a9c…fda03`; `project-src.tar.gz` sha256 `d81d54e2…84d59ea`; bản copy plugin/theme cũ trong `container/`; file đăng ký `runtime-registry/` |
| Đổi tên code/docs | 135 file đổi nội dung, 206 file/thư mục đổi tên; module `ecommerce-ext`, `ecommerce-theme`; package `com.ecommerce.ext`, `com.ecommerce.theme`; API `/api/v1/ecm/*`; bảng `ecm_*` |
| Build | `mvn clean install` cho ext (sdk, admin-plugin, web-plugin) và theme: exit 0, đủ 4 jar |
| DB | `RENAME TABLE` `ecm_newsletter` (12 dòng), `ecm_wishlist` (2 dòng) — số dòng bằng bản backup; đổi tên index; `ezy_run_script_histories` chuyển sang `ecommerce-ext/create_ecm_tables.sql`, bỏ dòng script xoá bảng; đổi `ezy_settings`, mã thanh toán `ECM_DEMO_SG`, shop `ecommerce-sg`, danh mục, slug/meta/link của 8 page+fragment (đổi tại chỗ, giữ id) |
| Pages/fragments | `ezy publish apply` 8/8 "published and verified"; backup ở `my-site/.artifacts/backups/20261122-124315` |
| Deploy | `.artifacts/releases/rename/deploy.sh` (kiểm sha256 release trước); web :8080 + admin :9090 lên lại, **Đính chính:** câu "0 ERROR" trước đây sai vì chỉ grep `ERROR`; log lúc đó có NPE `/account` (grep `NullPointer`). Từ boot #3: 0 NPE, 0 WARN/ERROR, 0 stacktrace |
| Test API | `tests/api/run.sh m1 m2 m3 f1 f2 f3 f5` (PRODUCT_ID=47, CURRENCY_ID=6): **61 pass, 2 fail** (2 fail: `GET /account` HTTP 500). Log: `/tmp/rename-api-test.log` |
| Dọn dữ liệu test | `cleanup-test-data.sh --apply --confirm DELETE-TEST-DATA`: xoá 3 user test, giữ `johnny.test1`, đơn #77 (PAID, `ECM_DEMO_SG`), 47 sản phẩm, admin |
| Bước 6 | `grep -rIil` chữ cũ ngoài `.artifacts`/`target`: 0 file (đã xoá dòng log hook của chính phiên này); `find -iname`: 0; `com.swx\|swx_\|/api/v1/swx`: 0. DB: 0 bảng `swx_`, 0 dòng setting/script/page/link/đơn mang tên cũ |
| Còn lại có chủ đích | Chỉ dữ liệu sản phẩm mã `SWX-*` (5 SKU) trong `product-attributes.json`, `data/seed/source/products.csv`, `data/seed/scripts/build-source.py`, `data/seed/SAMPLE-DATA.txt` và 10 tên ảnh `swx-*.jpg` |

## Rollback
`bash .artifacts/releases/rename/rollback.sh` — kiểm sha256 dump, đưa plugin/theme cũ từ backup vào container, nạp lại DB `db-blog-pre-db-change.sql`, khôi phục file đăng ký, restart 1 lần. Plugin/theme cũ chưa bị xoá: còn trong backup và trong `/tmp/old-*` của container.

## Chưa kiểm chứng
- Layout/giao diện thật của theme mới (chưa mở trình duyệt; xem lỗi dưới).
- Page `/standalone-pages/*` trả 404 vì page ở trạng thái DRAFT (như trước khi đổi).
- Chữ "Khang Nguyen" trên trang live: 0 lần xuất hiện vì layout theme chưa đúng (xem dưới); chưa kiểm được.
- Tồn đọng chủ ý, không viết lại: lịch sử bài viết/sản phẩm, mail đã gửi (`ezymail_*`), chỉ mục tìm kiếm (`ezy_data_indices`) còn chữ cũ; tên file ảnh `swx-*` trong bảng media.
- Tồn kho sản phẩm 47 chỉ còn 8 (mỗi lần chạy test mua 2 đơn vị, kho không được hoàn lại).
- Docs/kế hoạch cũ vẫn dùng chữ "clone" trong tên thư mục kế hoạch (`*-ecommerce-clone`).

## Việc còn tồn đọng (lịch sử trước restart #3; đã xử lý ở mục "Cập nhật sau restart #3")
1. **Theme mới không thắng `welcome` khi ghép layout.** `GET /order-status`, `/customer-service` render nội dung của theme mới nhưng bọc bằng layout `page` của theme `welcome` (tiêu đề trống, header rỗng). Bản cũ render đúng (`<title>Order Status | …`). Trong `.runtime/web/themes.txt` cả hai theme đều `active=true`; thứ tự ưu tiên có vẻ phụ thuộc tên theme. Hướng sửa đề xuất: đặt `welcome` `active=false`, restart 1 lần (lần thứ 3).
2. **`GET /account` 500 (NPE trong `EcmAccountViewController`).** Bản cũ từng bị đúng lỗi này (nguyên nhân gốc đã ghi ở báo cáo phase 8 cũ: classloader/`UserId` bị nhân đôi) rồi hết. Jar mới có cấu trúc class giống hệt jar cũ (38 mục, chỉ đổi tên), thư mục `lib` chỉ có 2 jar ext. Chưa chứng minh được nguyên nhân; nhiều khả năng cùng gốc với mục 1 (thứ tự nạp theo tên). Cần restart lần 3 để thử, sau đó chạy lại `run.sh m1`.
3. Sau khi 1–2 ổn: xoá `/tmp/old-*` và thư mục cũ trong container (hiện giữ để rollback).

## Cập nhật sau restart #3 (bằng chứng chạy thật)
**Nguyên nhân `/account` 500 (đã chứng minh trước khi sửa):** `AccountController.accountGet(..., @UserId long userId)` đọc `UserId` kiểu nguyên thủy; handler sinh tự động unbox `getArgument(UserId)` = null → NPE → 500, cho cả khách lẫn user (token giả vẫn 500, trong khi `/account/edit` → 302). Chỉ `/account` bị vì là URI duy nhất đăng ký 2 lần (controller gốc + `EcmAccountViewController`); lỗi cũ y hệt có trong log trước khi đổi tên (38 NPE) nên **không phải hồi quy do đổi tên**. Package `com.ecommerce.ext` có trong `module.properties` (`package=com.ecommerce.ext`), `dependencies=ecommerce,ezylogin,ezyaccount`; thứ tự classpath phẳng ezymail, ecommerce-ext, ecommerce, ezyaccount… Đã loại trừ: jar web-sdk trùng, thứ tự plugin, khác biệt cấu hình, compat guard 404, interceptor ecommerce, `RequestURIDecorator`. Chưa chứng minh vì sao interceptor xác thực nền tảng bỏ qua đúng URI `/account`.

**Đã sửa (1 lần restart gộp):**
1. `EcmCompatGuardInterceptor` (lớp có sẵn, không thêm class): với `AccountController`, nếu không có UserId và không có token thì ném `UserInvalidAccessTokenException` (→ 302 /login); có token hợp lệ thì gán UserId. Kèm dòng log INFO chẩn đoán.
2. Theme `welcome` `active=false` trong `.runtime/web/themes.txt` (kiểm sau boot vẫn `false`).

**Kết quả:** sau boot, dòng log chẩn đoán xuất hiện 0 lần và `/account` vẫn 302 → xác nhận 302 đến từ interceptor nền tảng/exception handler, không phải từ guard mới. Vì vậy chưa tách được đóng góp của (1) và (2); nhiều khả năng lỗi phụ thuộc thứ tự nạp theo build như đã ghi ở phase 8. Lỗi này nên coi là *không tái hiện* chứ chưa là *đã hiểu hoàn toàn*; guard (1) giữ lại làm phòng thủ.

| Kiểm tra | Kết quả |
|---|---|
| `/` | 200 |
| khách `/account`, `/account/edit` | 302 → /login |
| `/order-status`, `/customer-service`, `/` | 200, "Khang Nguyen" ×4 mỗi trang |
| `/abc-xyz` | 404 |
| API order-status đơn 77 | PAID, total `10,740,000₫`, đúng 6 khoá cho phép |
| `PRODUCT_ID=47 run.sh m1` | 6/6 pass (gồm khách `/account` → login, user `/account` → 200) |
| Log từ boot 06:01:37Z | 0 NPE, 0 WARN/ERROR, 0 stacktrace |
| Dọn test | xoá user test #262 (có backup SQL); còn 1 user, 1 đơn (#77) |

**Backup/rollback:** `.artifacts/backups/account-fix-261122/` (jar cũ/mới, `themes.txt.old`, `test-user-262.sql`, `SHA256SUMS`, `ROLLBACK.txt`). Jar mới sha256 `34faec09…140d` (khớp trong container). Rollback = 2 lệnh `docker cp` trong `ROLLBACK.txt` + restart (cần Batman duyệt).
**Chưa kiểm chứng:** mở trình duyệt thật cho giao diện; chỉ chạy m1 (tồn kho 47 chỉ 8, không chạy m3). `/tmp/old-*` và plugin cũ trong container **chưa xoá**, chờ Batman xác nhận.

## Giới hạn đã tuân thủ
Chỉ LOCAL, không đụng production/.env/mail/CORS, không đặt đơn thủ công (đơn do run.sh tạo đã dọn), token/secret không in ra log.
