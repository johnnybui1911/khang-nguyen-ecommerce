---
title: "ecommerce-fullstack"
description: "Clone full-stack ecommerce.com/en-SG trên Ezyplatform local: marketplace plugins + 1 plugin riêng + theme Ecommerce"
status: pending
priority: P1
effort: "~5-7 ngày làm việc (ước lượng, chưa tính rủi ro toolchain)"
tags: [ezyplatform, ecommerce, fullstack, local-only, ecommerce]
created: 2026-11-21
---

# ecommerce-fullstack

Nguồn quyết định: `.artifacts/reports/ecommerce-fullstack/brainstorm.md` (Mister Terrific, ak-brainstorm, 2026-11-21): contract, F0-F15, acceptance từng tính năng, 10 câu hỏi. Quy trình: `TEAM-WORKFLOW.md` mục 2. Plan tĩnh cũ `plans/261121-0212-ecommerce-clone/` dừng ở pause-state (`.artifacts/reports/ecommerce-clone/pause-state.md`); phần HTML/CSS tĩnh chỉ là nguyên liệu cho Phase 7.

## Overview
Site local `127.0.0.1:8080` (admin `:9090`) hành xử như ecommerce.com/en-SG: duyệt danh mục, search, filter/sort/load more, PDP có biến thể, wishlist, giỏ hàng, checkout thanh toán MOCK, đăng ký/đăng nhập/tài khoản/lịch sử đơn, customer service. Tiền tệ VND (giá gốc SGD quy đổi 1 SGD = 20.000 VND (tỷ giá demo cố định, làm tròn đến 1.000 ₫)). Không có newsletter/club (Johnny bỏ, Q8). Dữ liệu từ bảng DB thật, không hard-code HTML.

## Kiến trúc (Approach B, đã chốt)
| Lớp | Thành phần | Nguồn |
|---|---|---|
| Commerce core | `ecommerce` 1.5.8: sản phẩm/biến thể, category, giỏ, checkout, đơn, kho, tiền tệ | Marketplace, free |
| Auth | `ezylogin` 0.4.7: đăng ký, đăng nhập, session | Marketplace, free |
| Account | `ezyaccount` 0.2.4: hồ sơ, đổi mật khẩu, địa chỉ | Marketplace, free (Batman kiểm `ezy dev market info`) |
| Mail | `ezymail` 0.3.3: template + lịch sử mail (không gửi ra ngoài) | Marketplace, free |
| Phần thiếu | Plugin riêng `ecommerce-ext` (admin+web): wishlist, contact, order-status, mock payment, search/facet nếu ecommerce thiếu | Tự viết |
| Giao diện | Theme `ecommerce` (Thymeleaf), port từ HTML/CSS tĩnh | Tự viết |

Thiết kế API/schema/auth: [api-design.md](./api-design.md). Hợp đồng chính thức chốt ở cuối Phase 4 sau khi đọc API thật của ecommerce.

## Phases
| # | Phase | Owner | Status |
|---|-------|-------|--------|
| 1 | [Baseline, backup, health](./phase-01-start.md) | Cyborg | Pending |
| 2 | [Cài marketplace plugins + cấu hình shop VND](./phase-02-install-marketplace-plugins.md) | Cyborg | Pending |
| 3 | [Dev toolchain native (JDK8, Maven, SDK)](./phase-03-dev-toolchain.md) | Cyborg | Pending |
| 4 | [API spike + chốt contract](./phase-04-api-spike-and-contract.md) | Plastic Man (đọc), Batman chốt | Pending |
| 5 | [Plugin `ecommerce-ext` backend + test API](./phase-05-custom-plugin-backend.md) | Plastic Man | Pending |
| 6 | [Seed catalog 40 sản phẩm](./phase-06-catalog-seed-data.md) | Plastic Man (data: Mister Terrific) | Pending |
| 7 | [Theme Ecommerce frontend](./phase-07-ecommerce-theme-frontend.md) | Plastic Man | Pending |
| 8 | [QA E2E, security scan, code review](./phase-08-qa-security-review.md) | Martian Manhunter, Cyborg | Pending |
| 9 | [Ship local, rollback dry-run, journal](./phase-09-ship-local-journal.md) | Cyborg, Batman | Pending |

Thứ tự: 1 → 2 → (3 ∥ 4, khác owner, không chung file) → 5 → 6 → 7 → 8 → 9. Mỗi phase: ak-cook → ak-test → Martian Manhunter review độc lập; báo cáo ở `.artifacts/reports/ecommerce-fullstack/phase-NN-*.md` kèm log lệnh thật.

## Ownership (một writer / file)
- Plastic Man: writer duy nhất `ecommerce-ext/`, `ecommerce-theme/`, `my-site/`, `data/seed/`.
- Cyborg: writer duy nhất `.artifacts/backups/`, cài plugin, toolchain `~/ezyplatform-dev-site/`. Không sửa code app.
- Martian Manhunter: `tests/e2e/` và báo cáo QA; chỉ đọc code.
- Mister Terrific: `data/seed/source/*.csv` (dữ liệu nguồn), chỉ đọc phần còn lại.

## Gate an toàn
- Cần Johnny duyệt trước Phase 2 (cài plugin), Phase 3 (cài JDK/native), Phase 7 (đổi theme active).
- Backup DB trước mỗi lần cài plugin/migration/đổi theme. Không xoá DB/volume, không sửa `.env`, không in token.
- Mọi ghi lên server: dry-run trước, apply khi Batman duyệt bằng chứng dry-run.

## Success Criteria
- [ ] F0-F15 trong brainstorm đạt lệnh kiểm tương ứng (Phase 8 ghi đỏ/xanh từng mục).
- [ ] Full E2E flow (brainstorm §4) chạy trên browser sạch: 0 lỗi 5xx, 0 lỗi console.
- [ ] Test API tự động `ecommerce-ext` xanh; ak-security không còn finding High/Critical.
- [ ] Backup + lệnh restore đã dry-run; theme cũ khôi phục được.
