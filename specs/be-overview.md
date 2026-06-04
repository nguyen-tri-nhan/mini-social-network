# Backend Overview
## Mini Social Network — High Level Diagrams

---

## 1. System Architecture

```mermaid
graph TD
    FE["Browser / React SPA"]

    subgraph "Local: Docker Compose / k8s (kind)"
        GW["Traefik :8080\nAPI Gateway + Routing"]

        subgraph "Services"
            Auth["auth-service\n:8081 (dev)"]
            User["user-service\n:8082 (dev)"]
            Post["post-service\n:8083 (dev)"]
            Inter["interaction-service\n:8084 (dev)"]
            Notif["notification-service\n:8085 (dev)"]
        end

        subgraph "Data"
            PG[("PostgreSQL :5432\nDB: social\n─────────────\nschema: auth\nschema: users\nschema: post\nschema: interaction\nschema: notification")]
            Redis[("Redis :6379\ncounters · cache · unread")]
            Kafka["Kafka :9092\ntopic: social.events"]
            S3["LocalStack S3 :4566\nbucket: social-images"]
        end
    end

    FE -->|"HTTP :8080"| GW
    GW --> Auth & User & Post & Inter & Notif

    Auth    --> PG
    User    --> PG
    User    --> Redis
    Post    --> PG
    Post    --> Redis
    Post    --> S3
    Inter   --> PG
    Notif   --> PG
    Notif   --> Redis

    Inter -->|"comment.created\nvote.cast"| Kafka
    Post  -->|"article.created"| Kafka
    Kafka -->|"consume"| Notif
    Kafka -->|"consume counters"| Post
```

---

## 2. API Gateway — Routing Rules

```
http://localhost:8080
        │
        ├── /api/auth/**          → auth-service
        ├── /api/users/**         → user-service
        ├── /api/articles/**      → post-service
        ├── /api/comments/**      → interaction-service
        ├── /api/votes/**         → interaction-service
        └── /api/notifications/** → notification-service
```

> Không còn routing conflict — mỗi service sở hữu prefix độc lập.

---

## 3. Service Responsibilities

| Service | Owns | DB Schema | Port (dev) |
|---|---|---|---|
| **auth-service** | Credentials, JWT issue | `auth` | 8081 |
| **user-service** | User profile, avatar | `users` | 8082 |
| **post-service** | Article CRUD, S3 image, counter flush | `post` | 8083 |
| **interaction-service** | Comment, Vote (targetId + targetType) | `interaction` | 8084 |
| **notification-service** | Notification events, unread count | `notification` | 8085 |

---

## 4. Request Flow — Authenticated Request

```mermaid
sequenceDiagram
    participant FE as Browser
    participant GW as Traefik :8080
    participant SVC as Any Service :8080

    FE->>GW: GET /api/articles\nAuthorization: Bearer <jwt>
    GW->>GW: Route by PathPrefix
    GW->>SVC: GET /api/articles\nAuthorization: Bearer <jwt>
    SVC->>SVC: @RolesAllowed validates JWT\n(SmallRye JWT)
    SVC-->>FE: 200 OK { data: [...] }
```

---

## 5. Async Event Flow — Comment → Notification

```mermaid
sequenceDiagram
    participant U as User (Bob)
    participant IS as interaction-service
    participant K as Kafka\nsocial.events
    participant PS as post-service\n(consumer)
    participant NS as notification-service\n(consumer)
    participant R as Redis

    U->>IS: POST /api/comments\n{targetId, targetType: ARTICLE}
    IS->>IS: INSERT comment
    IS->>K: publish comment.created
    IS-->>U: 201 Created  ← không chờ async

    K-->>PS: consume comment.created
    PS->>R: INCR article:{id}:comment_count

    K-->>NS: consume comment.created
    NS->>NS: INSERT notification
    NS->>R: INCR noti_unread:{ownerId}
```

---

## 6. Counter Flush — Redis → PostgreSQL

```mermaid
flowchart LR
    Vote["User votes"] -->|"POST /api/votes"| IS["interaction-service"]
    IS -->|"publish vote.cast\n(delta)"| K["Kafka"]
    K -->|"consume"| PC["post-consumer"]
    PC -->|"INCR/DECR"| R["Redis\narticle:{id}:vote_count"]

    Job["CounterFlushJob\nevery 30s"] -->|"GET all article:*:*_count"| R
    Job -->|"UPDATE article SET\nvote_count = ?, comment_count = ?"| DB[("post.article")]
```

---

## 7. Image Upload Flow — S3 Presigned URL

```mermaid
sequenceDiagram
    participant FE as Browser
    participant PS as post-service
    participant S3 as S3 / LocalStack

    FE->>PS: POST /api/articles/images/presign\n?filename=photo.jpg
    PS->>S3: GeneratePresignedUrl
    S3-->>PS: presigned PUT URL (15 min TTL)
    PS-->>FE: { uploadUrl, imageUrl }

    FE->>S3: PUT photo.jpg (binary)\ndirectly to S3
    S3-->>FE: 200 OK

    FE->>PS: POST /api/articles\n{ description, imageUrl }
    PS-->>FE: 201 ArticleDto
```

---

## 8. RSQL Filter Flow — GET /api/articles

```mermaid
flowchart LR
    FE["GET /api/articles\n?filter=authorId==uuid\n&sort=voteCount,desc"]
    AR["ArticleResource\n@BeanParam ArticleListParams\n.toQuery()"]
    AS["ArticleService\nparseArticleFilter()\nparseArticleSort()"]
    SC["social-common\nGenericRsqlVisitor\n→ RsqlQuerySpec"]
    REPO["ArticleRepository\nfind(hql, sort, params)"]
    DB[("post.article")]

    FE --> AR --> AS --> SC --> AS --> REPO --> DB
```

**Whitelisted filter fields:** `authorId`, `createdAt`
**Whitelisted sort fields:** `createdAt` · `voteCount` · `commentCount`

---

## 9. Database — Schema per Service

```
PostgreSQL  db: social
│
├── schema: auth
│   └── credentials (id, username, email, password_hash, user_id)
│
├── schema: users
│   └── user_profile (id, username, email, firstname, lastname, avatar_url)
│
├── schema: post
│   └── article (id, description, image_url, author_id, visible,
│                vote_count, comment_count, created_at, updated_at)
│
├── schema: interaction
│   ├── comment (id, description, target_id, target_type, author_id, visible)
│   └── vote    (id, value, user_id, target_id, target_type)
│                UNIQUE(user_id, target_id, target_type)
│
└── schema: notification
    └── notification (id, type, actor_id, owner_id, article_id, seen)
```

---

## 10. Inter-service Communication

| From | To | Protocol | When |
|---|---|---|---|
| auth-service | user-service | REST (sync) | signup → tạo user profile |
| post-service | Redis | Direct | read/write counter |
| post-service | S3 | AWS SDK | presign + store image |
| interaction-service | Kafka | Async publish | comment.created, vote.cast |
| post-service | Kafka | Async publish | article.created |
| post-service | Kafka | Async consume | update counter via Redis |
| notification-service | Kafka | Async consume | tạo notification |
| user-service | Redis | Direct | cache profile (TTL 10m) |
| notification-service | Redis | Direct | unread count counter |

---

## 11. Module Structure (Gradle)

```
services/
├── social-common          # DTOs, Events, RSQL parser (pure Kotlin)
├── social-exception       # AppException + JAX-RS mappers

├── auth-service-dao       # Credentials entity + repo + Liquibase
├── auth-service           # Quarkus app: REST + JWT sign

├── user-service-dao       # UserProfile entity + repo + Liquibase
├── user-service           # service logic lib
├── user-api               # Quarkus app: REST
├── user-consumer          # Quarkus app: Kafka consumer

├── post-service-dao       # Article entity + repo + Liquibase
├── post-service           # service logic lib (ArticleService, S3, Counter, RSQL)
├── post-api               # Quarkus app: REST
├── post-consumer          # Quarkus app: Kafka consumer + CounterFlushJob

├── interaction-service-dao  # Comment + Vote entity + repo + Liquibase
├── interaction-service      # Quarkus app: REST + Kafka publish

├── notification-service-dao # Notification entity + repo + Liquibase
├── notification-service     # service logic lib
├── notification-api         # Quarkus app: REST
└── notification-consumer    # Quarkus app: Kafka consumer
```

---

## 12. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x + JVM 21 |
| Framework | Quarkus 3.x |
| ORM | Hibernate ORM + Panache Kotlin |
| DB Migration | Liquibase |
| Auth | SmallRye JWT (RS256) |
| Messaging | Kafka (SmallRye Reactive Messaging) |
| Cache | Redis 7 |
| Image Storage | AWS S3 / LocalStack |
| API Gateway | Traefik v3 |
| Build | Gradle (Kotlin DSL) |
| Container | Quarkus JIB (no Dockerfile) |
| Local infra | Docker Compose |
| Local k8s | kind |
| Production | AWS EKS + RDS + MSK + S3 |
