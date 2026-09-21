# Mỗi node kind là 1 container riêng — image không share xuyên node

## Câu hỏi ban đầu

Docker layer sharing đã verify là có thật (xem
[docker-layer-sharing.md](docker-layer-sharing.md)) — khi deploy lên k8s,
cơ chế share đó có tiếp tục hoạt động không?

## Trong 1 node: có, cùng cơ chế

Mỗi node k8s chạy containerd — cũng dùng content-addressable storage giống
Docker (layer định danh bằng hash, lưu 1 lần). 2 pod trên **cùng 1 node**
dùng chung base image → containerd của node đó lưu layer base đúng 1 lần,
share cho cả 2 pod.

## Giữa các node: KHÔNG

```
$ kind load docker-image --help
Loads docker images from host into all or specified nodes by name

Flags:
      --nodes strings   comma separated list of nodes to load images into
```

Verify từ chính CLI `kind` cài trên máy — mặc định (không truyền
`--nodes`) nạp ảnh vào **tất cả** node.

Lý do: mỗi "node" trong kind thực chất là **1 container Docker riêng biệt**
(`docker ps` thấy `social-control-plane`, `social-worker`, `social-worker2`
là 3 container độc lập) — mỗi container có filesystem riêng, containerd
riêng, không share gì với container/node khác. Nạp ảnh vào node A không
giúp node B có sẵn ảnh đó — phải nạp riêng, và layer base bị **lưu vật lý
riêng ở từng node** đã được nạp vào.

## Áp vào cluster cụ thể của project

3 node: `social-control-plane` (có taint `NoSchedule` — không pod app nào
từng được lên lịch chạy ở đó, xem ADR liên quan Traefik), `social-worker`,
`social-worker2`. `Makefile` (`make load`) gọi `kind load docker-image ...`
**không có `--nodes`** → base layer bị nạp lãng phí vào cả
`social-control-plane`, dù không pod nào từng chạy ở đó.

**Chưa áp dụng** (ghi nhận, để làm sau nếu cần): thêm
`--nodes social-worker,social-worker2` vào `load`/`load-%` trong Makefile
để bỏ phần lãng phí đó — sẽ giảm thời gian `kind load` (vốn đã ghi nhận là
chậm trong `tracking.md`) vì bớt 1 node phải nạp.

## Bài học tổng quát

"Nhiều node k8s" không tự động nghĩa là "nhiều máy vật lý share
storage/network" theo kiểu ta hay hình dung — với kind (dev local), mỗi
node chỉ là 1 container trên cùng máy, cô lập filesystem với nhau y hệt bất
kỳ 2 container Docker độc lập nào khác. Cơ chế share layer chỉ có tác dụng
**trong phạm vi 1 image store** — đổi phạm vi (nhiều node, nhiều máy, nhiều
cluster) là mất share, phải tính lại chi phí từ đầu cho từng phạm vi đó.
