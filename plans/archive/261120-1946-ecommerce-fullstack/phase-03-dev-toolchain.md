---
title: "Phase 3: Dev toolchain native"
status: done
priority: P1
owner: Cyborg
effort: "2h"
dependencies: [1]
gate: "Johnny duyệt Q2"
---

# Phase 3: Dev toolchain native (JDK 8, Maven, Ezyplatform SDK)

## Overview
`ezy dev check` báo thiếu jdk8, maven, sdk, ezyplatform_home; Docker một mình không build được plugin. Cài toolchain + một Ezyplatform native ở `~/ezyplatform-dev-site` trên cổng trống, song song Docker.

## Requirements
- [ ] JDK 8 (Temurin, arm64 nếu có) + Maven qua Homebrew; không đổi JAVA_HOME toàn máy nếu đang có JDK khác (dùng biến cục bộ cho dự án).
- [ ] Native Ezyplatform dev site ở cổng khác 8080/9090/2208/3005, DB riêng (không dùng chung MySQL Docker).
- [ ] `ezydev setup` theo skill `ezy-develop`.

## Success Criteria
- `ezy dev check` tất cả `ok`.
- Tạo được plugin mẫu rỗng, `mvn -q package` thành công, export ra artifact cài được (chưa cài lên Docker).
- Docker site vẫn 200 sau khi native chạy (không tranh cổng). Lệnh gỡ toolchain ghi trong báo cáo.
