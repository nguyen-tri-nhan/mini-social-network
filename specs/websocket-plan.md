# WebSocket Service — Implementation Plan

## 1. Tại sao tách thành service riêng

`notification-api` chỉ làm REST (list, mark seen). WebSocket là protocol khác, stateful connection, cần scale độc lập. Các tính năng tương lai (chat, real-time comments) sẽ overload notification nếu nhét vào.

| Concern | REST services | websocket-service |
|---|---|---|
| Protocol | HTTP stateless | TCP stateful (WS) |
| Scaling | Horizontal, stateless | Cần sticky session / Redis fan-out |
| Resource | CPU/memory per request | Memory per **connection** (idle) |
| Scope hiện tại | CRUD | Push gateway |
| Scope tương lai | — | Chat, live comments, presence |

---

## 2. Kiến trúc tổng thể

```
[Browser]
   │
   │  ws://host/ws?token=<jwt>
   ▼
[Traefik]  ←── PathPrefix(`/ws`)
   │  verify JWT signature
   │  extract sub → inject X-User-Id: "uuid"
   ▼
[websocket-service :8080]
   │  đọc X-User-Id header (KHÔNG verify JWT — Traefik đã làm)
   │
   ├── Connection map:  userId → Set<WebSocketConnection>
   │
   ├── Kafka consumer (social-events)
   │     COMMENT_CREATED  → push NOTIFICATION + COMMENT_ADDED
   │     VOTE_CAST        → push NOTIFICATION
   │     ARTICLE_CREATED  → push to followers (future)
   │     CHAT_MESSAGE     → push to room members (Phase 3)
   │
   └── Redis pub/sub  ←── fan-out across instances
         channel: ws:push:{userId}

┌─────────────────────────────────────────────────────┐
│ Multi-instance scaling                               │
│                                                      │
│  WS Instance A       WS Instance B                  │
│  user1 connected     user2 connected                 │
│      │                   │                          │
│      └──── Redis pub/sub ──────┘                    │
│            channel: ws:push:*                        │
│                                                      │
│  Kafka event arrives at Instance A                   │
│  → publish to Redis ws:push:{userId}                 │
│  → Instance B receives → sends to user2              │
└─────────────────────────────────────────────────────┘
```

---

## 3. Auth Architecture

### 3.1 External (Browser → WebSocket)

`/ws` là **public route** tại Traefik — không qua `jwt-verify` middleware. websocket-service tự verify JWT khi nhận connection:

```
Browser  →  ws://host/ws?token=<jwt>
                │
             Traefik  (chỉ route, không verify)
                │
                ▼
        websocket-service
                │  verify JWT signature (public key)
                │  extract sub → userId
                │  register connection
                │  nếu invalid → close 4401
```

Lý do để service tự verify: WS connection là long-lived và stateful — service cần biết userId để manage connection, không chỉ để check auth.

### 3.2 Internal (Service → Service)

Services gọi nhau qua k8s internal DNS, không qua Traefik → không có JWT. Dùng shared secret:

```
ServiceA  →  http://user-service:8080/internal/users/{id}
              Header: X-Service-Secret-Id: post-service
              Header: X-Service-Secret-Key: <shared-secret>
                │
        InternalAuthFilter (social-exception)
                │  so sánh key với app.internal.secret-key
                ▼
        handler chạy bình thường
```

Secret được inject qua env var `INTERNAL_SECRET_KEY`, lưu trong k8s Secret.

---

## 4. Tech Stack

| Component | Choice | Lý do |
|---|---|---|
| WS framework | `quarkus-websockets-next` | Reactive, built-in với Quarkus 3.x, tích hợp Mutiny |
| Kafka consumer | SmallRye Reactive Messaging (đã có) | Tái dùng pattern từ notification-consumer |
| Redis fan-out | `quarkus-redis-client` (đã có) | Đã cài trong infra, pub/sub API đơn giản |
| External auth | `X-User-Id` từ Traefik | Traefik verify JWT một lần, service không tự verify |
| Internal auth | `X-Service-Secret-Key` header | Đơn giản, không cần mTLS ở scale này |

---

## 5. Gradle Module

Chỉ cần **1 module** — không có DB (Phase 1-2), không cần DAO:

```
services/
└── websocket-service/       ← Quarkus app: WS endpoint + Kafka consumer
    ├── build.gradle.kts
    └── src/main/kotlin/com/nhan/social/ws/
        ├── WsEndpoint.kt        @WebSocket(path = "/ws")
        ├── ConnectionRegistry.kt  userId → Set<WebSocketConnection>
        ├── WsEventConsumer.kt   @Incoming("social-events-in")
        ├── WsPushService.kt     route event → connection / Redis
        └── WsMessage.kt         sealed class cho message types
```

**`settings.gradle.kts`** — thêm vào cuối:
```kotlin
// WebSocket (realtime push gateway)
"websocket-service",
```

**`websocket-service/build.gradle.kts`:**
```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform(project(":social-bom")))
    implementation(project(":social-common"))
    implementation(project(":social-exception"))

    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-websockets-next")   // ← WS
    implementation("io.quarkus:quarkus-messaging-kafka")    // ← Kafka consumer
    implementation("io.quarkus:quarkus-redis-client")       // ← fan-out
    implementation("io.quarkus:quarkus-smallrye-jwt")       // ← tự verify JWT (?token= param)
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
```

---

## 6. WebSocket Protocol

### 6.1 Connection

```
Browser  →  ws://host/ws?token=<jwt>   (prod: qua Traefik)
            ws://localhost:5173/ws?token=<jwt>  (dev: qua Vite proxy)
```

**Prod:** Traefik verify JWT, inject `X-User-Id`, forward đến websocket-service. Service đọc header, register connection.

**Dev (không qua Traefik):** websocket-service decode `?token=` query param — chỉ lấy `sub` claim, **không verify signature** (`%dev` profile only). Nếu header không có và token không decode được → close 4401.

### 5.2 Server → Client messages

```json
// Notification mới (COMMENT, VOTE)
{
  "type": "NOTIFICATION",
  "payload": {
    "id": "uuid",
    "notificationType": "COMMENT",
    "actorId": "uuid",
    "articleId": "uuid",
    "seen": false,
    "createdAt": "2026-06-05T10:00:00Z"
  }
}

// Comment mới trên article đang xem (Phase 2)
{
  "type": "COMMENT_ADDED",
  "payload": {
    "articleId": "uuid",
    "comment": {
      "id": "uuid",
      "description": "Nice post!",
      "authorId": "uuid",
      "createdAt": "2026-06-05T10:00:00Z"
    }
  }
}

// Tin nhắn chat (Phase 3)
{
  "type": "CHAT_MESSAGE",
  "payload": {
    "roomId": "uuid",
    "message": {
      "id": "uuid",
      "content": "Hey!",
      "senderId": "uuid",
      "createdAt": "2026-06-05T10:00:00Z"
    }
  }
}
```

### 6.3 Client → Server (Phase 2+)

```json
// Subscribe vào article để nhận live comments
{ "type": "SUBSCRIBE_ARTICLE", "articleId": "uuid" }
{ "type": "UNSUBSCRIBE_ARTICLE", "articleId": "uuid" }

// Phase 3: gửi chat
{ "type": "SEND_CHAT", "roomId": "uuid", "content": "Hello!" }
```

---

## 7. Kafka Event Flow

### Phase 1 — Notification push

```
User B comments on User A's article
    │
    ▼
interaction-service
  → INSERT comment
  → publish COMMENT_CREATED { commentId, articleId, actorId, articleAuthorId }
    │
    ├──▶ notification-consumer: INSERT notification (existing)
    │
    └──▶ websocket-service:
           articleAuthorId = "user-A"
           → find WS connections of "user-A"
           → send { type: "NOTIFICATION", payload: {...} }
```

### Phase 2 — Live comments

```
User B comments
    │
    ▼
websocket-service consumes COMMENT_CREATED
  → push NOTIFICATION to article author (owner)
  → push COMMENT_ADDED to all users subscribed to that articleId
      (subscription stored in-memory: articleId → Set<userId>)
```

### Phase 3 — Chat (new Kafka topic)

Add `EventType.CHAT_MESSAGE` to `social-common`.  
Add `chat-events` Kafka topic.  
`websocket-service` consumes `chat-events` → route to room members.

---

## 8. Scaling Strategy

### Phase 1 (single instance)

In-memory `ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketConnection>>`:
- Key: userId
- Value: set of connections (user có thể mở nhiều tab)

### Phase 2 (multi-instance — Redis pub/sub)

```kotlin
// Khi Kafka event đến instance nào đó:
fun push(userId: String, message: WsMessage) {
    // Gửi trực tiếp nếu user connected vào instance này
    connectionRegistry.getConnections(userId).forEach { it.sendText(json) }

    // Broadcast qua Redis để các instance khác cũng gửi
    redis.publish("ws:push:$userId", json)
}

// Mỗi instance subscribe Redis:
@PostConstruct
fun subscribeRedis() {
    redis.subscribe("ws:push:*") { channel, message ->
        val userId = channel.removePrefix("ws:push:")
        connectionRegistry.getConnections(userId).forEach { it.sendText(message) }
    }
}
```

---

## 9. Traefik — Thêm route `/ws`

```yaml
# k8s/traefik/ingressroute.yaml — thêm vào protected-routes
- match: PathPrefix(`/ws`)
  kind: Rule
  services:
    - name: websocket-service
      port: 8080
  # Traefik tự handle WebSocket upgrade (HTTP → WS) — không cần config thêm
```

> **Lưu ý sticky session:** khi scale websocket-service lên nhiều replicas, cần thêm Traefik sticky session middleware để client reconnect vào đúng instance (hoặc dùng Redis fan-out để không cần sticky).

---

## 10. Frontend Integration

FE vẫn gửi `?token=` — đây là cách DUY NHẤT browser có thể truyền auth khi mở WS connection (không set được header tùy ý). Traefik nhận, verify, inject `X-User-Id` rồi forward.

**Vite dev config** (`vite.config.ts`):
```ts
'/ws': {
  target: 'ws://localhost:8086',
  ws: true,
  rewriteWsOrigin: true,
}
```

```typescript
// src/hooks/useWebSocket.ts
import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '../stores/authStore'
import { qk } from './queryKeys'

const WS_URL = import.meta.env.VITE_WS_URL ?? '/ws'   // dev: proxy, prod: ws://host/ws

export function useWebSocket() {
  const token   = useAuthStore((s) => s.token)
  const qc      = useQueryClient()
  const wsRef   = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!token) return

    // token gửi qua query param — browser không cho set Authorization header trên WS
    const ws = new WebSocket(`${WS_URL}?token=${token}`)
    wsRef.current = ws

    ws.onmessage = (e) => {
      const msg = JSON.parse(e.data)

      switch (msg.type) {
        case 'NOTIFICATION':
          // Badge tự update, list tự refetch
          qc.invalidateQueries({ queryKey: qk.notifications.unread })
          qc.invalidateQueries({ queryKey: qk.notifications.list })
          break

        case 'COMMENT_ADDED':
          // Append comment vào cache mà không cần refetch
          qc.invalidateQueries({
            queryKey: qk.comments.byTarget(msg.payload.articleId, 'ARTICLE'),
          })
          break

        case 'CHAT_MESSAGE':
          // Phase 3
          break
      }
    }

    ws.onclose = () => {
      // Reconnect sau 3s
      setTimeout(() => wsRef.current?.readyState === WebSocket.CLOSED, 3000)
    }

    return () => ws.close()
  }, [token])
}
```

Mount trong `RootLayout.tsx`:
```tsx
export function RootLayout() {
  useWebSocket()   // ← thêm dòng này
  // ...
}
```

---

## 11. Phased Implementation

### Phase 1 — Notification push ✅ (MVP)
- [ ] Tạo `websocket-service` module
- [ ] `WsEndpoint.kt` — accept connection, verify JWT, register
- [ ] `ConnectionRegistry.kt` — in-memory userId → connections map
- [ ] `WsEventConsumer.kt` — consume `social-events`, route COMMENT/VOTE
- [ ] Traefik route `/ws`
- [ ] k8s manifest `websocket-service.yaml`
- [ ] Frontend: `useWebSocket` hook + mount in RootLayout
- [ ] WireMock: thêm WebSocket mock (dùng `@playwright/test` WS mock cho screenshots)

### Phase 2 — Live comments
- [ ] Client → Server: `SUBSCRIBE_ARTICLE` / `UNSUBSCRIBE_ARTICLE`
- [ ] Article subscription registry (in-memory: articleId → Set<userId>)
- [ ] Push `COMMENT_ADDED` to all subscribers of article
- [ ] Frontend: FeedPage subscribe article on comment section open

### Phase 3 — Chat
- [ ] Thêm `EventType.CHAT_MESSAGE` vào `social-common`
- [ ] Kafka topic `chat-events`
- [ ] `chat-service-dao` — `chat_message` table, `chat_room` table
- [ ] `chat-service` + `chat-api` — REST: create room, send message, history
- [ ] `websocket-service` consume `chat-events` → route to room members
- [ ] Redis fan-out cho multi-instance

---

## 12. Quyết định kiến trúc

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| WS vs SSE | WebSocket | Chat và live comments cần bidirectional, SSE chỉ 1 chiều |
| Service riêng vs nhét vào notification-api | Service riêng | Scale độc lập, scope rõ ràng, không pollute REST service |
| External auth | Traefik verify JWT → `X-User-Id` header | Service không tự verify JWT, Traefik làm một lần |
| Internal auth | `X-Service-Secret-Key` shared secret | Đơn giản, không cần mTLS, lưu trong k8s Secret |
| FE → WS auth | `?token=` query param | Browser không set Authorization header trên WS upgrade |
| Fan-out mechanism | Redis pub/sub | Đã có Redis trong infra, đơn giản hơn Kafka roundtrip |
| Connection state | In-memory Phase 1, Redis Phase 2 | YAGNI — single instance đủ cho MVP |
| Message format | Typed JSON `{ type, payload }` | Extensible, dễ thêm type mới, dễ switch/when ở client |
