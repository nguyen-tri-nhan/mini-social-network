# 0007: Mã hoá tin nhắn chat bằng envelope encryption (KMS) cả khi lưu lẫn trên Kafka, ID tin nhắn UUIDv7

Status: accepted

## Context
Tính năng nhắn tin 1:1 (`specs/messaging-plan.md`) sắp làm. Tra cách thế giới làm: app nhắn
tin cá nhân (Signal, WhatsApp, Messenger từ 12/2023) dùng E2EE; công cụ làm việc/cộng đồng
(Slack, Discord, cloud chat của Telegram) chỉ mã hoá khi truyền + khi lưu với **server giữ
khoá** — Slack nói rõ không làm E2EE vì mất tìm kiếm/bot; Messenger phải tự thiết kế hẳn
giao thức Labyrinth mới lưu được lịch sử E2EE đa thiết bị. Ngoài ra plan cũ phân trang theo
`created_at` bị lệch khi 2 tin trùng timestamp.

## Decision
- **Mức mã hoá:** envelope encryption, server giữ khoá (mô hình Slack EKM) — không E2EE.
  CMK trong KMS (LocalStack, `alias/social-chat`), 1 DEK AES-256 / conversation (đã bọc, lưu
  ở `conversation_key`, `EncryptionContext` = conversationId), nội dung mã hoá AES-256-GCM
  với AAD = `conversationId|messageId|keyVersion`.
- **Mã hoá trước khi vào outbox:** event `CHAT_MESSAGE` trên topic `social.chat` chỉ chở bản
  mã + DEK đã bọc; `websocket-service` tự unwrap qua KMS rồi giải mã trước khi push. Bản
  thường không bao giờ nằm trong Postgres, outbox, Kafka (kể cả dead-letter topic) hay log.
- **ID tin nhắn:** UUIDv7 sinh trong Kotlin (Postgres 15 chưa có `uuidv7()`), phân trang
  keyset chỉ theo `id` — cùng ý tưởng snowflake ID của Discord.

Chi tiết thiết kế: `messaging-plan.md` §12.

## Consequences
- Thêm công nghệ mới: KMS (`quarkus-amazon-kms`) trong lib dùng chung `chat-crypto` — gắn với
  câu hỏi học cụ thể "envelope encryption/KMS hoạt động thế nào" (constitution quy tắc 3); khai
  BOM đúng pattern ADR 0004.
- `websocket-service` trở thành điểm giải mã → **bắt buộc có xác thực WebSocket trước khi làm
  chat** (hiện WS public, ai subscribe cũng nhận bản đã giải mã). Quyết định đó để ADR riêng.
- Không chống được người có quyền gọi KMS bằng credentials của service hoặc chiếm được service
  — cái giá chấp nhận so với E2EE. Đổi lại giữ được tìm kiếm/kiểm duyệt phía server về sau.
- CMK nằm trong LocalStack ephemeral: nếu Postgres có volume bền mà LocalStack không, restart
  sẽ làm mọi tin nhắn cũ không giải mã được.

## Nguồn
- Signal — lịch sử chỉ trên thiết bị: https://conductatlas.com/platform/signal/signal-privacy-policy/provision/CA-P-038122/message-history-stored-on-user-devices-only/
- Meta — E2EE mặc định cho Messenger: https://engineering.fb.com/2023/12/06/security/building-end-to-end-security-for-messenger/
- Meta — giao thức Labyrinth: https://engineering.fb.com/wp-content/uploads/2023/12/theLabyrintHencryptedMessagestorageProtocol_12-6-2023.pdf
- Telegram — cloud chat vs secret chat: https://core.telegram.org/api/end-to-end
- Slack EKM: https://slack.com/resources/why-use-slack/slack-enterprise-key-management
- Slack không làm E2EE: https://techtarget.com/searchunifiedcommunications/news/252448559/Slack-encryption-will-soon-include-enterprise-key-management
- Discord lưu tin nhắn: https://resources.scylladb.com/blog/how-discord-stores-trillions-of-messages
- Discord snowflake + phân trang before/after: https://docs.discord.com/developers/reference
