# Microservices Migration Plan
## Mini Social Network → Quarkus + Kotlin

---

## Current Architecture (Monolith)

```mermaid
graph TD
    Browser["Browser (React SPA)"]
    Nginx["Nginx Reverse Proxy"]
    Monolith["Spring Boot Monolith\n(Java 8)"]
    DB["PostgreSQL\n(single schema)"]
    Imgur["Imgur API"]

    Browser --> Nginx
    Nginx --> Monolith
    Monolith --> DB
    Browser --> Imgur
```

---

## Option 1 — Bounded Context Split (Recommended)

### Service Map

```mermaid
graph TD
    Browser["Browser (React SPA)"]
    GW["API Gateway\n(Kong / Nginx)"]

    AuthSvc["auth-service\nQuarkus + Kotlin\n/api/auth/**"]
    UserSvc["user-service\nQuarkus + Kotlin\n/api/users/**"]
    PostSvc["post-service\nQuarkus + Kotlin\n/api/articles/**"]
    InterSvc["interaction-service\nQuarkus + Kotlin\n/api/votes, /api/comments"]
    NotiSvc["notification-service\nQuarkus + Kotlin\n/api/notifications/**"]

    AuthDB["auth_db\nPostgreSQL"]
    UserDB["user_db\nPostgreSQL"]
    PostDB["post_db\nPostgreSQL"]
    InterDB["interaction_db\nPostgreSQL"]
    NotiDB["notification_db\nPostgreSQL"]

    Broker["Message Broker\n(Kafka)"]
    Imgur["Imgur API"]

    Browser --> GW
    GW --> AuthSvc
    GW --> UserSvc
    GW --> PostSvc
    GW --> InterSvc
    GW --> NotiSvc

    AuthSvc --> AuthDB
    UserSvc --> UserDB
    PostSvc --> PostDB
    PostSvc --> Imgur
    InterSvc --> InterDB
    NotiSvc --> NotiDB

    InterSvc -->|"comment.created\nvote.cast"| Broker
    PostSvc -->|"article.created"| Broker
    Broker --> NotiSvc
```

### Service Responsibilities

| Service | Owns | Exposes |
|---|---|---|
| **auth-service** | JWT issue/validate, credentials | `POST /auth/signup`, `POST /auth/signin` |
| **user-service** | User profile, avatar, roles | `GET /users/me`, `GET /users/{id}` |
| **post-service** | Article CRUD, image via Imgur | `GET/POST /articles`, `GET /articles/{id}` |
| **interaction-service** | Comments + Votes | `POST /articles/{id}/comments`, `POST /votes/{articleId}` |
| **notification-service** | Notification events, read/unread | `GET /notifications`, `PATCH /notifications/{id}/seen` |

### Inter-service Communication

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant IS as interaction-service
    participant NS as notification-service
    participant Kafka as Kafka

    C->>GW: POST /articles/{id}/comments
    GW->>IS: forward + JWT
    IS->>IS: save comment
    IS->>Kafka: publish comment.created {articleId, authorId, ownerId}
    IS-->>GW: 201 Created
    GW-->>C: 201 Created
    Kafka-->>NS: consume comment.created
    NS->>NS: create Notification record
```

---

## Option 2 — Fine-grained (6 Services)

### Service Map

```mermaid
graph TD
    Browser["Browser (React SPA)"]
    GW["API Gateway"]

    AuthSvc["auth-service"]
    UserSvc["user-service"]
    ArticleSvc["article-service"]
    CommentSvc["comment-service"]
    VoteSvc["vote-service"]
    NotiSvc["notification-service"]

    AuthDB[("auth_db")]
    UserDB[("user_db")]
    ArticleDB[("article_db")]
    CommentDB[("comment_db")]
    VoteDB[("vote_db")]
    NotiDB[("notification_db")]

    Broker["Kafka"]

    Browser --> GW
    GW --> AuthSvc & UserSvc & ArticleSvc & CommentSvc & VoteSvc & NotiSvc

    AuthSvc --- AuthDB
    UserSvc --- UserDB
    ArticleSvc --- ArticleDB
    CommentSvc --- CommentDB
    VoteSvc --- VoteDB
    NotiSvc --- NotiDB

    CommentSvc -->|comment.created| Broker
    VoteSvc -->|vote.cast| Broker
    Broker --> NotiSvc
```

### Trade-offs

| | Option 1 | Option 2 |
|---|---|---|
| Number of services | 5 | 6 |
| Complexity | Medium | High |
| Independent scaling | Good | Best |
| Operational overhead | Medium | High |
| Recommended for | Small–Medium team | Large team / high traffic |

---

## Option 3 — Pragmatic (3 Services)

### Service Map

```mermaid
graph TD
    Browser["Browser (React SPA)"]
    GW["API Gateway"]

    AuthSvc["auth-service\nsignup · signin · JWT"]
    CoreSvc["core-service\narticles · comments · votes"]
    UserSvc["user-service\nprofile · avatar"]
    NotiSvc["notification-service\nasync events"]

    CoreDB[("core_db")]
    AuthDB[("auth_db")]
    UserDB[("user_db")]
    NotiDB[("notification_db")]

    Broker["Kafka"]

    Browser --> GW
    GW --> AuthSvc & CoreSvc & UserSvc & NotiSvc
    AuthSvc --- AuthDB
    CoreSvc --- CoreDB
    UserSvc --- UserDB
    NotiSvc --- NotiDB
    CoreSvc -->|events| Broker
    Broker --> NotiSvc
```

---

## Recommended: Option 1 — Migration Roadmap

### Phase Overview

```mermaid
gantt
    title Migration Phases
    dateFormat  YYYY-MM-DD
    section Phase 1 — Foundation
    Setup Quarkus projects         :p1a, 2026-06-03, 7d
    API Gateway config             :p1b, after p1a, 3d
    Kafka setup                    :p1c, after p1a, 3d

    section Phase 2 — Auth & User
    auth-service                   :p2a, after p1b, 7d
    user-service                   :p2b, after p2a, 5d

    section Phase 3 — Core Content
    post-service                   :p3a, after p2b, 7d
    interaction-service            :p3b, after p3a, 7d

    section Phase 4 — Async
    notification-service           :p4a, after p3b, 5d
    Kafka event wiring             :p4b, after p4a, 3d

    section Phase 5 — Frontend
    Update React API clients       :p5a, after p4b, 5d
    E2E testing                    :p5b, after p5a, 5d
```

### Phase 1 — Foundation

```mermaid
flowchart LR
    A["Init Quarkus\nprojects per service\n(Kotlin + Gradle)"] --> B["Configure\nAPI Gateway\n(route rules)"]
    B --> C["Deploy\nKafka\n(docker-compose)"]
    C --> D["Shared\nJWT validation\nlib (common module)"]
```

**Deliverables:**
- Mono-repo structure với 5 Quarkus subproject
- Docker Compose cập nhật (gateway + kafka + per-service DB)
- Common Kotlin module: `JwtPrincipal`, `ApiResponse`, base exceptions

### Phase 2 — Auth & User Services

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant AS as auth-service
    participant US as user-service

    C->>GW: POST /auth/signup {username, email, password}
    GW->>AS: forward
    AS->>AS: hash password, save credentials
    AS->>US: gRPC/REST createUserProfile {userId, name, email}
    AS-->>C: 200 OK

    C->>GW: POST /auth/signin
    GW->>AS: forward
    AS-->>C: {accessToken, refreshToken}

    C->>GW: GET /users/me (Bearer token)
    GW->>GW: validate JWT (auth-service public key)
    GW->>US: forward with userId header
    US-->>C: UserProfile
```

**Stack per service:**
```
quarkus-resteasy-reactive
quarkus-hibernate-orm-panache-kotlin
quarkus-smallrye-jwt          ← JWT generate/validate
quarkus-flyway                ← DB migration
quarkus-kotlin
```

### Phase 3 — Post & Interaction Services

```mermaid
flowchart TD
    Client --> GW["API Gateway"]
    GW -->|"POST /articles"| PS["post-service"]
    GW -->|"GET /articles"| PS
    GW -->|"POST /articles/{id}/comments"| IS["interaction-service"]
    GW -->|"POST /votes/{articleId}"| IS

    PS -->|"HTTP GET /users/{id}"| US["user-service\n(author info)"]
    IS -->|"HTTP GET /users/{id}"| US
    IS -->|"HTTP GET /articles/{id}"| PS

    PS --- PostDB[("post_db")]
    IS --- InterDB[("interaction_db")]
```

**Data ownership note:**
- `post-service` owns `article` table — interaction-service never queries it directly, calls REST.
- `interaction-service` stores `article_id` as a reference (no FK across service boundaries).

### Phase 4 — Notification Service (Async)

```mermaid
flowchart LR
    IS["interaction-service"] -->|"Kafka\ncomment.created\nvote.cast"| K["Kafka\ntopic: social.events"]
    PS["post-service"] -->|"Kafka\narticle.created"| K
    K -->|consume| NS["notification-service"]
    NS --> NotiDB[("notification_db")]
    NS -->|"SSE / WebSocket\n(future)"| Client
```

**Kafka event schema (JSON):**
```json
{
  "eventType": "comment.created",
  "actorId": 42,
  "targetUserId": 7,
  "articleId": 123,
  "occurredAt": "2026-06-03T10:00:00Z"
}
```

### Phase 5 — Frontend Migration

```mermaid
flowchart TD
    Old["Current: all calls to\nlocalhost:8080/api/**"]
    New["New: all calls to\ngateway:80/api/**"]

    Old --> S1["Update base URL\nin http.jsx"]
    S1 --> S2["No auth header changes\n(JWT same format)"]
    S2 --> S3["Add notification polling\nor SSE client"]
    S3 --> New
```

---

## Target Directory Structure

```
mini-social-network/
├── services/
│   ├── auth-service/          # Quarkus + Kotlin
│   ├── user-service/          # Quarkus + Kotlin
│   ├── post-service/          # Quarkus + Kotlin
│   ├── interaction-service/   # Quarkus + Kotlin
│   └── notification-service/  # Quarkus + Kotlin
├── common/
│   └── social-common/         # Shared Kotlin lib (DTOs, JWT utils)
├── frontend/                  # React (unchanged)
├── gateway/
│   └── kong.yml               # API Gateway config
├── infra/
│   └── docker-compose.yml     # Full stack
└── specs/
    ├── summary.md
    └── migration-plan.md
```

---

## Risk & Mitigation

| Risk | Impact | Mitigation |
|---|---|---|
| Data consistency across services | High | Saga pattern for multi-service writes; avoid distributed transactions |
| Latency increase (network hops) | Medium | gRPC for internal sync calls; cache user profiles in post-service |
| JWT validation in each service | Medium | Shared `social-common` lib with Quarkus SmallRye JWT |
| Kafka single point of failure | Medium | Kafka cluster (3 brokers) in production |
| Frontend breakage during migration | Low | Keep old monolith running; migrate behind feature flag on gateway |
