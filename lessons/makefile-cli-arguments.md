# Vì sao Makefile chỉ nhận `VAR=value`, không nhận cú pháp `--flag`

## Bối cảnh

Sau khi thêm `RERUN=1` vào Makefile để ép Gradle rebuild (xem
[gradle-up-to-date-caching.md](gradle-up-to-date-caching.md)), câu hỏi đặt
ra: sao không làm cú pháp quen thuộc hơn kiểu `make build --no-cache`
(giống `docker build --no-cache`, hay `npm run build -- --no-cache`)?

## Verify thật

```
$ make build-auth --no-cache
make: unrecognized option `--no-cache'
Usage: make [options] [target] ...
```

`make` tự chặn ngay từ lúc parse dòng lệnh, không tới được `Makefile`.

## Vì sao

`make` coi mọi thứ bắt đầu bằng `--` (hoặc `-`) là **option của chính nó**
— nó có 1 danh sách cố định option biết trước (`-n`/`--dry-run`,
`-j`/`--jobs`, `-B`/`--always-make`, `-C`/`--directory`...). Gõ 1 flag nó
không biết → lỗi ngay, không có khái niệm "truyền tiếp flag lạ xuống
recipe" như một số công cụ khác.

So sánh với `npm run build -- --no-cache`: npm có quy ước `--` để tách biệt
"argument của npm" và "argument truyền cho script bên dưới" — `make` không
có cơ chế tương đương.

## Cách duy nhất Makefile nhận "tham số" từ CLI

**Biến** (`VAR=value`) — đây không phải là cách vòng, mà là **cơ chế chính
thức duy nhất** Make hỗ trợ để tham số hoá behavior từ dòng lệnh:

```makefile
RERUN :=
ifeq ($(RERUN),1)
RERUN_FLAG := --rerun-tasks
endif
```

```bash
make build RERUN=1
```

Biến gán trên dòng lệnh **override** giá trị gán bằng `:=`/`=` trong chính
Makefile — đây là hành vi chuẩn của Make (command-line variable luôn thắng).

## Điểm dễ nhầm: `make -B`/`--always-make`

Make cũng có sẵn `-B` = "chạy lại toàn bộ target bất kể gì", nghe giống ý
muốn nhưng **không giải quyết đúng vấn đề gốc**: `-B` chỉ ảnh hưởng việc
**Make** có skip target của chính nó hay không. Các target build trong
project này đã khai `.PHONY` — nghĩa là Make **luôn chạy lại chúng mỗi lần
gọi**, chưa bao giờ tự skip từ phía Make. Vấn đề "BUILD SUCCESSFUL nhưng
không ra image thật" nằm ở tầng **Gradle** (task-level up-to-date check),
hoàn toàn tách biệt với Make — `-B` của Make không chạm được tới lớp đó,
phải dùng đúng cờ của công cụ bên trong (`gradle --rerun-tasks`).

## Bài học tổng quát

Khi 1 Makefile target gọi 1 công cụ build khác (Gradle, npm, cargo...), có
**2 lớp caching độc lập chồng lên nhau**: Make's own target-skip (dựa vào
`.PHONY`/file timestamp) và cơ chế cache riêng của công cụ bên trong. Muốn
"force rebuild" đúng chỗ, phải biết đang cần bypass lớp nào — `make -B`
không tự động lan xuống bypass luôn cache của Gradle/npm/cargo bên trong.
