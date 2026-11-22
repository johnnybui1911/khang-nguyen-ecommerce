# Brainstorm: clone ecommerce.com/en-SG trên Ezyplatform (local)

Date: 2026-11-21 02:12 (Asia/Saigon) | Skill: ak-brainstorm | Requested by: Johnny Bui via Batman | Author: Mister Terrific (BA)

## Evidence đã kiểm tra
- Site project: `ezyplatform/my-site/` (EzyPlatform – EzyArticle; Thymeleaf/HTML/JS/CSS; cấu trúc `page-fragments/` và `pages/`, mỗi mục có `meta.json`, `head.html`, `content.html`, `foot.html`). Nguồn: `my-site/AGENTS.md`, `my-site/README.md`.
- Live state lúc 02:12: web `127.0.0.1:8080` trả 200, admin `127.0.0.1:9090` trả 302 (chuyển trang đăng nhập, tức đang phản hồi; trước đó từng báo không phản hồi).
- Quy trình bắt buộc: `ezyplatform/TEAM-WORKFLOW.md` mục 2 (task khó): ak-brainstorm → ak-plan → ak-cook → ak-test → ak-code-review → ak-ship → ak-journal; mỗi phase một owner file; chỉ LOCAL; backup trước publish; che token.
- Plan viết tay cũ được chuyển vào `.artifacts/archive/ecommerce-clone-v0/` (còn thư mục `my-site/pages/ecommerce-*` đang có nội dung bản cũ). Chỉ dùng làm dữ liệu tham khảo (danh sách trang, section), không coi là căn cứ.

## Outcome
Bản clone giao diện ecommerce.com/en-SG gồm 4 trang, chạy trên Ezyplatform local, xem được tại `http://127.0.0.1:8080/<slug>`, xây bằng page-fragment dùng chung (theme, header, footer) và được publish qua quy trình `ezy publish plan` (dry run) rồi apply, có backup trước publish.

## Scope trang (4 trang, quyết định mặc định)
| # | Slug local | Nguồn en-SG | Loại |
|---|---|---|---|
| 1 | `home` | `/en-SG/` | Homepage |
| 2 | `necklaces` | `/en-SG/c-0101/` | Category (lưới sản phẩm) |
| 3 | `product-detail-sample` | `/en-SG/p-5517117/...` | Product detail |
| 4 | `customer-service` | `/en-SG/s-customer-service-overview` | Trang CMS |

Fragment dùng chung: theme (CSS/JS), header (promo bar, utility bar, menu drawer), footer, category-strip. Mỗi mục một thư mục riêng, một writer.

## Constraints
- Chỉ LOCAL (web 8080, admin 9090). Không production, không đụng `.env`, không thao tác DB trực tiếp, không in hoặc commit `admin_access_token` trong `environment.json`.
- Mọi bước ghi lên server (publish, media upload/visibility, restore) chạy dry run trước; backup/restore point trước publish.
- Ảnh, logo, thương hiệu thuộc Ecommerce: chỉ demo local, không phát hành công khai.
- Tên media khớp `^[a-zA-Z0-9_-]+(.[a-zA-Z0-9_-]+)*$`; `meta.json` không có `id`, nhận diện bằng slug (tên thư mục).
- Một writer tại một thời điểm cho mỗi file/thư mục; người khác chỉ đọc/review.
- Báo cáo phải có Status (DONE | DONE_WITH_CONCERNS | BLOCKED), bằng chứng chạy thật, phần chưa kiểm chứng.

## Non-goals
Giỏ hàng, checkout, đăng nhập, tìm kiếm thật, lọc/sắp xếp/"Load more" thật, wishlist, đa quốc gia/đa ngôn ngữ, API sản phẩm, giá thời gian thực, chat widget, cookie banner, SEO production, deploy production. Các link ngoài 4 trang trỏ `#` hoặc về trang gần nhất.

## Acceptance criteria
1. 4 trang trả HTTP 200 trên `127.0.0.1:8080`; header/footer đến từ fragment dùng chung, không nhân bản.
2. Bố cục đúng ở desktop 1280px và mobile 390px (QA so ảnh chụp với nguồn en-SG, ghi lại điểm lệch).
3. Không ảnh/CSS/JS vỡ (0 request 4xx trong console/network ở 4 trang); link nội bộ giữa 4 trang hoạt động.
4. Dry run publish chạy sạch và được ghi lại trước khi apply; backup tồn tại trước apply; có cách rollback đã dry-run.
5. QA độc lập (Martian Manhunter) PASS; ak-code-review không còn blocker; Tech Lead tự đo lại 1 số quan trọng.
6. Không có token/secret trong log, báo cáo, file; `.env` và DB không bị đụng (kiểm bằng so sánh trước/sau).

## Trade-offs (3 hướng triển khai)
| Hướng | Phụ thuộc mạnh nhất vào | Hỏng trước khi |
|---|---|---|
| A. Tự dựng lại HTML/CSS từ ảnh chụp và cấu trúc quan sát được (khuyến nghị) | Quan sát đủ section từ trang công khai | Trang nguồn đổi bố cục hoặc chặn truy cập giữa chừng; chi phí chỉ là sửa lại thủ công |
| B. Lưu HTML gốc rồi vá lại sang Thymeleaf | Mã nguồn gốc sao chép được sạch | JS/CSS gốc gọi API Ecommerce, vỡ khi chạy local; rủi ro bản quyền cao hơn |
| C. Chỉ homepage | Người dùng chấp nhận scope hẹp | Người dùng cần điều hướng giữa trang; không đạt scope đã yêu cầu |

Khuyến nghị: A, giữ đủ 4 trang vì đó là scope Johnny đã giao; rẻ nhất để bỏ.

Better approaches: none — recommended direction is the requested one (Ezyplatform fragments/pages là cơ chế bản địa của dự án, xem `my-site/AGENTS.md`).

## Rủi ro mở
- Thư mục `my-site/pages/ecommerce-*` bản cũ vẫn còn; cần quyết định trong plan là làm lại trên bản sạch (backup rồi xoá/ghi đè bởi một writer duy nhất).
- Bản quyền ảnh/logo: chỉ demo local.
- Nguồn en-SG có thể chặn tải tự động hoặc đổi nội dung (sản phẩm giảm giá, Black Friday).

## Unresolved questions
- Trang chủ có cần phục vụ ở `/` (thay `home`) không? Mặc định: không, dùng `/home`.
- Ai là writer của `my-site/` trong giai đoạn build? Mặc định: Plastic Man.

Status: DONE
