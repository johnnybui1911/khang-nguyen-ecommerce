---
phase: 7
title: "QA, code review, ship, journal"
status: pending
priority: P1
effort: "3h"
dependencies: [6]
---

# Phase 7: QA, code review, ship, journal

## Goal
Kiểm độc lập, rà soát, đóng gói và ghi quyết định.

## Owner (chỉ đọc trừ báo cáo)
- QA: Martian Manhunter. Ghi báo cáo vào `my-site/.artifacts/reports/ecommerce-clone/`.
- Review: skill ak-code-review (toàn bộ diff, ưu tiên bảo mật token/`.env`). Sửa lỗi do Plastic Man (writer), không phải reviewer.
- Ship: ak-ship (chỉ khi Johnny muốn PR/commit; mặc định bỏ qua vì `ezyplatform/` chưa là git repo).
- Journal: ak-journal ghi quyết định và bài học.

## Steps
1. QA đối chiếu tiêu chí nghiệm thu 1-6 của brainstorm report: 4 trang 200, desktop 1280 và mobile 390 có ảnh chụp, 0 request 4xx, link nội bộ, theo dõi fragment dùng chung.
2. QA kiểm `.env`, `environment.json`, DB không đổi (so với baseline Phase 1) và không có token trong log/báo cáo.
3. ak-code-review; blocker quay lại writer.
4. Tech Lead tự đo lại 1 số quan trọng (ví dụ status 4 URL).
5. ak-ship nếu được yêu cầu; ak-journal ghi lại.

## Done when
QA PASS hoặc ghi rõ điểm lệch chấp nhận được; review không còn blocker; báo cáo có Status.

## Rollback
Như Phase 6.
