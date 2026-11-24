# Đề xuất restructure & dọn dẹp dự án Khang Nguyên

- Người viết: Batman (Tech Lead). Bản 1: 2026-11-24 00:0x. Bản 2: 2026-11-24 ~01:20. **Bản 3: 2026-11-24 ~01:50, thêm mục (e) cấu trúc đích và (f) hướng dẫn thêm mới.**
- Status: **ĐỀ XUẤT, chưa thực hiện gì.** Không file code nào bị sửa, xoá hay đổi tên.
- **Bản 2 phân tích trên checkout `main` local @ `eef4086`** (= `origin/main`, đã gồm PR #1 `feat/ui-redesign`, working tree sạch). Bản 1 chạy trên export của cùng commit này nên phần lớn số liệu không đổi. Không đụng worktree `ezyplatform-ui`.
- Mốc "lúc clone": commit `c674c91`. `../ecommerce-base` có cùng lịch sử, dừng ở commit này.
- Cách kiểm: `grep` trên checkout; `curl` và **trình duyệt** vào site local `127.0.0.1:8080`; `SELECT` (chỉ đọc) trên DB local `blog`.
- Site local **đang chạy đúng code `eef4086`**: `main.css` và `ecommerce.js` do site trả về trùng sha với file trong repo (`15f20935…`, `9028bed5…`). Vì vậy **không cần build hay deploy lại**.

> Ký hiệu thay đổi so với bản 1: **🆕** = mới thêm, **✏️** = số liệu hoặc nhận định đã sửa, **✅ trình duyệt** = đã kiểm trên trình duyệt. Mục không gắn ký hiệu: đã chạy lại, kết quả giữ nguyên.
>
> ✏️ Việc `main` chậm 48 commit và 15 file sửa dở **đã xử lý** (JohnnyBot, 2026-11-24): các thay đổi dở được cất vào nhánh local `backup/local-wip-2026-11-24` (1 commit WIP, chưa push), sau đó `main` fast-forward lên `eef4086`. Cần xem bản sửa dở cũ thì vào nhánh đó.

---

## (a) Cấu trúc hiện tại và các vấn đề

```
ezyplatform/                      repo johnnybui1911/khang-nguyen-ecommerce
├── ecommerce-ext/                plugin backend (tên chung chung, không phải tên shop)
│   ├── ecommerce-ext-sdk/        entity, repo, EcmDemoPaymentService   (package com.ecommerce.ext; 72 file có `com.ecommerce`)
│   ├── ecommerce-ext-web-plugin/ API /api/v1/ecm + view controller     (class tiền tố Ecm*)
│   ├── ecommerce-ext-admin-plugin/  không có code Java, chỉ có messages scaffold
│   ├── scripts/                  import-catalog.py, cleanup/rollback test data, __pycache__ (!)
│   └── tests/api/run.sh
├── ecommerce-theme/
│   ├── ecommerce-theme-theme/    "theme-theme" bị lặp; class WebEcommerceTheme*
│   └── scripts/                  build-product-attributes.py, phase-07-rollback-theme.sh (script dùng 1 lần)
├── my-site/                      bản CMS (pages + page-fragments): tất cả đều DRAFT, không được serve
│   ├── page-fragments/ecommerce/{header,footer,category-strip,theme}
│   └── pages/{home,customer-service,ngoc-hoa-dien-…,vong-ngoc-demo,example-page}
├── data/seed/khang-nguyen/       35 sản phẩm + 35 ảnh (đang dùng)
├── docs/  (design mockups, brief) và plans/ (9 plan + journals): khoảng 8,2 MB / 20,2 MB tracked
└── .claude/ .agents/ .agentkit/  cấu hình tool (122 file tracked)
```

### Vấn đề chính

| # | Vấn đề | Bằng chứng | Mức |
|---|---|---|---|
| A1 | **Link chết trong menu:** `/ngoc-hoa-dien-tan-cuong-thanh-hai` được link 8 lần (theme: header 2, footer 1, home 1; CMS: header 2, footer 1, home 1) nhưng trả **404** | ✅ trình duyệt: bấm "Đọc bài viết" ở trang chủ thì ra trang "Không tìm thấy trang", request trả 404. `/blog/ngoc-hoa-dien-…` cũng 404. Trong DB không có post này; `meta.json` ghi DRAFT | **Lỗi, khách đang thấy** |
| A2 | **Hai nguồn UI song song.** Theme templates là bản thật. `my-site/` là bản sao CMS, nhưng mọi bản ghi trong DB đều DRAFT nên không được serve. ✏️ CSS **596 dòng** (42,6 KB; bản 1 ghi nhầm ~914) tồn tại ở **7 chỗ** (1 nguồn + 6 bản chép): `main.css`, `docs/design/kn.css`, `theme/head.html` và inline trong 4 page CMS (`home`, `customer-service`, `ngoc-hoa-dien-…`, `vong-ngoc-demo`) | `diff main.css kn.css`: lệch 1 dòng; `diff main.css <style> của head.html`: lệch 1 dòng. Commit `d2e9666` (sửa 1 dòng CSS) phải sửa đúng 7 file, đúng bằng số bản chép. DB `ezyarticle_posts`: fragment 1–4, page 5 và 8 đều `DRAFT` | Cao (sửa 1 chỗ, phải sửa theo 6 chỗ) |
| A3 | Tên kỹ thuật là "ecommerce" chung chung, không phải tên shop: thư mục, `artifactId`, package `com.ecommerce.*`, `project_name=Ecommerce Ext/Theme`, khoá settings `ecommerce_ext.*` | `module.properties`, `pom.xml` | Trung bình |
| A4 | Còn dấu vết tiền tố của site gốc: **107 DOM id `sw*`** (`sw`+`Main`, `sw`+`Pdp`, `sw`+`Cats`, `sw`+`NewsForm`…) nằm trong ✏️ **33 file**: 26 template theme, `ecommerce.js`, 6 file CMS. CSS đã chuyển sang `kn-*`, còn id thì chưa | `grep -ohE '\bsw[A-Z][A-Za-z0-9_]*'` → 107 id khác nhau | Trung bình |
| A5 | Lặp tên `ecommerce-theme/ecommerce-theme-theme`. Đặt tên class không thống nhất: plugin dùng `Ecm*`, theme dùng `WebEcommerceTheme*`. Slug fragment trong DB bị dính chữ: `ecommercecategory-strip`, `ecommerceheader` | Danh sách file; DB | Thấp |
| A6 | Trang có route nhưng không có nghiệp vụ thật: `/gift-card-balance` (JS ghi rõ *"no gift-card service exists in this demo"*), `/stores` (`stores.json` có `"stores": []` nhưng footer vẫn link tới) | `ecommerce.js:1291-1292`, `static/data/stores.json`. ✅ trình duyệt `/stores`: khách thấy tiêu đề "Cửa hàng của chúng tôi" và một ô trống ghi **"Chưa có cửa hàng để hiển thị."**, không có địa chỉ, bản đồ hay giờ mở cửa | Trung bình |
| A7 | Dữ liệu test lộ ra ngoài: blog post duy nhất đang PUBLISHED là *"Cách chọn vòng ngọc phỉ thuý hợp mệnh (bài thử)"*. Mã thanh toán cũ `ECM_DEMO_SG` trong `tests/api/run.sh:114`, dù đã bỏ Singapore. 🆕 DB còn setting `ecommerce_payment_service_ECM_DEMO_SG_secret_key` (giá trị đã che) | DB `ezyarticle_posts` id 11; grep; `ezy_settings` | Trung bình |
| A8 | Phần scaffold của CLI chưa ai động tới (xem mục c) | `git diff c674c91..origin/main`: không đổi | Thấp |
| A9 🆕 | Menu danh mục bị **hardcode trong template**, sau đó JS thay bằng dữ liệu API, và hai nguồn này lệch nhau. Template: "Vòng ngọc, Nhẫn ngọc, Chuỗi hạt, **Set nhẫn vòng**". API: "**Set nhẫn & vòng**, Vòng ngọc, Chuỗi hạt, Nhẫn ngọc". Hệ quả: khi tải trang, menu đổi thứ tự và đổi chữ (nháy) | ✅ trình duyệt: ngay sau khi tải, trang chủ hiện thứ tự của template, còn `/stores` và `/products` hiện thứ tự của API. `header.html:15-18,44-47`, `ecommerce.js:1722` | Thấp (UX) |

---

## (b) Quy ước đặt tên và bảng đổi tên

### Quy ước đề xuất

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Slug thương hiệu (thư mục, artifact, module) | `khangnguyen` | `khangnguyen-ext` |
| Package Java | `vn.khangnguyen.<module>` | `vn.khangnguyen.ext.web` |
| Tiền tố class | `Kn` (khớp với CSS `kn-*` đã có) | `KnProductApiController`, `KnThemeHomeController` |
| CSS class | `kn-` (giữ nguyên, đã sạch) | `kn-card` |
| DOM id / JS hook | `kn` + PascalCase | `knPdpImg` |
| Route storefront | tiếng Anh, kebab-case (giữ nguyên, ổn định cho SEO) | `/products/{code}` |
| Slug nội dung (bài viết) | tiếng Việt không dấu, kebab, nằm dưới `/blog/` | `/blog/ngoc-hoa-dien-tan-cuong-thanh-hai` |
| Bảng DB, API path | **giữ** `ecm_*`, `/api/v1/ecm` | Đổi rủi ro cao mà không mang lại giá trị cho khách |

### Bảng đổi tên cũ → mới

| # | Cũ | Mới | Lý do | Rủi ro |
|---|---|---|---|---|
| R1 | ✅ **Đã làm (Phase 5, 2026-11-24).** DOM id `sw*` (107 id) | `kn*` | Bỏ dấu vết site gốc, thống nhất với `kn-*` | **TB**: id là hợp đồng giữa template và `ecommerce.js`. Phải đổi cả hai trong một commit, có script, rồi chạy QA UI toàn site |
| R2 | `href="/ngoc-hoa-dien-tan-cuong-thanh-hai"` (8 chỗ) | `/blog/ngoc-hoa-dien-tan-cuong-thanh-hai` (publish thành BLOG post) | Sửa A1. `/blog` đã chạy được | Thấp |
| R3 | Package `com.ecommerce.ext` / `com.ecommerce.theme` | `vn.khangnguyen.ext` / `vn.khangnguyen.theme` | Tên riêng của shop | **TB**: đổi `package=` trong `module.properties` ảnh hưởng tới bean scan. Cần build và deploy local |
| R4 | Class `Ecm*` (✏️ 34 file), `WebEcommerceTheme*` (7), `EcommerceExt*/EcommerceThemeTheme*` (test) | `Kn*`, `KnTheme*` | Một tiền tố cho cả dự án | Thấp–TB (chỉ trong Java, IDE refactor được) |
| R5 | Thư mục `ecommerce-ext/ecommerce-ext-*` | `khangnguyen-ext/khangnguyen-ext-*` | Tên shop | **Cao**: tên module là định danh plugin trong EzyPlatform (thư mục cài, `ezy_run_script_histories`). Đợt rename trước từng suýt chạy lại script drop bảng |
| R6 | `ecommerce-theme/ecommerce-theme-theme` | `khangnguyen-theme/khangnguyen-theme` | Bỏ lặp "theme-theme" | **Cao**: theme đang active, phải activate lại |
| R7 | `project_name=Ecommerce Ext/Theme`, `description=Theme for Ecommerce Theme` | `Khang Nguyen Ext/Theme`, mô tả thật | Hiển thị trong admin | Thấp |
| R8 | Thư mục `my-site/page-fragments/ecommerce/` (slug DB `ecommerceheader`…) | `page-fragments/kn/` (slug `kn-header`) hoặc xoá theo C-B1 | Slug đang dính chữ | Thấp (đều DRAFT) |
| R9 | Mã thanh toán `ECM_DEMO_SG` trong `tests/api/run.sh` (🆕 và setting `ecommerce_payment_service_ECM_DEMO_SG_secret_key` trong DB) | `ECM_DEMO` | Đã bỏ SG | Thấp (setting DB: đổi tên hoặc xoá sau khi backup) |
| R10 | Bảng `ecm_newsletter`, `ecm_wishlist`, API `/api/v1/ecm` | **Giữ nguyên** | Không lộ ra cho khách. Đổi thì phải `RENAME TABLE`, sửa lịch sử script và JS | Không đổi |

---

## (c) Nên xoá hoặc dọn, kèm bằng chứng

Ký hiệu: **X** = xoá được ngay, **C** = cần anh quyết về nghiệp vụ trước, **K** = giữ.

### C-A. Code và asset không có reference

| # | Mục | Bằng chứng không dùng | Đề xuất |
|---|---|---|---|
| A1 | `ecommerce-ext/scripts/__pycache__/import-catalog.cpython-39.pyc` | File build của Python, đang bị track trong git | **X** + thêm `__pycache__/` vào `.gitignore` |
| A2 | `ecommerce-ext-admin-plugin/src/main/resources/messages/messages*.properties` (267 key × 2 ngôn ngữ) | 0/267 key được tham chiếu ở bất kỳ file `.java/.html/.js` nào. Nội dung là scaffold CLI: `cosmo_waving`, `exp_rate`, `gold_rate`, `dashboard.bots`… | **X**. Giữ lại đúng 1 key `ecommerce_ext` nếu admin cần tên plugin |
| A3 | `static/js/jquery.min.js` (88 KB) và thẻ `<script>` ở `templates/page.html:83` | `ecommerce.js`: 0 lần gọi `$(`, `$.`, `jQuery`. Template và CMS: 0. HTML render của `/`, `/products`, `/checkout`, `/shopping-cart`: 0. Mỗi trang chỉ nạp 2 script: jQuery và `ecommerce.js`. ✅ **trình duyệt (không sửa code):** nạp HTML thật của từng trang vào một iframe sạch, đã bỏ thẻ jQuery (`typeof jQuery` = `undefined`). **11 trang** (`/`, `/products`, `/products/KN-031`, `/shopping-cart`, `/login`, `/account`, `/blog`, `/stores`, `/customer-service`, `/size-guide`, `/book-an-appointment`) đều có **0 lỗi JS**. **Thêm vào giỏ** chạy đúng (nút đổi sang "Đã có trong giỏ", `ecm.bag` ghi 1 dòng), **trang giỏ hàng** hiện đúng sản phẩm và tạm tính 4.800.000 ₫. Đã xoá giỏ thử sau khi kiểm. **Chưa kiểm:** bước đặt đơn trong `/checkout`, vì trang này bắt đăng nhập (chuyển sang `/login`), mà đặt đơn sẽ ghi vào DB, trái với ràng buộc chỉ đọc | **Đã chốt: giữ jQuery (anh Johnny, 2026-11-24).** Lý do G5: layout mẫu và ví dụ trong tài liệu chính thức EzyPlatform dùng jQuery; plugin chính thức (ezylogin/ezyaccount) có thể cần; lợi ích bỏ chỉ ~30 KB gzip, đã cache. Giữ nguyên `jquery.min.js` và thẻ script ở `page.html:83` |
| A4 | `static/images/kn-033.jpg` | Không file nào tham chiếu (5 ảnh còn lại đều có) | **X** |
| A5 | `ecommerce-theme/scripts/phase-07-rollback-theme.sh` | Script rollback dùng 1 lần về theme "Freestyle 0.2.9" và `EcmOrderViewController` (class này không còn). Cần file backup trong `.artifacts/tmp-p7/` | **X**, hoặc chuyển vào `plans/archive/` |
| A6 | `my-site/AGENTS_UPDATED.md` (16 dòng) | Bản nháp đi kèm `AGENTS.md`, không đổi từ lúc clone | **X** (gộp vào `AGENTS.md` nếu còn ý) |
| A7 | `ecommerce-ext/SKILL.md` và `ecommerce-theme/SKILL.md` (516 dòng, giống nhau); `*/AGENTS.md` (189 dòng × 2) | Tài liệu CLI, không đổi từ lúc clone | **K/C**: giữ 1 bản ở root nếu agent cần |

### C-B. Page/route không dùng hoặc không có dữ liệu

| # | Page/route | Bằng chứng | Đề xuất |
|---|---|---|---|
| B1 | `my-site/pages/example-page/` | Scaffold CLI (`"title": "Example Page"`), không có trong DB, 0 link tới, `/example-page` trả 404 | **X** |
| B2 | `my-site/pages/vong-ngoc-demo/` (kèm 3 ảnh) | Page demo, không có trong DB, chỉ tự link tới chính nó, `/vong-ngoc-demo` trả 404. Đã có trang sản phẩm thật `/products/{code}` | **X**. ✏️ Bản sửa dở cũ của file này nằm ở nhánh `backup/local-wip-2026-11-24`, `main` đã sạch |
| B3 | `my-site/pages/home/`, `customer-service/` và 4 fragment | Trong DB đều DRAFT; storefront dùng `templates/home.html` và `customer-service.html`. ✏️ Mỗi file nhúng sẵn toàn bộ 596 dòng CSS | **Đã chốt: theme là nguồn UI duy nhất (anh Johnny, 2026-11-24).** Xoá `pages/home`, `pages/customer-service` và `page-fragments/ecommerce/*`. Bản ghi DRAFT tương ứng trong DB (`ezyarticle_posts`: 4 FRAGMENT, 2 STANDALONE_PAGE) giữ nguyên, không được serve |
| B4 | `my-site/pages/ngoc-hoa-dien-tan-cuong-thanh-hai/` | Có nội dung thật, nhưng không có trong DB và URL trả 404, trong khi menu đang link tới | **Giữ nội dung, chuyển thành BLOG post**, rồi sửa link (R2) |
| B5 | `/gift-card-balance` + `templates/gift-card-balance.html` + `pages.giftcard` trong JS + key `page.giftCardBalance` | 0 link tới (chỉ có controller). JS ghi chú không có dịch vụ thẻ quà tặng, mọi mã đều nhận cùng một câu trả lời | **X** (hoặc **C** nếu shop định bán thẻ quà tặng) |
| B6 | `/stores` + `stores.json` rỗng | Footer và `appointment.html` link tới. ✅ trình duyệt: trang hiện "Chưa có cửa hàng để hiển thị." | **C**: điền địa chỉ thật (ví dụ cửa hàng 5 Đặng Dung, Ba Đình nếu anh xác nhận), hoặc bỏ link và bỏ route |
| B7 | Blog post id 11 "… (bài thử)" đang PUBLISHED | DB | **C**: unpublish, hoặc thay bằng bài thật (B4) |
| K | `/reset-password`, `/newsletter/confirm`, `/newsletter/unsubscribe` | Không có link trong site **là đúng**: người dùng vào từ link trong email | **K** |

### C-C. Phần còn nguyên từ lúc clone (đối chiếu `c674c91` với `origin/main`)

Không đổi từ lúc clone: toàn bộ `ecommerce-ext/` (trừ `EcmStoreContactApiController`, `import-catalog.py`, `run.sh`), các file scaffold của theme, và `my-site/{README,AGENTS,docker-compose}`.

| # | Mục | Nhận xét | Đề xuất |
|---|---|---|---|
| C1 | `src/test/resources/static/{swagger-ui-bundle.js,swagger-ui.css,swagger.html}` × 3 module (~5,1 MB) | Scaffold CLI để xem Swagger khi chạy test | **C**: giữ nếu team dùng Swagger, nếu không thì xoá để repo nhẹ đi |
| C2 | `ConvertScriptGenerator`, `UnitTestGenerator`, `*SwaggerGenerator`, `*IntegrationTestRunner` × 2 | Công cụ dev do CLI sinh | **K** (đổi tên theo R4) |
| C3 | `export*.sh/.bat`, `checkstyle.xml`, `local.properties`, `.ignore_license` | Build/CLI cần | **K** |
| C4 | `my-site/README.md` | Hướng dẫn chung của extension VS Code, không có gì riêng của dự án | **C**: viết lại ngắn cho dự án |
| C5 | `EcmCompat*`, `EcmAuthGuard`, `EcmRateLimiter` | Lớp bảo mật đang chạy (Origin/CSRF, rate limit) | **K**: không xoá |
| C6 | `Ecm{Account,Checkout,PaymentCallback,ProductCheckout}ViewController` (11 dòng, chỉ `extends`) | Bật route của platform | **K** |
| C7 | `plans/261120-*`, `plans/261121-*`, `plans/journals/2026-11-20-*`, `plans/reports/brainstorm-*`, `plans/*/baseline`, `screenshots` | Plan của giai đoạn clone site gốc (nhắc tới `/en-SG`, `necklaces`, Watches) | Chuyển vào `plans/archive/`, **không xoá** (là lịch sử quyết định) |
| C8 | `docs/design/kn.css` | Bản sao `main.css` (lệch 1 dòng) | **K** làm mockup, nhưng ghi rõ: nguồn thật là `main.css` |

---

## (d) Kế hoạch theo phase (an toàn trước, rủi ro sau)

Áp dụng cho mọi phase: chỉ làm trên LOCAL, tạo nhánh riêng từ `origin/main`, và **backup DB + tar source** trước khi deploy. Một phase một PR. Writer: Lin (Java/scripts) hoặc Pixel (template/JS/CSS). Rigel QA độc lập. Anh Johnny duyệt trước mỗi phase.

| Phase | Nội dung | Owner | Kiểm tra xong | Rủi ro / rollback |
|---|---|---|---|---|
| **0. Đồng bộ** | ✏️ **Đã xong phần đồng bộ** (`main` = `origin/main` = `eef4086`, bản dở cất ở `backup/local-wip-2026-11-24`). Còn lại: tạo nhánh `chore/cleanup-p1` từ `main` | Batman | `git status` sạch, HEAD = `origin/main` | Thấp |
| **1. Sửa lỗi khách thấy** | Publish bài Hòa Điền thành BLOG post, đổi 8 link sang `/blog/…` (R2). Unpublish "bài thử" (B7). Xử lý `/stores` (B6) | Pixel (link), Lin (DB/CMS) | `curl` mọi link trong header/footer/home trả 200. Không còn 404 | Thấp. Revert commit, re-publish |
| **2. Xoá dead code (không ảnh hưởng runtime)** | A1, A2, A4, A5, A6, B1, B2, C7 (chuyển vào archive). Thêm `.gitignore` cho `__pycache__` | Lin | `mvn clean install` xanh, `tests/api/run.sh` xanh, grep 0 reference | Rất thấp. `git revert` |
| **3. Xoá phần có ảnh hưởng runtime** (A3 đã chốt giữ jQuery, xem G5) | B5 (gift card: route, template, JS, i18n), C1 (nếu đồng ý) | Pixel (template/JS), Lin (controller) | Build xanh. QA trên trình duyệt ở 390/1280: console 0 lỗi, add-to-cart → checkout → order chạy được. `/gift-card-balance` trả 404 | Thấp–TB. Backup theme jar, redeploy bản cũ |
| **4. Một nguồn UI duy nhất** | Quyết B3: theme là nguồn thật, xoá bản sao CMS của home/customer-service/fragments (hoặc chiều ngược lại). Xoá các bản CSS inline | Batman quyết → Pixel | Grep: CSS chỉ còn ở `main.css` (+ mockup). UI không đổi (so ảnh chụp trước/sau) | TB. Backup bản ghi `ezyarticle_posts` |
| **5. Đổi tên trong code (không đổi định danh module)** | R1 (`sw*`→`kn*`, có script, 1 commit), A9 (bỏ menu hardcode, hoặc đồng bộ cho khớp API), R4 (`Ecm*`/`WebEcommerceTheme*`→`Kn*`), R7, R9 | Pixel (R1), Lin (R4, R7, R9) | `grep -E '\bsw[A-Z]'` = 0. Build và test xanh. QA toàn bộ 25 route | TB. `git revert` + redeploy |
| **6. (Tuỳ chọn) Đổi định danh module** | R3, R5, R6, R8: đổi package, thư mục, artifact, module. **Giữ** bảng `ecm_*` và API path (R10) | Lin, Rigel dry-run rollback | Plugin/theme mới cài và active, bản cũ đã gỡ. Bảng `ecm_*` giữ nguyên số dòng. Script drop **không** chạy lại | **Cao**. Làm theo mẫu `plans/261122-1215-rename-ecommerce` (backup DB + sha256, ghi lịch sử script trước khi boot). Cần anh duyệt riêng |

**Khuyến nghị của tôi:** làm Phase 0–3 ngay (rủi ro thấp, sửa được lỗi 404 khách đang thấy). Phase 4–5 làm sau khi UI ổn định. Phase 6 chỉ làm nếu có lý do thật (ví dụ tách `ecommerce-base` làm template cho shop khác), vì công nhiều mà khách không thấy khác biệt gì.

### Cần anh Johnny quyết
(✏️ Đã bỏ câu hỏi về 48 commit / 15 file sửa dở, vì đã xử lý xong như ghi ở đầu báo cáo.)
> Bản 3: danh sách mới nhất (thêm câu 4) nằm ở cuối mục (f).
1. B3: ✅ **Đã chốt: theme là nguồn UI duy nhất (anh Johnny, 2026-11-24).**
2. B5/B6: shop có bán thẻ quà tặng không? Có địa chỉ cửa hàng thật để đưa vào `/stores` không?
3. Phase 6 (đổi tên module/package): làm hay không?

### Phần chưa kiểm chứng (bản 2)
- Checkout đặt đơn khi không có jQuery: không còn cần chạy, vì A3 đã chốt giữ jQuery (anh Johnny, 2026-11-24). Nếu sau này bỏ jQuery thì phải chạy lại E2E đăng nhập → đặt đơn bằng tài khoản test.
- A9 (menu nháy): mới quan sát qua snapshot ngay sau khi tải trang, chưa đo thời gian và chưa chụp ảnh trước/sau.

---

## (e) 🆕 Cấu trúc đích sau khi sửa (bản 3)

Ký hiệu: **[−]** xoá · **[→]** chuyển chỗ / gộp · **[✎]** đổi tên · **[+]** thêm mới · không ký hiệu = giữ nguyên.
Cấu trúc **hiện tại** xem ở mục (a). Dưới đây là cấu trúc ở 2 mốc.

### Mốc 1: sau Phase 0–3 (chưa đổi tên module, chưa động tới `my-site`)

Ở mốc này cây thư mục gần như giữ nguyên. Chủ yếu là **bớt file**, chưa đổi tên thư mục nào. Lý do: an toàn, không phải cài lại plugin hay theme.

```
ezyplatform/                                   repo khang-nguyen-ecommerce
├── ecommerce-ext/                             PLUGIN BACKEND của shop (API, logic, bảng DB riêng)
│   ├── ecommerce-ext-sdk/                     code dùng chung: entity, repo, payment (dùng chung cho web + admin)
│   ├── ecommerce-ext-web-plugin/              API /api/v1/ecm/* + view controller cần dữ liệu (/categories/{slug}, /orders/{id})
│   ├── ecommerce-ext-admin-plugin/            script SQL tạo bảng + menu admin
│   │   └── src/main/resources/messages/       [−] 266/267 key scaffold (giữ 1 key tên plugin) (C-A2)
│   ├── scripts/                               import-catalog.py, cleanup/rollback test data
│   │   └── __pycache__/                       [−] file build Python (C-A1) + [+] dòng `__pycache__/` trong .gitignore
│   └── tests/api/                             test API (run.sh: [✎] ECM_DEMO_SG → ECM_DEMO, R9)
├── ecommerce-theme/
│   ├── ecommerce-theme-theme/                 THEME (giao diện thật khách thấy)
│   │   ├── src/main/java/.../controller/view/ route của trang (@DoGet), vd /stores, /size-guide
│   │   └── src/main/resources/
│   │       ├── templates/                     Thymeleaf: page.html (layout), fragments/, blog/, pages/, *.html
│   │       │   └── gift-card-balance.html     [−] nếu shop không bán thẻ quà tặng (C-B5) + route + JS + i18n
│   │       ├── static/js/jquery.min.js        (giữ, đã chốt 2026-11-24)
│   │       ├── static/images/kn-033.jpg       [−] (C-A4)
│   │       ├── static/{css,js,data}/          main.css (NGUỒN CSS DUY NHẤT), ecommerce.js, stores.json (điền địa chỉ thật, C-B6)
│   │       └── messages/                      chữ hiển thị en + vi của theme
│   └── scripts/
│       └── phase-07-rollback-theme.sh         [→] plans/archive/ (C-A5)
├── my-site/                                   bản CMS (EzyArticle). Mốc 1 GIỮ, chỉ bớt:
│   ├── pages/example-page/                    [−] scaffold CLI (C-B1)
│   ├── pages/vong-ngoc-demo/                  [−] page demo (C-B2)
│   ├── pages/ngoc-hoa-dien-…/                 [→] nội dung chuyển thành BLOG post /blog/ngoc-hoa-dien-… (B4, R2)
│   └── AGENTS_UPDATED.md                      [−] (C-A6)
├── data/seed/khang-nguyen/                    dữ liệu mẫu thật (35 sp + ảnh + categories.csv), dùng cho import-catalog.py
├── docs/                                      tài liệu: brief, design mockup (docs/design/kn.css = bản mockup, ghi rõ nguồn thật là main.css)
├── plans/                                     plan + journal theo đợt
│   └── archive/                               [+] [→] plan/journal giai đoạn clone 261120-*, 261121-*, brainstorm-*, phase-07 script (C-C7)
├── .artifacts/                                (git-ignore, ~99 MB, chỉ trên máy) backup, release, báo cáo QA
└── .claude/ .agents/ .agentkit/ .omp/         cấu hình công cụ AI, không phải code shop
```

### Mốc 2: sau Phase 6 (đã gộp nguồn UI ở Phase 4, đổi tên ở Phase 5–6)

> ✏️ **Bản 4:** theo tài liệu chính thức, đích nên là **một** project `khangnguyen/` (`khangnguyen-sdk`, `-admin-plugin`, `-web-plugin`, `-theme`) thay cho hai thư mục `khangnguyen-ext/` + `khangnguyen-theme/` dưới đây. Xem G4.

```
ezyplatform/                                   [✎ tuỳ chọn] tên repo/thư mục: khang-nguyen
├── khangnguyen-ext/                           [✎] ← ecommerce-ext/            (R5)  module EzyPlatform: khangnguyen-ext
│   ├── khangnguyen-ext-sdk/                   [✎] package vn.khangnguyen.ext.{entity,repo,payment}, class Kn* (R3, R4)
│   ├── khangnguyen-ext-web-plugin/            [✎] vn.khangnguyen.ext.web.controller.{api,view,guard}
│   ├── khangnguyen-ext-admin-plugin/          [✎] scripts/ (bảng ecm_* GIỮ NGUYÊN, R10), menus.properties
│   ├── scripts/                               import-catalog.py, cleanup-test-data.sh …
│   └── tests/api/
├── khangnguyen-theme/                         [✎] ← ecommerce-theme/          (R6)
│   └── khangnguyen-theme/                     [✎] ← ecommerce-theme-theme/  (hết lặp "theme-theme")
│       ├── src/main/java/vn/khangnguyen/theme/web/controller/view/
│       │   ├── KnThemeHomeController.java     [✎] ← WebEcommerceThemeHomeController (route trang tĩnh của shop)
│       │   ├── KnThemeBlogController.java     [✎]
│       │   └── KnThemePageController.java     [✎]
│       └── src/main/resources/
│           ├── templates/                     id DOM kn* thay cho sw* (R1); menu danh mục chỉ lấy từ API (A9)
│           ├── static/css/main.css            nguồn CSS duy nhất
│           ├── static/js/khangnguyen.js       [✎ tuỳ chọn] ← ecommerce.js
│           └── messages/
├── my-site/                                   [−]/[→] Phase 4, nếu chọn "theme là nguồn UI duy nhất":
│   ├── pages/{home,customer-service}/         [−] bản sao của template theme (DRAFT, không được serve)
│   ├── page-fragments/ecommerce/*             [−] 4 fragment DRAFT (hoặc [✎] page-fragments/kn/ nếu giữ CMS, R8)
│   ├── environment.json, docker-compose.yml   GIỮ: cấu hình `ezy` CLI + Docker local (environment.json đã git-ignore)
│   └── mail-templates/, web/messages/         [+] chỉ khi cần: mẫu email (ezy mail), chữ dùng chung EzySupport (ezy i18n)
├── scripts/                                   [+] deploy-local.sh, rollback-local.sh (hiện chỉ nằm trong .artifacts/releases/, bị git-ignore)
├── data/seed/khang-nguyen/
├── docs/
│   ├── MODULES.md                             [+] danh sách plugin/theme cài từ marketplace + version (xem mục f)
│   └── design/
├── plans/ (+ archive/)
└── .artifacts/ .claude/ .agents/ .agentkit/ .omp/
```

**Không đổi ở cả 2 mốc:** bảng DB `ecm_*`, API `/api/v1/ecm/*`, route storefront (`/products`, `/stores`…), CSS `kn-*`. Lý do ở R10: đổi thì rủi ro cao mà khách không thấy khác gì.

**Vì sao Phase 6 đổi tên thư mục lại rủi ro cao:** tên thư mục module chính là **tên module trong EzyPlatform** (`$EZYPLATFORM_HOME/web/plugins/ecommerce-ext`, `web/themes/ecommerce-theme`), là tiền tố URL admin, và là khoá trong `ezy_run_script_histories`. Đổi tên = cài như một module mới, gỡ module cũ, activate lại theme. Đã kiểm trong container: `web/plugins/ecommerce-ext`, `web/themes/ecommerce-theme`.

---

## (f) 🆕 Sau này thêm mới thì đặt ở đâu

> ✏️ **Bản 4:** đã đối chiếu với tài liệu chính thức ở mục (g). Sửa chính: khai báo phụ thuộc cần cả `@ComponentsScan` (G2c); lệnh chính thức là `ezy.sh`/admin, `ezy dev` là lệnh tắt (G2d); chữ dùng chung trên server ở `web/resources/messages/` (G3b).

**Nguồn:** `ezy --help`, `ezy dev --help`, `ezy publish|i18n|media|new --help` (ezyplatform skills **0.6.0**), skill `ezy-develop` / `ezy-guide` / `ezy-i18n` / `ezy-media` của plugin ezyplatform, và `ecommerce-theme/SKILL.md` (mục 2, 3.2, 10, 11). Có chạy thật `ezy dev market search ecommerce` (đọc marketplace, trả về 11 kết quả) và `ezy dev status`. **Không cài hay export gì.**

### Nguyên tắc chính (rút từ tài liệu CLI)

1. **Plugin/theme cài từ marketplace KHÔNG nằm trong repo.** Chúng được cài vào `$EZYPLATFORM_HOME/{admin,web,socket}/{plugins,themes}/<tên>/` trên server. Repo chỉ **ghi lại sự phụ thuộc**: `dependencies=` trong `module.properties` và `<tên>.version` trong `pom.xml` gốc. Docs ghi rõ: *"Never edit files of installed plugins"*. Muốn đổi hành vi thì extend từ project của mình (subclass, override hook, template riêng).
2. **Chỉ có 1 theme active** trên một site. Thêm trang thì thêm vào theme của shop, không thêm theme mới.
3. **Tính năng mới nhỏ** (liên quan shop): đặt trong plugin `ecommerce-ext` (sau Phase 6 là `khangnguyen-ext`). **Chỉ tạo plugin riêng** khi đó là một miền độc lập, có thể bật/tắt riêng hoặc dùng lại cho site khác.
4. **Lệnh `ezy dev …` cần một EzyPlatform cài native** (file trên đĩa). Docs viết: *"A Docker-only install cannot be used for development"*. Máy này đang có **2 môi trường**, xem cảnh báo F-1 bên dưới. **Đã chốt: Docker :8080 là môi trường chính, native tắt và giữ lại (anh Johnny, 2026-11-24).**

### Bảng: Loại → Đặt ở đâu → Lệnh/cách tạo → Tên ví dụ

| Loại | Đặt ở đâu | Lệnh / cách tạo | Tên ví dụ |
|---|---|---|---|
| **Cách cài chính thức (đã chốt 2026-11-24)** | Plugin/theme marketplace cài trên site Docker :8080 | Trang quản trị bản A `:9090`: **Add New → Active**. Code tự viết deploy như hiện nay (`docker cp` + restart). `ezy dev` chỉ dùng trên bản native khi cần thử plugin lạ | |
| **Plugin có sẵn trên marketplace** (vd giao hàng, thanh toán) | **Không vào repo.** Cài vào `$EZYPLATFORM_HOME/web/plugins/<tên>`, `admin/plugins/<tên>`. Repo: thêm `<tên>` vào `dependencies=` của `module.properties` bị ảnh hưởng, version vào `pom.xml` gốc, và [+] 1 dòng trong `docs/MODULES.md` | Tìm: `ezy dev market search <từ khoá>`, `ezy dev market info <tên>`, `ezy dev market docs <tên>`. Cài: `ezy dev install-plugin <tên>` (tự cài plugin phụ thuộc, activate, restart). Code mình cần gọi plugin đó: `ezy dev link <tên> -m web-plugin,theme` (tự thêm `dependencies=`, sync version), rồi `ezy dev export`. Nâng version: nâng trong admin (Modules) → `ezy dev sync-versions` → `ezy dev export` | `ezydelivery` (free, 0.7.7), `ezypayment` (free, 0.2.5), kết quả `market search` |
| **Theme có sẵn trên marketplace** | Không vào repo. `$EZYPLATFORM_HOME/web/themes/<tên>` | `ezy dev install-plugin <tên> --activate-theme`. ⚠ Theme mới active sẽ **thay** theme Khang Nguyên (chỉ 1 theme active). Chỉ dùng để tham khảo hoặc làm lại toàn bộ giao diện | `fashion` ($25) |
| **Plugin tự viết, tính năng nhỏ** (vd trả góp, khắc chữ lên nhẫn) | Trong plugin có sẵn: entity/repo trong `ecommerce-ext-sdk`, API trong `ecommerce-ext-web-plugin/.../controller/api`, script SQL **file mới** trong `ecommerce-ext-admin-plugin/src/main/resources/scripts/`, menu admin trong `menus.properties` | Viết code theo lớp ở `ecommerce-theme/SKILL.md` mục 2 và 11 (entity → model → service → request/validator → response → controller). Không sửa file SQL đã chạy, luôn thêm file mới. Build + cài: `ezy dev export` | `KnEngravingApiController`, bảng `ecm_engraving_request`, script `create_ecm_engraving_tables.sql` |
| **Plugin tự viết, miền riêng** (vd chương trình khách thân thiết) | Thư mục mới **cạnh** `ecommerce-ext/` ở gốc repo, ví dụ `kn-loyalty/` (gồm `kn-loyalty-sdk/`, `-web-plugin/`, `-admin-plugin/`) | `ezy dev create kn-loyalty --group vn.khangnguyen --modules web-plugin,admin-plugin --out <gốc repo>`. Có ≥ 2 module thì SDK tự thêm `kn-loyalty-sdk`. Rồi `ezy dev link ecommerce` (nếu cần dùng sản phẩm/đơn) → `ezy dev export`. Đọc `AGENTS.md`, `SKILL.md` sinh ra trước khi viết code. Tên: chữ thường, số, `-`; tên này **thành tên module và tiền tố URL admin** (`/kn-loyalty/...`), nên chọn kỹ ngay từ đầu | `kn-loyalty`, package `vn.khangnguyen.loyalty`, class `KnLoyalty*` |
| **Theme mới / theme con** | Không khuyến nghị. Tài liệu và CLI **không có cơ chế "theme con"** (đã tra `ezy dev --help`, `ezy-develop`, `SKILL.md`). Muốn đổi giao diện thì sửa ngay trong theme của shop, hoặc override template/hook của plugin từ theme | Nếu thật sự cần theme thứ hai (vd làm lại toàn bộ thương hiệu): `ezy dev create kn-theme-v2 --group vn.khangnguyen --modules theme` → `ezy dev export` (theme mới sẽ thay theme cũ) | `kn-theme-v2` |
| **Page mới cho khách, tĩnh** (vd `/bao-hanh`, `/cham-soc-ngoc`) | Template: `ecommerce-theme-theme/src/main/resources/templates/<trang>.html` (dùng layout `page.html`). Route: thêm `@DoGet` vào `WebEcommerceThemeHomeController` (Phase 5+: `KnThemeHomeController`). Chữ: `messages/messages{,_vi}.properties` của theme. Link: `templates/fragments/header.html` / `footer.html` | Viết theo `SKILL.md` mục 10 (View controller) → `ezy dev export`. Route tiếng Anh kebab-case để ổn định SEO (theo quy ước b) | `templates/warranty.html`, `@DoGet("/warranty")`, key `page.warranty` |
| **Page mới cần dữ liệu từ DB** (vd `/collections/{slug}`) | View controller trong **web-plugin** `.../web/controller/view/` (như `/categories/{slug}` đang có), template trong theme. Dữ liệu tương tác thì JS gọi API `controller/api` | Như trên. Chỉ đưa biến cấp trang vào view, phần còn lại JS lấy từ API (`SKILL.md` mục 10) | `KnCollectionViewController`, `templates/collection.html` |
| **Bài viết / nội dung** (cẩm nang ngọc, tin tức) | **Không phải code.** Là bài BLOG trong EzyArticle (admin → bài viết), hiển thị ở `/blog/<slug>` bằng `templates/blog/details.html` đã có | Tạo trong admin. Slug tiếng Việt không dấu, kebab | `/blog/ngoc-hoa-dien-tan-cuong-thanh-hai` |
| **Page do CMS quản lý** (`my-site/pages/<slug>/`) | Chỉ dùng nếu anh **không** chọn "theme là nguồn duy nhất" (câu hỏi 1). Mỗi page là 1 thư mục: `content.html`, `head.html`, `foot.html`, `meta.json` (+ `*.vi.html`) | Kéo về: `ezy pull page:<slug> --write`. Đẩy lên: `ezy publish plan page:<slug>` (chạy thử) → `ezy publish apply page:<slug> --yes`. ⚠ CLI **chỉ cập nhật post đã có**: tạo page mới phải tạo 1 lần bằng VS Code extension hoặc admin | `my-site/pages/bao-hanh/` |
| **CSS / JS / ảnh giao diện** | `ecommerce-theme-theme/src/main/resources/static/{css,js,images}/`. CSS chỉ thêm vào `main.css` (không chép inline ra chỗ khác, xem A2) | Sửa file → `ezy dev export` (docs: kể cả khi chỉ đổi template/static, export vẫn là cách an toàn) | `static/images/banner-tet-2027.webp` |
| **Ảnh sản phẩm, ảnh nội dung** | Thư viện media của server (`/api/v1/media/…`), không đưa vào theme | Upload trong admin, hoặc `ezy media upload <file>` (chạy thử) → `--yes`. Tên file không dấu, không khoảng trắng | `vong-phi-thuy-kn-036.webp` |
| **Chữ hiển thị (i18n)** | Chữ của trang trong theme: `theme/.../messages/messages{,_vi}.properties`. Chữ trong admin của plugin: `admin-plugin/.../messages/`. Chữ dùng chung trên server (EzySupport, sửa được trong admin): `my-site/web/messages/messages_<lang>.properties` | Theme/plugin: sửa file → `ezy dev export`. EzySupport: `ezy i18n diff web --lang vi` → `ezy i18n push web --lang vi --yes` (chỉ đẩy key mới hoặc đã đổi) | key `page.warranty.title` (en + vi) |
| **Dữ liệu mẫu / catalog** | `data/seed/khang-nguyen/` (`products.csv`, `categories.csv`, `images/`) | `ecommerce-ext/scripts/import-catalog.py` (đã dùng ở đợt catalog 22/11). Dữ liệu test thì có `cleanup-test-data.sh`, `rollback-test-data.sh` | `data/seed/khang-nguyen/products.csv` |
| **Bảng DB mới** | Script trong admin-plugin `src/main/resources/scripts/`, **file mới** cho mỗi thay đổi | Trước tiên kiểm bảng đã có: `ezy dev tables core`, `ezy dev tables ecommerce` (docs: tái dùng bảng có sẵn trước, tạo mới sau cùng). Giữ tiền tố `ecm_` cho thống nhất (R10) | `ecm_store_location` |
| **Script vận hành** (deploy, rollback) | [+] `scripts/` ở gốc repo (tracked). Hiện `deploy.sh`/`rollback.sh` chỉ nằm trong `.artifacts/releases/` nên **không có trong git** | Đưa vào repo ở Phase 2, che hết token | `scripts/deploy-local.sh` |

### Cảnh báo F-1: máy đang có 2 EzyPlatform, chỉ 1 cái dùng được cho `ezy dev`

> **Đã chốt: Docker :8080 là môi trường chính, native tắt và giữ lại (anh Johnny, 2026-11-24).** Phần "Đề xuất" ở cuối mục này là bản trước khi chốt, không còn áp dụng.

| Môi trường | Cổng | Đang chạy (đã kiểm 24/11) | Vai trò |
|---|---|---|---|
| Docker `ezy-my-site-…` (từ `my-site/docker-compose.yml`) | web `:8080`, admin `:9090` | theme `ecommerce-theme`, plugin `ecommerce-ext` (đã kiểm trong container) | Site local team QA đang dùng. Deploy bằng `docker cp` + restart (`.artifacts/releases/*/deploy.sh`) |
| Native `~/ezyplatform-dev-site` | web `:18080`, admin `:19090` | `ezy dev status`: theme **`swarovski-theme`**, plugin **`swarovski-ext`** (tên cũ, chưa có bản đổi tên) | Môi trường duy nhất `ezy dev install-plugin/link/export` dùng được |

Hệ quả: nếu sau này chạy `ezy dev install-plugin` hay `ezy dev export` thì plugin sẽ vào bản **native** (tên swarovski cũ), không vào site Docker `:8080` mà team đang QA. **Đề xuất cũ (đã thay bởi quyết định trên):** chọn native làm môi trường dev chính, đưa `ecommerce-ext` + `ecommerce-theme` lên đó một lần (`ezy dev export --dir ~/ezyplatform-dev-site`), và giữ Docker cho QA hoặc tắt hẳn. Việc này cần anh duyệt vì có cài/gỡ module trên local (thêm thành câu hỏi 4 bên dưới).

### Bật lại bản native

Bản native `~/ezyplatform-dev-site` đã tắt từ 2026-11-24 và được giữ nguyên (file, `mysql-data`, `jdk`, `mysql-src`). Môi trường chính là Docker (web `:8080`, admin `:9090`).

```bash
# bật (lần đầu mất vài phút: chờ admin tối đa 420s, web 300s)
bash ~/ezyplatform-dev-site/ezyplatform-ctl.sh start --dir ~/ezyplatform-dev-site
# kiểm tra / tắt
bash ~/ezyplatform-dev-site/ezyplatform-ctl.sh status --dir ~/ezyplatform-dev-site
bash ~/ezyplatform-dev-site/ezyplatform-ctl.sh stop --dir ~/ezyplatform-dev-site
```

- Cổng: web `http://127.0.0.1:18080`, admin `http://127.0.0.1:19090`. MySQL riêng của bản native, cấu hình ở `~/ezyplatform-dev-site/mysql.cnf`, dữ liệu ở `mysql-data`.
- Luôn truyền `--dir ~/ezyplatform-dev-site` để script không nhầm sang thư mục khác.
- Không dùng `ezy dev` hay lệnh Docker của bản Docker cho bản native: hai môi trường độc lập.
- **Không chạy `uninstall`**: lệnh này xoá cả thư mục (`mysql-data`, `jdk`).
- Không có LaunchAgent, crontab hay login item nào tự bật bản native.

### Cập nhật "Cần anh Johnny quyết" (bản 3)
1. B3: ✅ **Đã chốt: theme là nguồn UI duy nhất (anh Johnny, 2026-11-24).** Dòng "Page do CMS quản lý" trong bảng (f) không dùng.
2. B5/B6: thẻ quà tặng, địa chỉ cửa hàng thật cho `/stores`.
3. Phase 6 (đổi tên module/package): làm hay không.
4. 🆕 F-1: **Đã chốt: Docker :8080 là môi trường chính, native tắt và giữ lại (anh Johnny, 2026-11-24).**

### Phần chưa kiểm chứng (bản 3)
- `ezy dev create`, `install-plugin`, `link`, `export`: mới đọc `--help` và docs, **chưa chạy** (chạy sẽ cài/sửa module, ngoài phạm vi chỉ đọc).
- "Không có theme con": kết luận từ việc không thấy trong CLI và docs đã đọc, chưa hỏi đội EzyPlatform.
- Đường dẫn `my-site/web/messages/` cho EzySupport lấy từ docs `ezy-i18n`, thư mục này hiện chưa có trong repo.

---

## (g) 🆕 Đối chiếu với tài liệu chính thức EzyPlatform (bản 4, 24/11)

**Nguồn:** ezyplatform.com (trang chủ, Chợ) và mục "Hướng dẫn" mà ezyplatform.com trỏ tới, nằm trên `youngmonkeys.org/ezyplatform/guides/…`. Chỉ đọc web, không đăng ký, không tải/cài gì. Có đối chiếu thêm bằng cách **đọc** (không sửa) bản native `~/ezyplatform-dev-site/ezyplatform`.

Ký hiệu: ✅ xác nhận · ❌ sai/cần sửa · ⚠️ đúng một phần · ➖ tài liệu không nói tới.

| # | Điểm trong báo cáo | Trạng thái | Tài liệu nói gì | Nguồn |
|---|---|---|---|---|
| G1 | "Không có theme con" (bảng f) | ➖ + ✅ phần liên quan | Không có trang nào nói về theme con / kế thừa theme. Tài liệu xác nhận **chỉ 1 theme active**, nhưng một theme có **nhiều page**, phục vụ được nhiều domain (mỗi domain 1 page, chia bằng Nginx). Web plugin có thể "can interfere with interface components provided by the theme through JavaScript or mechanisms defined by the theme" → cách mở rộng giao diện chính thức là **web plugin cung cấp fragment/API cho theme**, không phải theme con. Kết luận của bảng (f) giữ nguyên | [Understanding themes and plugins](https://youngmonkeys.org/ezyplatform/guides/understanding-ezyplatform-themes-and-plugins), [EzyPlatform structure](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-structure) |
| G2a | Plugin/theme marketplace **không vào repo**, cài lên server | ✅ | Cài qua admin: *Plugins > Web/Admin/Socket*, *Themes* → **Add New** → tìm → Add → Active (tự hỏi cài plugin phụ thuộc). Có nút **Update manually** để up zip tự build. Nâng version cũng làm trong admin (*Update to x* → *Apply*). Không nhắc gì tới việc đưa vào repo | [Add a plugin](https://youngmonkeys.org/ezyplatform/guides/add-a-plugin-to-your-ezyplatform), [Update a plugin](https://youngmonkeys.org/ezyplatform/guides/update-a-plugin) |
| G2b | Nằm ở `$EZYPLATFORM_HOME/{admin,web,socket}/{plugins,themes}/<tên>/` | ✅ | Bảng "Where EzyPlatform keeps them": `admin/plugins`, `web/plugins`, `web/themes`, `socket/plugins`, `socket/apps`. Máy native khớp: `web/plugins/{ecommerce,ezyaccount,ezyarticle,ezylogin,ezymail,swarovski-ext}`, `web/themes/{swarovski-theme,welcome}`. (Trang "structure of a plugin project" ghi theme → `admin/themes/…`, khác 2 nguồn kia, nhiều khả năng là lỗi đánh máy của tài liệu) | [Understanding themes and plugins](https://youngmonkeys.org/ezyplatform/guides/understanding-ezyplatform-themes-and-plugins), [Structure of a plugin project](https://youngmonkeys.org/ezyplatform/guides/the-structure-of-a-plugin-project) |
| G2c | Khai báo phụ thuộc: `dependencies=` trong `module.properties` + version trong `pom.xml` | ⚠️ | Tài liệu `link` chỉ nói 2 việc: thêm dependency vào **`pom.xml`** và thêm package của plugin được link vào **`@ComponentsScan`** (lớp `XxxStartupTest`). **Không nhắc `dependencies=`**. Nhưng `dependencies=` có thật trong module đã cài (`swarovski-ext`: `dependencies=ecommerce,ezylogin,ezyaccount`; repo mình cũng có) và admin dựa vào đó để hỏi cài plugin phụ thuộc. → Sửa bảng (f): khi dùng plugin khác cần **3 chỗ**: `pom.xml`, `@ComponentsScan` trong StartupTest, `dependencies=` | [Link a plugin](https://youngmonkeys.org/ezyplatform/guides/link-a-plugin) |
| G2d | Tên lệnh: `ezy dev install-plugin`, `ezy dev create`, `ezy dev link` | ⚠️ | Tài liệu chính thức dùng **EzyPlatform SDK** `ezy.sh` (`ezy.sh cp <tên> -g <group> -p <package> -i admin-plugin,theme,web-plugin`, `ezy.sh link <tên> -m …`, `bash export.sh`) và **cli.sh** của server (start/console/test DB). Lệnh `ezy dev …` trong bảng (f) là của CLI `ezyplatform skills 0.6.0` (bọc ngoài SDK), **không có trong tài liệu web**. Cài plugin marketplace theo tài liệu là làm **trong admin**. → Bảng (f) nên ghi cả hai: cách chính thức (admin / `ezy.sh`) và lệnh tắt `ezy dev` | [SDK commands](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-sdk-commands), [CLI commands](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-cli-commands), [Create a plugin](https://youngmonkeys.org/ezyplatform/guides/create-a-plugin) |
| G3a | Chữ i18n của theme/plugin ở `src/main/resources/messages/messages{,_vi}.properties` | ✅ | "Create language files `messages_<language_name>.properties` in the `src/main/resources/messages` directory… `messages.properties` will be the default and in English". Dùng `[[#{key}]]` trong HTML, `ezyweb.messages.key` trong JS | [Multi language](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-multi-languages) |
| G3b | Chữ dùng chung ở `my-site/web/messages/messages_<lang>.properties` | ⚠️ | Tài liệu web **không nhắc** `my-site/web/messages/`. Đường dẫn thật trên server là **`$EZYPLATFORM_HOME/web/resources/messages/`** (máy native có sẵn `messages{,_vi,_ja,_ko,_zh,_ar}.properties`), khớp với cấu hình `pom.xml` mẫu trong tài liệu (resource `/web/resources`). `my-site/web/messages/` chỉ là thư mục **bản sao local** do lệnh `ezy i18n` dùng. Bật thêm ngôn ngữ (vi, mặc định vi) làm trong admin *Settings > Website > Languages* | [Multi language](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-multi-languages), [Setup multiple languages for web](https://youngmonkeys.org/ezyplatform/guides/setup-multiple-languages-for-web) |
| G3c | Page = template trong theme + controller; page CMS là tuỳ chọn | ✅ | Theme chứa `src/main/resources/templates`, `src/main/resources/static`, và "web page controllers". Page mới: file trong `templates/` có `layout:decorate="~{ezytheme}"`, dùng layout `page.html` + `fragments/header`, `fragments/footer`. Admin **cũng tạo được page** cho theme qua giao diện quản trị (và Vibe Coding). Nội dung dài/hay đổi "should fetch it from the database". → Khớp đề xuất "theme là nguồn duy nhất cho layout/trang cố định; nội dung là bài viết trong admin" | [Understanding themes and plugins](https://youngmonkeys.org/ezyplatform/guides/understanding-ezyplatform-themes-and-plugins), [Designing and programming a theme](https://youngmonkeys.org/ezyplatform/guides/designing-and-programming-a-theme) |
| G3d | Static asset trong `static/{css,js,images}` | ✅ | "divide it into subdirectories… js… css… images". Lưu ý của tài liệu: mọi file trong `static/` đều **public**, không để thông tin nhạy cảm. Cấu hình nên để ở **Settings** trong admin, tránh để trong `src/main/resources` | [Structure of a plugin project](https://youngmonkeys.org/ezyplatform/guides/the-structure-of-a-plugin-project) |

### G4. Quy ước cấu trúc và đặt tên chính thức so với đề xuất

| Điểm | Tài liệu chính thức | Đề xuất hiện tại (b, e) | Nên theo |
|---|---|---|---|
| Cấu trúc project | **Một** project `book-store/` chứa các module `book-store-sdk`, `-admin-plugin`, `-web-plugin`, `-theme` (+ socket nếu cần), cùng `pom.xml`, `export.sh`, `local.properties` ở gốc. Khi export, mọi module cài dưới **cùng một tên** `book-store` ([nguồn](https://youngmonkeys.org/ezyplatform/guides/the-structure-of-a-plugin-project)) | Hai project riêng `khangnguyen-ext/` và `khangnguyen-theme/khangnguyen-theme/` | ❌ **Sửa Mốc 2 (e):** theo chính thức thì là 1 project `khangnguyen/` với `khangnguyen-sdk/`, `khangnguyen-admin-plugin/`, `khangnguyen-web-plugin/`, `khangnguyen-theme/`. Đây cũng là **nguyên nhân gốc** của tên lặp `ecommerce-theme-theme`: base tạo theme bằng một project riêng tên `ecommerce-theme`, SDK đặt module là `<project>-theme`. Gộp làm một thì hết lặp. Rủi ro vẫn **Cao** như R5/R6 (đổi tên module cài đặt, activate lại theme), chỉ làm ở Phase 6 |
| Tên project | `hello-world`, `book-store`: chữ thường, gạch nối ([SDK commands](https://youngmonkeys.org/ezyplatform/guides/ezyplatform-sdk-commands)) | `khangnguyen`, `kn-loyalty` | ✅ Hợp lệ |
| Group / package | `-g com.your_company` bắt buộc; `-p` tuỳ chọn, mặc định = group + tên project bỏ gạch thay `_` (vd `com.your_company.hello_world`); ví dụ chính thức `org.youngmonkeys.bookstore` | `vn.khangnguyen.<module>` | ✅ Hợp lệ. Khi tạo phải **truyền `-p vn.khangnguyen`** (hoặc `vn.khangnguyen.loyalty`), nếu không SDK sẽ sinh `vn.khangnguyen.khangnguyen` |
| Tên `ecommerce` | Trên Chợ đã có plugin chính thức **`ecommerce`** (Young Monkeys, `org.youngmonkeys.ecommerce`, 1.5.8, miễn phí) ([nguồn](https://ezyplatform.com/market/items/ecommerce)). Plugin của mình **phụ thuộc** vào nó (`dependencies=ecommerce`) | Đổi `com.ecommerce`/`ecommerce-ext` → `vn.khangnguyen`/`khangnguyen` | ✅ **Thêm lý do cho R3/R5:** tên `ecommerce-ext`, `com.ecommerce` dễ nhầm với plugin chính thức `ecommerce` mà mình đang dựa vào. Bảng `ecm_*` và `/api/v1/ecm` **giữ nguyên** (R10) càng đúng vì cùng hệ với plugin `ecommerce` |
| Template admin | Phải nằm trong `templates/{project name}/` để không trùng plugin khác | Admin plugin hiện **không có** `templates/` (chỉ API + menu) | ➖ Chưa áp dụng. Ghi vào bảng (f): khi thêm trang admin thì đặt ở `templates/khangnguyen/…` |
| File bắt buộc | Mỗi module (trừ sdk) có `module.properties`; `src/test/resources/AllTests.tng.xml` và `assembly.xml` là bắt buộc; `menus.properties` chỉ cho admin plugin | Phase 2 dọn file thừa | ⚠️ **Lưu ý cho Phase 2:** không xoá `AllTests.tng.xml`, `assembly.xml`, `export*.sh/.bat`, `local.properties` dù trông như file mẫu |
| CSS / HTML theme | Khuyến nghị Bootstrap 5, BEM, biến CSS cho giá trị dùng chung, `script` đặt cuối body | `kn-*` + `main.css` duy nhất (A2) | ✅ Khớp hướng gộp CSS về một file |

### G5. Phát hiện thêm liên quan Phase 3 (bỏ jQuery)

Layout mẫu `page.html` trong tài liệu chính thức nạp **jQuery** và gọi `$(document).ready(… ezyweb.format…Elements() …)`; ví dụ JS cho web dùng `$.ajax` với `ezyweb.processUpdateApiErrors` ([Designing a theme](https://youngmonkeys.org/ezyplatform/guides/designing-and-programming-a-theme), [JS functions in web](https://youngmonkeys.org/ezyplatform/guides/available-javascript-functions-in-web)). Nghĩa là các fragment của plugin chính thức (`ezylogin`, `ezyaccount`…) **có thể cần jQuery**. Đã kiểm theme của mình: không gọi `ezyweb.*`, chỉ dùng fragment `ezycommon/loading` (fragment này không dùng `$`, chỉ có `${…}` của Thymeleaf). Đã chốt: giữ jQuery (anh Johnny, 2026-11-24). Nếu về sau bỏ jQuery thì **điều kiện:** nếu nhúng fragment của `ezylogin`/`ezyaccount` vào theme thì phải nạp lại jQuery.

### Cập nhật "Phần chưa kiểm chứng" (bản 4)
- ~~"Không có theme con"~~ → tài liệu chính thức không có cơ chế này (G1). Chưa hỏi trực tiếp đội EzyPlatform (Discord).
- ~~Đường dẫn `my-site/web/messages/`~~ → đường dẫn server là `$EZYPLATFORM_HOME/web/resources/messages/` (G3b).
- Lệnh `ezy dev …` vẫn chưa chạy; tài liệu web chỉ mô tả `ezy.sh` và thao tác trong admin (G2d).
- Mốc 2 kiểu "một project `khangnguyen/`" (G4) mới là đề xuất trên giấy, chưa thử export. Cần chạy thử trên native trước khi chốt Phase 6.
- Bảng `ecm_*` có phải của plugin chính thức `ecommerce` hay không: chưa xác minh (chỉ thấy tên trùng hệ). Không ảnh hưởng khuyến nghị "giữ nguyên".
