# Lessons

Ghi chú khái niệm/kiến thức học được trong quá trình làm project — khác với
`tracking.md` (log sự cố theo thời gian), `specs/` (design doc, quyết định
kiến trúc), `docs/` (hướng dẫn thao tác). Ở đây là **giải thích khái niệm để
nhớ lại sau**, mỗi topic 1 file, viết chi tiết đủ để đọc lại không cần nhớ
ngữ cảnh lúc học.

## Danh sách

| File | Chủ đề |
|---|---|
| [gradle-up-to-date-caching.md](gradle-up-to-date-caching.md) | Gradle UP-TO-DATE check hoạt động thế nào, vì sao "BUILD SUCCESSFUL" có thể nói dối |
| [makefile-cli-arguments.md](makefile-cli-arguments.md) | Vì sao Makefile chỉ nhận `VAR=value`, không nhận cú pháp `--flag` tuỳ ý |
| [container-base-image-comparison.md](container-base-image-comparison.md) | So sánh base image JRE — Alpine (musl) vs UBI/Debian/Chainguard (glibc), số đo thật |
| [jib-layering-vs-base-image-choice.md](jib-layering-vs-base-image-choice.md) | JIB tối ưu layer-splitting để build nhanh — khác hẳn việc chọn base nhỏ nhất |
| [docker-layer-sharing.md](docker-layer-sharing.md) | Docker/containerd share layer theo content-hash — cơ chế thật, verify bằng `docker system df -v` |
| [kind-multi-node-image-loading.md](kind-multi-node-image-loading.md) | Mỗi node kind là 1 container riêng — `kind load` không share layer xuyên node |
| [kafka-advertised-listeners-local-dev.md](kafka-advertised-listeners-local-dev.md) | Vì sao port-forward suông không đủ để local dev service nói chuyện với Kafka trong cluster |
| [jwt-storage-security.md](jwt-storage-security.md) | localStorage vs httpOnly cookie cho JWT — đánh đổi bảo mật thật |
