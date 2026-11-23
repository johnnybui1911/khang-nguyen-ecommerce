---
title: "Ecommerce clone phase 6: publish local"
date: 2026-11-20
summary: "Apply 8 post local, phát hiện STANDALONE_PAGE bỏ head/foot, chuyển page thành tài liệu đầy đủ, render thật 4 trang OK"
---

# Ecommerce clone phase 6: publish local

## What happened
Backup + restore dry run, apply 8 target. Render thật không có CSS vì 4 post live là STANDALONE_PAGE (apply giữ type live, server bỏ head/foot). Sửa local: inline head/foot vào content.html, meta pageType STANDALONE_PAGE, apply lại 4 page.
## Decision
Không đổi postType trên server (CLI không hỗ trợ). Không có post v0 thừa để Trash; 63 media v0 archive, không xoá.
## Next steps
Media thật cho ảnh giữ chỗ; tìm nguyên nhân container restart; review bởi Minh Lý/Martian Manhunter.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
