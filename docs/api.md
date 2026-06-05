# API Reference

## Base URL

| Environment | URL |
|---|---|
| Local (dev) | `http://localhost:{port}/api` |
| Local (kind) | `http://localhost/api` |
| Production | `https://api.social.nhan.dev/api` |

---

## Authentication

All protected endpoints require a JWT Bearer token:

```
Authorization: Bearer <token>
```

- Issued by `auth-service` on signin
- Expiry: 7 days
- Algorithm: RS256
- Claims: `sub` (userId UUID), `username`, `groups` (roles)

**Public endpoints (no token required):**
```
POST /api/auth/signup
POST /api/auth/signin
```

---

## Response Format

### Success

```json
{
  "success": true,
  "data": { ... }
}
```

Paginated list:
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "total": 100,
    "page": 0,
    "size": 20,
    "hasNext": true
  }
}
```

### Error

```json
{
  "success": false,
  "error": {
    "errorCode": "03-0001",
    "errorMessage": "Article not found",
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
  }
}
```

**Error code format:** `"{service-code}-{type}"`

| Service code | Service |
|---|---|
| `01` | auth-service |
| `02` | user-service |
| `03` | post-service |
| `04` | interaction-service |
| `05` | notification-service |

| Type suffix | Meaning |
|---|---|
| `0001` | Not found |
| `0002` | Conflict |
| `0003` | Unauthorized |
| `0004` | Forbidden |
| `0005` | Bad request / validation |

---

## Endpoints

### Auth

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/signup` | Register → returns JWT |
| `POST` | `/api/auth/signin` | Login → returns JWT |

**Signup request:**
```json
{
  "username": "nhan",
  "email": "nhan@example.com",
  "password": "secret",
  "firstname": "Nhan",
  "lastname": "Nguyen"
}
```

**Auth response:**
```json
{
  "accessToken": "eyJ...",
  "userId": "uuid",
  "username": "nhan"
}
```

---

### Users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users/me` | Current user profile |
| `GET` | `/api/users/{id}` | User profile by ID |
| `PUT` | `/api/users/me` | Update profile |

---

### Articles

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/articles` | Feed (paginated, filterable) |
| `GET` | `/api/articles/{id}` | Single article |
| `POST` | `/api/articles` | Create article |
| `DELETE` | `/api/articles/{id}` | Soft-delete (owner only) |
| `POST` | `/api/articles/images/presign` | Get S3 presigned upload URL |

**Feed query params:**
```
GET /api/articles?page=0&size=20&filter=authorId=="uuid"&sort=createdAt,desc
```

Filter uses RSQL syntax. Available fields: `authorId`, `visible`, `createdAt`.

**Presign response:**
```json
{
  "uploadUrl": "https://s3.../photo.jpg?X-Amz-Signature=...",
  "imageUrl": "https://s3.../photo.jpg"
}
```

---

### Comments

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/comments?targetId={id}&targetType=ARTICLE` | List comments |
| `POST` | `/api/comments` | Add comment |
| `DELETE` | `/api/comments/{id}` | Soft-delete (author only) |

**Create comment:**
```json
{
  "targetId": "article-uuid",
  "targetType": "ARTICLE",
  "description": "Nice post!"
}
```

---

### Votes

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/votes` | Cast vote |

**Cast vote:**
```json
{
  "targetId": "article-uuid",
  "targetType": "ARTICLE",
  "value": 1
}
```

`value`: `1` (upvote), `-1` (downvote), `0` (remove vote).

---

### Notifications

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/notifications` | List notifications (paginated) |
| `GET` | `/api/notifications/unread-count` | Unread badge count |
| `PATCH` | `/api/notifications/{id}/seen` | Mark single as seen |
| `PATCH` | `/api/notifications/seen-all` | Mark all as seen |

---

## WebSocket

Connect to `ws://host/ws` (no auth required).

**Subscribe to a topic:**
```json
{ "type": "SUBSCRIBE", "topic": "user_{userId}_notification" }
{ "type": "SUBSCRIBE", "topic": "article_{articleId}_comment_added" }
```

**Unsubscribe:**
```json
{ "type": "UNSUBSCRIBE", "topic": "user_{userId}_notification" }
```

**Server push — notification:**
```json
{
  "topic": "user_{userId}_notification",
  "type": "NOTIFICATION",
  "payload": {
    "notificationType": "COMMENT",
    "actorId": "uuid",
    "articleId": "uuid"
  }
}
```

**Server push — live comment:**
```json
{
  "topic": "article_{articleId}_comment_added",
  "type": "COMMENT_ADDED",
  "payload": {
    "commentId": "uuid",
    "articleId": "uuid",
    "actorId": "uuid"
  }
}
```
