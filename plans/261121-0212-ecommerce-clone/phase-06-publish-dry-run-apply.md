---
phase: 6
title: "Publish: dry run rồi apply (local)"
status: pending
priority: P1
effort: "1h"
dependencies: [2, 3, 4, 5]
---

# Phase 6: Publish dry run rồi apply

## Goal
Đưa fragment và 4 trang lên server local một cách có kiểm soát, có backup và rollback.

## Owner (một writer)
- Writer: Plastic Man (người duy nhất chạy lệnh ghi). Cyborg kiểm vận hành và dry-run rollback (chỉ đọc).

## Steps
1. Backup mới: chép trạng thái `my-site/` và baseline server vào `.artifacts/backups/<yymmdd-hhmm>-pre-publish/`.
2. `ezy publish plan` (dry run); lưu kết quả vào `.artifacts/reports/`; rà từng thay đổi, không có thay đổi ngoài 4 page và 4 fragment.
3. Dry run rollback/restore, ghi kết quả.
4. Xin Johnny Bui duyệt rõ ràng việc apply lên server local (đây là ghi lên server, cần người duyệt theo quy tắc của site).
5. `ezy publish apply`; kiểm 4 slug trả 200 trên `127.0.0.1:8080`.
6. Ghi báo cáo, che token.

## Done when
- Có bằng chứng dry run, backup, dry-run rollback, kết quả apply, 4 URL 200.
- Không có production, `.env`, DB bị đụng.

## Rollback
Restore từ backup pre-publish (đã dry-run ở bước 3); nếu cần, khôi phục lại file cục bộ từ backup Phase 1.
