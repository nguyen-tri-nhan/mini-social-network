# API Standard
## Mini Social Network — Microservices

> **Status:** Draft — pending review

---

## 1. Base URL

| Environment | URL |
|---|---|
| Local (qDev) | `http://localhost:{port}/api` |
| Local (kind) | `http://localhost/api` (via Traefik) |
| Production | `https://api.social.nhan.dev/api` |

Port per service:

| Service | Port |
|---|---|
| auth-service | 8081 |
| user-service | 8082 |
| post-service | 8083 |
| interaction-service | 8084 |
| notification-service | 8085 |

---

## 2. Authentication

JWT Bearer token trong `Authorization` header:

```
Authorization: Bearer <token>
```

- Token được cấp bởi `auth-service` khi signin
- Expiry: 7 ngày
- Algorithm: RS256
- Claims: `sub` (userId UUID), `username`, `groups` (roles)

**Public endpoints** (không cần token):

```
POST /api/auth/signup
POST /api/auth/signin
```

---

## 3. Response Format

### 3.1 Success

Mọi response thành công đều wrap trong `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... }
}
```

Hoặc chỉ message (no data):

```json
{
  "success": true,
  "message": "Done"
}
```

### 3.2 Error

Lỗi dùng cùng `ApiResponse` wrapper — `data` và `error` đối xứng nhau:

```json
{
  "success": false,
  "error": {
    "errorCode": "03-0001",
    "errorMessage": "Article uuid not found",
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
  }
}
```

| Field | Type | Mô tả |
|---|---|---|
| `error.errorCode` | `"xx-xxxx"` | Service code + error type (xem section 5) |
| `error.errorMessage` | String | Human-readable message |
| `error.traceId` | String | OpenTelemetry trace ID — dùng để tra log trên Jaeger/Grafana |

### 3.3 Pagination

Các endpoint trả về danh sách dùng `PageResponse<T>`:

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

Query params chuẩn:

| Param | Default | Max | Mô tả |
|---|---|---|---|
| `page` | `0` | — | 0-indexed |
| `size` | `10` | `50` | Items per page |

---

## 4. HTTP Status Codes

| Code | Khi nào dùng |
|---|---|
| `200 OK` | GET, PATCH thành công |
| `201 Created` | POST tạo resource mới |
| `204 No Content` | DELETE thành công |
| `400 Bad Request` | Validation error, invalid input |
| `401 Unauthorized` | Thiếu hoặc sai token |
| `403 Forbidden` | Có token nhưng không có quyền |
| `404 Not Found` | Resource không tồn tại |
| `409 Conflict` | Duplicate (username, email) |
| `500 Internal Server Error` | Lỗi không xác định |

---

## 5. Error Codes

Format: `"{service}-{type}"` — mỗi phần là số.

### Service codes

| Code | Service |
|---|---|
| `00` | System / generic (validation, internal error) |
| `01` | auth-service |
| `02` | user-service |
| `03` | post-service |
| `04` | interaction-service |
| `05` | notification-service |

### Error type codes

| Code | HTTP | Mô tả |
|---|---|---|
| `0001` | 404 | Not found |
| `0002` | 409 | Conflict / duplicate |
| `0003` | 401 | Unauthorized |
| `0004` | 403 | Forbidden |
| `0005` | 400 | Bad request |
| `0006` | 400 | Validation error |
| `9999` | 500 | Internal error |

### Ví dụ

| errorCode | Nghĩa |
|---|---|
| `01-0002` | auth-service — username/email đã tồn tại |
| `01-0003` | auth-service — sai credentials |
| `03-0001` | post-service — article không tìm thấy |
| `03-0004` | post-service — không phải owner |
| `04-0001` | interaction-service — comment không tìm thấy |
| `00-0006` | Validation error (từ bất kỳ service nào) |
| `00-9999` | Internal error |

---

## 6. Endpoints

### Auth — `POST /api/auth/signup`

**Request:**
```json
{
  "username": "nhan",
  "email": "nhan@example.com",
  "password": "secret123",
  "firstname": "Nhan",
  "lastname": "Nguyen"
}
```

**Validation:**
- `username`: không rỗng
- `email`: format email hợp lệ
- `password`: tối thiểu 6 ký tự
- `firstname`, `lastname`: không rỗng

**Response `201`:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "userId": "uuid",
    "username": "nhan"
  }
}
```

---

### Auth — `POST /api/auth/signin`

**Request:**
```json
{
  "identifier": "nhan",
  "password": "secret123"
}
```

`identifier` chấp nhận cả username lẫn email.

**Response `200`:** same as signup response.

---

### Users — `GET /api/users/me`

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "nhan",
    "email": "nhan@example.com",
    "firstname": "Nhan",
    "lastname": "Nguyen",
    "avatarUrl": "https://...",
    "createdAt": "2026-06-03T10:00:00Z"
  }
}
```

---

### Users — `PATCH /api/users/me`

**Request** (tất cả optional):
```json
{
  "firstname": "Nhan",
  "lastname": "Nguyen",
  "avatarUrl": "https://s3.../avatar.jpg"
}
```

**Response `200`:** updated `UserProfileDto`

---

### Articles — `GET /api/articles`

**Query:** `?page=0&size=10`

**Response `200`:** `PageResponse<ArticleDto>`

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "description": "Hello world",
        "imageUrl": "https://...",
        "authorId": "uuid",
        "voteCount": 12,
        "commentCount": 3,
        "createdAt": "2026-06-03T10:00:00Z",
        "updatedAt": "2026-06-03T10:00:00Z"
      }
    ],
    "total": 100,
    "page": 0,
    "size": 10,
    "hasNext": true
  }
}
```

---

### Articles — `POST /api/articles`

**Request:**
```json
{
  "description": "My post",
  "imageUrl": "https://..."
}
```

Ít nhất một trong hai (`description` hoặc `imageUrl`) phải có.

**Response `201`:** `ArticleDto`

---

### Articles — `DELETE /api/articles/{id}`

Chỉ owner mới xoá được (soft delete).

**Response `204`:** No content

---

### Articles — `POST /api/articles/images/presign`

Lấy pre-signed S3 URL để upload ảnh trực tiếp từ browser.

**Query:** `?filename=photo.jpg&contentType=image/jpeg`

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://s3.../presigned?...",
    "imageUrl": "https://s3.../images/uuid-photo.jpg"
  }
}
```

**Flow:**
1. Client gọi endpoint này → nhận `uploadUrl` + `imageUrl`
2. Client PUT ảnh lên `uploadUrl` trực tiếp
3. Client dùng `imageUrl` khi tạo article

---

### Comments — `GET /api/articles/{articleId}/comments`

**Query:** `?page=0&size=20`

**Response `200`:** `PageResponse<CommentDto>`

---

### Comments — `POST /api/articles/{articleId}/comments`

**Request:**
```json
{
  "description": "Great post!"
}
```

Max 1000 ký tự.

**Response `201`:** `CommentDto`

---

### Comments — `DELETE /api/comments/{commentId}`

Chỉ author mới xoá được.

**Response `204`:** No content

---

### Votes — `POST /api/articles/{id}/vote`

```json
{ "value": 1 }
```

| value | Nghĩa |
|---|---|
| `1` | Upvote |
| `0` | Retract |
| `-1` | Downvote |

**Response `200`:** `VoteDto`

---

### Votes — `POST /api/comments/{id}/vote`

Same as article vote.

---

### Notifications — `GET /api/notifications`

**Query:** `?page=0&size=20`

**Response `200`:** `PageResponse<NotificationDto>`

---

### Notifications — `GET /api/notifications/unread-count`

**Response `200`:**
```json
{
  "success": true,
  "data": { "count": 5 }
}
```

---

### Notifications — `PATCH /api/notifications/{id}/seen`

**Response `204`:** No content

---

### Notifications — `PATCH /api/notifications/seen-all`

**Response `204`:** No content

---

## 7. Validation Rules

| Field | Rule |
|---|---|
| `username` | NotBlank, max 50 chars |
| `email` | Valid email format |
| `password` | Min 6 chars |
| `description` (comment) | NotBlank, max 1000 chars |
| `description` (article) | Max 2000 chars |
| `page` | >= 0 |
| `size` | 1–50 |
| `vote.value` | -1, 0, or 1 |

Validation error response:
```json
{
  "errorCode": "VALIDATION_ERROR",
  "errorMessage": "signup.request.password: size must be between 6 and 2147483647",
  "traceId": "uuid",
  "timestamp": "..."
}
```

---

## 8. Naming Conventions

- **URL path**: `kebab-case` — `/api/articles`, `/api/seen-all`
- **JSON fields**: `camelCase` — `authorId`, `createdAt`, `imageUrl`
- **Error codes**: `SCREAMING_SNAKE_CASE` — `NOT_FOUND`, `VALIDATION_ERROR`
- **Timestamps**: ISO-8601 UTC — `2026-06-03T10:00:00Z`
- **IDs**: UUID v4

---

## 9. TODO / Open Questions

- [ ] Định nghĩa service-specific error codes (`AUTH_001`, `POST_001`...)
- [ ] Token refresh endpoint (`POST /api/auth/refresh`)
- [ ] Rate limiting strategy (Traefik middleware hay service level?)
- [ ] File size limit cho image upload (hiện tại không enforce ở API layer)
- [ ] Sorting options cho feed (`?sort=trending` vs `?sort=latest`)
- [ ] Soft delete visibility — admin có thể xem hidden articles không?
