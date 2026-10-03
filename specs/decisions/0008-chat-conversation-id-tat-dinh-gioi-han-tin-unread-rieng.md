# 0008: Chat 1:1 — ID conversation tất định, giới hạn tin 4.096 ký tự, unread tách khỏi notification

Status: accepted

## Context
3 câu hỏi mở của `messaging-plan.md` §8. Muốn tạo conversation idempotent mà không thêm cột/khoá ở
DB; idempotent thuần ở tầng service không đúng khi chạy nhiều instance (khoá trong RAM chỉ đúng 1
instance, khoá Redis vẫn phải đọc DB và có rủi ro hết hạn, `Idempotency-Key` chỉ chống gửi lại
cùng 1 request chứ không chống A↔B nhắn nhau cùng lúc). Muốn bỏ giới hạn độ dài vì cột Postgres
không giới hạn — nhưng mọi outbox dùng chung 1 connector Debezium, Kafka mặc định từ chối message
> ~1 MB. Muốn chat có icon/badge riêng như Facebook thay vì đổ vào chuông notification.

## Decision
- **ID conversation 1:1 tất định:** `UUID.nameUUIDFromBytes("dm:<userId nhỏ>:<userId lớn>")`;
  tạo bằng `INSERT … ON CONFLICT (id) DO NOTHING RETURNING id` — khoá chính sẵn có chống trùng,
  chỉ bên tạo được row mới insert participant và gọi KMS.
- **Độ dài:** cột không giới hạn, API `@Size(max = 4096)` + zod `.max(4096)` (như Telegram).
- **Unread:** icon chat + badge riêng trong Navbar, không dùng `notification-service`; theo dõi bằng
  `conversation_participant.last_read_message_id` (UUIDv7, chỉ tiến lên), kéo từ Phase 2 lên Phase 1.
- **Tên/avatar người kia:** `chat-api` consume `social.user` vào cache `user_ref` (pattern ADR 0006).

Chi tiết: `messaging-plan.md` §13.

## Consequences
- conversationId **không còn bí mật** (tính được từ 2 userId public) → mọi endpoint phải kiểm tra
  participant, và topic WS bắt buộc theo người nhận (`user_{id}_chat`) kèm xác thực WS — topic
  `conversation_{id}_chat` bị bỏ. Xác thực WS vẫn là blocker chưa chốt.
- Giới hạn độ dài là hàng rào bảo vệ connector dùng chung: 1 tin quá lớn từng có thể làm đứng toàn
  bộ event (notification, đếm comment, tạo profile) và phình WAL qua replication slot.
- `chat-api` thêm 1 consumer Kafka (`social.user`) — cùng mô hình interaction-service, có DLQ.
- Dropdown xem trước tin cuối phải giải mã từng conversation → tới N lần gọi KMS khi cache nguội.

## Nguồn
- Debezium `RecordTooLargeException` khi bản ghi vượt giới hạn Kafka: https://kafka-options-explorer.conduktor.io/debezium/errors/all-record-too-large
- Giới hạn ký tự Discord/Telegram/Slack: https://textrepeater.it.com/blog/character-limits-on-every-platform
