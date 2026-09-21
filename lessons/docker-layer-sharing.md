# Docker/containerd share layer theo content-hash

## Câu hỏi ban đầu

9 service, mỗi image ~400-490MB theo `docker images` — vậy tổng dung lượng
đĩa thật phải trả có phải ~9 × 450MB ≈ 4GB không?

## Không — verify bằng `docker system df -v`

```
REPOSITORY                    SIZE      SHARED SIZE   UNIQUE SIZE
nhan/auth-service              435MB     285.7MB       149.7MB
nhan/post-api                  451MB     285.8MB       164.9MB
nhan/notification-consumer     472MB     371.5MB       100.2MB
eclipse-temurin:21-jre-alpine  287MB     285.7MB       0.9MB    ← base tự nó
```

Docker tự phân biệt `SHARED SIZE` (phần layer đang được ≥1 image khác cùng
tham chiếu) và `UNIQUE SIZE` (phần chỉ riêng image đó có). Base image
(`eclipse-temurin:21-jre-alpine`) chỉ có **0.9MB thật sự riêng của chính
nó** — gần như toàn bộ 285.7MB còn lại được tính "shared" vì 9 service kia
đang cùng trỏ vào đúng layer đó.

## Cơ chế

Mỗi layer trong image định danh bằng **hash nội dung** (SHA256 của layer
đó). Docker (và containerd bên dưới) lưu layer **đúng 1 lần trên đĩa** theo
hash — bất kể bao nhiêu image tham chiếu tới nó. Image manifest chỉ là 1
danh sách con trỏ (hash) tới các layer, không copy dữ liệu.

Runtime cũng vậy: container chỉ có 1 lớp ghi mỏng (writable layer) riêng
của nó; toàn bộ layer image bên dưới là **đọc-only, mount chung** — không
copy riêng cho từng container đang chạy.

## Hệ quả thực tế

Cộng dồn cột "SIZE" của N image cùng base **không phải** chi phí đĩa thật
phải trả. Chi phí thật ≈ (base + dependency chung, trả 1 lần) + Σ(phần
UNIQUE riêng từng service). Ví dụ: đổi sang base nhỏ hơn 30% chỉ tiết kiệm
đúng 1 lần (phần base), không nhân theo 9 service — lợi ích thật nhỏ hơn con
số "9 × (86MB tiết kiệm)" nghe có vẻ hấp dẫn.

## Giới hạn của cơ chế này — chỉ đúng trong CÙNG 1 image store

Docker Desktop VM có 1 image store dùng chung cho mọi image pull/build trên
máy đó — sharing áp dụng toàn cục trong phạm vi đó. Nhưng khi deploy lên
k8s, **mỗi node là 1 image store riêng** — sharing không tự động lan qua
node khác. Chi tiết:
[kind-multi-node-image-loading.md](kind-multi-node-image-loading.md).

## Bài học tổng quát

Khi ước lượng chi phí lưu trữ/băng thông cho nhiều image cùng base, luôn
kiểm tra công cụ có báo `SHARED`/`UNIQUE` không (`docker system df -v`) thay
vì cộng thô cột "Size" — con số cộng thô luôn overstate nếu các image có
layer chung.
