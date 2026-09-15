# 0003: Thử BOM native quarkiverse-amazon-services cho post-api — thất bại, giữ nguyên ADR 0002

Status: superseded by 0004 (kết luận "post-api vẫn không build được" không còn đúng — nguyên nhân gốc thật sự được tìm ra và fix ở 0004, không phải version BOM)

## Context

Research (xem lịch sử conversation, không lưu file research riêng) phát hiện
có **2 BOM `quarkus-amazon-services-bom` khác nhau**: bản mirror
`io.quarkus.platform:quarkus-amazon-services-bom` (đang dùng, lag lại — xem
ADR 0002) và bản native `io.quarkiverse.amazonservices:quarkus-amazon-services-bom`
do chính project quarkiverse-amazon-services tự release, cập nhật liên tục
(bản mới nhất `3.22.1`, phát hành 10/9/2026). Giả thuyết: dùng bản native sẽ
né được lỗi "different platform streams" vì không đăng ký như thành viên họ
`io.quarkus.platform`.

## Decision

Đã thử đổi `post-service` + `post-api` sang
`io.quarkiverse.amazonservices:quarkus-amazon-services-bom`, cả 2 version
`3.20.1` và `3.22.1`. **Cả 2 đều fail, với 2 lỗi khác nhau** (không phải lỗi
"platform stream" nữa):

- `3.20.1`: `The configuration class io.quarkus.vertx.http.runtime.HttpConfiguration
  must be an interface annotated with @ConfigRoot and @ConfigMapping`
- `3.22.1`: `java.lang.NoSuchMethodException:
  io.quarkus.runner.bootstrap.AugmentActionImpl.<init>(...)`

Cả 2 đều là dấu hiệu **conflict version thật ở tầng `quarkus-core`/
`quarkus-bootstrap`** (class-shape mismatch, method không tồn tại) — không
phải chỉ là nhãn "stream" bị chặn ở validation layer như ADR 0002 mô tả. Kết
luận: giả thuyết "đổi groupId là né được" **sai trong thực tế** — đã revert
`post-service`/`post-api` về đúng trạng thái ADR 0002
(`io.quarkus.platform:quarkus-amazon-services-bom:3.20.1`), verify lại 18/19
module build sạch, không regression.

## Consequences

- `post-api` **vẫn không build (JIB) được** — kết luận của ADR 0002 vẫn đúng
  và giờ có thêm bằng chứng: đây không phải vấn đề chọn sai BOM/groupId, mà
  là quarkiverse-amazon-services (ở mọi version đã thử) thực sự chưa tương
  thích với `quarkus-bom:3.25.1` ở tầng dependency sâu hơn nhãn "stream".
- Không nên thử thêm các version khác của `io.quarkiverse.amazonservices:
  quarkus-amazon-services-bom` mà không có lý do mới — đã thử bản cũ nhất
  gần với lúc project pin (`3.20.1`) và bản mới nhất hiện có (`3.22.1`), cả 2
  đều fail theo cách khác nhau, khả năng cao mọi version ở giữa cũng vậy.
- 2 hướng còn lại chưa thử (nêu ở ADR 0002, vẫn chưa chọn): (a) hạ
  `quarkusPlatformVersion` toàn project xuống dòng cũ hơn tương thích, hoặc
  (b) bỏ hẳn BOM, tự pin version `quarkus-amazon-s3` + AWS SDK thủ công. Cả 2
  đều cần quyết định đánh đổi rõ ràng, chưa làm.
