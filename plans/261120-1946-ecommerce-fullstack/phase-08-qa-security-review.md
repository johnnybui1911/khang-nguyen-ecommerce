---
title: "Phase 8: QA E2E, security scan, code review"
status: todo
priority: P1
owner: "Martian Manhunter (QA), Cyborg (ak-security)"
effort: "1d"
dependencies: [7]
---

# Phase 8: QA, security, code review

## Requirements
- [ ] Martian Manhunter chạy ak-test + ak-web-testing: Full E2E flow brainstorm §4 trên browser sạch, test IDOR với 2 user, test mobile 390 px; bảng F0-F15 đỏ/xanh có bằng chứng.
- [ ] Cyborg chạy ak-security trên `ecommerce-ext/`, `ecommerce-theme/`: auth/session, IDOR, CSRF, XSS (input contact), rate limit, secret trong log/artifact.
- [ ] ak-code-review toàn bộ diff; phân loại Blocker / Nên sửa / Gợi ý kèm file:dòng.
- [ ] Bug quay lại Plastic Man theo track fix-bug (test tái hiện đỏ→xanh), Martian Manhunter kiểm lại.

## Success Criteria
- 0 Blocker, 0 security High/Critical còn mở; E2E 0 lỗi 5xx, 0 lỗi console.
- Báo cáo `phase-08-qa.md`, `phase-08-security.md`, `phase-08-review.md`.
