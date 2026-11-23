---
phase: 1
title: "Baseline, backup và dọn bản cũ"
status: pending
priority: P1
effort: "1h"
dependencies: []
---

# Phase 1: Baseline, backup và dọn bản cũ

## Goal
Có điểm khôi phục đã kiểm chứng và một thư mục `my-site/` sạch trước khi dựng lại.

## Owner (một writer)
- Writer: Plastic Man. Ghi duy nhất vào `my-site/.artifacts/backups/`, `my-site/pages/ecommerce-*`, `my-site/page-fragments/ecommerce/`.
- Reader/verify: Cyborg (kiểm backup đọc được, không đụng `.env`, DB).

## Files
- Create: `my-site/.artifacts/backups/<yymmdd-hhmm>-pre-ecommerce/` (bản sao `pages/`, `page-fragments/`, kèm checksum)
- Modify: `my-site/pages/ecommerce-*` và `my-site/page-fragments/ecommerce/` (bản cũ; chỉ xoá sau khi backup xác nhận). Bản cũ đã nằm ở `.artifacts/archive/ecommerce-clone-v0/`.
- Không đụng: `.env`, `environment.json`, DB, `example-page`.

## Steps
1. Kiểm trạng thái: web `127.0.0.1:8080` trả 200, admin `127.0.0.1:9090` phản hồi (302 trước đăng nhập là bình thường). Ghi lại.
2. Backup `pages/` và `page-fragments/` vào thư mục backup, ghi sha256 từng file.
3. Chạy dry run đồng bộ/pull (chỉ đọc) để chụp trạng thái server hiện tại làm baseline; không in token.
4. Xác nhận bản cũ đã có trong archive, rồi loại bỏ `ecommerce-*` khỏi `my-site/` (không xoá archive).

## Done when
- Thư mục backup tồn tại, checksum khớp khi kiểm lại.
- Baseline server (danh sách page/fragment hiện có) lưu trong `.artifacts/reports/`.
- `.env` và `environment.json` không đổi (so checksum/mtime trước-sau).

## Rollback
Chép lại `pages/` và `page-fragments/` từ thư mục backup; bản cũ còn trong `.artifacts/archive/ecommerce-clone-v0/`.

## Risks
Xoá nhầm thư mục ngoài `ecommerce-*`. Giảm thiểu: liệt kê danh sách sẽ xoá trước, so với backup.
