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
| Notification | 10/10 | — |
| Image Upload | 1/3 | Delete image on post delete, client resize |
| Feed & Discovery | 1/4 | Follow-based feed, trending, user search |
| Frontend UX | 0/12 | Tất cả UX features chưa làm |
| Chat | 7/12 | Read receipt, typing, group chat, đính kèm ảnh, chặn người dùng |

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
- [x] **Fix `make deploy-services-sequential` chạy đơn lẻ (bỏ qua `make deploy`) làm mọi service kẹt `ContainerCreating` ~10 phút** — target này không khai `k8s-secrets` làm prerequisite, nên Secret `jwt-keys` không tồn tại → pod nào mount `/etc/jwt` (auth-service, user-api, post-api, interaction-service, notification-api) kẹt vĩnh viễn (`FailedMount: secret "jwt-keys" not found`, kubelet tự retry ~46s/lần nhưng không bao giờ thành công). `websocket-service` không dính vì route public. Fix gốc: thêm `k8s-secrets` làm prerequisite (`deploy-services-sequential: k8s-secrets`) — idempotent, an toàn tuyệt đối. Verify: chạy `make k8s-secrets` thủ công → toàn bộ pod đang kẹt tự phục hồi (kubelet tự retry mount thành công), không cần restart/redeploy gì thêm.
- [x] **Migrate frontend từ Tailwind CSS sang Material UI (MUI) v7, bỏ hẳn Tailwind** — quay lại MUI (project gốc từng dùng MUI 4.12.3 trước khi rewrite sang Tailwind, xem `specs/frontend.md`), lần này dùng `sx` prop trực tiếp trong từng component thay vì utility class. Cài `@mui/material @emotion/react @emotion/styled`, pin `react-is` khớp React 18 (tránh lỗi runtime prop-type theo doc chính thức MUI v7), thêm `resolve.conditions: ['mui-modern',...]` vào `vite.config.ts` cho layout export mới của v7. Gỡ `tailwindcss`/`postcss`/`autoprefixer`/`clsx`/`tailwind-merge`, xoá `tailwind.config.js`/`postcss.config.js`, xoá `components/ui/index.tsx` (bộ component tự viết tay — **không phải shadcn/ui thật như README từng ghi sai**, sửa lại luôn). Convert toàn bộ 12 file dùng Tailwind sang MUI component (`Navbar`, `Sidebar`, `RootLayout`, `ArticleCard`, `CreatePost` — Dialog thay modal tự dựng, `CommentSection`, `LoginPage`, `SignUpPage`, `ProfilePage`, `FeedPage`, `NotificationsPage`). Tiện sửa 2 lỗi phát hiện giữa chừng: thiếu `aria-label` trên icon-only button (đúng vấn đề `specs/frontend.md` từng flag từ bản gốc, chưa từng fix) và thiếu `src/vite-env.d.ts` (khiến `npm run build` lỗi type `import.meta.env`, lộ ra vì đây là lần đầu build production được chạy thật trong repo này). Verify đầy đủ: `typecheck` sạch, `vitest run` 32/32 pass (bao gồm sửa đúng 1 test tự nó lỗi từ trước — regex `/like/i` khớp nhầm cả "Dislike"), `npm run build` thành công, và test **sống bằng Playwright** trên backend thật: signup → login → tạo post → vote → mở comment — toàn bộ hoạt động đúng, 0 lỗi console.
- [x] **Fix session "ma" — token còn hợp lệ về chữ ký nhưng user thật đã mất (sau xoá/tạo lại cluster), FE vẫn hiện màn hình đã login được** — `client.ts`'s response interceptor cố tình bỏ qua cả toast lẫn auto-logout cho `404` (đúng cho case "xem profile người khác không tồn tại"), nhưng hệ quả phụ: `GET /api/users/me` 404 cũng bị nuốt im lặng, để `RootLayout` ở trạng thái nửa-login (`isAuthenticated()` chỉ check có token trong localStorage, không check còn valid). Fix cục bộ trong `RootLayout.tsx` (không đụng rule `!== 404` dùng chung, tránh vỡ case profile người khác): thêm `isError` từ query `/me`, `useEffect` gọi `logout()` bất kỳ khi nào `/me` fail — coi mọi lỗi `/me` là "phiên hết hạn", không phải lỗi tạm thời. Verify sống bằng Playwright: tạo user thật → xoá thẳng row `user_profile` trong Postgres (giả lập mất data sau recreate cluster) → inject token cũ (vẫn valid chữ ký) vào `localStorage` → reload → xác nhận tự redirect `/login` + `localStorage.getItem('jwt')` thành `null`.
- [x] **Thêm outbox cho `user-service-dao` (trước đó chưa có outbox nào) để fix race signup→`/me` 404 — làm đúng outbox pattern thay vì WS bắn trực tiếp từ raw Kafka event `social.auth`** — root cause: `SignUpPage` gọi `/me` ngay sau signup, trong khi `user_profile` được ghi async qua `auth-service` outbox → Debezium → Kafka → `user-consumer`, không có gì đảm bảo write đã xong khi `/me` bắn ra → 404 (và với fix `RootLayout` ở trên, giờ còn tự động logout luôn, càng lộ rõ bug). Cân nhắc rồi loại bỏ hướng "WS lắng nghe thẳng `social.auth`" vì `user-consumer` và `websocket-service` sẽ là 2 consumer group độc lập trên cùng topic, không có gì đảm bảo thứ tự giữa 2 bên — không thật sự hết race. Chọn đúng outbox pattern: `user-service-dao` được thêm bảng `outbox` (entity/repo/Liquibase changeset mới), `UserService.createFromEvent()` ghi 1 row `USER_READY` vào đó **trong cùng transaction** với `user_profile` (đảm bảo chỉ bắn tín hiệu sau khi chắc chắn ghi xong) — thêm `users.outbox` vào `table.include.list` của connector Debezium (`k8s/infra/debezium.yaml` + push trực tiếp qua REST `PUT /connectors/social-outbox-connector/config`, không cần restart worker). `websocket-service` thêm channel Kafka `user-events-in` (topic `social.user`, group `ws-user-group`) + push WS tới topic `user_${userId}_ready`. FE: `waitForUserReady.ts` (one-shot WS subscribe-and-wait qua `/ws` proxy mới trong `vite.config.ts`, luôn resolve — fallback timeout 8s nếu WS lỗi, không kẹt vô hạn), `SignUpPage` hiện màn hình loading "Đang tạo tài khoản…" trong lúc chờ, chỉ gọi `/me` sau khi có tín hiệu. Verify sống end-to-end bằng Playwright + `kubectl`/`psql` thật: signup → loading screen hiện đúng → `users.outbox` có row `USER_READY` (cùng timestamp với `user_profile`, lệch ~6ms) → `websocket-service` log xác nhận subscribe `social.user` + client subscribe đúng topic `user_{id}_ready` → `GET /api/users/me` trả **`200` ngay lần gọi đầu tiên** (không còn 404 race) → vào thẳng home feed đúng user.
- [x] **Fix logout xong tạo acc mới, profile/navbar còn hiện tên user cũ tới khi F5** — `QueryClient` là singleton module-level, `qk.users.me` và `qk.notifications.*` (`queryKeys.ts`) là key tĩnh không scope theo user id (không như `qk.users.detail(id)`/`qk.articles.list(authorId==...)`), và `logout()` trong `authStore.ts` trước đó chỉ xoá token/user khỏi Zustand + localStorage, không đụng gì tới cache React Query. Trong `staleTime: 30_000` (default ở `main.tsx`), data cũ vẫn được coi là "fresh" nên `useQuery` trả thẳng từ cache lúc mount thay vì refetch — `RootLayout`'s `useEffect(() => setUser(me), [me])` set luôn user CŨ vào store nếu logout→signup xảy ra trong vòng 30s. F5 "fix" được chỉ vì tạo `QueryClient` mới (cache trống), không phải do gì khác. Fix gốc, không phải patch từng key: tách `queryClient` ra module riêng `src/lib/queryClient.ts` (trước đó tạo inline trong `main.tsx`), gọi `queryClient.clear()` ngay trong `logout()` — đảm bảo bất kỳ query nào gắn với "current user" trong tương lai cũng tự động an toàn, không phải nhớ patch thủ công từng key mới. Verify: `typecheck` sạch, `vitest run` 32/32 pass, và **test sống đúng kịch bản lỗi** bằng Playwright — quan trọng: dùng click Link trong app (SPA client-side nav) chứ không phải `page.goto()` (native `page.goto()` reload cả document, tự tạo `QueryClient` mới nên che mất bug) — logout (click) → Sign up (click Link) → tạo acc thứ 2 → vào Feed → click Profile (Link) → tên mới hiện đúng ngay, không có gì cũ sót lại.
- [x] **Thêm toast realtime khi có notification (comment/vote) — trước đó chỉ polling 30s, không WS, không toast** — backend đã có sẵn hạ tầng push (`WsEventConsumer.handleComment`/`handleVote` push message `NOTIFICATION` tới topic `user_${authorId}_notification`) nhưng payload thiếu discriminator: `type` trong `ServerMessage` luôn là chuỗi cố định `"NOTIFICATION"` (dùng để route topic), không phải `eventType` gốc (`COMMENT_CREATED`/`VOTE_CAST`) — FE nhận được không biết để hiển thị label nào. Thêm `"eventType" to event.eventType.name` vào payload lúc push (chỉ ở nhánh notification, không đụng nhánh `COMMENT_ADDED` cho live-comment-viewer vốn không cần). FE: tách `TYPE_LABEL` (trước đó định nghĩa cứng trong `NotificationsPage.tsx`) ra `lib/notificationLabels.ts` dùng chung; thêm `hooks/useNotificationSocket.ts` — khác `waitForUserReady.ts` (one-shot rồi đóng), hook này **sống suốt session** (mount ở `RootLayout`, theo `me?.id`), tự reconnect sau 3s nếu bị đóng bất kỳ lý do gì (không phân biệt lỗi mạng tạm thời với logout — cleanup effect tự lo khi `userId` đổi/unmount), nhận `NOTIFICATION` → `toast()` + invalidate `qk.notifications.unread`/`qk.notifications.list`. Verify sống end-to-end: 2 user thật (1 browser session qua Playwright + 1 qua API trực tiếp) — user B comment vào post của user A → toast "Someone commented on your post" hiện ngay trên browser A (không cần F5/đợi poll) → `NotificationsPage` hiện đúng label giống hệt (dùng chung `TYPE_LABEL`). Lưu ý phát hiện thêm (không phải bug, chỉ là đặc tính hệ thống): badge unread-count có độ trễ vài giây so với toast — do WS push (qua `websocket-service`, consume trực tiếp `social.interaction`) và ghi DB `Notification` (qua `notification-consumer`, cũng consume `social.interaction` nhưng là consumer group độc lập) chạy song song không đảm bảo thứ tự hoàn thành, nên `invalidateQueries` gọi ngay khi nhận toast đôi khi refetch trước khi DB kịp ghi xong — tự đúng lại trong khoảng vài giây nhờ `refetchInterval: 30_000` sẵn có ở `Navbar`, chấp nhận được cho use case này, không làm outbox riêng cho notification read-model (over-engineer so với yêu cầu).
- [x] **Bấm vào toast/item trong notification list → điều hướng đúng tới đích (bài viết, comment) — trước đó bấm chỉ mark-seen, không điều hướng** — thiết kế theo hướng scalable: `lib/notificationTarget.ts` là 1 map `{eventType → resolver}` duy nhất (thêm loại notification mới sau này chỉ thêm 1 entry, không sửa `NotificationsPage`/`useNotificationSocket`). Backend có 1 gap chặn tính năng: `NotificationService.createFromEvent()` chỉ set `articleId` cho `COMMENT_CREATED` (đọc `payload["articleId"]`), còn `VOTE_CAST` thì KHÔNG BAO GIỜ set `articleId` dù cột đã tồn tại sẵn — payload vote chỉ có `targetId`/`targetType`, không có field tên `articleId`. Fix: khi `targetType=="ARTICLE"` thì `targetId` CHÍNH LÀ articleId (vote trên comment: FE hiện chưa cho vote comment nên bỏ qua case đó, biết trước và chấp nhận). Áp dụng fix tương tự cho `WsEventConsumer.handleVote()` (WS payload cho toast cũng thiếu `articleId`) để toast và notification-list dùng chung 1 resolver, hành vi nhất quán. FE: route mới `/articles/:id` (`ArticleDetailPage.tsx`, dùng lại `articlesApi.getById` đã có sẵn nhưng chưa từng được gọi ở đâu) — `ArticleCard` thêm prop `defaultShowComments`/`highlightCommentId`, `CommentSection` thêm `highlightCommentId` + tự scroll-into-view + highlight bg khi khớp `commentId` (chỉ có ở toast — payload WS giàu hơn bản ghi DB; click từ list thì tới bài viết, comment đã expand sẵn nhưng không highlight được comment cụ thể do bảng `notification` chưa lưu `commentId`, chấp nhận là giới hạn hiện tại thay vì thêm cột mới cho use case chưa chắc cần). Toast dùng sonner's `action` button ("View") thay vì làm cả toast clickable — đơn giản hơn, a11y tốt hơn. Bonus tìm ra khi verify: `bgcolor: 'primary.50'` (dùng ở cả `NotificationsPage`'s unseen-item bg lẫn code mới viết) không phải token hợp lệ trong theme này (`theme.ts` chỉ khai `primary.main`/`dark`, không có scale số `50/100/...` như `grey`) — sx bgcolor set 1 CSS value không tồn tại, silently no-op, background luôn trong suốt dù logic đúng. Đổi cả 2 chỗ sang `action.selected` (token luôn có sẵn trong mọi MUI theme). Verify sống end-to-end qua Playwright + API thật: comment/vote từ user B → toast trên browser A có nút "View" → bấm → đúng URL `/articles/{id}?comment={id}` → landing đúng bài viết, comment list tự expand, đúng comment có background highlight (xác nhận qua `getComputedStyle` = `rgba(0,0,0,0.08)`); click item "voted on your post" từ `/notifications` → điều hướng đúng `/articles/{id}` (không `?comment=`, đúng vì vote không gắn 1 comment cụ thể).
- [x] **Fix comment/notification (list + toast) chỉ hiện "User"/"Someone" thay vì tên thật** — `CommentSection.tsx` và `NotificationsPage.tsx` chưa từng fetch profile của `authorId`/`actorId`, chỉ hardcode literal text — trong khi `ArticleCard.tsx` đã làm đúng việc này cho tác giả bài viết từ trước (`useQuery(qk.users.detail(id))`). Áp dụng lại đúng pattern đó: tách `CommentItem` (trong `CommentSection.tsx`) và `NotificationItem` (trong `NotificationsPage.tsx`) thành sub-component riêng — bắt buộc vì `useQuery` không gọi được trong `.map()` ở component cha (rule of hooks), mỗi item tự fetch tác giả/actor của chính nó qua `usersApi.getById`, dùng chung cache key `qk.users.detail(id)` nên không tốn thêm request nếu user đó đã được fetch ở chỗ khác (vd đang xem chính bài viết của họ). Tiện fix luôn toast (`useNotificationSocket.ts`) cho nhất quán — dù không được hỏi tới nhưng để lại "Someone" ở đó trong khi list/comment đã hiện tên thật sẽ lệch UX; dùng `qc.fetchQuery(...)` (bản imperative của `useQuery`, ngoài component) để resolve tên actor trước khi gọi `toast()`, cùng chung cache nên nếu tên đã có sẵn thì gần như tức thời. Verify sống qua Playwright + API thật: comment mới từ user thật → cả 3 nơi (comment thread trên `/articles/:id`, notification list, toast) đều hiện đúng tên thật ("Comment Sender", "Nhân Nguyễn") thay vì "User"/"Someone"; `typecheck` sạch, `vitest run` 32/32 pass.
- [x] **Bỏ hẳn client-side N+1 (mỗi comment/notification tự fetch tên tác giả) — chuyển sang materialized user-cache qua Kafka ở backend, theo ADR 0006** — hỏi "sao FE phải tự gọi từng phần, sao BE không enrich luôn" → phân tích 3 hướng (REST đồng bộ, gRPC+batch, materialized view qua Kafka) ở giả định quy mô 1 triệu user cùng online → chọn materialized view (không hướng nào gọi đồng bộ sống nổi ở quy mô đó mà không tự tái phát minh lại cache cục bộ). `interaction-service` được thêm bảng `user_ref` (userId→username/firstname/lastname/avatarUrl) + consumer mới subscribe `social.user` (topic đã có từ việc build `USER_READY` trước đó) — khi tạo Comment/Vote, join với `user_ref` cục bộ (không gọi mạng) để trả tên ngay trong `CommentDto` VÀ nhúng tên actor vào payload event `COMMENT_CREATED`/`VOTE_CAST` khi publish outbox. `notification-service` không cần cache riêng — chỉ copy thẳng field tên actor có sẵn trong payload (đã được interaction-service nhúng), lưu đông cứng trong `Notification` (khác `Comment` — join tươi mỗi lần đọc, `Notification` đóng băng tên lúc tạo, 2 chiến lược có chủ đích khác nhau). Vá luôn 1 gap phát hiện giữa chừng: `UserService.update()` trước đó KHÔNG bắn event nào khi sửa profile — nếu không vá, cache ở các service khác sẽ không bao giờ thấy tên mới; thêm `EventType.USER_PROFILE_UPDATED` (event riêng, không tái dùng `USER_UPDATED` đã có sẵn — phát hiện `USER_UPDATED` đang được auth-service dùng cho việc khác hoàn toàn (signup, discriminator `action=CREATED`), tái dùng sẽ gây nhầm lẫn tên). FE: bỏ hẳn `useQuery(usersApi.getById(...))` ở `CommentItem`/`NotificationItem`/`useNotificationSocket` — đọc thẳng field đã enrich từ response, không còn network call phụ nào. Verify sống: build 19 module + `interaction-service`/`notification-service` unit test pass, deploy thật lên cluster — xác nhận consumer mới replay đúng lịch sử topic `social.user` (`auto.offset.reset=earliest`) nhưng data cũ (event bắn trước migration này) chỉ có `userId`, tên rỗng — đúng dự đoán, tự lành khi user đó tương tác lại (test bằng 1 lần `PATCH /api/users/me` no-op → cache backfill đúng ngay). Playwright xác nhận: 8 comment trên 1 bài viết → **0 request `/api/users/{id}`** cho tác giả comment (trước đó N request, N=số tác giả khác nhau); notification list 11 item → **0 request** `/api/users/{id}`; so sánh trực tiếp: comment/notification tạo SAU migration hiện tên thật ngay, tạo TRƯỚC thì fallback "User"/"Someone" (đúng thiết kế, không phải bug).
- [x] **Audit specs ↔ implementation (29/9/2026) + sửa các lệch** — đối chiếu các doc chuẩn (constitution, ADR, `api-standard`, `api-contract`, `feature.md`, `outbox-pattern`, `service-dependencies`, `be-overview`, `websocket-plan`) với code, phân tích tĩnh (cluster đang tắt). Sửa code:
  1. **`InternalAuthFilter` không bao giờ chặn gì** — check `uriInfo.path.startsWith("internal")` nhưng `UriInfo.getPath()` của RESTEasy Reactive 3.25.1 luôn có `/` đầu (đọc source thật: trả `context.normalizedPath()`) → secret `X-Service-Secret-Key` trên `/internal/**` chưa từng được kiểm. Sửa + thêm `InternalAuthFilterTest` (5 test; chạy thử với code cũ thì fail đúng 2 case "phải chặn" → test bắt được bug). Kéo theo: filter giờ chặn thật nên `post-api` phải đọc cùng biến env với phía gọi — thêm `app.internal.secret-key=${INTERNAL_SECRET_KEY:...}` (comment cũ ghi env `INTERNAL_SECRET_KEY` là sai, MP Config map sang `APP_INTERNAL_SECRET_KEY`); nếu lệch, `resolveOwner` degrade êm → không tạo notification nào.
  2. **Signup firstname/lastname > 100 ký tự làm hỏng tài khoản vĩnh viễn** — `SignUpRequest` chỉ `@NotBlank`, `credentials` không lưu tên nên signup 201, rồi `user-consumer` ghi `user_profile` (varchar 100) lỗi, exception bị nuốt → không bao giờ có profile, `/me` luôn 404. Thêm `@Size` (username 50, email 255, first/last 100) + zod `.max(100)` ở `SignUpPage`/`ProfilePage` + `SignUpRequestValidationTest`.
  3. **Bảng outbox phình mãi** — doc ghi Debezium tự xoá row, thực tế không (EventRouter không xoá). Thêm `OutboxRepository.emit()` ở 4 service: insert → `flush()` → delete cùng transaction (cách Debezium khuyến nghị, mặc định của `quarkus-debezium-outbox`; SMT "automatically filters out DELETE" — verify qua doc chính thức). `flush()` bắt buộc, không Hibernate có thể huỷ cả insert lẫn delete. Hệ quả debug: xem event trên Kafdrop, không query bảng outbox nữa.
  4. **Consumer nuốt mọi exception → event lỗi mất âm thầm** — bỏ catch-all ở 4 consumer ghi state (user, post, notification, `UserRefEventConsumer`), thêm `failure-strategy=dead-letter-queue`. `websocket-service` giữ best-effort có chủ đích. (`UserRefEventConsumer` cũ còn tệ hơn: lỗi lúc commit `@Transactional` xảy ra ngoài try/catch → với strategy mặc định `fail` sẽ giết cả channel.)
  5. **Bug phát hiện thêm khi sửa (4): comment count không bao giờ tăng** — `post-consumer` đọc `payload["targetId"]` cho `COMMENT_CREATED` nhưng payload thật (`InteractionService.addComment`) dùng key `articleId` → luôn `return` sớm. Khớp quan sát trước đó (bài có 9 comment vẫn hiện "0 comments"). Sửa + `InteractionEventConsumerTest` (4 test, payload lấy đúng như producer).
  6. Paging `page<0`/`size<1` trả 500 (`Page` của Panache ném `IllegalArgumentException`) → thêm `@Min` + `@Valid @BeanParam` → 400; thêm `quarkus-hibernate-validator` cho `notification-api` (trước chỉ có transitive runtime).
  Verify: `./gradlew build` 19 module xanh, 43 test pass (13 mới); FE typecheck + 32/32 vitest. **Chưa verify live** (cluster tắt) — khi bật lại cần check: (a) event vẫn chảy sau đổi sang insert+delete, bảng outbox rỗng; (b) `?size=0` → 400; (c) interaction→post-api `/internal` vẫn 200; (d) comment count tăng đúng; (e) topic `dead-letter-topic-*` được tạo khi có event lỗi. Docs đồng bộ lại: `feature.md` (path, topic, 9.2/9.4 hoá ra đã làm, thêm 6.8/6.9/9.13, tổng 57), `api-contract`, `api-standard` (tự mâu thuẫn §5/§7/§8), `service-dependencies`, `outbox-pattern` (bảng khác biệt đầu doc), `be-overview` (bỏ ForwardAuth, schema-per-service, Redis fan-out chưa làm, cảnh báo WS auth). **Chưa sửa:** WS public để lộ noti của người khác (payload có tên actor từ ADR 0006) — là quyết định thiết kế, đề xuất cách sửa ở `specs/messaging-plan.md` §11.

- [x] **Xác thực WebSocket (ADR 0009) + chat 1:1 có mã hoá (ADR 0007/0008) — `specs/messaging-plan.md` Bước 0 + Phase 1 (2/10/2026)** — branch `feature/chat-messaging`.
  - **WS auth:** `websocket-service` verify JWT (`quarkus-smallrye-jwt`, token qua `Sec-WebSocket-Protocol` → `propagate-subprotocol-headers`), `@Authenticated`, `TopicAuthorizer` chỉ cho subscribe `user_{x}_*` khi `x == jwt.subject` (+ `article_*_comment_added`), sai → `ERROR forbidden`. FE `lib/wsClient.ts`: 1 kết nối/tab, refcount topic, backoff, phát "reconnected", đóng khi logout; `useNotificationSocket`/`waitForUserReady` chuyển sang dùng nó.
  - **Backend chat:** `social-common` `Uuid7`; module mới `chat-crypto` (`ChatCipher` AES-256-GCM + AAD, `DataKeyProvider` KMS + cache 5 phút), `chat-service-dao` (schema `chat`, 5 bảng + outbox), `chat-api` (`ConversationResource`, `ChatService`, `UserRefEventConsumer`); `websocket-service` thêm `ChatEventConsumer` (`social.chat` → giải mã → push `user_{id}_chat` cho mọi participant). Danh sách conversation 1 query/trang (`JOIN LATERAL … LIMIT 1`), unread 1 query.
  - **Infra:** LocalStack `s3,kms` + init hook tạo CMK `alias/social-chat` với **id + key material cố định** (Community không persist → restart sẽ ra CMK mới, mọi DEK cũ thành rác); limit 384Mi → 768Mi (bật KMS bị OOMKilled lúc khởi động); mount init bằng `subPath` (mount cả thư mục thì LocalStack chạy cả bản trong `..data/` → chạy 2 lần). Schema `chat`, `chat.outbox` vào Debezium, route Traefik `/api/conversations` + `/openapi/chat`, `k8s/services/chat-api.yaml`, Makefile `build-chat`/`load-chat`/`up-chat`, deploy order.
  - **FE:** `/users/:id` (nút "Message", tên tác giả ở bài/comment thành link), `/messages[/:id]` (`ConversationList`, `MessageThread`, `MessageInput` zod ≤ 4.096), `ChatMenu` cạnh chuông, `useChatSocket`, link Messages ở sidebar.
  - **Verify:** backend 77 test (mới: Uuid7 4, chat-crypto 11, ChatService 11, TopicAuthorizer 5, ChatEventConsumer 3), FE typecheck + 49 vitest. **Live trên kind:** 20/20 check API+WS (không token → bị từ chối; subscribe topic người khác → forbidden; 2 phía mở ra cùng conversation; người ngoài 404; 4.097 ký tự 400; gửi lại cùng `clientMessageId` → cùng tin, không push lần 2; unread/mark read) · DB chỉ có bytea (34 byte = 18 byte UTF-8 + 16 tag), Kafka `social.chat` không có bản thường, mọi bảng outbox rỗng · restart LocalStack + chat-api rồi đọc lịch sử cũ → `kms.Decrypt` 200, giải được · UI Playwright 2 trình duyệt: profile → Message → gửi (hiện 1 lần, pending → xác nhận) → badge bên kia = 1 không cần reload → dropdown → mở thread → badge về 0 → trả lời hiện realtime. Kèm theo verify 4/5 mục còn treo của đợt audit 29/9: `?size=0`/`page=-1` → 400, `/internal` qua secret vẫn chạy (notification tạo đúng, không có warning), comment count tăng sau flush, outbox rỗng — **còn (e) DLQ chưa thử**.
  - **Sự cố build:** `make up` fail "docker load … short read" — cache layer Jib nằm trong `$TMPDIR` (`/var/folders/…/T/jib-core-application-layers-cache`), macOS dọn file cũ nhưng giữ thư mục → Jib tưởng có cache. Xoá thư mục đó là hết; sẽ tái diễn sau vài ngày không build.

- [x] **Fix nhiều tab cùng trình duyệt: tab cũ "biến" thành acc mới, và tệ hơn là gửi request nhân danh acc mới trong khi UI vẫn hiện acc cũ (3/10/2026)** — mọi tab (kể cả mọi cửa sổ ẩn danh đang mở cùng lúc) dùng chung `localStorage`; `api/client.ts` đọc `jwt` từ `localStorage` mỗi request nên tab B signup/login acc khác là tab A đổi danh tính ngay, trong khi store/cache/WS của tab A vẫn là người cũ. Dựng lại bằng Playwright (1 context, 2 page): tab A hiện "Old Acc" nhưng bài đăng từ tab A có `authorId` của acc mới. Cùng gốc: logout ở tab này, tab kia vẫn giữ WS bằng token cũ (vẫn nhận notification/chat). Sửa: (1) request và WS dùng token trong store của chính tab (`wsClient.setTokenProvider`, tránh import vòng), `localStorage` chỉ để giữ phiên qua reload; (2) `authStore` nghe sự kiện `storage`: tab khác logout → logout theo + `/login`; tab khác là **user khác** (so `sub`) → reload `/` (dọn sạch store/cache/WS một lần, không dọn từng phần); cùng user đăng nhập lại → chỉ nhận token mới. Race phát hiện khi rà: tab A reload sang acc vừa signup có thể gặp `/me` 404 tạm thời (profile tạo async) → `RootLayout` logout → xoá jwt dùng chung → kéo tab đang signup ra ngoài; `/me` giờ retry 404 tới ~8s (như `waitForUserReady`) trước khi coi là session ma. Verify: typecheck, 54 vitest (+5 test `syncWithOtherTab`), Playwright: đổi acc ở tab khác → tab A tự reload thành acc mới và đăng bài đúng acc đó; logout ở tab khác → tab A về `/login`; signup thật bằng UI ở tab B khi tab A đang mở → cả 2 tab thành acc mới, không tab nào bị đá ra.

### Ưu tiên cao
- [x] **Spin up kind cluster** — apply toàn bộ k8s manifests, test happy path (2/10/2026, `make up`, 10 service + Debezium Ready)
- [ ] **Commit frontend** — add tất cả untracked frontend files vào git

### Ưu tiên trung bình
- [ ] **Config AWS SDK đúng cho môi trường thật** — chat-api/websocket-service đang ép `quarkus.kms.endpoint-override` + static creds `test` ở mọi profile (lên AWS sẽ gọi `localhost:4566`); post-api ngược lại: S3 override chỉ ở `%dev` nên trong kind có thể không dùng LocalStack (chưa verify upload ảnh). Hướng sửa: default đúng cho AWS, LocalStack khai qua env `QUARKUS_*_ENDPOINT_OVERRIDE` + `AWS_ACCESS_KEY_ID` trong manifest kind; thêm `software.amazon.awssdk:sts` nếu dùng IRSA; ghi mục vận hành KMS trên AWS vào ADR 0007.
- [ ] **Frontend: wire API thật** — kiểm tra từng page gọi đúng endpoint
- [ ] **Frontend: loading/error states** — skeleton cards, error toast (Sonner)
- [ ] **Frontend: inline comments** — thay modal bằng inline trong card
- [ ] **Frontend: like/vote** — optimistic update + rollback
- [ ] **Frontend: responsive mobile** — bottom nav bar

### Ưu tiên thấp
- [ ] Refresh token endpoint (`POST /api/auth/refresh`)
- [ ] Delete image on S3 khi xóa bài
- [ ] Upload avatar
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
