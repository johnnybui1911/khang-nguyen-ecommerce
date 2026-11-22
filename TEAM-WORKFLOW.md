# Quy trình team Technical (v1, 2026-11-14, Trọng Tín)

Quy tắc chung (mọi quy trình)
- Mặc định chỉ LOCAL. Production, xoá DB/volume, đụng .env cần anh Đức duyệt riêng.
- Mỗi dự án một người ghi tại một thời điểm (writer). Người khác chỉ đọc/review.
- Backup/restore point trước mọi publish. Che token/secret trong log, báo cáo, chat.
- Báo cáo kết thúc: Status (DONE | DONE_WITH_CONCERNS | BLOCKED), bằng chứng, chưa kiểm chứng, tồn đọng.
- Không báo xong khi chưa có bằng chứng chạy thật. Tech Lead tự đo lại số quan trọng.
- Báo cáo lưu vào <dự án>/.artifacts/reports/<task>/ bởi writer.

## 1. Task dễ / nhanh (sửa nhỏ, 1 người, ≤ ~1 giờ, ít file)
Skill: ak-scout (nếu chưa rõ file) → ak-cook (--fast) → ak-test → ak-code-review (nhẹ)
1. Tech Lead chốt: kết quả, giới hạn, tiêu chí xong (3 dòng).
2. Fullstack Dev (writer) làm; backup nếu publish.
3. Test hẹp nhất có thể, rồi QA kiểm 1 lượt đúng phần đổi.
4. Tech Lead xác minh 1 số đo/bằng chứng rồi báo.

## 2. Task khó / nhiều việc
Skill: ak-brainstorm → ak-plan → (ak-team hoặc ak-orchestrate cho song song) → ak-cook → ak-test → ak-code-review → ak-ship → ak-journal
1. BA/Tech Lead: outcome, ràng buộc, non-goals, acceptance criteria. Việc mơ hồ: ak-brainstorm. Bài toán khó: ak-advise.
2. ak-plan: tạo plan theo phase trong plans/. Anh Đức duyệt khi rủi ro cao.
3. Chia phase, mỗi phase có owner file rõ. Song song chỉ khi không đụng cùng file (ak-worktree cô lập khi cần).
4. Mỗi phase: ak-cook → ak-test; Minh Lý review UX/BA song song (chỉ đọc).
5. QA độc lập toàn bộ; DevOps/An Vũ kiểm vận hành, dry-run rollback.
6. ak-code-review toàn bộ, ak-ship khi cần PR/release, ak-journal ghi quyết định.
7. Tech Lead hậu kiểm, tổng hợp báo cáo.

## 3. Fix bug
Skill: ak-scout → ak-debug (tìm nguyên nhân gốc trước) → ak-fix → ak-test (test tái hiện) → ak-code-review
1. Mô tả hiện tượng, kỳ vọng, cách tái hiện. Thiếu thì QA tái hiện trước.
2. Chứng minh nguyên nhân (log/test/số đo) TRƯỚC khi sửa. Không đoán.
3. Chọn cách sửa theo nguyên nhân; nhiều hướng thì Tech Lead quyết.
4. Viết/điều chỉnh test tái hiện lỗi (đỏ → xanh). Không làm yếu test.
5. QA kiểm lại đúng lỗi + hồi quy lân cận. Lỗi bảo mật: thêm ak-security-scan.
6. Báo: nguyên nhân gốc, thay đổi, bằng chứng trước/sau.
Sự cố khẩn: sửa tối thiểu + rollback sẵn, hậu kiểm sau (ak-retro nếu lặp lại).

## 4. Review PR
Skill: ak-review-pr (PR GitHub) hoặc ak-code-review (diff/commit/local); ak-security-scan nếu đụng auth/payment/input
1. Đọc mô tả PR, issue, plan liên quan. Lấy diff bằng gh.
2. Rà: đúng nghiệp vụ, bug/hồi quy, bảo mật (threat model: PR lưu/lộ gì), breaking change, test, bảo trì.
3. Phân loại: Blocker / Nên sửa / Gợi ý. Mỗi góp ý kèm file:dòng và lý do.
4. Chạy test/build local nếu rủi ro cao hoặc đụng contract công khai.
5. Kết luận: Approve / Request changes / Comment. Chỉ post lên GitHub (--reply) khi anh Đức đồng ý.
6. Không tự merge. Không --fix nếu chưa được giao.
