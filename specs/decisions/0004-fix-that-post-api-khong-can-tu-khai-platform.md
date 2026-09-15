# 0004: post-api không cần tự khai platform amazon-services-bom — fix thật cho ADR 0002/0003

Status: accepted

## Context

ADR 0002 tách `quarkus-amazon-services-bom` khỏi `social-bom` dùng chung,
import trực tiếp vào cả `post-service` **và** `post-api`. ADR 0003 thử đổi
groupId BOM cho `post-api` để né lỗi "platform stream" — thất bại, lộ ra
conflict version thật khác. Câu hỏi đặt lại: tại sao `post-service` build
được mà `post-api` (chỉ dùng lại đúng S3 setup của `post-service`) lại
không?

Điều tra: `post-service` không có plugin `id("io.quarkus")` (chỉ là Kotlin
library thường) → không bao giờ chạy `quarkusAppPartsBuild` → Quarkus
`CurateOutcomeBuildStep` (bước soi "platform stream") không bao giờ được gọi
cho module này. `post-api` có `id("io.quarkus")` → chạy augmentation thật,
bước soi này MỚI kích hoạt — và nó chỉ soi các platform BOM được khai
**trực tiếp** trong chính build script của module Quarkus app đang build,
không soi BOM chỉ đến gián tiếp qua 1 dependency project khác. `post-api`
tự thêm dòng `implementation(platform("io.quarkus.platform:quarkus-amazon-
services-bom:3.20.1"))` ở ADR 0002 — chính dòng đó khiến nó bị soi, trong
khi hoàn toàn không cần thiết vì `post-service` đã `api(platform(...))` BOM
đó rồi, propagate transitive sang `post-api` qua `implementation(project(":
post-service"))` bình thường (Gradle `api` platform vẫn truyền version
constraint xuyên module, chỉ là không được Quarkus tooling coi là "platform
trực tiếp" của module tiêu thụ).

Verify: xoá dòng platform thừa khỏi `post-api/build.gradle.kts`, giữ
nguyên ở `post-service` → `./gradlew build` **19/19 module pass** (lần đầu
tiên trong session).

## Decision

Xoá `implementation(platform("io.quarkus.platform:quarkus-amazon-services-
bom:3.20.1"))` khỏi `post-api/build.gradle.kts`. Chỉ giữ platform này ở
`post-service/build.gradle.kts` (module thực sự cần khai trực tiếp vì các
class dùng S3 — `S3Service` — nằm ở đây).

## Consequences

- `./gradlew build -x test` **19/19 module pass**. `./gradlew test` toàn bộ
  cũng pass, không regression. Không còn service nào bị chặn build vì lý do
  này.
- Nguyên tắc rút ra để tránh lặp lại: **module Quarkus app (`id("io.quarkus")`)
  không nên tự khai trực tiếp 1 platform BOM nếu module dependency nó phụ
  thuộc đã khai bằng `api(platform(...))` rồi** — khai trực tiếp thừa và có
  thể kích hoạt validation không cần thiết của Quarkus tooling. Chỉ khai
  trực tiếp platform ở module thực sự SỞ HỮU logic dùng extension đó.
- ADR 0002 vẫn đúng về việc tách BOM khỏi `social-bom` dùng chung (không đổi).
  ADR 0003 (thử đổi groupId) coi như không cần thiết nữa — nguyên nhân gốc
  không phải version BOM sai, mà là khai platform ở sai chỗ.
