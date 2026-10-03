# Service Dependencies & Thứ tự Deploy

Bản đồ phụ thuộc thật giữa các service — lấy từ đọc code + config trực tiếp
(`application.properties`, REST client, Kafka consumer config), không suy
đoán từ kiến trúc lý thuyết. Mục đích: biết cái nào cần lên trước cái nào khi
deploy trên máy yếu tài nguyên (kind local), tránh CPU/memory storm khi 9 JVM
cold-start cùng lúc — xem `tracking.md` phần Kafka/OTel để biết bối cảnh sự
cố đã gặp.

---

## Tầng phụ thuộc

```
Tầng 0 — Infra
  Postgres, Redis, Kafka, LocalStack, LGTM (song song được, deploy-infra)

Tầng 1 — App service chỉ cần infra, không gọi service khác
  auth-service · post-api · user-api · notification-api · websocket-service · chat-api
  (chat-api + websocket-service cần LocalStack KMS có key alias/social-chat — init hook tạo sẵn)

Tầng 2 — App service gọi service khác (đồng bộ, qua REST)
  interaction-service  →  cần post-api đã lên (soft dependency, xem dưới)

Tầng 3 — Consumer, phụ thuộc Kafka + hưởng lợi khi producer đã tồn tại
  user-consumer · post-consumer · notification-consumer

Tầng 4 — Debezium / kafka-connect
  Cần outbox table tồn tại (Liquibase của auth-service/post-api/
  interaction-service/user-api/chat-api đã chạy) — Makefile đã đúng thứ tự này sẵn
  (deploy-debezium chạy sau deploy-services). `table.include.list` của
  connector phải liệt kê đủ 5 bảng: auth, post, interaction, users, chat.

Tầng 5 — Traefik routes
```

## Bảng phụ thuộc chi tiết (grep trực tiếp từ code, cập nhật 29/9/2026)

| Service | Postgres | Redis | Kafka consume (topic) | Publish (qua outbox) | S3 | Gọi service khác |
|---|---|---|---|---|---|---|
| `auth-service` | ✅ | — | — | `social.auth` | — | — |
| `post-api` | ✅ | ✅ | — | `social.post` | ✅ | — |
| `user-api` | ✅ | ✅ | — | `social.user` (`USER_PROFILE_UPDATED`) | — | — |
| `notification-api` | ✅ | ✅ | — | — | — | — |
| `websocket-service` | — | ✅ | `social.interaction`, `social.user`, `social.chat` | — | — | — (KMS Decrypt qua LocalStack) |
| `interaction-service` | ✅ | — | `social.user` (cache `user_ref`, ADR 0006) | `social.interaction` | — | **`post-api`** (REST, `/internal/articles/{id}`) |
| `chat-api` | ✅ | — | `social.user` (cache `user_ref`, ADR 0006) | `social.chat` | — | — (KMS qua LocalStack, ADR 0007) |
| `user-consumer` | ✅ | ✅ (transitive qua `user-service`) | `social.auth` | `social.user` (`USER_READY`) | — | — |
| `post-consumer` | ✅ | ✅ | `social.interaction` | — | — | — |
| `notification-consumer` | ✅ | ✅ | `social.interaction` | — | — | — |

Consumer ghi state (user-consumer, post-consumer, notification-consumer,
interaction-service, chat-api) dùng `failure-strategy=dead-letter-queue`: event lỗi đi
sang topic `dead-letter-topic-<channel>` thay vì bị nuốt. `websocket-service`
cố tình giữ best-effort (push toast cũ phát lại sau là vô nghĩa).

Tất cả service REST đã có sẵn `readinessProbe: httpGet /q/health/ready` trong
`k8s/services/*.yaml` — dùng được thẳng làm tín hiệu "Ready" cho script deploy
tuần tự, không cần tự chế health check.

## Coupling đồng bộ duy nhất — không cần decouple

`interaction-service` gọi `post-api` qua `PostApiClient` (`GET /internal/
articles/{id}`) để lấy `authorId` khi tạo comment/vote, phục vụ publish
notification event. Code thật (`InteractionService.kt`):

```kotlin
val authorId = try {
    postApiClient.getArticle(targetId).authorId
} catch (e: Exception) {
    log.warnf("Could not fetch article author for %s: %s", targetId, e.message)
    null
}
```

**Đã tự graceful-degrade** — nếu `post-api` không reachable, comment/vote vẫn
thành công, chỉ mất thông tin `authorId` trong notification (log warning, không
throw). Lưu ý: vì degrade êm như vậy nên lỗi cấu hình secret (`INTERNAL_SECRET_KEY`
lệch giữa 2 service → `/internal` trả 401) sẽ không làm vỡ request nào mà chỉ
khiến **không còn notification nào được tạo** — kiểm tra log warning này khi
đổi secret. Đây là điểm coupling đồng bộ **duy nhất** trong toàn hệ thống; mọi
giao tiếp còn lại giữa các service đều qua Kafka/outbox pattern (async, đúng
chủ đích thiết kế ban đầu — xem `specs/outbox-pattern.md`).

**Kết luận: không cần viết spec đề xuất decoupling.** 1 điểm coupling, đã
graceful-degrade sẵn, không phải "quá nhiều coupling" — chỉ cần đảm bảo thứ
tự deploy hợp lý (nêu dưới) để tránh log warning không cần thiết lúc mới lên,
không phải vấn đề kiến trúc cần sửa.

## Thứ tự deploy khuyến nghị

```
1. auth-service
2. post-api              ← trước interaction-service vì (6) cần nó
3. user-api
4. notification-api
5. websocket-service
6. interaction-service   ← sau post-api
7. chat-api
8. user-consumer
9. post-consumer
10. notification-consumer
```

(1-5 không phụ thuộc lẫn nhau, thứ tự trong nhóm không quan trọng; 8-10 tương
tự.) Tự động hoá bằng `scripts/deploy-sequential.sh` / `make deploy-services-
sequential` — xem README của Makefile (`make help`).
