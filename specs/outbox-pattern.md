# Outbox Pattern & Debezium

---

## 1. Vấn đề hiện tại — Dual Write

Nhìn vào `AuthService.kt` và `InteractionService.kt`:

```kotlin
// AuthService.kt:49-62
@Transactional
fun signup(request: SignUpRequest): AuthResponse {
    repo.persist(credentials)                              // ① ghi DB
    emitter.sendAndAwait(objectMapper.writeValueAsString(event))  // ② ghi Kafka
    ...
}
```

```kotlin
// InteractionService.kt:50-61
@Transactional
fun addComment(authorId: UUID, request: CreateCommentRequest): CommentDto {
    commentRepo.persist(comment)                           // ① ghi DB
    emitter.sendAndAwait(objectMapper.writeValueAsString(event))  // ② ghi Kafka
    ...
}
```

**Đây là Dual Write — 2 hệ thống riêng biệt, không có transaction chung.**

```
Scenario A — Kafka down sau khi DB commit:
  ① persist(comment)   → DB: comment row tồn tại ✅
  ② sendAndAwait(...)  → Kafka: TIMEOUT ❌
  Kết quả: comment có trong DB nhưng không có notification, counter không tăng

Scenario B — Transaction rollback sau khi Kafka publish:
  ① persist(comment)   → ghi vào DB buffer
  ② sendAndAwait(...)  → Kafka: published ✅
  ③ @Transactional rollback vì lý do khác
  Kết quả: Kafka đã nhận event nhưng DB không có comment row
           → consumer xử lý event cho entity không tồn tại

Scenario C — Service crash giữa chừng:
  ① persist(comment)   → commit ✅
  [CRASH]
  ② sendAndAwait(...)  → không bao giờ chạy
  Kết quả: giống Scenario A
```

**Hậu quả thực tế trong project:**
- Comment tồn tại nhưng `article.comment_count` không tăng
- User signup thành công nhưng `user_profile` không được tạo → 404 khi gọi `/api/users/me`
- Vote được lưu nhưng notification không đến chủ bài

---

## 2. Outbox Pattern

**Ý tưởng:** Thay vì ghi thẳng vào Kafka, ghi vào bảng `outbox` trong **cùng DB transaction** với entity chính. Sau đó một process riêng đọc bảng outbox và publish lên Kafka.

```
Trước (Dual Write):
  Transaction ─── persist(comment)
                └── sendAndAwait(Kafka)   ← ngoài transaction!

Sau (Outbox Pattern):
  Transaction ─── persist(comment)
                └── persist(outbox_row)   ← trong transaction ✅
                                               ↓ (async)
                                         Debezium đọc outbox
                                               ↓
                                         Publish lên Kafka
```

DB transaction đảm bảo: **comment và outbox_row luôn tồn tại cùng nhau, hoặc không tồn tại cả hai**.

---

## 3. Debezium — Change Data Capture

**Debezium là gì?**

Debezium là CDC (Change Data Capture) connector của Red Hat. Nó kết nối vào PostgreSQL WAL (Write-Ahead Log) — stream log mà PostgreSQL dùng để recovery.

```
PostgreSQL
  ├── data files
  └── WAL (Write-Ahead Log)   ← ghi mọi thay đổi trước khi apply
         │
         └─── Debezium connector đọc WAL
                     │
                     └─── publish thay đổi lên Kafka
```

Mọi `INSERT/UPDATE/DELETE` trên bảng được config → Debezium capture → gửi lên Kafka. **Không cần polling, không cần code trong app.**

**Tại sao Debezium thay vì tự poll outbox?**

| Approach | Vấn đề |
|---|---|
| App poll outbox mỗi N giây | Latency (N giây delay), tốn query DB |
| App dùng trigger PostgreSQL | Trigger chạy trong transaction → vẫn có race condition |
| **Debezium đọc WAL** | **At-least-once, near-real-time, không tốn query, idempotent** |

WAL là source of truth — Debezium chỉ đọc những gì đã committed. Nếu Debezium crash, nó resume từ WAL offset đã lưu — không miss event.

---

## 4. Debezium Outbox Event Router

Debezium có SMT (Single Message Transform) tên **Outbox Event Router** — thiết kế đặc biệt cho pattern này.

**Schema bảng outbox chuẩn:**

```sql
CREATE TABLE outbox (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,   -- "comment" | "vote" | "article" | "user"
    aggregate_id   UUID        NOT NULL,   -- ID của entity liên quan
    event_type     VARCHAR(50) NOT NULL,   -- "COMMENT_CREATED" | "VOTE_CAST" | ...
    payload        JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Debezium đọc bảng này và:
- Route mỗi row đến Kafka topic khác nhau dựa vào `aggregate_type`
- **Tự DELETE row** sau khi publish (để bảng không phình to)
- Dùng `id` làm Kafka message key → deduplication

---

## 5. Áp dụng vào project này

### 5.1 Schema change — mỗi service thêm bảng outbox

Vì project dùng **1 PostgreSQL, 5 schema**, mỗi service có outbox riêng:

```
social (database)
├── schema: auth         → auth.outbox
├── schema: interaction  → interaction.outbox   ← quan trọng nhất
└── schema: post         → post.outbox
```

*(user và notification không publish event nên không cần outbox)*

**Liquibase changeset — ví dụ `interaction-service-dao`:**

```xml
<!-- 0004-create-outbox.xml -->
<changeSet id="0004" author="nhan">
    <createTable tableName="outbox" schemaName="interaction">
        <column name="id" type="uuid" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true"/>
        </column>
        <column name="aggregate_type" type="varchar(50)">
            <constraints nullable="false"/>
        </column>
        <column name="aggregate_id" type="uuid">
            <constraints nullable="false"/>
        </column>
        <column name="event_type" type="varchar(50)">
            <constraints nullable="false"/>
        </column>
        <column name="payload" type="jsonb">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="timestamptz" defaultValueComputed="NOW()">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

### 5.2 Entity — OutboxEntry

```kotlin
// interaction-service-dao/entity/OutboxEntry.kt
@Entity
@Table(name = "outbox")
class OutboxEntry : PanacheEntityBase() {
    @Id @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()

    @Column(name = "aggregate_type", nullable = false, length = 50)
    lateinit var aggregateType: String   // "comment" | "vote"

    @Column(name = "aggregate_id", columnDefinition = "uuid", nullable = false)
    lateinit var aggregateId: UUID

    @Column(name = "event_type", nullable = false, length = 50)
    lateinit var eventType: String       // "COMMENT_CREATED" | "VOTE_CAST"

    @Column(columnDefinition = "jsonb", nullable = false)
    lateinit var payload: String         // JSON string

    @Column(name = "created_at", updatable = false)
    var createdAt: Instant = Instant.now()
}
```

### 5.3 Service — thay emitter bằng outboxRepo.persist()

**Trước:**
```kotlin
// InteractionService.kt
@Transactional
fun addComment(authorId: UUID, request: CreateCommentRequest): CommentDto {
    commentRepo.persist(comment)
    emitter.sendAndAwait(objectMapper.writeValueAsString(event))  // ← dual write
    return comment.toDto()
}
```

**Sau:**
```kotlin
// InteractionService.kt
@Transactional
fun addComment(authorId: UUID, request: CreateCommentRequest): CommentDto {
    commentRepo.persist(comment)

    outboxRepo.persist(OutboxEntry().apply {
        this.aggregateType = "comment"
        this.aggregateId   = comment.id
        this.eventType     = "COMMENT_CREATED"
        this.payload       = objectMapper.writeValueAsString(mapOf(
            "commentId"       to comment.id.toString(),
            "targetId"        to comment.targetId.toString(),
            "targetType"      to comment.targetType,
            "actorId"         to authorId.toString(),
        ))
    })
    // Không còn emitter — Debezium sẽ đọc outbox và publish lên Kafka
    return comment.toDto()
}
```

**Cả hai persist trong cùng `@Transactional` → atomic.**

### 5.4 Xoá Kafka emitter khỏi services

Sau khi áp dụng Outbox Pattern, các service **không còn inject Kafka emitter**:

| Service | Trước | Sau |
|---|---|---|
| `interaction-service` | `MutinyEmitter` + Kafka dep | Chỉ cần `OutboxRepository` |
| `post-service` | `MutinyEmitter` + Kafka dep | Chỉ cần `OutboxRepository` |
| `auth-service` | `MutinyEmitter` + Kafka dep | Chỉ cần `OutboxRepository` |
| `post-consumer` | Kafka consumer | Không đổi |
| `notification-consumer` | Kafka consumer | Không đổi |

---

## 6. Debezium Setup

### 6.1 Bật logical replication trên PostgreSQL

Debezium đọc WAL ở level `logical` — cần config PostgreSQL:

```sql
-- Phải set trước khi Debezium connect
ALTER SYSTEM SET wal_level = logical;
ALTER SYSTEM SET max_wal_senders = 4;
ALTER SYSTEM SET max_replication_slots = 4;
```

Với Docker/k8s, truyền qua command args:

```yaml
# k8s/infra/postgres.yaml — thêm args
containers:
  - name: postgres
    image: postgres:15-alpine
    args:
      - -c
      - wal_level=logical
      - -c
      - max_wal_senders=4
      - -c
      - max_replication_slots=4
```

Tạo user REPLICATION:
```sql
CREATE ROLE debezium_user REPLICATION LOGIN PASSWORD 'debezium';
GRANT SELECT ON ALL TABLES IN SCHEMA interaction TO debezium_user;
GRANT SELECT ON ALL TABLES IN SCHEMA post TO debezium_user;
GRANT SELECT ON ALL TABLES IN SCHEMA auth TO debezium_user;
```

### 6.2 Kafka Connect + Debezium connector

Debezium chạy như Kafka Connect plugin:

```yaml
# k8s/infra/debezium.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-connect
  namespace: social
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: kafka-connect
          image: debezium/connect:2.7
          env:
            - name: BOOTSTRAP_SERVERS
              value: kafka:9092
            - name: GROUP_ID
              value: debezium-connect
            - name: CONFIG_STORAGE_TOPIC
              value: debezium.configs
            - name: OFFSET_STORAGE_TOPIC
              value: debezium.offsets
            - name: STATUS_STORAGE_TOPIC
              value: debezium.status
          ports:
            - containerPort: 8083   # Kafka Connect REST API
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-connect
  namespace: social
spec:
  selector: { app: kafka-connect }
  ports:
    - port: 8083
```

### 6.3 Đăng ký PostgreSQL Connector

Sau khi Kafka Connect up, POST connector config:

```bash
curl -X POST http://kafka-connect:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "social-outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "debezium_user",
      "database.password": "debezium",
      "database.dbname": "social",
      "database.server.name": "social",
      "plugin.name": "pgoutput",

      "table.include.list": "interaction.outbox,post.outbox,auth.outbox",

      "transforms": "outbox",
      "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
      "transforms.outbox.table.field.event.id": "id",
      "transforms.outbox.table.field.event.key": "aggregate_id",
      "transforms.outbox.table.field.event.type": "event_type",
      "transforms.outbox.table.field.event.payload": "payload",
      "transforms.outbox.route.by.field": "aggregate_type",
      "transforms.outbox.route.topic.replacement": "social.${routedByValue}.events",
      "transforms.outbox.table.tombstone.on.empty.payload": "false",

      "tombstones.on.delete": "false"
    }
  }'
```

**Outbox Event Router tự động route:**
| `aggregate_type` | Kafka topic |
|---|---|
| `comment` | `social.comment.events` |
| `vote` | `social.vote.events` |
| `article` | `social.article.events` |
| `user` | `social.user.events` |

Consumers cập nhật topic name trong `application.properties`:
```properties
# post-consumer: lắng nghe comment + vote events riêng
mp.messaging.incoming.comment-events-in.topic=social.comment.events
mp.messaging.incoming.vote-events-in.topic=social.vote.events
```

---

## 7. Flow sau khi áp dụng

```
POST /api/comments
      │
      ▼
InteractionService.addComment()
  @Transactional {
    INSERT INTO interaction.comment (...)    ①
    INSERT INTO interaction.outbox (...)     ②  ← cùng transaction
  } COMMIT                                  ③
      │
      │ 201 trả về user ngay             ④ ← không chờ Kafka
      │
      ▼ (async, vài ms sau)
PostgreSQL WAL
      │
      ▼
Debezium connector đọc WAL
      │ thấy INSERT vào interaction.outbox
      ▼
Kafka topic: social.comment.events
      │
      ├──► post-consumer: INCR article:comment_count
      └──► notification-consumer: INSERT notification
```

**Guarantees:**
- Nếu app crash sau COMMIT → Debezium sẽ đọc WAL và publish khi restart
- Nếu Kafka down → Debezium retry (lưu offset trong Kafka Connect)
- Nếu Debezium crash → resume từ WAL offset đã lưu
- **At-least-once delivery** → consumer phải idempotent

---

## 8. Idempotent Consumer

Vì at-least-once, consumer có thể nhận event 2 lần. Cần handle:

```kotlin
// NotificationEventConsumer.kt
@Incoming("social.comment.events")
@Transactional
fun consume(message: String) {
    val event = objectMapper.readValue(message, OutboxEvent::class.java)

    // Idempotency check: dùng event ID (UUID từ outbox.id)
    if (notificationRepo.existsByEventId(event.id)) {
        log.info("Duplicate event ${event.id}, skipping")
        return
    }

    notificationRepo.persist(Notification().apply {
        this.eventId = event.id   // ← lưu event ID để dedup
        ...
    })
}
```

---

## 9. Trade-offs

| | Dual Write (hiện tại) | Outbox + Debezium |
|---|---|---|
| **Consistency** | ❌ có thể mất event | ✅ atomic |
| **Latency** | Thấp (publish inline) | Thấp (Debezium near-realtime ~ms) |
| **Complexity** | Thấp | Cao hơn (thêm Debezium, WAL config) |
| **Infra** | Kafka | Kafka + Kafka Connect + Debezium |
| **Debug** | Dễ (log trong service) | Khó hơn (trace qua WAL → Debezium → Kafka) |
| **Schema coupling** | Không | Cần maintain outbox table |
| **PostgreSQL config** | Không | Cần `wal_level=logical` |

**Nên apply cho service nào trước?**

`interaction-service` là ưu tiên cao nhất vì:
1. Comment → counter (nếu miss → sai số liệu hiển thị)
2. Comment → notification (nếu miss → user không nhận thông báo)
3. Đây là nghiệp vụ quan trọng nhất, tần suất cao nhất

`auth-service` là ưu tiên thứ hai vì nếu miss `USER_CREATED` event → user không có profile → 404 ngay sau signup.
