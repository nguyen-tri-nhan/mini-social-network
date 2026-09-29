# Backend Overview
## Mini Social Network — High Level Diagrams

---

## 1. System Architecture

```mermaid
graph LR
    FE(["Browser\nReact SPA"])
    WSC(["Browser\nWebSocket"])

    GW["Traefik :8080\nRouting only (JWT verify ở từng service)"]

    subgraph APIS["REST APIs"]
        Auth["auth-service\n:8081"]
        UserApi["user-api\n:8082"]
        PostApi["post-api\n:8083"]
        Inter["interaction-service\n:8084"]
        NotiApi["notification-api\n:8085"]
        WsSvc["websocket-service\n:8086"]
    end

    subgraph DBS["PostgreSQL :5432 — database social"]
        AuthDB[("schema auth")]
        UserDB[("schema users")]
        PostDB[("schema post")]
        InterDB[("schema interaction")]
        NotiDB[("schema notification")]
    end

    S3["S3 / LocalStack\n:4566"]
    Redis[("Redis :6379\ncounters · cache · unread")]

    subgraph MSG["Event Bus"]
        Debezium["Debezium\nCDC outbox → Kafka"]
        Kafka["Kafka :9092\nsocial.auth · social.post\nsocial.interaction · social.user"]
    end

    subgraph CONS["Kafka Consumers"]
        UserCon["user-consumer :8182\ngroup: user-group"]
        PostCon["post-consumer :8183\ngroup: counter-group"]
        NotiCon["notification-consumer :8185\ngroup: notification-group"]
    end

    FE  -->|"HTTP /api"| GW
    WSC -->|"ws:// /ws"| GW
    GW  --> Auth & UserApi & PostApi & Inter & NotiApi & WsSvc

    Auth    --> AuthDB
    UserApi --> UserDB
    PostApi --> PostDB & S3
    Inter   --> InterDB
    NotiApi --> NotiDB & Redis

    AuthDB  -->|"WAL"| Debezium
    PostDB  -->|"WAL"| Debezium
    InterDB -->|"WAL"| Debezium
    UserDB  -->|"WAL"| Debezium
    Debezium --> Kafka

    Kafka -->|"social.auth"| UserCon
    Kafka -->|"social.interaction"| PostCon & NotiCon
    Kafka -->|"social.interaction · social.user"| WsSvc
    Kafka -->|"social.user"| Inter

    UserCon --> UserDB & Redis
    PostCon --> PostDB & Redis
    NotiCon --> NotiDB & Redis
    WsSvc   -.-> Redis
    Inter   -.->|"REST /internal/articles"| PostApi
```

> Mũi tên nét đứt Inter → PostApi: coupling đồng bộ duy nhất giữa các service,
> xem `service-dependencies.md`.

---

## 2. API Gateway — Routing & Auth

```
http://localhost:8080
        │
        ├── /api/auth/**          → auth-service          (PUBLIC — no JWT)
        │
        ├── /api/users/**         → user-api              ┐
        ├── /api/articles/**      → post-api              │ PROTECTED
        ├── /api/comments/**      → interaction-service   │ Traefik chỉ route;
        ├── /api/votes/**         → interaction-service   │ mỗi service tự verify JWT
        ├── /api/notifications/** → notification-api      │ (@RolesAllowed)
        │                                                  ┘
        └── /ws/**                → websocket-service      (PUBLIC — no auth, pub/sub channels)
```

**JWT verification flow (protected routes)** — không có ForwardAuth (bỏ theo
ADR 0005):
```
Request → Traefik (route theo path, không đụng token)
  → Service nhận Authorization: Bearer <jwt>
  → SmallRye JWT verify chữ ký RS256 bằng public key cục bộ
    (mp.jwt.verify.publickey.location) + @RolesAllowed("ROLE_USER")
  → hợp lệ: userId = jwt.subject · sai/thiếu: 401
```

**Internal service-to-service:**
```
ServiceA → http://serviceB:8080/internal/**
  Header: X-Service-Secret-Key: <shared-secret>    ← InternalAuthFilter (social-exception)
```

---

## 3. Service Responsibilities

| Module | Type | Owns | DB Schema | Port (dev) |
|---|---|---|---|---|
| **auth-service** | REST | Credentials, JWT sign/verify | `auth` | 8081 |
| **user-api** | REST | User profile read/update | `users` | 8082 |
| **user-consumer** | Kafka | Create user_profile on signup | `users` | 8182 |
| **post-api** | REST | Article CRUD, S3 presign | `post` | 8083 |
| **post-consumer** | Kafka | Counter flush Redis → DB | `post` | 8183 |
| **interaction-service** | REST | Comment, Vote | `interaction` | 8084 |
| **notification-api** | REST | Notification list, mark seen | `notification` | 8085 |
| **notification-consumer** | Kafka | Create notification from events | `notification` | 8185 |
| **websocket-service** | WS + Kafka | Push realtime events to clients | — | 8086 |

---

## 4. Request Flow — Authenticated Request

```mermaid
sequenceDiagram
    participant FE as Browser
    participant GW as Traefik :8080
    participant SVC as Any Service

    FE->>GW: GET /api/articles\nAuthorization: Bearer <jwt>
    GW->>SVC: GET /api/articles\nAuthorization: Bearer <jwt> (chuyển nguyên)
    SVC->>SVC: verify RS256 bằng public key cục bộ\n@RolesAllowed · userId = jwt.subject
    SVC-->>FE: 200 OK { data: [...] }
```

---

## 5. Outbox Pattern — Service → Kafka

Tất cả event đều đi qua outbox pattern. Services **không publish trực tiếp** vào Kafka.

```mermaid
sequenceDiagram
    participant SVC as Service
    participant DB as PostgreSQL\n(schema.outbox)
    participant DEZ as Debezium CDC
    participant K as Kafka

    SVC->>DB: BEGIN TRANSACTION\nINSERT entity\nINSERT outbox {aggregateType, payload}\nCOMMIT

    DEZ->>DB: read WAL stream
    DEZ->>K: publish to social.${aggregateType}\n(social.auth / social.post / social.interaction)
```

**aggregateType → topic mapping:**

| aggregateType | Kafka topic | Publisher |
|---|---|---|
| `auth` | `social.auth` | auth-service |
| `post` | `social.post` | post-api (via post-service) |
| `interaction` | `social.interaction` | interaction-service |
| `user` | `social.user` | user-consumer (`USER_READY`) · user-api (`USER_PROFILE_UPDATED`) |
| `chat` | `social.chat` | websocket-service (Phase 3) |

Service ghi outbox qua `OutboxRepository.emit()` (insert → flush → delete cùng
transaction) nên bảng outbox luôn gần như rỗng — muốn xem event thì xem trên
Kafka (Kafdrop), không query bảng outbox.

---

## 6. Async Event Flow — Comment → Notification + Counter + WebSocket

```mermaid
sequenceDiagram
    participant U as User (Bob)
    participant IS as interaction-service
    participant DB as interaction.outbox
    participant DEZ as Debezium
    participant K as Kafka\nsocial.interaction
    participant PC as post-consumer
    participant NC as notification-consumer
    participant WS as websocket-service
    participant R as Redis

    U->>IS: POST /api/comments
    IS->>DB: INSERT comment\nINSERT outbox (aggregateType=interaction)
    IS-->>U: 201 Created ← không chờ async

    DEZ->>DB: read WAL
    DEZ->>K: publish COMMENT_CREATED

    K-->>PC: consume (counter-group)
    PC->>R: INCR article:{id}:comment_count

    K-->>NC: consume (notification-group)
    NC->>NC: INSERT notification
    NC->>R: INCR noti_unread:{ownerId}

    K-->>WS: consume (ws-group)
    WS->>WS: push NOTIFICATION to article owner\npush COMMENT_ADDED to article subscribers
```

---

## 7. Counter Flush — Redis → PostgreSQL

```mermaid
flowchart LR
    Vote["User votes"] -->|"POST /api/votes"| IS["interaction-service"]
    IS -->|"INSERT outbox\naggregateType=interaction"| DB[("interaction.outbox")]
    DB -->|"CDC WAL"| DEZ["Debezium"]
    DEZ -->|"VOTE_CAST event"| K["social.interaction"]
    K -->|"consume"| PC["post-consumer"]
    PC -->|"INCR/DECR"| R["Redis\narticle:{id}:vote_count"]

    Job["CounterFlushJob\nevery 30s"] -->|"GET article:*:*_count"| R
    Job -->|"UPDATE article SET\nvote_count = ?, comment_count = ?"| PG[("post.article")]
```

---

## 8. WebSocket — Realtime Push

Pub/sub channel model — FE tự subscribe vào topic cần, server route theo topic name. Không cần auth ở WS level.

```
Browser  →  ws://host/ws   (public, no auth)
                │
         websocket-service
                │  FE gửi: { "type": "SUBSCRIBE", "topic": "user_{userId}_notification" }
                │  FE gửi: { "type": "SUBSCRIBE", "topic": "article_{articleId}_comment_added" }
                │
                │  Registry: topic → Set<WsConnection>
                │
                ├── Kafka consumer: social.interaction
                │     COMMENT_CREATED → push to "user_{authorId}_notification"
                │                    → push to "article_{articleId}_comment_added"
                │     VOTE_CAST      → push to "user_{targetAuthorId}_notification"
                ├── Kafka consumer: social.user
                │     USER_READY     → push to "user_{userId}_ready" (signup chờ profile)
                │
                └── Redis pub/sub (multi-instance fan-out) — CHƯA IMPLEMENT
                      (TODO Phase 2 trong WsPushService.kt; hiện chỉ 1 instance,
                       TopicRegistry là map in-memory)
```

> ⚠️ Model "public, không auth" dựa trên giả định payload không nhạy cảm. Từ
> ADR 0006, payload `NOTIFICATION` chứa tên actor, còn topic
> `user_{userId}_notification` đoán được (userId lộ trong mọi ArticleDto/CommentDto)
> → ai cũng subscribe được noti của người khác, kể cả biết ai vote bài họ. Chưa
> quyết định cách sửa — đề xuất ở `messaging-plan.md` §11 (blocker của tính năng chat).

**Topics FE subscribe:**

| Topic | Ai subscribe | Nhận gì |
|---|---|---|
| `user_{userId}_notification` | FE tự dùng userId từ JWT client-side | `NOTIFICATION` — comment/vote vào bài của mình |
| `article_{articleId}_comment_added` | FE khi mở bài đang xem | `COMMENT_ADDED` — live comments |
| `room_{roomId}_chat` (Phase 3) | FE khi vào chat room | `CHAT_MESSAGE` |

---

## 9. Image Upload Flow — S3 Presigned URL

```mermaid
sequenceDiagram
    participant FE as Browser
    participant PS as post-api
    participant S3 as S3 / LocalStack

    FE->>PS: POST /api/articles/images/presign\n?filename=photo.jpg
    PS->>S3: GeneratePresignedUrl
    S3-->>PS: presigned PUT URL (15 min TTL)
    PS-->>FE: { uploadUrl, imageUrl }

    FE->>S3: PUT photo.jpg (binary) directly
    S3-->>FE: 200 OK

    FE->>PS: POST /api/articles\n{ description, imageUrl }
    PS-->>FE: 201 ArticleDto
```

---

## 10. Database — Schema per Service

Mỗi service sở hữu dữ liệu riêng — không service nào đọc/ghi bảng của service khác.  
Local: 1 PostgreSQL instance, 1 database `social`, mỗi service 1 **schema** riêng
(`auth`, `users`, `post`, `interaction`, `notification`) — Liquibase +
Hibernate đặt `default-schema` theo từng service. Các khối dưới đặt tên `*_db`
theo ý nghĩa logic, không phải database thật.  
Production (dự kiến): mỗi service có RDS instance riêng.

```
┌─────────────────────────────────┐
│  auth_db  (auth-service)        │
│  ├── credentials                │
│  │   id · username · email      │
│  │   password_hash · user_id    │
│  └── outbox                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  user_db  (user-service)        │
│  └── user_profile               │
│      id · username · email      │
│      firstname · lastname       │
│      avatar_url · created_at    │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  post_db  (post-service)        │
│  ├── article                    │
│  │   id · description           │
│  │   image_url · author_id      │
│  │   vote_count · comment_count │
│  │   visible · created_at       │
│  └── outbox                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  interaction_db                 │
│  (interaction-service)          │
│  ├── comment                    │
│  │   id · description           │
│  │   target_id · target_type    │
│  │   author_id · visible        │
│  ├── vote                       │
│  │   id · value · user_id       │
│  │   target_id · target_type    │
│  │   UNIQUE(user_id,            │
│  │          target_id,          │
│  │          target_type)        │
│  └── outbox                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  notification_db                │
│  (notification-service)         │
│  └── notification               │
│      id · type · actor_id       │
│      owner_id · article_id      │
│      seen · created_at          │
└─────────────────────────────────┘
```

> Cross-service data (vd. tên tác giả) — **không có cross-DB query**. Comment/
> notification: interaction-service giữ bảng cache `user_ref` (consume
> `social.user`) và nhúng tên actor vào event payload (ADR 0006). Tác giả bài
> viết trong feed: FE vẫn tự gọi `GET /api/users/{id}` (chưa chuyển sang cách trên).

---

## 11. Inter-service Communication

| From | To | Protocol | Channel | When |
|---|---|---|---|---|
| auth-service | PostgreSQL outbox | SQL | — | signup → ghi outbox |
| Debezium | Kafka `social.auth` | CDC WAL | — | detect outbox INSERT |
| user-consumer | PostgreSQL | SQL | `social.auth` | USER_UPDATED (action=CREATED) → tạo user_profile |
| user-service | Redis | Direct | — | cache profile (TTL 10m) |
| post-api | PostgreSQL outbox | SQL | — | article created → ghi outbox |
| Debezium | Kafka `social.post` | CDC WAL | — | detect outbox INSERT |
| interaction-service | PostgreSQL outbox | SQL | — | comment/vote → ghi outbox |
| Debezium | Kafka `social.interaction` | CDC WAL | — | detect outbox INSERT |
| post-consumer | Redis | Direct | `social.interaction` | INCR counter |
| post-consumer | PostgreSQL | SQL | — | CounterFlushJob mỗi 30s |
| notification-consumer | PostgreSQL | SQL | `social.interaction` | INSERT notification |
| notification-consumer | Redis | Direct | — | INCR noti_unread |
| websocket-service | WS clients | Direct (in-memory `TopicRegistry`) | `social.interaction`, `social.user` | push to connected clients (Redis fan-out: chưa làm) |
| user-consumer | PostgreSQL outbox | SQL | — | tạo user_profile → ghi outbox `USER_READY` |
| user-api | PostgreSQL outbox | SQL | — | sửa profile → ghi outbox `USER_PROFILE_UPDATED` |
| Debezium | Kafka `social.user` | CDC WAL | — | detect `users.outbox` INSERT |
| interaction-service | PostgreSQL (`user_ref`) | SQL | `social.user` | upsert cache tên user (ADR 0006) |
| interaction-service → post-api | HTTP internal | `X-Service-Secret-Key` | k8s DNS | sync call `/internal/articles/{id}` (secret: biến `INTERNAL_SECRET_KEY` ở cả 2 phía) |

---

## 12. Module Structure (Gradle)

```
services/
├── social-bom             # java-platform: wraps Quarkus BOM + declares lib versions
├── social-common          # DTOs, Events (per-domain), RSQL parser (pure Kotlin)
├── social-exception       # AppException, JAX-RS mappers, InternalAuthFilter

├── auth-service-dao       # Credentials + OutboxEntry entity/repo + Liquibase
├── auth-service           # Quarkus app: REST + JWT sign

├── user-service-dao       # UserProfile entity/repo + Liquibase
├── user-service           # service logic lib
├── user-api               # Quarkus app: REST
├── user-consumer          # Quarkus app: Kafka ← social.auth

├── post-service-dao       # Article + OutboxEntry entity/repo + Liquibase
├── post-service           # service logic lib (ArticleService, S3Service, CounterService, RSQL)
├── post-api               # Quarkus app: REST
├── post-consumer          # Quarkus app: Kafka ← social.interaction + CounterFlushJob

├── interaction-service-dao  # Comment + Vote + OutboxEntry entity/repo + Liquibase
├── interaction-service      # Quarkus app: REST + outbox; consume social.user (cache user_ref)

├── notification-service-dao # Notification entity/repo + Liquibase
├── notification-service     # service logic lib
├── notification-api         # Quarkus app: REST
├── notification-consumer    # Quarkus app: Kafka ← social.interaction

└── websocket-service        # Quarkus app: WebSocket + Kafka ← social.interaction
```

---

## 13. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 + JVM 21 |
| Framework | Quarkus 3.25.x |
| ORM | Hibernate ORM + Panache Kotlin |
| DB Migration | Liquibase (per-schema changelogs) |
| Auth (external) | SmallRye JWT RS256 — mỗi service tự verify (ADR 0005, không ForwardAuth) |
| Auth (internal) | X-Service-Secret-Key shared secret |
| Messaging | Kafka via Debezium CDC (outbox pattern) |
| Event Routing | Per-domain topics: `social.auth`, `social.post`, `social.interaction`, `social.user` |
| Cache | Redis 7 (counters · profile cache TTL 10m · unread count) |
| Image Storage | AWS S3 / LocalStack (presigned URL upload) |
| API Gateway | Traefik v3 (chỉ routing theo path) |
| Build | Gradle 9 + Kotlin DSL + social-bom (version catalog) |
| Container | Quarkus JIB (no Dockerfile) |
| Local infra | Docker Compose + kind (k8s) |
| Production | AWS EKS + RDS + MSK + S3 |
| Observability | LGTM stack (Grafana · Loki · Tempo · Mimir) + OpenTelemetry |
