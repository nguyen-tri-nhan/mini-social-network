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
  auth-service · post-api · user-api · notification-api · websocket-service

Tầng 2 — App service gọi service khác (đồng bộ, qua REST)
  interaction-service  →  cần post-api đã lên (soft dependency, xem dưới)

Tầng 3 — Consumer, phụ thuộc Kafka + hưởng lợi khi producer đã tồn tại
  user-consumer · post-consumer · notification-consumer

Tầng 4 — Debezium / kafka-connect
  Cần outbox table tồn tại (Liquibase của auth-service/post-api/
  interaction-service đã chạy) — Makefile đã đúng thứ tự này sẵn
  (deploy-debezium chạy sau deploy-services).

Tầng 5 — Traefik routes
```

## Bảng phụ thuộc chi tiết (grep trực tiếp từ code, 16/9/2026)

| Service | Postgres | Redis | Kafka consume (topic) | S3 | Gọi service khác |
|---|---|---|---|---|---|
| `auth-service` | ✅ | — | — | — | — |
| `post-api` | ✅ | ✅ | — | ✅ | — |
| `user-api` | ✅ | ✅ | — | — | — |
| `notification-api` | ✅ | ✅ | — | — | — |
| `websocket-service` | — | ✅ | `social.interaction` | — | — |
| `interaction-service` | ✅ | — | — | — | **`post-api`** (REST, `/internal/articles/{id}`) |
| `user-consumer` | ✅ | ✅ (transitive qua `user-service`) | `social.auth` | — | — |
| `post-consumer` | ✅ | ✅ | `social.interaction` | — | — |
| `notification-consumer` | ✅ | ✅ | `social.interaction` | — | — |

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
throw). Đây là điểm coupling đồng bộ **duy nhất** trong toàn hệ thống; mọi
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
7. user-consumer
8. post-consumer
9. notification-consumer
```

(1-5 không phụ thuộc lẫn nhau, thứ tự trong nhóm không quan trọng; 7-9 tương
tự.) Tự động hoá bằng `scripts/deploy-sequential.sh` / `make deploy-services-
sequential` — xem README của Makefile (`make help`).
