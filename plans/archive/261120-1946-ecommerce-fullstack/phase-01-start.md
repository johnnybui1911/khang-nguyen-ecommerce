---
title: "Phase 1: Baseline, backup, health"
status: todo
priority: P1
owner: Cyborg
effort: "1h"
dependencies: []
---

# Phase 1: Baseline, backup, health

## Overview
Chụp trạng thái hiện tại và có backup khôi phục được trước khi cài bất cứ thứ gì. Chỉ đọc + tạo backup, không đổi server.

## Requirements
- [ ] Container `ezy-my-site-e29d88-ezyplatform-1` healthy (lúc 02:5x đang "health: starting" sau restart; cả :8080/:9090 trả 000). Tìm nguyên nhân restart/đứt kết nối (log container, RAM colima) trước khi đi tiếp.
- [ ] Ghi baseline: danh sách plugin admin/web/socket, theme đang active, 8 page/fragment ecommerce, `ezy status`.
- [ ] mysqldump DB Ezyplatform ra `.artifacts/backups/261121-fullstack-pre/` (nén, kiểm `gzip -t`, đếm `CREATE TABLE` > 0), ghi lệnh restore (chưa chạy).
- [ ] Token admin: theo Q11, mặc định chỉ ghi nhận, không tự đổi (đổi có thể đụng `environment.json`/.env).

## Implementation Steps
1. `docker ps`, `docker inspect --format '{{.State.Health}}'`, `docker logs --since 30m` (che secret), `colima status`.
2. `ezy check`, `ezy status`, liệt kê `/app/ezyplatform/{admin,web,socket}/plugins`, theme active.
3. mysqldump qua `docker exec` (credential lấy trong container, không in ra), lưu + checksum.

## Success Criteria
- `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/` = 200 và admin `ezy check` OK trong 3 lần liên tiếp cách 1 phút.
- File backup tồn tại, > 0 byte, `gzip -t` OK, sha256 ghi trong báo cáo `phase-01-baseline.md`.
- Báo cáo không chứa token/mật khẩu (`grep -iE 'token|password' ` chỉ ra tên biến, không giá trị).

## Bổ sung 2026-11-21 (từ brainstorm mục 7)
- Gate RAM: `docker info | grep "Total Memory"` phải ≥ 3.8 GiB trước khi sang Phase 2 (hiện 1.91 GiB). Docker chạy qua Colima: sau backup DB, Johnny chạy (hoặc duyệt cho Cyborg chạy) `colima stop && colima start --memory 4 --cpu 4`, rồi kiểm lại container healthy + :8080/:9090. Team không tự chạy.
- Cài plugin qua admin UI `http://127.0.0.1:9090/modules/add?keyword=<plugin>` (`ezy dev install-plugin` không chạy trên bản Docker). Plugin ecommerce tự tạo bảng `ecommerce_*` → backup MySQL bắt buộc trước.
