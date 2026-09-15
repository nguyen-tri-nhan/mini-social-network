# 0002: Tách quarkus-amazon-services-bom khỏi social-bom dùng chung + fix index-dependency thiếu của auth-service

Status: accepted

## Context

`./gradlew build` (JIB packaging) fail cho **cả 19 module** vì `social-bom`
import chung `quarkus-bom:3.25.1` và `quarkus-amazon-services-bom:3.20.1` —
2 BOM khác "platform stream", Quarkus `CurateOutcomeBuildStep` reject combo
này (xem ADR 0001). Đã thử nâng amazon-services-bom lên bản mới nhất trên
Maven Central (`3.22.3`, `3.23.0.CR1`) — quarkiverse-amazon-services **chưa
release bản nào cùng stream với `3.25.x`**, nên không version nào của BOM này
hết conflict được với `quarkus-bom:3.25.1` cả. Hạ `quarkusPlatformVersion` cả
project xuống `3.22.3` để khớp thì né được lỗi BOM nhưng đổi core Quarkus
version cho toàn bộ 19 module — rủi ro/blast radius lớn hơn cần thiết, vì chỉ
`post-service` và `post-api` thực sự dùng S3 (`grep` xác nhận).

## Decision

- Bỏ `quarkus-amazon-services-bom` khỏi `social-bom` (platform dùng chung).
- Import trực tiếp `quarkus-amazon-services-bom:3.20.1` chỉ ở
  `post-service/build.gradle.kts` và `post-api/build.gradle.kts` — 2 module
  duy nhất cần S3. 17 module còn lại không còn thấy 2 BOM khác stream nữa.
- Nhân tiện fix 1 bug lộ ra sau khi qua được lỗi BOM: `auth-service`
  thiếu `quarkus.index-dependency.auth-service-dao.*` trong
  `application.properties` (mọi service khác — user-api, post-api,
  notification-api — đều có khai báo tương tự cho dao/service module của
  mình, riêng auth-service thì thiếu) → Quarkus không index được
  `CredentialsRepository`/`OutboxRepository` thành CDI bean, ArC validate
  fail. Đã thêm 2 dòng còn thiếu.

## Consequences

- **18/19 module build được** (`./gradlew build -x test` pass), verify bằng
  build thật, không chỉ đọc code. `./gradlew test` vẫn pass toàn bộ, không
  regressions.
- **`post-api` vẫn fail `build`** (đóng gói JIB) — đây là giới hạn thật của
  hệ sinh thái quarkiverse-amazon-services, không phải lỗi cấu hình có thể tự
  fix bằng cách đổi version. Tính năng upload ảnh (S3 presign) vẫn chạy được
  ở `quarkusDev` (không hit `quarkusAppPartsBuild`), nhưng **không đóng gói
  được image để deploy k8s cho post-api cho tới khi**: (a) quarkiverse-
  amazon-services release bản tương thích stream `3.25.x`, hoặc (b) chấp
  nhận hạ `quarkusPlatformVersion` toàn project xuống dòng cũ hơn (đánh đổi
  đã nêu ở Context, chưa chọn), hoặc (c) bỏ hẳn amazon-services-bom, tự pin
  version `quarkus-amazon-s3` + `software.amazon.awssdk:*` thủ công (chưa
  thử — rủi ro compatibility với extension đã lâu không release, lần cuối
  01/2025).
- `post-consumer` (cũng phụ thuộc `post-service` qua `api(platform(...))`
  transitively) **không bị conflict** dù có transitive dependency tới BOM đó
  — quan sát thực nghiệm, chưa rõ cơ chế chính xác vì sao Quarkus curate
  outcome không áp dụng cho nó; không đào sâu thêm vì không cần thiết cho
  quyết định này.
