# API Contract
## Mini Social Network — Microservices

> Trạng thái thực tế của các endpoint đã implement.
> Legend: ✅ Implemented · ⬜ Planned · 🔍 RSQL enabled

---

## 1. auth-service — `POST :8081`

| Method | Path | Auth | Body | Response | Status |
|---|---|---|---|---|---|
| POST | `/api/auth/signup` | Public | `SignUpRequest` | `AuthResponse` | ✅ |
| POST | `/api/auth/signin` | Public | `SignInRequest` | `AuthResponse` | ✅ |
| POST | `/api/auth/refresh` | Public | `{ refreshToken }` | `AuthResponse` | ⬜ |

### Request / Response

**SignUpRequest**
```json
{
  "username": "nhan",
  "email": "nhan@example.com",
  "password": "secret123",
  "firstname": "Nhan",
  "lastname": "Nguyen"
}
```

**SignInRequest**
```json
{ "identifier": "nhan", "password": "secret123" }
```

**AuthResponse**
```json
{ "accessToken": "eyJ...", "userId": "uuid", "username": "nhan" }
```

---

## 2. user-service — `GET|PATCH :8082`

| Method | Path | Auth | Body / Params | Response | Status |
|---|---|---|---|---|---|
| GET | `/api/users/me` | Required | — | `UserProfileDto` | ✅ |
| GET | `/api/users/{id}` | Required | — | `UserProfileDto` | ✅ |
| PATCH | `/api/users/me` | Required | `UpdateProfileRequest` | `UserProfileDto` | ✅ |
| GET | `/api/users` | Required | `?filter=` 🔍 | `Page<UserProfileDto>` | ⬜ |

### Request / Response

**UpdateProfileRequest** (all optional)
```json
{ "firstname": "Nhan", "lastname": "Nguyen", "avatarUrl": "https://..." }
```

**UserProfileDto**
```json
{
  "id": "uuid", "username": "nhan", "email": "nhan@example.com",
  "firstname": "Nhan", "lastname": "Nguyen",
  "avatarUrl": "https://...", "createdAt": "2026-06-03T10:00:00Z"
}
```

---

## 3. post-service — `GET|POST|DELETE :8083`

| Method | Path | Auth | Body / Params | Response | Status |
|---|---|---|---|---|---|
| GET | `/api/articles` | Required | `?filter=` 🔍 `&sort=` `&page=` `&size=` | `Page<ArticleDto>` | ✅ 🔍 |
| GET | `/api/articles/{id}` | Required | — | `ArticleDto` | ✅ |
| POST | `/api/articles` | Required | `CreateArticleRequest` | `ArticleDto` | ✅ |
| DELETE | `/api/articles/{id}` | Required | — | `204` | ✅ |
| POST | `/api/articles/images/presign` | Required | `?filename=&contentType=` | `PresignResponse` | ✅ |
| PATCH | `/api/articles/{id}` | Required | `UpdateArticleRequest` | `ArticleDto` | ⬜ |

### RSQL Filter — `GET /api/articles`

Filterable fields:

| Field | Type | Example |
|---|---|---|
| `authorId` | UUID | `filter=authorId==<uuid>` |
| `createdAt` | ISO-8601 | `filter=createdAt=gt=2026-01-01T00:00:00Z` |

Sortable fields: `createdAt` (default desc), `voteCount`, `commentCount`

```
GET /api/articles?filter=authorId==<uuid>&sort=createdAt,desc&page=0&size=10
GET /api/articles?sort=voteCount,desc
GET /api/articles?filter=authorId=in=(<uuid1>,<uuid2>)
```

### Request / Response

**CreateArticleRequest**
```json
{ "description": "Hello world", "imageUrl": "https://s3.../image.jpg" }
```

**ArticleDto**
```json
{
  "id": "uuid", "description": "Hello world", "imageUrl": "https://...",
  "authorId": "uuid", "voteCount": 12, "commentCount": 3,
  "createdAt": "2026-06-03T10:00:00Z", "updatedAt": "2026-06-03T10:00:00Z"
}
```

**PresignResponse**
```json
{ "uploadUrl": "https://s3.../presigned?...", "imageUrl": "https://s3.../images/uuid.jpg" }
```

---

## 4. interaction-service — `GET|POST|DELETE :8084`

> Domain-based: `/api/comments` và `/api/votes` — không overlap với post-service.
> `targetType` cho phép interact với bất kỳ loại content nào trong tương lai.

| Method | Path | Auth | Body / Params | Response | Status |
|---|---|---|---|---|---|
| GET | `/api/comments` | Required | `?targetId=&targetType=ARTICLE&page=&size=` | `Page<CommentDto>` | ✅ |
| POST | `/api/comments` | Required | `CreateCommentRequest` | `CommentDto` | ✅ |
| DELETE | `/api/comments/{id}` | Required | — | `204` | ✅ |
| PATCH | `/api/comments/{id}` | Required | `UpdateCommentRequest` | `CommentDto` | ⬜ |
| POST | `/api/votes` | Required | `CastVoteRequest` | `VoteDto` | ✅ |

### Request / Response

**CreateCommentRequest**
```json
{ "targetId": "uuid", "targetType": "ARTICLE", "description": "Great post!" }
```

**CastVoteRequest**
```json
{ "targetId": "uuid", "targetType": "ARTICLE", "value": 1 }
```
`targetType`: `ARTICLE` | `COMMENT` (extensible)
`value`: `1` upvote · `0` retract · `-1` downvote

**CommentDto**
```json
{
  "id": "uuid", "description": "Great post!",
  "targetId": "uuid", "targetType": "ARTICLE",
  "authorId": "uuid",
  "authorUsername": "nhan", "authorFirstname": "Nhan", "authorLastname": "Nguyen",
  "authorAvatarUrl": "https://...",
  "createdAt": "2026-06-03T10:00:00Z"
}
```
`author*` join từ cache `user_ref` cục bộ của interaction-service (ADR 0006) —
có thể là chuỗi rỗng nếu cache chưa có user đó; `authorAvatarUrl` bị bỏ khỏi
JSON khi null (`serialization-inclusion=non-null`).

**VoteDto**
```json
{ "id": "uuid", "value": 1, "userId": "uuid", "targetId": "uuid", "targetType": "ARTICLE" }
```

---

## 5. notification-service — `GET|PATCH :8085`

| Method | Path | Auth | Body / Params | Response | Status |
|---|---|---|---|---|---|
| GET | `/api/notifications` | Required | `?page=&size=` | `Page<NotificationDto>` | ✅ |
| GET | `/api/notifications/unread-count` | Required | — | `{ count: 5 }` | ✅ |
| PATCH | `/api/notifications/{id}/seen` | Required | — | `204` | ✅ |
| PATCH | `/api/notifications/seen-all` | Required | — | `204` | ✅ |

### Response

**NotificationDto**
```json
{
  "id": "uuid", "type": "COMMENT_CREATED",
  "actorId": "uuid",
  "actorUsername": "nhan", "actorFirstname": "Nhan", "actorLastname": "Nguyen",
  "actorAvatarUrl": "https://...",
  "ownerId": "uuid", "articleId": "uuid",
  "seen": false, "createdAt": "2026-06-03T10:00:00Z"
}
```
`type`: `COMMENT_CREATED` | `VOTE_CAST` (tên `EventType` gốc). `actor*` được
đóng băng lúc tạo notification (ADR 0006) — không có ở notification tạo trước
khi có ADR đó. `articleId` là đích điều hướng khi bấm vào notification (vote
trên comment thì null).

---

## 5b. chat-api — `:8087` (messaging-plan.md, ADR 0007/0008)

| Method | Path | Auth | Body / Params | Response | Status |
|---|---|---|---|---|---|
| POST | `/api/conversations` | Required | `{ targetUserId }` | `ConversationDto` — idempotent, cùng cặp user luôn ra cùng id | ✅ |
| GET | `/api/conversations` | Required | `?before=<lastMessage.id>&size=20` | `CursorPage<ConversationDto>` — chỉ conversation đã có tin, mới nhất trước | ✅ |
| GET | `/api/conversations/unread-count` | Required | — | `{ count: n }` — số **conversation** chưa đọc | ✅ |
| GET | `/api/conversations/{id}` | Required | — | `ConversationDto` | ✅ |
| POST | `/api/conversations/{id}/read` | Required | `{ messageId }` | `204` — chỉ tiến lên | ✅ |
| GET | `/api/conversations/{id}/messages` | Required | `?before=<messageId>&size=30` (≤ 100) | `CursorPage<MessageDto>` — mới nhất trước | ✅ |
| POST | `/api/conversations/{id}/messages` | Required | `{ clientMessageId, content }` (content 1–4.096 ký tự) | `201 MessageDto` — gửi lại cùng `clientMessageId` trả tin cũ | ✅ |

Không phải participant → `404` (không lộ conversation tồn tại). `targetUserId` là chính mình → `400`;
không có trong `user_ref` → `404`. `clientMessageId` đã dùng ở conversation khác → `409`.

**ConversationDto**
```json
{
  "id": "uuid",
  "otherUser": { "id": "uuid", "username": "an", "firstname": "An", "lastname": "Le", "avatarUrl": "https://..." },
  "lastMessage": { "...": "MessageDto, content cắt ~100 ký tự" },
  "unread": true
}
```

**MessageDto** — `id` là UUIDv7 (thứ tự theo giờ server)
```json
{ "id": "uuid", "clientMessageId": "uuid", "conversationId": "uuid", "senderId": "uuid",
  "content": "Hey!", "createdAt": "2026-10-02T10:00:00Z" }
```

**CursorPage\<T\>** — phân trang keyset, trang sau truyền `before` = id cuối của trang hiện tại
```json
{ "items": [ ... ], "hasMore": true }
```

---

## 6. Shared — Pagination

Tất cả endpoint trả list đều dùng:

```
?page=0   (0-indexed, default 0, < 0 → 400)
?size=10  (default 10 cho articles, 20 cho comments/notifications; < 1 → 400; > 50 bị kẹp về 50)
```

**PageResponse\<T\>**
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "total": 120,
    "page": 0,
    "size": 10,
    "hasNext": true
  }
}
```

---

## 7. RSQL Status

| Endpoint | RSQL | Filter Fields | Sort Fields |
|---|---|---|---|
| `GET /api/articles` | ✅ | `authorId`, `createdAt` | `createdAt`, `voteCount`, `commentCount` |
| `GET /api/comments` | ⬜ | — | — |
| `GET /api/notifications` | ⬜ | — | — |
| `GET /api/users` | ⬜ (endpoint chưa có) | — | — |
