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
  "authorId": "uuid", "createdAt": "2026-06-03T10:00:00Z"
}
```

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
  "id": "uuid", "type": "COMMENT",
  "actorId": "uuid", "ownerId": "uuid", "articleId": "uuid",
  "seen": false, "createdAt": "2026-06-03T10:00:00Z"
}
```

---

## 6. Shared — Pagination

Tất cả endpoint trả list đều dùng:

```
?page=0   (0-indexed, default 0)
?size=10  (default 10, max 50)
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
| `GET /api/articles/{id}/comments` | ⬜ | — | — |
| `GET /api/notifications` | ⬜ | — | — |
| `GET /api/users` | ⬜ (endpoint chưa có) | — | — |
