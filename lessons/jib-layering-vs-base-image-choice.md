# JIB tối ưu layer-splitting để build nhanh — khác hẳn việc chọn base nhỏ nhất

## Câu hỏi ban đầu

"Khi design ra container-jib thì người ta đã tìm phương án gần như tối ưu
rồi đúng không?" — Không hẳn. JIB tối ưu **1 trục cụ thể**, không phải "làm
image nhỏ nhất có thể".

## JIB thật sự tối ưu cái gì

JIB (công cụ của Google, Quarkus dùng qua `quarkus-container-image-jib`)
được thiết kế cho:

1. **Chia layer theo tần suất thay đổi** — tách base image, dependencies
   (thư viện bên thứ 3, ít đổi), resources, và class của chính app (đổi
   thường xuyên nhất) thành các layer riêng. Sửa code app → chỉ layer cuối
   cùng cần build/push lại, các layer trước (base + deps) giữ nguyên cache.
2. **Không cần Docker daemon** — JIB build image bằng cách thao tác trực
   tiếp định dạng OCI/Docker (tar layer), có thể push thẳng lên registry mà
   không cần `docker build` — hữu ích trong CI không có Docker-in-Docker.
3. **Build reproducible** — bỏ metadata không xác định (timestamp...) theo
   mặc định, cùng input → cùng digest output.

Không cái nào trong 3 việc trên liên quan tới **chọn base image nào**.

## Bằng chứng — so layer digest giữa 2 service khác hẳn nhau

```
auth-service: 1b349a33... 1c471471... a0a26836... eff99705... a4fd05c0... | (khác từ đây)
post-api:     1b349a33... 1c471471... a0a26836... eff99705... a4fd05c0... | (khác từ đây)
              └────────── 5 layer đầu giống hệt hash (base + JRE) ──────────┘
```
(lấy từ `docker inspect <image> --format '{{.RootFS.Layers}}'`)

5 layer đầu giống hệt dù 2 service code hoàn toàn khác nhau — đúng cơ chế
JIB: base/JRE là 1 layer cố định, tách biệt khỏi code app. Đây là bằng
chứng JIB làm đúng việc nó được thiết kế để làm.

## Nhưng base nào được đưa vào layer đó — vẫn là quyết định của dev

Default của Quarkus khi không set `quarkus.jib.base-jvm-image` là
`registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` — **557MB**, không
phải lựa chọn nhỏ nhất tồn tại (xem
[container-base-image-comparison.md](container-base-image-comparison.md)
cho toàn bộ so sánh). JIB không hề "chọn hộ" base tối ưu size — quyết định
đó luôn bỏ ngỏ, mặc định của Quarkus thiên về enterprise-support (Red Hat
UBI) hơn là size nhỏ nhất.

## Bài học tổng quát

"Công cụ X được thiết kế tốt" không có nghĩa "mọi thông số output của X đã
được tối ưu". Cần hỏi cụ thể: công cụ đó tối ưu **trục nào** (ở đây: cơ chế
build/cache layer), còn trục khác (size, security posture của base...) có
thể vẫn hoàn toàn mở, đợi người dùng tự quyết.
