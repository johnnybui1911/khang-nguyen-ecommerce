---
title: "ecommerce-clone"
description: "Dựng lại bản clone ecommerce.com/en-SG (4 trang) trên Ezyplatform local, đúng quy trình team Technical"
status: pending
priority: P1
effort: "21h"
tags: [ezyplatform, ecommerce, local-only]
created: 2026-11-21
---

# ecommerce-clone

Nguồn quyết định: `plans/reports/brainstorm-261121-0212-ecommerce-clone.md` (outcome, constraints, non-goals, acceptance criteria). Quy trình: `TEAM-WORKFLOW.md` mục 2.

## Overview
Clone giao diện 4 trang (home, necklaces, mesmera-necklace, customer-service) bằng page-fragment dùng chung, chạy local `127.0.0.1:8080` (admin `127.0.0.1:9090`). Chỉ LOCAL; không đụng `.env`, DB, production; không in token; mọi ghi lên server chạy dry run trước và cần Johnny duyệt apply; ảnh/thương hiệu Ecommerce chỉ demo local. Plan viết tay cũ đã lưu trữ ở `.artifacts/archive/ecommerce-clone-v0/`, chỉ là dữ liệu tham khảo.

## Goals

| # | Goal | Priority |
|---|------|----------|
| 1 | 4 trang trả 200 trên web local, header/footer từ fragment dùng chung | P1 |
| 2 | Bố cục đúng desktop 1280 và mobile 390, 0 request 4xx | P1 |
| 3 | Backup trước publish, dry run publish và rollback có bằng chứng | P1 |

## Phases

| # | Phase | Status |
|---|-------|--------|
| 1 | [Baseline, backup và dọn bản cũ](./phase-01-baseline-backup-reset.md) | Pending |
| 2 | [Fragment dùng chung](./phase-02-shared-fragments.md) | Pending |
| 3 | [Homepage](./phase-03-homepage.md) | Pending |
| 4 | [Category và Product detail](./phase-04-category-and-product.md) | Pending |
| 5 | [Trang CMS customer-service](./phase-05-customer-service.md) | Pending |
| 6 | [Publish dry run rồi apply](./phase-06-publish-dry-run-apply.md) | Pending |
| 7 | [QA, code review, ship, journal](./phase-07-qa-review-ship-journal.md) | Pending |

## Ownership
Writer duy nhất cho `my-site/`: Plastic Man. Martian Manhunter: QA độc lập. Cyborg: backup/rollback, an toàn vận hành. Minh Lý: review UX/BA chỉ đọc. Phase 3-5 chạy tuần tự trừ khi có writer khác và thư mục riêng.

## Success Criteria
- [ ] Tiêu chí nghiệm thu 1-6 trong brainstorm report đều đạt, có bằng chứng.
- [ ] Backup, dry run publish, dry run rollback đã ghi trước apply.
- [ ] QA PASS, ak-code-review không còn blocker, ak-journal đã ghi.

## Approval
Rủi ro trung bình (chỉ local). Johnny duyệt plan trước khi giao build, và duyệt riêng bước apply ở Phase 6.

<!-- slug: ecommerce-clone -->
