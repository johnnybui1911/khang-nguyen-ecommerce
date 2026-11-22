---
title: Khang Nguyên — giá, liên hệ, đặt lịch, lọc mệnh, SePay, phí ship
status: BACKLOG (anh Johnny hoãn 22/11 13:2x)
created: 2026-11-22
scope: LOCAL only
writer: Plastic Man (một writer duy nhất)
---

Status: BACKLOG (anh Johnny hoãn 22/11 13:2x)

# Kế hoạch tính năng Khang Nguyên

## Outcome, ràng buộc, non-goals, acceptance
**Outcome:** 7 hạng mục anh Johnny chốt lúc 13:15 chạy được trên LOCAL, có bằng chứng chạy thật.
**Ràng buộc:** không production; không đọc/sửa file môi trường dotenv; không CORS; không mail/SMS; không đặt đơn tay (đơn test qua `ecommerce-ext/tests/api/run.sh` hoặc script test riêng rồi dọn); backup DB đầy đủ + sha256 vào `.artifacts/backups/` trước migration; tối đa **1 restart web** cho cả đợt (gộp mọi thay đổi, backup jar + lệnh rollback trước; nếu cần restart thêm thì dừng và báo Batman); đơn #77 giữ nguyên.
**Non-goals:** làm lại UI/UX (anh Johnny gửi design sau; dùng style sẵn có, giữ tiền tố `.sw-`); tích hợp hãng vận chuyển; đăng ký SePay thật; gửi mail/SMS.
**Acceptance:** danh sách "Kiểm cuối" ở cuối file này.

## Điều kiện bắt đầu (cổng)
Phase 1+ chỉ chạy khi việc catalog 35 món Khang Nguyên có report DONE/DONE_WITH_CONCERNS. Lúc 13:20: chưa có `data/seed/khang-nguyen/`, chưa có plan/report catalog. Phase 0 (file này) không đụng code/DB/seed.

## Ghi chú kỹ thuật đã quét (13:20)
- DB MySQL tên `blog` (container `ezy-my-site-e29d88-mysql-1`); bảng `ecm_*` hiện có: `ecm_newsletter`, `ecm_wishlist`. Bảng sản phẩm nằm nhóm `ecommerce_*`; cần xác định cột giá thật ở Phase 2 (`ecommerce_products` không có cột `price`; có thể ở bảng giá riêng).
- Mã nguồn phần mở rộng: `ecommerce-ext/ecommerce-ext-{sdk,web-plugin,admin-plugin}` (package `com.ecommerce.ext`; đã có `EcmCompatGuardInterceptor`, `EcmRateLimiter`, `EcmCheckoutApiController`, `EcmOrderApiController`...). Theme: `ecommerce-theme/ecommerce-theme-theme`.
- Test API có sẵn: `ecommerce-ext/tests/api/run.sh`, `check-order-status.py`.

## Chốt phạm vi SePay (22/11)
Xác thực bằng header `Authorization: Apikey <KEY>` (không phải `Bearer`); thành công trả HTTP 200, body `{"success": true}`; SePay gửi lại tối đa 7 lần nên bắt buộc chống ghi trùng theo id giao dịch SePay. Không dùng key thật, không sửa `.env`, không production.

## Tóm tắt SePay (đọc từ docs.sepay.vn và sepay.vn/bang-gia, 2026-11-22)
- **VietQR động:** ảnh QR tại `https://qr.sepay.vn/img?acc=<STK>&bank=<NGAN_HANG>&amount=<SO_TIEN>&des=<NOI_DUNG>` (trang docs hiện ghi miền `vietqr.app/img` với cùng tham số; công cụ tạo ở qr.sepay.vn; sẽ chọn miền theo kiểm tra thật khi triển khai). Chỉ `acc` và `bank` bắt buộc. `des` tối đa 94 ký tự, tự bỏ dấu và ký tự đặc biệt (chỉ giữ chữ, số, khoảng trắng). Tuỳ chọn: `template` (trống/`compact`/`qronly`/`standee`), `showinfo`, `holder`, `store`. **Nội dung CK = mã đơn** (chỉ chữ và số, ví dụ `KN00077`).
- **Webhook:** SePay gửi `HTTP POST` JSON (mặc định) tới URL cấu hình. Trường: `id` (số, id giao dịch SePay), `gateway`, `transactionDate` (`yyyy-MM-dd HH:mm:ss`), `accountNumber`, `subAccount`, `code` (mã thanh toán SePay tách theo tiền tố đã cấu hình, có thể null), `content`, `transferType` (`in`/`out`), `description`, `transferAmount` (số), `accumulated`, `referenceCode`.
- **Phản hồi hợp lệ:** HTTP 200 hoặc 201 **và** body `{"success": true}`; SePay chờ tối đa 30 giây. Thiếu `success:true` chỉ bị ghi thất bại, không retry.
- **Xác thực:** 4 kiểu (HMAC-SHA256, API Key, OAuth 2.0, không xác thực). Với API Key, SePay gửi header `Authorization: Apikey <KEY>`. Quyết định: dùng API Key, so sánh hằng thời gian; thiếu/sai/key cấu hình trống → 401.
- **Retry và idempotent:** SePay chỉ gọi lại khi không kết nối/quá thời gian, hoặc khi bật "gửi lại khi HTTP ngoài 200–299" và nhận mã lỗi. Tối đa 7 lần, cách 1, 1, 2, 3, 5, 8, 13 phút (Fibonacci, khoảng 33 phút). Cùng giao dịch có thể đến nhiều lần: chống trùng theo `id`, cột `UNIQUE` + `INSERT IGNORE`. Quyết định: webhook trùng trả 200 `{"success": true}` (đã xử lý), không đổi trạng thái đơn.
- **IP gửi webhook (whitelist tuỳ chọn):** 172.236.138.20, 172.233.83.68, 171.244.35.2, 151.158.108.68, 151.158.109.79, 103.255.238.139, 45.57.137.67 và 2 địa chỉ IPv6. Không bắt buộc vì đã có API key.
- **Test:** SePay có Test Mode và "Gửi thử"/"Mô phỏng giao dịch"; ở local dùng `curl` giả lập payload.
- **Gói/phí (sepay.vn/bang-gia):** FREE 0đ/tháng, 50 giao dịch/tháng (vượt vẫn cho phép, tính phí sau), có Webhook và API; STARTUP 120.000đ/tháng cho 180 giao dịch/tháng; các bậc cao hơn theo số giao dịch. Dữ liệu giao dịch lưu 6 tháng. Webhook không áp dụng cho QR tĩnh NAPAS. **Chưa xác minh:** giá vượt mức từng gói, điều kiện từng ngân hàng, thời gian duyệt tài khoản.
- **Mã nhận diện:** SePay tách `code` theo tiền tố thanh toán cấu hình trong tài khoản (ví dụ `KN`). Fallback của ta: tìm mã đơn bằng regex trên `content` khi `code` rỗng.

## Phase, owner, kiểm tra
Một writer cho mọi file; không song song. Migration có rollback đi kèm. Mọi thay đổi gộp vào 1 build jar + 1 restart.

| Phase | Nội dung | File owner (dự kiến) | Kiểm tra hẹp nhất |
|---|---|---|---|
| 0 | Plan + tóm tắt SePay | file này | tồn tại |
| 1 | Backup DB + sha256, backup jar, rollback script | `.artifacts/backups/khang-nguyen-features/` | `shasum -c`, dump có "Dump completed" |
| 2 | **Giá** 1–5 triệu (chuỗi 1.200.000, nhẫn 2.000.000, set 3.500.000, vòng 4.800.000): cập nhật `data/seed/khang-nguyen/products.csv` + DB; cờ CẦN XÁC NHẬN giữ ở seed/DB, ẩn ở UI; import chạy lại không nhân đôi | seed csv, script import của catalog | SQL: 35 SP, min/max trong [1.000.000, 5.000.000]; import lần 2 vẫn 35 |
| 3 | **Migration** `ecm_appointment`, `ecm_payment_txn`, `ecm_product_menh`, cấu hình; kèm rollback SQL | `ecommerce-ext-sdk` (entity/repository), `ecommerce-ext/scripts/` | chạy migration + rollback trên bản sao rồi áp lên DB thật |
| 4 | **Liên hệ** chỉ Facebook + Gọi điện (tạm bỏ Zalo, anh Johnny chốt 22/11): cấu hình một chỗ (placeholder ghi rõ), nút ở trang chi tiết SP + footer, thanh nổi cố định trên mobile | web-plugin, theme (fragment, CSS `.sw-`) | curl trang SP và footer có `facebook.com/khangnguyenngocbao68` và `tel:`, không có `zalo` |
| 5 | **Đặt lịch** tại 5 Đặng Dung, Ba Đình, Hà Nội: form (tên, SĐT, ngày giờ, SP tuỳ chọn, ghi chú); SĐT VN (0/+84, 10 số); chặn giờ quá khứ; honeypot + `EcmRateLimiter`; lưu `ecm_appointment`; admin xem/đổi trạng thái mới/đã xác nhận/huỷ; không mail/SMS | web-plugin, admin-plugin, theme | POST hợp lệ → có dòng; quá khứ/SĐT sai bị từ chối; admin 200 + đổi trạng thái |
| 6 | **Lọc mệnh** Kim/Mộc/Thủy/Hỏa/Thổ, chọn nhiều (OR), `/products?menh=moc,kim`; gán mặc định theo brief; admin sửa được; cờ CẦN XÁC NHẬN | web-plugin, admin-plugin, theme | số kết quả `?menh=moc`, `?menh=kim,thuy` khớp SQL |
| 7 | **SePay**: trang đơn hiện QR + số tiền + nội dung CK + STK placeholder; webhook xác thực `Authorization: Apikey`, idempotent theo `id`, chỉ PAID khi mã đơn khớp, `transferAmount >= tổng đơn`, đơn đang chờ thanh toán; ghi `ecm_payment_txn`; cấu hình key trống → từ chối | web-plugin, theme | (a) key đúng + khớp → PAID, 200 `{"success": true}`; (b) gửi lại cùng id giao dịch → vẫn 200 `{"success": true}`, không đổi gì thêm (unique key id SePay trong DB); (c) `Bearer`, sai key, thiếu key hoặc chưa cấu hình key → 401, không đổi đơn; (d) sai số tiền hoặc sai mã đơn → không PAID |
| 8 | **Ship** 30.000₫, miễn phí từ 3.000.000₫ (cấu hình được), "Nhận tại cửa hàng" 0₫; tổng tính phía server | web-plugin (checkout), theme | đơn test < 3tr = +30.000; ≥ 3tr = 0; nhận tại cửa hàng = 0 |
| 9 | Build jar, 1 restart (có rollback), kiểm cuối, ảnh chụp, dọn dữ liệu test | `.artifacts/reports/khang-nguyen-features/` | toàn bộ danh sách Kiểm cuối |
| 10 | Report | `report.md` cùng thư mục plan | Status + bằng chứng + danh sách cần anh Johnny gửi |

Phase 2–8 sửa code/DB, không restart; chỉ Phase 9 restart đúng 1 lần.

## Cấu hình (không dùng file dotenv)
Giữ trong bảng cấu hình/properties của plugin: `contact.facebook` = `https://www.facebook.com/khangnguyenngocbao68`; `contact.phone` = placeholder ghi rõ "PLACEHOLDER"; `shipping.fee` = 30000; `shipping.freeFrom` = 3000000; `store.address` = "5 Đặng Dung, Ba Đình, Hà Nội"; `sepay.apiKey`, `sepay.bank`, `sepay.account`, `sepay.accountHolder`, `sepay.codePrefix` để trống/placeholder. Khi `sepay.apiKey` trống, webhook luôn trả 401.

## Migration và rollback
Trước migration: `mysqldump --single-transaction --routines --triggers --databases blog` → `.artifacts/backups/khang-nguyen-features/db-pre.sql` + `.sha256`. Migration `CREATE TABLE IF NOT EXISTS` (chỉ thêm bảng/cột mới), kèm `rollback.sql` (DROP các bảng/cột mới). Giá cũ được lưu vào bảng sao lưu trước khi UPDATE. Rollback đầy đủ: nạp lại dump có kiểm sha256 + `docker cp` jar cũ + restart (cần Batman duyệt).

## Rủi ro
- Chưa biết cột giá thật và cách import catalog (chờ catalog).
- Lỗi `/account` 500 từng phụ thuộc thứ tự nạp; sau restart phải kiểm lại.
- Chỉ 1 restart: nếu lỗi sau restart thì dừng và báo.
- SePay chưa có tài khoản: chỉ kiểm được bằng webhook giả lập; định dạng `code`/tiền tố cần xác nhận khi có tài khoản.

## Kiểm cuối — bằng chứng vào `.artifacts/reports/khang-nguyen-features/`
SQL 35 SP và khoảng giá; curl FB/tel ở chi tiết SP và footer; đặt lịch hợp lệ/quá khứ/SĐT sai + admin 200 và đổi trạng thái; `?menh=moc`, `?menh=kim,thuy` khớp SQL; webhook đúng/trùng/sai tiền/sai key; phí ship 3 trường hợp; `/`, `/products` 200, `/abc-xyz` 404, `/account` khách 302, đơn #77 (tra đơn 77 + `johnny.test1@khangnguyen.com` vẫn 200, tổng 10.740.000₫); log từ boot grep NPE/Exception/ERROR và phân loại; 4 ảnh chụp (lọc mệnh, form đặt lịch, trang QR, admin lịch hẹn); dọn dữ liệu test, giữ #77, johnny.test1, admin.

## Danh sách anh Johnny cần gửi (sẽ có trong report.md)
SĐT thật (Zalo tạm bỏ); tài khoản SePay + API key + số tài khoản/ngân hàng/tên chủ TK/tiền tố mã đơn (gửi qua kênh bảo mật, không qua chat); xác nhận giá và mệnh từng món (cờ CẦN XÁC NHẬN); mức phí ship và ngưỡng miễn phí; file design.
