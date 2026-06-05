# WebSocket Service — Implementation Plan

## 1. Kafka Topic Strategy

### Per-domain topics

```
Producer              Topic                 Consumer(s)
──────────────────────────────────────────────────────────────────────
auth-service      →   social.auth       →   user-consumer
                      USER_UPDATED              (tạo user_profile)

post-api          →   social.post       →   (future: search index, feed ranking)
                      ARTICLE_CREATED
                      ARTICLE_DELETED

interaction-      →   social.interaction →  notification-consumer  (lưu DB)
service               COMMENT_CREATED    →  post-consumer          (update counter)
                      VOTE_CAST          →  websocket-service      (push realtime)

websocket-        →   social.chat        →  websocket-service      (Phase 3)
service               CHAT_MESSAGE              (route đến room members)
```

Mỗi consumer chỉ subscribe topic mình cần — không nhận event thừa, không filter bỏ.

### websocket-service Kafka subscriptions

```kotlin
// application.properties — websocket-service
mp.messaging.incoming.interaction-events-in.topic=social.interaction
mp.messaging.incoming.interaction-events-in.group.id=ws-group

# Phase 3
mp.messaging.incoming.chat-events-in.topic=social.chat
mp.messaging.incoming.chat-events-in.group.id=ws-chat-group
```

Group ID `ws-group` độc lập với `notification-group` và `counter-group` — Kafka fan-out đảm bảo cả 3 group đều nhận đủ events từ `social.interaction`.

---

## 2. Tại sao tách thành service riêng

`notification-api` chỉ làm REST (list, mark seen). WebSocket là protocol khác, stateful connection, cần scale độc lập.

| Concern | REST services | websocket-service |
|---|---|---|
| Protocol | HTTP stateless | TCP stateful (WS) |
| Scaling | Horizontal, stateless | Cần sticky session / Redis fan-out |
| Resource | CPU/memory per request | Memory per **connection** (idle) |
| Scope hiện tại | CRUD | Push gateway |
| Scope tương lai | — | Chat, live comments, presence |

---

## 3. Kiến trúc tổng thể

Pub/sub channel model — FE tự subscribe vào topic cần, server route theo topic name. Không cần auth ở WS level.

```
[Browser]
   │
   │  ws://host/ws   (public, no auth)
   ▼
[Traefik]  ←── PathPrefix(`/ws`) — route only, no middleware
   │
   ▼
[websocket-service :8080]
   │
   │  FE gửi: { "type": "SUBSCRIBE", "topic": "user_{userId}_notification" }
   │  FE gửi: { "type": "SUBSCRIBE", "topic": "article_{articleId}_comment_added" }
   │
   ├── Topic registry:  topic → Set<WebSocketConnection>
   │
   ├── Kafka consumer (social.interaction)
   │     COMMENT_CREATED  → push to "user_{authorId}_notification"
   │                      → push to "article_{articleId}_comment_added"
   │     VOTE_CAST        → push to "user_{targetAuthorId}_notification"
   │
   └── Redis pub/sub  ←── fan-out across instances
         channel: ws:topic:{topicName}

┌─────────────────────────────────────────────────────┐
│ Multi-instance scaling                               │
│                                                      │
│  WS Instance A              WS Instance B            │
│  sub: user_A_notification   sub: user_B_notification │
│         │                          │                 │
│         └──────── Redis pub/sub ───┘                 │
│               channel: ws:topic:*                    │
│                                                      │
│  Kafka event arrives at Instance A                   │
│  → push locally to connected clients on Instance A   │
│  → publish to Redis ws:topic:{topicName}             │
│  → Instance B receives → push to its connected clients│
└─────────────────────────────────────────────────────┘
```

---

## 4. WS Topics

| Topic | Subscribe khi nào | Nhận gì |
|---|---|---|
| `user_{userId}_notification` | Luôn luôn khi login — FE dùng userId từ JWT đã decode client-side | `NOTIFICATION` — comment/vote vào bài của mình |
| `article_{articleId}_comment_added` | FE mở bài và expand comment section | `COMMENT_ADDED` — live comments |
| `room_{roomId}_chat` | FE vào chat room (Phase 3) | `CHAT_MESSAGE` |

> **Security note:** topic name là guessable — ai biết userId của user B có thể subscribe `user_B_notification`. Data trong notification (ai comment vào bài nào) không nhạy cảm, chấp nhận được ở scale này.

---

## 5. Tech Stack

| Component | Choice | Lý do |
|---|---|---|
| WS framework | `quarkus-websockets-next` | Reactive, built-in với Quarkus 3.x, tích hợp Mutiny |
| Kafka consumer | SmallRye Reactive Messaging (đã có) | Tái dùng pattern từ notification-consumer |
| Redis fan-out | `quarkus-redis-client` (đã có) | Đã cài trong infra, pub/sub API đơn giản |
| Auth | Không có — WS là public route | Topic name không chứa data nhạy cảm |

---

## 6. Gradle Module

Chỉ cần **1 module** — không có DB (Phase 1-2), không cần DAO:

```
services/
└── websocket-service/
    ├── build.gradle.kts
    └── src/main/kotlin/com/nhan/social/ws/
        ├── WsEndpoint.kt          @WebSocket(path = "/ws")
        ├── TopicRegistry.kt       topic → Set<WebSocketConnection>
        ├── WsEventConsumer.kt     @Incoming("interaction-events-in")
        ├── WsPushService.kt       resolve topic name → push local + Redis
        └── WsMessage.kt           data classes cho message types
```

**`settings.gradle.kts`** — thêm vào cuối:
```kotlin
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
    implementation("io.quarkus:quarkus-websockets-next")
    implementation("io.quarkus:quarkus-messaging-kafka")
    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
}
```

---

## 7. WebSocket Protocol

### Connection

```
Browser  →  ws://host/ws        (prod: qua Traefik)
            ws://localhost:8086/ws  (dev: trực tiếp)
```

Không cần token. FE connect xong thì gửi SUBSCRIBE messages.

### Client → Server

```json
{ "type": "SUBSCRIBE",   "topic": "user_{userId}_notification" }
{ "type": "UNSUBSCRIBE", "topic": "user_{userId}_notification" }

{ "type": "SUBSCRIBE",   "topic": "article_{articleId}_comment_added" }
{ "type": "UNSUBSCRIBE", "topic": "article_{articleId}_comment_added" }
```

### Server → Client

```json
// Topic: user_{userId}_notification
{
  "topic": "user_{userId}_notification",
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

// Topic: article_{articleId}_comment_added
{
  "topic": "article_{articleId}_comment_added",
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

// Topic: room_{roomId}_chat  (Phase 3)
{
  "topic": "room_{roomId}_chat",
  "type": "CHAT_MESSAGE",
  "payload": {
    "roomId": "uuid",
    "message": { "id": "uuid", "content": "Hey!", "senderId": "uuid" }
  }
}
```

---

## 8. Kafka Event Flow

### COMMENT_CREATED

```
User B comments on User A's article (articleId=X)
    │
    ▼
interaction-service
  → INSERT comment
  → INSERT outbox { COMMENT_CREATED, payload: { commentId, articleId=X, actorId=B, targetId=A } }
    │
    ├──▶ notification-consumer: INSERT notification (existing)
    │
    └──▶ websocket-service:
           push to "user_A_notification"         ← article author
           push to "article_X_comment_added"     ← anyone viewing the article
```

### VOTE_CAST

```
User B votes on User A's article
    │
    ▼
websocket-service consumes VOTE_CAST { targetId, targetType, actorId=B, delta }
  → push to "user_A_notification"
```

### Phase 3 — Chat

Add `EventType.CHAT_MESSAGE` to `social-common`.
`websocket-service` consumes `social.chat` → push to `room_{roomId}_chat`.

---

## 9. Scaling Strategy

### Phase 1 (single instance)

```kotlin
// TopicRegistry.kt
val registry = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketConnection>>()

fun subscribe(topic: String, conn: WebSocketConnection) =
    registry.getOrPut(topic) { CopyOnWriteArraySet() }.add(conn)

fun getConnections(topic: String): Set<WebSocketConnection> =
    registry[topic] ?: emptySet()
```

### Phase 2 (multi-instance — Redis pub/sub)

```kotlin
// WsPushService.kt
fun push(topic: String, message: String) {
    // push local connections on this instance
    topicRegistry.getConnections(topic).forEach { it.sendText(message) }

    // fan-out to other instances via Redis
    redis.publish("ws:topic:$topic", message)
}

// subscribe Redis on startup
@PostConstruct
fun subscribeRedis() {
    redis.subscribe("ws:topic:*") { channel, message ->
        val topic = channel.removePrefix("ws:topic:")
        topicRegistry.getConnections(topic).forEach { it.sendText(message) }
    }
}
```

---

## 10. Frontend Integration

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

const WS_URL = import.meta.env.VITE_WS_URL ?? '/ws'

export function useWebSocket() {
  const userId = useAuthStore((s) => s.userId)   // decoded from JWT client-side
  const qc     = useQueryClient()
  const wsRef  = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!userId) return

    const ws = new WebSocket(WS_URL)
    wsRef.current = ws

    ws.onopen = () => {
      // subscribe to personal notification topic
      ws.send(JSON.stringify({ type: 'SUBSCRIBE', topic: `user_${userId}_notification` }))
    }

    ws.onmessage = (e) => {
      const msg = JSON.parse(e.data)
      switch (msg.type) {
        case 'NOTIFICATION':
          qc.invalidateQueries({ queryKey: qk.notifications.unread })
          qc.invalidateQueries({ queryKey: qk.notifications.list })
          break
        case 'COMMENT_ADDED':
          qc.invalidateQueries({
            queryKey: qk.comments.byTarget(msg.payload.articleId, 'ARTICLE'),
          })
          break
      }
    }

    ws.onclose = () => {
      setTimeout(() => wsRef.current?.readyState === WebSocket.CLOSED, 3000)
    }

    return () => ws.close()
  }, [userId])
}

// Subscribe vào article khi mở comment section
export function useArticleSubscription(articleId: string) {
  const wsRef = useRef<WebSocket | null>(null)  // reuse connection từ useWebSocket

  useEffect(() => {
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(JSON.stringify({ type: 'SUBSCRIBE',   topic: `article_${articleId}_comment_added` }))
    return () => {
      ws.send(JSON.stringify({ type: 'UNSUBSCRIBE', topic: `article_${articleId}_comment_added` }))
    }
  }, [articleId])
}
```

---

## 11. Phased Implementation

### Phase 1 — Notification push (MVP)
- [ ] Tạo `websocket-service` module
- [ ] `WsEndpoint.kt` — accept connection, handle SUBSCRIBE/UNSUBSCRIBE messages
- [ ] `TopicRegistry.kt` — in-memory topic → connections map
- [ ] `WsEventConsumer.kt` — consume `social.interaction`, resolve topic, push
- [ ] `WsPushService.kt` — push local + Redis pub/sub
- [ ] Traefik route `/ws` (public, đã có)
- [ ] k8s manifest `websocket-service.yaml`
- [ ] Frontend: `useWebSocket` hook + mount in RootLayout

### Phase 2 — Live comments
- [ ] `useArticleSubscription` hook — FE subscribe khi mở comment section
- [ ] Đảm bảo `COMMENT_ADDED` push đúng vào `article_{id}_comment_added`

### Phase 3 — Chat
- [ ] Thêm `EventType.CHAT_MESSAGE` vào `social-common`
- [ ] `chat-service-dao` — `chat_message`, `chat_room` tables
- [ ] `chat-api` — REST: create room, send message, history
- [ ] `websocket-service` consume `social.chat` → push to `room_{roomId}_chat`
- [ ] Redis fan-out cho multi-instance

---

## 12. Quyết định kiến trúc

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| WS vs SSE | WebSocket | Chat và live comments cần bidirectional, SSE chỉ 1 chiều |
| Service riêng vs nhét vào notification-api | Service riêng | Scale độc lập, scope rõ ràng |
| Auth model | Public — không auth WS | Topic name không chứa data nhạy cảm; FE tự biết userId từ JWT client-side |
| Routing model | Pub/sub by topic name | Đơn giản hơn userId-routing; scale tự nhiên; FE tự chọn topic cần |
| Fan-out mechanism | Redis pub/sub | Đã có Redis trong infra, pub/sub API đơn giản |
| Internal auth | `X-Service-Secret-Key` shared secret | Đơn giản, không cần mTLS ở scale này |
