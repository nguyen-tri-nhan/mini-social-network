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
| Kafdrop (Kafka Web UI, debug thủ công) | `k8s/infra/kafdrop.yaml` | ✅ Config done |
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

- [x] **Fix BOM version mismatch** — tách `quarkus-amazon-services-bom` khỏi `social-bom` dùng chung, chỉ import trực tiếp ở `post-service`/`post-api`. 18/19 module build được (`./gradlew build` pass), tiện fix luôn bug `auth-service` thiếu `quarkus.index-dependency.auth-service-dao.*`. **`post-api` vẫn không build (JIB) được** — giới hạn thật của quarkiverse-amazon-services (chưa release bản tương thích quarkus-bom 3.25.x), không phải lỗi cấu hình. Xem `specs/decisions/0002-*.md`.
- [x] **Thử BOM native `io.quarkiverse.amazonservices` cho post-api** — thất bại (2 version thử đều lộ conflict version thật ở quarkus-core/bootstrap, không phải chỉ nhãn "stream"). Đã revert về ADR 0002. Xem `specs/decisions/0003-*.md` (đã superseded).
- [x] **Fix 2 bug `Makefile`** — `k8s-secrets` giờ nằm trong `deploy`/`up` đúng thứ tự (sau infra, trước services); `up-%` restart đúng tên deployment thật qua bảng `DEPLOYS_<nhóm>` thay vì stem sai tên. Không ADR (build-tooling fix).
- [x] **Thêm `scripts/shutdown-k8s.sh` + `make shutdown`** — xoá cluster kind `social` có xác nhận, tự dọn `kubectl port-forward` chạy nền trước khi xoá. Verify chạy thật trên cluster `social` đang tồn tại — đúng hành vi từ chối chạy khi không có `-y` và không phải tty.
- [x] **Kafka trên k8s chạy được, verify thật bằng cluster sống — fix 5 bug liên tiếp trong `k8s/infra/kafka.yaml`:**
  1. YAML comma trong flow-mapping bị cắt giá trị env (`KAFKA_PROCESS_ROLES` v.v.)
  2. `enableServiceLinks` mặc định của k8s tiêm `KAFKA_PORT` trùng biến deprecated-fatal của cp-kafka
  3. Service `kafka` thiếu port 9093 (CONTROLLER)
  4. `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093` (qua Service) dính hairpin NAT trên kindnetd — đổi sang `1@localhost:9093` (broker+controller cùng 1 process)
  5. `readinessProbe` dùng `kafka-topics --list` cũng dính hairpin NAT y hệt (client redirect sang advertised.listeners sau bootstrap) — đổi sang `tcpSocket: port 9092`, thuần network-level, không qua Kafka protocol

  **Verify bằng cluster sống** (không chỉ đọc log paste): `kubectl rollout status` → `successfully rolled out`, `kubectl get pods` → `1/1 Running` cho cả Kafka lẫn Kafdrop, gọi thẳng Kafdrop REST API (`curl` qua port-forward) → thấy `broker/1`, xác nhận Kafdrop connect được Kafka thật. Chưa verify tiếp outbox→Debezium→Kafka→consumer end-to-end.
- [x] **Fix `quarkus.otel.metrics.exporter=otlp` khiến toàn bộ 9 service crash lúc start** — property này ép dùng raw upstream OpenTelemetry SDK (cần dependency `opentelemetry-exporter-sender-*` không có trong classpath) thay vì exporter built-in Vert.x của Quarkus (default `cdi`, vẫn xuất OTLP đúng endpoint). Xoá dòng đó ở cả 9 `application.properties`. Verify bằng rebuild + redeploy thật trên cluster sống — log sạch, `Installed features` có `opentelemetry`, không crash.
- [x] **Fix `user-consumer` thiếu `quarkus.redis.hosts`** — `user-consumer` kéo theo `quarkus-redis-client` transitive qua `user-service` (dùng cho cache profile ở `user-api`) nhưng `application.properties` chưa từng khai property đọc `REDIS_URL` (dù k8s manifest đã set sẵn env đó) → "Redis host not configured" lúc start. Thêm `quarkus.redis.hosts=${REDIS_URL:redis://localhost:6379}`, khớp pattern các service khác.
- [x] **Phát hiện resource pressure thật trên kind local** — Docker Desktop chỉ cấp 4 CPU/3.83GB, deploy 9 service cùng lúc (`kubectl apply -f k8s/services/`) → 9 JVM cold-start đồng thời → Postgres connection pool timeout hàng loạt + API server (`kubectl`) timeout, không phải bug code. Xác nhận qua `docker stats` (CPU 110-117%) và verify: cùng code, cùng resource, deploy tuần tự (đợi Ready mới sang cái kế) thì qua hết — không cần tăng resource để fix, dù tăng vẫn nên làm cho thoải mái hơn.
- [x] **Thêm `specs/service-dependencies.md` + `scripts/deploy-sequential.sh` + `make deploy-services-sequential`** — bản đồ phụ thuộc thật giữa 9 service (đọc code, không suy đoán): chỉ 1 coupling đồng bộ (`interaction-service` → `post-api`, đã tự graceful-degrade, không cần decouple), còn lại thuần qua Kafka/outbox. `make deploy`/`make up` giờ mặc định dùng bản tuần tự (đổi từ `deploy-services` sang `deploy-services-sequential`) — thứ tự: `auth-service → post-api → user-api → notification-api → websocket-service → interaction-service → user-consumer → post-consumer → notification-consumer`.
- [x] **Thêm Kafdrop 4.3.0** (`k8s/infra/kafdrop.yaml`, `make kafdrop`) — debug/test produce message Kafka thủ công, port-forward khi cần, không route qua Traefik.
- [x] **Fix thật: `post-api` build được, 19/19 module pass** — nguyên nhân gốc không phải version BOM mà là `post-api` tự khai platform `quarkus-amazon-services-bom` trực tiếp một cách thừa thãi (post-service đã khai + export transitive rồi). Xoá dòng thừa đó → `./gradlew build` + `test` **19/19 module pass**, không còn service nào bị chặn build. Xem `specs/decisions/0004-*.md`.
- [x] **Fix `quarkus.otel.logs.enabled` thiếu** — thêm vào cả 9 `application.properties` (giống `traces.enabled`), tận dụng `otel-lgtm` đã có sẵn để đẩy log qua OTLP, không cần thêm DaemonSet log-shipper (Promtail đã EOL 2/2026, thay bằng Alloy — nhưng không cần cho nhu cầu "chỉ xem log/trace 9 service nhà mình", không phải log infra).
- [x] **Fix `debezium/connect:2.7` ImagePullBackOff** — tag `2.7` chưa bao giờ tồn tại trên Docker Hub (chỉ có tag đầy đủ `2.7.x.Final`), lỗi cấu hình có sẵn từ đầu, không phải mới phát sinh. Đổi thành `2.7.3.Final` (bản mới nhất dòng 2.7.x, verify qua Docker Hub API).
- [x] **Fix Traefik chưa từng cài được — `make cluster-create` fail âm thầm từ đầu, verify bằng cluster sống:** `helm status` cho thấy release `STATUS: failed` ngay từ lần đầu — chart có port nội bộ `traefik` (dashboard) mặc định cũng `containerPort: 8080`, đụng port `web` mình set 8080. Fix 4 việc trong `Makefile`: (1) dời dashboard sang `ports.traefik.port=9090`; (2) ghim Traefik vào đúng node `social-control-plane` bằng `nodeSelector`+`toleration` (kind `extraPortMappings` chỉ forward host:8080 tới đúng node đó — Traefik từng bị xếp lịch nhầm qua worker, `docker port` xác nhận); (3) `ingressRoute.dashboard.enabled=true` (mặc định false, thiếu thì dashboard 404 dù pod đúng chỗ); (4) `helm install` → `helm upgrade --install` để idempotent. Verify: `curl localhost:8080/api/auth/signup` → `405` đúng route qua Traefik, `make traefik-dashboard` → `200`. Thêm target `make traefik-dashboard` (Makefile).
- [x] **Fix `/openapi/{users,post,notifications}` 404 qua Traefik — bug đặt sai tên Service, có sẵn từ đầu** — `k8s/services/{user,post,notification}-api.yaml`: Deployment đặt đúng tên (`user-api`...) nhưng Service copy-paste nhầm tên module lib (`user-service`, `post-service`, `notification-service` — trùng tên `services/user-service` v.v., không phải module API). IngressRoute trỏ đúng tên `user-api`/`post-api`/`notification-api` nên không tìm thấy Service → 404, dù pod hoàn toàn khoẻ (verify port-forward thẳng vào pod → 200). `auth-service`/`interaction-service` không dính vì Service đặt tên đúng sẵn (trùng tên Deployment, không có module lib riêng cùng tên gây nhầm). Sửa 3 file, xoá 3 Service tên sai trên cluster sống, verify lại `curl` cả 5 route → 200/200/200/200/200.
- [x] **Fix `interaction-service.yaml` thiếu `POST_API_URL`** — thêm `POST_API_URL: http://post-api:8080` vào manifest. Property runtime (không phải build-time), chỉ cần `kubectl apply` + restart pod, **không cần rebuild image**.
- [x] **Frontend local dev chạy được, verify end-to-end thật** — `npm install` + `npm run typecheck` sạch, `npm run dev` (Vite `:3000`) proxy `/api` → Traefik `:8080` (`vite.config.ts`) đúng chuẩn dev-proxy, không phải "tự bind". Test thật: signup đủ field → `201` + JWT thật, user lưu Postgres thật. Thêm `make frontend-install/dev/typecheck/test/build` + `make db-forward`/`make db-psql` vào Makefile.
- [x] **Fix `jwt-verify` ForwardAuth — chưa từng chạy được từ đầu, chặn cứng mọi route protected (500)** — 2 lỗi cộng dồn: (1) Traefik ở namespace `kube-system` không resolve được tên Service trần `auth-service` (namespace `social`) qua DNS cross-namespace; (2) endpoint `/api/auth/verify` chưa từng được implement (chỉ có `/signup`, `/signin`). Phát hiện thêm: mọi service đã tự verify JWT cục bộ qua `mp.jwt.verify.publickey.location` + `@RolesAllowed` — không service nào đọc header `X-User-Id` mà ForwardAuth định forward (grep `services/` ra 0 kết quả) → ForwardAuth là lớp trùng lặp, chưa từng cần thiết. Xoá hẳn Middleware + reference khỏi `k8s/traefik/ingressroute.yaml` và `infra/traefik/dynamic/routes.yaml`. Verify: không token → `401` đúng (trước: `500`). Xem `specs/decisions/0005-*.md`.
- [x] **Fix pipeline outbox→Kafka→consumer vỡ hoàn toàn — 2 bug cộng dồn trong `k8s/infra/debezium.yaml` + connector config, phát hiện khi verify `/api/users/me` lần đầu:**
  1. `KEY_CONVERTER_SCHEMAS_ENABLE`/`VALUE_CONVERTER_SCHEMAS_ENABLE` (không tiền tố) bị entrypoint image `debezium/connect` bỏ qua hoàn toàn — image chỉ đặc cách 1 danh sách cố định biến không tiền tố (`BOOTSTRAP_SERVERS`, `GROUP_ID`, `*_STORAGE_TOPIC`, `KEY_CONVERTER`, `VALUE_CONVERTER`...), property khác bắt buộc tiền tố `CONNECT_` (verify qua source `docker-entrypoint.sh` của image). Đổi thành `CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE`/`CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE` → hết envelope `{schema,payload}` bọc ngoài.
  2. Payload column trong outbox table là JSON dạng string — thiếu `transforms.outbox.table.expand.json.payload=true` (SMT `EventRouter`) khiến payload bị JSON-string-hoá 2 lớp (`"{\"eventType\":...}"` — Jackson nhận 1 string thay vì object). Verify đúng property qua doc chính thức Debezium (WebFetch), thêm vào connector config trong `k8s/infra/debezium.yaml` + apply trực tiếp qua REST API (`PUT /connectors/social-outbox-connector/config`, không cần rebuild/restart worker).
  Verify bằng signup thật end-to-end: `POST /api/auth/signup` → `POST /api/auth/verify` (giờ đã bỏ) → outbox → Kafka (raw JSON, không envelope) → `user-consumer` xử lý sạch → `GET /api/users/me` → **`200` với data đúng**.
- [x] **Thêm `make forward-up`/`forward-down`/`forward-status`** — gộp 4 port-forward hay dùng (Grafana, Kafdrop, Traefik dashboard, Postgres) chạy nền bằng `nohup ... & echo $! > pidfile`, không cần mở nhiều terminal riêng từng cái nữa. Fix luôn collision Grafana `:3000` trùng cổng Vite dev server frontend — đổi sang `:3001`. Verify thật: bật cả 4, `curl` từng endpoint đều `200`/TCP reachable, `forward-down` xác nhận port đóng hẳn (connection refused), `forward-status` phản ánh đúng trạng thái sống/chết qua `kill -0`.
- [x] **Sự cố dây chuyền: Kafka bị OOM-killed (exit 137) → mất sạch topic (ephemeral, không PVC) → gần như toàn bộ pod trong namespace restart hàng loạt (post-api, user-api, notification-*, interaction-service, websocket-service...)** — nguyên nhân: rolling update `kafka-connect` (để áp fix env var) khiến 2 JVM `kafka-connect` (rất nặng, quét hàng chục connector plugin) chạy song song gần 1 tiếng trên máy resource hạn chế → OOM. `post-api` bị crash giữa lúc chạy Liquibase migration, để lại lock treo (`databasechangeloglock.locked=true`, `lockedby` = chính pod đã chết) → restart vô hạn "waiting for changelog lock". Fix: (1) `UPDATE post.databasechangeloglock SET locked=false...` giải phóng lock treo; (2) scale `kafka-connect` về 0 rồi lên lại 1 — đảm bảo chỉ 1 JVM tồn tại tại 1 thời điểm (né lặp lại OOM, cùng nguyên lý `deploy-sequential.sh`); (3) topic `debezium.status`/`debezium.configs` bị Kafka tự tạo lại với `cleanup.policy=delete` sai (default) sau khi mất — Kafka Connect yêu cầu `compact` nghiêm ngặt cho `status.storage.topic`, gây crash loop vĩnh viễn cho worker mới — fix bằng `kafka-configs --alter --add-config cleanup.policy=compact` (không dùng delete+recreate vì dính race: worker cũ đang sống tự động tạo lại topic sai config nhanh hơn); (4) connector `social-outbox-connector` mất đăng ký hoàn toàn (do `debezium.configs` — nơi lưu config connector — bị xoá theo Kafka) → đăng ký lại qua REST API. Bài học: **không để 2 bản `kafka-connect` chạy song song** — nếu cần đổi env var sau này, cân nhắc `kubectl scale --replicas=0` rồi `1` thay vì `kubectl apply` (trigger RollingUpdate mặc định).

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
