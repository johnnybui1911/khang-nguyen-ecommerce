---
title: "Phase 4: API spike + chốt contract"
status: done
priority: P1
owner: "Plastic Man (đọc/thử), Batman chốt"
effort: "3h"
dependencies: [2]
---

# Phase 4: API spike + chốt contract

## Overview
Xác minh bằng request thật API của ecommerce/ezylogin/ezyaccount để quyết định phần nào `ecommerce-ext` phải viết. Chỉ đọc và gọi API local; không viết code sản phẩm.

## Requirements
- [ ] Bảng endpoint thật (method, path, auth, request, response mẫu đã che dữ liệu) cho: catalog list/detail, category, search, filter/facet, sort/paging, cart CRUD, checkout, order list/detail, address.
- [ ] Extension point payment của ecommerce: có cho đăng ký payment method riêng không (`ezy dev find-class`, `decompile`). Quyết định A7 dạng method hay endpoint.
- [ ] Session ezylogin: cookie nào, CSRF thế nào, cách plugin riêng lấy userId.
- [ ] Địa chỉ: checkout dùng địa chỉ của ecommerce hay ezyaccount; chọn một nguồn.
- [ ] Theme: cấu trúc theme Thymeleaf, cách override template trang của ecommerce/ezylogin/ezyaccount bằng page fragment.

## Success Criteria
- `api-design.md` cập nhật `status: frozen`, mỗi dòng A1-A9 ghi "giữ" hoặc "bỏ, dùng <endpoint plugin>" kèm bằng chứng (lệnh curl + mã HTTP).
- Batman duyệt contract; thay đổi so với nháp được liệt kê trong báo cáo phase.
