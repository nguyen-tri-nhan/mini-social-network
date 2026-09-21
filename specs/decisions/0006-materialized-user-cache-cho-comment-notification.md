# 0006: Materialized user-cache (qua Kafka) để enrich tên tác giả trong Comment/Notification

Status: accepted

## Context
`Comment` (interaction-service) và `Notification` (notification-service) chỉ
lưu `authorId`/`actorId` — tên hiển thị phải FE tự gọi `GET /api/users/{id}`
cho từng item, dựa vào React Query cache để giảm số request trùng. Hỏi "1
triệu user cùng online thì chọn cách nào" → so 3 hướng (BE gọi đồng bộ REST,
gRPC+batch, materialized view qua Kafka) — ở quy mô lớn, mọi hướng gọi đồng
bộ (REST hay gRPC) đều biến `user-service` thành single dependency cho mọi
read-path hiển thị tên, dễ cascade khi `user-service` chậm/down.

## Decision
Chọn materialized view: `interaction-service` tự giữ 1 bảng cache cục bộ
`user_ref` (`userId → username, firstname, lastname, avatarUrl`), cập nhật
qua consume topic `social.user` (event `USER_READY` lúc tạo profile,
`USER_UPDATED` lúc sửa profile — `UserService.update()` trước đó KHÔNG bắn
event nào, đây là gap được vá luôn trong lần này). Khi tạo Comment/Vote,
`interaction-service` join với `user_ref` cục bộ (không gọi mạng) để: (1)
trả tên tác giả ngay trong `CommentDto`, (2) nhúng tên actor vào payload của
event `COMMENT_CREATED`/`VOTE_CAST` khi publish ra outbox.

`notification-service` KHÔNG cần tự giữ cache riêng — nó COPY thẳng các field
tên actor đã có sẵn trong payload `COMMENT_CREATED`/`VOTE_CAST` (do
interaction-service nhúng vào) khi tạo `Notification`, lưu denormalized luôn
trong bảng (chấp nhận "đóng băng" tên tại thời điểm notification được tạo —
đúng ngữ nghĩa của 1 thông báo, không cần join lại).

`websocket-service` không đổi gì — payload nó forward cho FE giờ tự động có
sẵn tên actor vì interaction-service đã nhúng từ gốc.

## Consequences
- FE bỏ hẳn `useQuery(usersApi.getById(...))` ở `CommentItem`/
  `NotificationItem`/toast — backend trả tên sẵn, không còn network call phụ
  nào ở client.
- `Comment` không lưu tên (join tươi từ `user_ref` mỗi lần đọc → luôn cập
  nhật theo tên mới nhất), còn `Notification` lưu đông cứng tên lúc tạo (2
  chiến lược khác nhau có chủ đích, không phải thiếu nhất quán).
- Thêm eventual-consistency: `interaction-service` có thể có tên cũ trong
  `user_ref` vài giây sau khi user đổi tên (đến khi consume kịp
  `USER_UPDATED`) — chấp nhận được cho use case hiển thị tên.
- Đánh đổi ngược: thêm 1 bảng + 1 consumer mới ở `interaction-service`, và
  event contract của `USER_READY`/`USER_UPDATED`/`COMMENT_CREATED`/
  `VOTE_CAST` đổi (thêm field, additive — không breaking các consumer cũ chỉ
  đọc field họ cần).
