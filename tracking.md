# Project Tracking — Mini Social Network

> Cập nhật: 2026-06-04

---

## Tổng quan

Dự án đang trong giai đoạn **rewrite từ monolith Spring Boot sang microservices Quarkus + Kotlin**, chạy trên Kubernetes (kind local / EKS prod). Frontend cũng đang được viết lại từ React 17 + MUI sang React + TypeScript + Tailwind CSS.

---

## Backend — Trạng thái từng service

### Shared Libraries
| Module | Trạng thái | Nội dung |
|---|---|---|
| `social-common` | ✅ Done | `UserDto`, `ApiResponse`, `SocialEvents`, RSQL helper |
| `social-exception` | ✅ Done | `AppException` + subclasses, 3 exception mappers, `ErrorCodes`, traceId |

### Auth Service
| Layer | Trạng thái | Nội dung |
|---|---|---|
| `auth-service-dao` | ✅ Done | `Credentials` entity, `CredentialsRepository`, `OutboxEntry`, `OutboxRepository` |
| `auth-service` | ✅ Done | `AuthResource`, `AuthService`, `JwtService`, `DevTokenResource` |

> **Ghi chú:** auth-service không tách api/consumer — chỉ có REST, không consume Kafka.  
> Outbox entity/repo đã có; cần xác nhận Debezium đọc được bảng outbox chưa.

### User Service
| Layer | Trạng thái | Nội dung |
|---|---|---|
| `user-service-dao` | ✅ Done | `UserProfile` entity + repository |
| `user-service` | ✅ Done | `UserService`, `UserDtos` (shared logic lib) |
| `user-api` | ✅ Done | `UserResource` (REST) |
| `user-consumer` | ✅ Done | `UserEventConsumer` (Kafka: xử lý `user.created` event từ auth) |

### Post Service
| Layer | Trạng thái | Nội dung |
|---|---|---|
| `post-service-dao` | ✅ Done | `Article` entity + repo, `OutboxEntry`, `OutboxRepository` |
| `post-service` | ✅ Done | `ArticleService`, `S3Service`, `CounterService`, `ArticleDtos`, RSQL filter |
| `post-api` | ✅ Done | `ArticleResource`, `ArticleListParams`, `S3PresignerProducer` |
| `post-consumer` | ✅ Done | `InteractionEventConsumer`, `CounterFlushJob` (flush Redis → DB mỗi 30s) |

### Interaction Service
| Layer | Trạng thái | Nội dung |
|---|---|---|
| `interaction-service-dao` | ✅ Done | `Comment`/`Vote` entities + repos, `OutboxEntry`, `OutboxRepository` |
| `interaction-service` | ✅ Done | `InteractionService`, `InteractionResource`, `InteractionDtos` |

> **Ghi chú:** interaction-service không tách api/consumer — chỉ publish Kafka, không consume.

### Notification Service
| Layer | Trạng thái | Nội dung |
|---|---|---|
| `notification-service-dao` | ✅ Done | `Notification` entity + repository |
| `notification-service` | ✅ Done | Shared logic lib |
| `notification-api` | ✅ Done | `NotificationResource` (REST: list, mark seen) |
| `notification-consumer` | ✅ Done | `NotificationEventConsumer` (Kafka: `comment.created`, `vote.cast`) |

---

## Backend — Patterns đã implement

| Pattern | Trạng thái | Chi tiết |
|---|---|---|
| Outbox Pattern | 🔧 Partial | `OutboxEntry` + `OutboxRepository` có trong 3 DAOs (auth, interaction, post). Debezium k8s config (`k8s/infra/debezium.yaml`) đã có. Cần verify Debezium connector đã config đúng. |
| Redis Counter + Flush | ✅ Done | `CounterService` + `CounterFlushJob` trong post-consumer, flush về DB mỗi 30s |
| RSQL Filtering | ✅ Done | `ArticleFilter` + `Rsql` helper trong post-service/social-common |
| API/Consumer Split | ✅ Done | user, post, notification — mỗi nhóm tách thành 2 Quarkus app riêng |
| JWT RS256 | ✅ Done | Private key ký trong auth-service, public key verify trong services + Traefik |
| Exception Handling | ✅ Done | `social-exception` shared, tất cả services dùng chung |

---

## Infrastructure — Kubernetes

### Infra Components
| Thành phần | K8s Manifest | Trạng thái |
|---|---|---|
| Kafka (Strimzi) | `k8s/infra/kafka.yaml` | ✅ Config done |
| PostgreSQL | `k8s/infra/postgres.yaml` | ✅ Config done |
| Redis | `k8s/infra/redis.yaml` | ✅ Config done |
| LocalStack (S3) | `k8s/infra/localstack.yaml` | ✅ Config done |
| Debezium | `k8s/infra/debezium.yaml` | ✅ Config done |
| LGTM Stack (Grafana/Loki/Tempo/Mimir) | `k8s/infra/lgtm.yaml` | ✅ Config done |
| JWT Secret | `k8s/infra/jwt-secret.yaml` | ✅ Config done |
| Traefik IngressRoute | `k8s/traefik/ingressroute.yaml` | ✅ Config done |
| Namespaces | `k8s/namespaces.yaml` | ✅ Config done |

### Service Manifests
| Service | Manifest | Trạng thái |
|---|---|---|
| auth-service | `k8s/services/auth-service.yaml` | ✅ Done |
| user-api | `k8s/services/user-api.yaml` | ✅ Done |
| user-consumer | `k8s/services/user-consumer.yaml` | ✅ Done |
| post-api | `k8s/services/post-api.yaml` | ✅ Done |
| post-consumer | `k8s/services/post-consumer.yaml` | ✅ Done |
| interaction-service | `k8s/services/interaction-service.yaml` | ✅ Done |
| notification-api | `k8s/services/notification-api.yaml` | ✅ Done |
| notification-consumer | `k8s/services/notification-consumer.yaml` | ✅ Done |

> **Chưa có:** Deployment cho `user-service-dao`, `post-service-dao` (library, không deploy riêng — đúng rồi).  
> **Chưa có:** kind cluster chưa được spin up và verify end-to-end.

---

## Frontend — Trạng thái

Frontend đang được viết lại (tất cả files dưới `frontend/src/` là **untracked** — chưa commit).

### Stack mới
- React + TypeScript + Vite + Tailwind CSS
- TanStack React Query v5 (server state)
- Zustand (client/UI state)
- shadcn/ui components

### Đã có (chưa commit)
| Thành phần | Trạng thái | Nội dung |
|---|---|---|
| Pages | 🔧 Partial | `FeedPage`, `LoginPage`, `SignUpPage`, `NotificationsPage`, `ProfilePage` |
| API Layer | 🔧 Partial | `articles.ts`, `auth.ts`, `interactions.ts`, `notifications.ts`, `client.ts`, `queryKeys.ts` |
| Stores | 🔧 Partial | `authStore.ts` (Zustand) |
| Components | 🔧 Partial | `article/`, `comment/`, `layout/`, `ui/` directories |
| Types | 🔧 Partial | `types/` directory |
| Hooks | 🔧 Partial | `hooks/` directory |

### Chưa làm (Frontend UX)
- [ ] Infinite scroll
- [ ] Optimistic update (like/comment)
- [ ] Relative timestamp ("2 giờ trước")
- [ ] Inline comments trong card
- [ ] Loading skeleton states
- [ ] Empty states
- [ ] Responsive mobile (bottom nav)
- [ ] Image drag & drop + preview + validation
- [ ] Form validation với Zod
- [ ] Dark mode

---

## Feature Completion (từ specs/feature.md)

| Area | Done / Total | Còn lại nổi bật |
|---|---|---|
| Authentication | 3/6 | Refresh token, logout/revoke, forgot password |
| User Profile | 3/5 | Upload avatar (S3), Follow/Unfollow |
| Post | 5/8 | Edit post, user feed, search |
| Comment | 3/5 | Edit comment, nested reply |
| Vote | 4/4 | **Complete** |
| Notification | 6/7 | Push notification (SSE/WebSocket) |
| Image Upload | 1/3 | Delete image on post delete, client resize |
| Feed & Discovery | 1/4 | Follow-based feed, trending, user search |
| Frontend UX | 0/12 | Tất cả UX features chưa làm |

---

## Việc cần làm tiếp theo

### Ưu tiên cao
- [ ] **Verify Debezium connector** — đảm bảo outbox → Kafka hoạt động end-to-end
- [ ] **Spin up kind cluster** — apply toàn bộ k8s manifests, test happy path
- [ ] **Commit frontend** — add tất cả untracked frontend files vào git

### Ưu tiên trung bình
- [ ] **Frontend: wire API thật** — kiểm tra từng page gọi đúng endpoint
- [ ] **Frontend: loading/error states** — skeleton cards, error toast (Sonner)
- [ ] **Frontend: inline comments** — thay modal bằng inline trong card
- [ ] **Frontend: like/vote** — optimistic update + rollback
- [ ] **Frontend: responsive mobile** — bottom nav bar

### Ưu tiên thấp
- [ ] Refresh token endpoint (`POST /api/auth/refresh`)
- [ ] Delete image on S3 khi xóa bài
- [ ] Upload avatar
- [ ] Notification qua SSE/WebSocket
- [ ] CI/CD GitHub Actions
- [ ] EKS production setup

---

## Kiến trúc module hiện tại (services/settings.gradle.kts)

```
social-common          ← shared DTOs, events, RSQL
social-exception       ← exception mappers

auth-service-dao       ← entities + Liquibase
auth-service           ← REST: signup/signin/JWT

user-service-dao
user-service           ← shared logic
user-api               ← REST
user-consumer          ← Kafka: user.created

post-service-dao
post-service           ← shared logic + S3 + RSQL
post-api               ← REST
post-consumer          ← Kafka consumer + CounterFlushJob

interaction-service-dao
interaction-service    ← REST + Kafka publish (không consume)

notification-service-dao
notification-service   ← shared logic
notification-api       ← REST
notification-consumer  ← Kafka: comment.created, vote.cast
```
