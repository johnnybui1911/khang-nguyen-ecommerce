# Thay catalog bằng 35 sản phẩm Khang Nguyên (LOCAL)
Outcome: shop chỉ còn 35 sản phẩm Khang Nguyên (giá 1–5 triệu), không còn 47 sản phẩm mẫu cũ.
Ràng buộc: local, không restart web, backup trước, đơn #77 giữ nguyên tổng 10.740.000₫. Ngoài phạm vi: tính năng mới (backlog).
Cách làm: backup DB → importer (API admin) tạo 4 danh mục, 35 ảnh, 35 sản phẩm → SQL xoá dữ liệu cũ → kiểm → báo cáo.
Tiêu chí: xem `report.md`.
