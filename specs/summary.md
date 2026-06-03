# Mini Social Network — Project Summary

## 1. Overview

Mini Social Network is a full-stack social media web application where authenticated users can create posts (articles) with text and images, view a feed of all posts, vote on content, and comment. It follows a classic client–server architecture with a React SPA frontend and a Spring Boot REST API backend, backed by PostgreSQL.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 17, Material-UI 4, React Router 5, Axios |
| Backend | Spring Boot 2.4.3, Java 8, Spring Security, JPA/Hibernate |
| Database | PostgreSQL 12, Liquibase (migrations) |
| Auth | JWT (JJWT 0.9.0), BCrypt |
| Image Storage | Imgur API (external) |
| Containerization | Docker, Docker Compose, Nginx |
| Build | Maven (BE), npm (FE) |

---

## 3. Architecture

```
[Browser / React SPA]
        │
        │ HTTP/JSON (JWT Bearer)
        ▼
[Nginx reverse proxy]
        │
        ▼
[Spring Boot REST API  :8080]
   ┌────────────────────────┐
   │  Controller Layer       │  AuthController, ArticleController,
   │                         │  CommentController, VoteController
   ├────────────────────────┤
   │  Facade Layer           │  ArticleFacade, CommentFacade, VoteFacade
   ├────────────────────────┤
   │  Service Layer          │  Business logic, DTO conversion
   ├────────────────────────┤
   │  Repository Layer       │  JPA repositories (Spring Data)
   └────────────────────────┘
        │
        ▼
[PostgreSQL :5432]
        │
[Imgur API] ← image uploads from frontend
```

**Patterns used:** MVC, Facade, Service, Repository, DTO/Resource, JWT stateless auth.

---

## 4. Data Models

### User (`user_account`)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| username | String | unique |
| email | String | unique |
| firstname | String | |
| lastname | String | |
| password | String | BCrypt hashed |
| avatar | String | URL |

### Article (`article`)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| description | String | post body |
| image | String | Imgur URL |
| user_id | FK → User | author |
| visible | Boolean | soft delete |
| created_at / updated_at | Timestamp | audited |

### Comment (`comment`)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| description | String | |
| user_id | FK → User | |
| article_id | FK → Article | |
| visible | Boolean | soft delete |

### Vote (`vote`)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| vote | byte | -1 (down), 0 (neutral), 1 (up) |
| user_id | FK → User | |
| article_id | FK → Article | |

### Notification (`notification`)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| type | byte | event type enum |
| seen | Boolean | read/unread |
| fromUser_id | FK → User | actor |
| toUser_id | FK → User | recipient |
| article_id | FK → Article | context |

### Role (`role`)
- `ROLE_ADMIN`, `ROLE_USER`

---

## 5. API Endpoints

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/signup` | Public | Register a new user |
| POST | `/api/auth/signin` | Public | Login; returns JWT token |

### Articles
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/articles/POST` | Required | Create a new article |
| GET | `/api/articles` | Required | List all articles (feed) |
| GET | `/api/articles/{id}` | Required | Get single article |
| GET | `/api/articles/{id}/comment` | Required | Get comments on article |

### Comments
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/articles/{id}/comment` | Required | Add comment to article |

### Votes
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/votes/{articleId}` | Required | Vote on article — **not yet implemented** |
| POST | `/api/votes/{commentId}` | Required | Vote on comment — **not yet implemented** |

### User
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/user/getme` | Required | Get current authenticated user |

---

## 6. Frontend Routes

| Path | Access | Component |
|---|---|---|
| `/` | Authenticated | `HomePage` — news feed |
| `/login` | Public | `Login` — login form |
| `/signup` | Public | `SignUp` — registration form |
| `/logout` | Authenticated | `Logout` — clears session |

---

## 7. Requirements (Rewritten)

### Functional Requirements

#### FR-01: User Registration
- A visitor can create an account by providing username, email, first name, last name, and password.
- Username and email must be unique across the system.
- Password is stored as a BCrypt hash; plain text is never persisted.
- On success the system returns a confirmation; the user is redirected to login.

#### FR-02: User Login
- A registered user can log in with username or email plus password.
- On success the server issues a signed JWT (7-day expiry) returned in the response body.
- The client stores the token in `localStorage` under the key `JWT` and attaches it as a `Bearer` header on every subsequent request.
- Receiving an HTTP 401 from any endpoint automatically logs the user out and redirects to `/login`.

#### FR-03: View News Feed
- An authenticated user sees a paginated/streamed list of all articles sorted by creation date (newest first).
- Each article card shows: author avatar, author name, post timestamp, post description, post image (if any), total vote count, and total comment count.

#### FR-04: Create Article
- An authenticated user can compose a new post consisting of a text description and an optional image.
- If an image is selected the client uploads it to Imgur and stores the returned URL; no binary data is stored in the database.
- The new article is attributed to the currently authenticated user and appears in the feed immediately after creation.

#### FR-05: View Article Comments
- A user can expand an article to view all comments, each showing: commenter avatar, name, comment text, and timestamp.

#### FR-06: Add Comment
- An authenticated user can submit a text comment on any article.
- The comment is attributed to the commenter and associated with the target article.

#### FR-07: Vote on Article (Planned)
- An authenticated user can cast an upvote (+1) or downvote (-1) on an article.
- A user may change or retract their vote (0 = neutral).
- The aggregate vote count is displayed on each article card.

#### FR-08: Vote on Comment (Planned)
- Same mechanics as FR-07, applied to comments.

#### FR-09: Notifications (Planned)
- The system generates a notification to the article owner when another user comments or votes on their article.
- Notifications carry a `seen` flag; users can mark them as read.
- The navbar indicates the count of unread notifications.

#### FR-10: User Profile
- An authenticated user can retrieve their own profile information (`GET /api/user/getme`).

---

### Non-Functional Requirements

#### NFR-01: Security
- All passwords are BCrypt-hashed before storage; plain text never leaves the service layer.
- JWT tokens are signed with a configured secret and expire after 7 days.
- CSRF protection is disabled (stateless token auth); CORS is explicitly configured.
- Role-based access control enforced at the service layer (`ROLE_USER`, `ROLE_ADMIN`).
- **Known gap:** JWT secret `"JWTSuperSecretKey"` is hardcoded in `application.properties`; it must be externalised via environment variable before production use.

#### NFR-02: Performance
- Article feed must return within 500 ms for up to 1,000 articles.
- Image storage is offloaded to Imgur to keep database payloads small.

#### NFR-03: Scalability
- Stateless JWT auth means the backend can be horizontally scaled behind a load balancer without session affinity.
- Database connections are managed by Hibernate's connection pool.

#### NFR-04: Reliability
- Soft deletes (`visible` flag) prevent accidental data loss for articles and comments.
- Liquibase manages schema migrations deterministically; rollback scripts should be maintained.

#### NFR-05: Portability
- The full stack (frontend, backend, database) runs via `docker compose up` with no host dependencies beyond Docker.

---

## 8. Use Cases

### UC-01: Register Account

**Actor:** Visitor  
**Precondition:** Not logged in  
**Main Flow:**
1. Visitor navigates to `/signup`.
2. Fills in username, email, first name, last name, password, and confirm-password.
3. Client validates format (email regex, password length ≥ 6, passwords match).
4. Client POSTs to `POST /api/auth/signup`.
5. Server checks uniqueness of username and email.
6. Server creates user with `ROLE_USER`, returns `200 OK`.
7. Client redirects to `/login`.

**Alternate — duplicate username/email:**
- Server returns `400 Bad Request` with message; client displays error toast.

---

### UC-02: Log In

**Actor:** Registered User  
**Precondition:** Has valid account  
**Main Flow:**
1. User navigates to `/login`.
2. Enters username/email and password.
3. Client POSTs to `POST /api/auth/signin`.
4. Server validates credentials, signs JWT, returns token.
5. Client stores JWT in `localStorage`, redirects to `/` (feed).

**Alternate — wrong credentials:**
- Server returns `401 Unauthorized`; client shows error toast.

---

### UC-03: View News Feed

**Actor:** Authenticated User  
**Precondition:** Valid JWT in `localStorage`  
**Main Flow:**
1. User navigates to `/`.
2. Client GETs `GET /api/articles` with Bearer token.
3. Server returns list of `ArticleResource` objects.
4. Client renders `NewsFeed` with `Article` cards.
5. Each card shows author, timestamp, content, vote count, comment count.

---

### UC-04: Create Post

**Actor:** Authenticated User  
**Precondition:** Logged in  
**Main Flow:**
1. User clicks "Create Post" in the navbar.
2. `CreateArticleForm` dialog opens.
3. User writes description; optionally selects an image.
4. If image selected: client encodes to base64, POSTs to Imgur API, receives image URL.
5. Client POSTs `{description, image}` to `POST /api/articles/POST`.
6. Server creates `Article` entity linked to the authenticated user.
7. Server returns `ArticleResource`; client closes dialog and refreshes feed.

**Alternate — Imgur upload fails:**
- Client shows error notification; article creation is aborted.

---

### UC-05: Comment on Article

**Actor:** Authenticated User  
**Precondition:** Logged in, viewing an article  
**Main Flow:**
1. User opens comments section on an article.
2. Types a comment and submits.
3. Client POSTs `{description}` to `POST /api/articles/{id}/comment`.
4. Server creates `Comment` entity.
5. Comment appears in the comments list.

---

### UC-06: Vote on Article (Planned)

**Actor:** Authenticated User  
**Precondition:** Logged in  
**Main Flow:**
1. User clicks upvote (👍) or downvote (👎) on an article.
2. Client POSTs `{vote: 1 or -1}` to `POST /api/votes/{articleId}`.
3. Server upserts the `Vote` record for this (user, article) pair.
4. Updated vote count returned; article card reflects new total.

**Alternate — retract vote:**
- User clicks same vote button again; vote is set to `0`.

---

### UC-07: Receive Notification (Planned)

**Actor:** Authenticated User (article owner)  
**Precondition:** Another user commented/voted on their article  
**Main Flow:**
1. Trigger: comment or vote action creates a `Notification` record.
2. Article owner's navbar badge shows unread count.
3. Owner opens notification panel, sees activity summary.
4. Owner clicks a notification — marked `seen = true`.

---

## 9. Known Gaps & Issues

| # | Area | Description | Severity |
|---|---|---|---|
| G-01 | Security | JWT secret hardcoded as `"JWTSuperSecretKey"` in `application.properties` | High |
| G-02 | Backend | `VoteController` methods are stubs (TODO comments, no implementation) | High |
| G-03 | Frontend | `Comments` component renders placeholder text, not real comment data | Medium |
| G-04 | Frontend | Notification UI components are scaffolded but not wired to backend | Medium |
| G-05 | Backend | No pagination on `GET /api/articles` — full table scan at scale | Medium |
| G-06 | Frontend | JWT stored in `localStorage` is vulnerable to XSS; `httpOnly` cookie preferred | Medium |
| G-07 | Backend | `visible` soft-delete flag exists but delete endpoints are not exposed | Low |
| G-08 | Backend | `ArticleStatistics` entity has no read/write path in controllers | Low |
| G-09 | Security | Imgur `CLIENT_ID` committed in `.env`; should be in `.env.local` (git-ignored) | Low |
| G-10 | Testing | Only unit test scaffolding exists; no integration or E2E tests | Low |

---

## 10. Recommended Next Steps (Priority Order)

1. **Externalise secrets** — move JWT secret and DB password to environment variables; update `application.properties` to use `${JWT_SECRET}`.
2. **Implement vote endpoints** — complete `VoteService` and `VoteController`; add unique constraint on `(user_id, article_id)` in the vote table.
3. **Wire comments in frontend** — connect `Comments.jsx` to `GET /api/articles/{id}/comment` and `POST /api/articles/{id}/comment`.
4. **Pagination** — add Spring Data `Pageable` to `ArticleRepository.findAll()`; update frontend to support infinite scroll or page controls.
5. **Notification system** — fire notifications from `CommentService` and `VoteService`; add `GET /api/notifications` endpoint; connect frontend badge.
6. **Replace localStorage JWT with httpOnly cookie** — reduces XSS attack surface; requires backend `Set-Cookie` support and CORS `credentials: include`.
7. **Add integration tests** — use Testcontainers for PostgreSQL; cover auth, article CRUD, and voting flows.
