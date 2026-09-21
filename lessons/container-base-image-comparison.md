# So sánh base image JRE — Alpine (musl) vs UBI/Debian/Chainguard (glibc)

## Bối cảnh

Mỗi image service build ra ~400-490MB (`docker images`). Đặt câu hỏi: có
base nào nhẹ hơn `eclipse-temurin:21-jre-alpine` (đang dùng) mà vẫn chạy tốt
không? Nghiên cứu bằng cách **pull thật về đo**, không lấy số từ blog.

## Số đo thật (pull trực tiếp, `docker images <tag>`)

| Base | libc | Có shell? | DISK USAGE | CONTENT SIZE (nén) |
|---|---|---|---|---|
| `ubi9/openjdk-21-runtime:1.24` (default Quarkus khi không set gì) | glibc | có | 557MB | 137MB |
| `eclipse-temurin:21-jre` (Debian, KHÔNG phải Alpine) | glibc | có | 479MB | 118MB |
| `cgr.dev/chainguard/jre:latest` (Wolfi) | glibc | tối giản | 434MB | 107MB |
| `eclipse-temurin:21-jre-alpine` (đang dùng trong project) | musl | có | 287MB | 74.3MB |
| `gcr.io/distroless/java21-debian12:nonroot` | glibc | **không** | 270MB | 62.2MB |
| `bellsoft/liberica-openjre-alpine:21` | musl | có | 201MB | 50.8MB |

`CONTENT SIZE` (đã nén) mới là con số quyết định thời gian push/pull
registry và dung lượng lưu trữ thật — `DISK USAGE` (chưa nén, "Size" hiện
trong `docker images` mặc định) overstate so với chi phí thật.

## Quy luật rõ — trục quyết định là libc, không phải tên vendor

Mọi base dùng **glibc** (UBI, Debian-slim Temurin, Chainguard/Wolfi) đều
nặng gấp ~2x mọi base dùng **musl** (2 biến thể Alpine) — không phải trùng
hợp giữa các vendor, glibc tự nó to hơn musl đáng kể ở mọi bản phân phối đã
thử.

**Vì sao vẫn có người chọn glibc dù to hơn:** `musl` (Alpine) đôi khi lệch
hành vi với native library/JNI code viết cho glibc — rủi ro tương thích có
thật nhưng hẹp (chủ yếu ảnh hưởng thư viện native, hiếm khi ảnh hưởng code
Java thuần). Chainguard/Wolfi sinh ra chính vì muốn nhỏ theo tinh thần
Alpine **mà vẫn giữ glibc** — kết quả nhỏ hơn UBI/Debian nhưng vẫn to hơn cả
2 lựa chọn musl.

## Nguồn

- [Quarkus container-image guide](https://quarkus.io/guides/container-image)
  — xác nhận default thật (`ubi9/openjdk-21-runtime`) + property
  `quarkus.jib.base-jvm-image`
- Số đo: tự `docker pull` + `docker images` từng base, không suy diễn

## Áp dụng cho project này

`eclipse-temurin:21-jre-alpine` đã chạy ổn cả session với JDBC (Postgres),
Kafka client, Redis client, AWS S3 SDK — **không có dấu hiệu tương thích
musl nào từng gặp**. Không có bằng chứng cụ thể để phải trả thêm ~2x dung
lượng cho glibc. Quyết định: **giữ nguyên Alpine gốc**, không đổi sang
BellSoft (dù nhỏ hơn ~30%) — lợi ích thực tế nhỏ hơn tưởng tượng vì base
được share giữa các image (xem
[docker-layer-sharing.md](docker-layer-sharing.md)), không nhân theo số
service.
