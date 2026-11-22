---
title: "Phase 7: Theme Ecommerce frontend"
status: todo
priority: P1
owner: Plastic Man
effort: "2d"
dependencies: [5, 6]
gate: "Johnny duyệt Q3 (đổi theme active)"
---

# Phase 7: Theme `ecommerce` (Thymeleaf)

## Overview
Port HTML/CSS tĩnh (`my-site/` + `.artifacts/reports/ecommerce-clone/phase-0x-*`) thành theme dùng dữ liệu thật: home, category + filter/sort/load more, search, PDP biến thể, wishlist, giỏ, checkout + mock pay, xác nhận đơn, đăng ký/đăng nhập (override ezylogin), tài khoản/lịch sử đơn (override ezyaccount/ecommerce), customer service, order-status, store finder. Không có ô newsletter/club (Q8: bỏ; footer static cũ có ô này thì gỡ). Giá hiển thị VND định dạng `1.234.000 ₫`.

## Requirements
- [ ] Ghi tên theme hiện tại; dump DB trước khi kích hoạt theme mới.
- [ ] Phát triển + kiểm trên native dev site trước, rồi mới cài lên Docker.
- [ ] Render bằng `th:text` (escape); JS gọi API cùng origin, không nhúng token.
- [ ] CSS có thực sự được tải (lỗi cũ: bản live không có `<style>`/`<img>`): kiểm network 0 request 4xx.
- [ ] Mỗi trang 2 ảnh chụp 1280 px và 390 px.

## Success Criteria
- Mọi link trong menu/footer 200, không 404.
- Từng dòng F1-F13 của brainstorm §4 chạy đúng lệnh kiểm trên Docker :8080.
- Lệnh rollback theme (kích hoạt lại theme cũ) đã dry-run.
