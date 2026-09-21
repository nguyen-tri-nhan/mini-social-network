# Gradle UP-TO-DATE check — vì sao "BUILD SUCCESSFUL" có thể nói dối

## Bối cảnh

Sau khi xoá thư mục cache của JIB (`jib-core-application-layers-cache`, bị
corrupt), chạy lại `make build`:

```
BUILD SUCCESSFUL in 2s
150 actionable tasks: 150 up-to-date
```

2 giây, "successful" — nhưng `docker images nhan/post-api` **không có gì**.
Image chưa từng được build lại thật.

## Cơ chế

Gradle không chạy lại 1 task nếu nó tin task đó "up-to-date" — điều kiện: so
sánh **input đã đăng ký** (source code, config) và **output đã đăng ký** với
lần chạy trước, lưu trong cache riêng của Gradle (không nằm trong `build/`
của project mà ở `~/.gradle/caches/...`). Nếu input không đổi và output vẫn
"còn đó" theo hiểu biết của Gradle → skip, không chạy lại.

Vấn đề: với task build container image (JIB), thứ Gradle **thật sự theo
dõi** làm "output" chỉ là 2 file nhỏ trong `build/`:
```
build/jib-image.id
build/jib-image.digest
```
Đây chỉ là **bằng chứng** (marker file) rằng lần trước JIB đã build xong 1
image với digest đó — không phải bản thân cái image. Gradle **không có cách
nào biết** (và không có cơ chế nào để) hỏi lại Docker daemon "image với
digest này còn thật không". Docker là hệ thống ngoài, nằm ngoài input/output
mà Gradle đăng ký theo dõi.

## Chuỗi sự kiện thật đã xảy ra

1. Có 1 lần build **thành công thật** → Gradle ghi `jib-image.id`/`.digest`.
2. Cache JIB bị hỏng ở 1 lần build sau đó → build **fail** — nhưng lần fail
   không xoá 2 file marker kia (chúng vẫn còn từ lần thành công trước).
3. Xoá cache JIB (theo gợi ý sửa lỗi) → JIB tạo lại cache rỗng — nhưng
   **image thật trong Docker daemon đã không còn tồn tại** từ trước rồi
   (verify bằng `docker images -a` — không thấy digest đó ở đâu cả).
4. `make build` chạy lại → Gradle thấy source code không đổi, `jib-image.id`
   vẫn còn → kết luận "không có gì để làm" → skip toàn bộ, kể cả bước JIB
   containerize thật → báo `BUILD SUCCESSFUL, up-to-date` dù chưa hề đụng
   lại Docker.

## Cách phát hiện (khi nghi ngờ)

```bash
docker images <tên-image>          # image có thật trong daemon không
cat services/<svc>/build/jib-image.id   # digest Gradle nghĩ là output
docker images -a | grep <digest ngắn>   # digest đó có tồn tại ở đâu không
```

Nếu Gradle báo up-to-date nhưng `docker images` không thấy gì → chính xác
tình huống này.

## Fix

```bash
gradle ... --rerun-tasks
```
Ép Gradle bỏ qua toàn bộ up-to-date check, chạy lại thật mọi task trong
graph. Đã thêm vào Makefile: `make build RERUN=1` (xem `Makefile`, biến
`RERUN`/`RERUN_FLAG`).

## Bài học tổng quát

Up-to-date/caching mechanism (không chỉ Gradle — CI cache, Docker layer
cache, bất kỳ hệ build nào dùng input/output fingerprint) **chỉ đáng tin nếu
side-effect nó tạo ra không bị ai đó xoá/đổi từ bên ngoài phạm vi nó theo
dõi**. Xoá 1 phần cache "bên trong" (JIB) mà không xoá luôn phần "bên ngoài"
tương ứng (marker file Gradle theo dõi) → 2 lớp cache lệch pha nhau, dẫn tới
trạng thái "thành công giả".
