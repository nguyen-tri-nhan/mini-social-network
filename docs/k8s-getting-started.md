# K8s Getting Started — từ lúc vừa `git clone`

Hướng dẫn đưa toàn bộ stack (6 service REST/WS + 3 consumer, Kafka, Debezium,
Postgres, Redis, LocalStack, LGTM, Traefik) chạy trên **kind** (Kubernetes
local qua Docker), dùng `Makefile` có sẵn trong repo thay vì gõ tay `kubectl`.

> **Đọc trước khi bắt đầu:** luồng này đang được verify dần trên kind thật
> (xem `specs/decisions/0001-*.md` → `0004-*.md` và mục "Tình trạng thực tế"
> cuối file) — build + infra layer (Postgres/Redis/Kafka) đã chạy thật, tìm
> và fix nhiều bug thật trong lúc verify (không phải đọc code suy đoán).
> **Chưa verify hết**: Debezium connector, outbox → Kafka → consumer
> end-to-end, upload ảnh qua LocalStack trên k8s.

Chạy `make help` bất cứ lúc nào để xem toàn bộ target có sẵn (mọi target có
comment `##` phía trên đều hiện ra ở đây).

---

## 0. Prerequisites

| Công cụ | Bắt buộc | Cài (macOS) |
|---|---|---|
| Docker Desktop (đang chạy) | ✅ | — |
| Java 21 | ✅ | `brew install openjdk@21` |
| kind | ✅ | `brew install kind` |
| kubectl | ✅ | `brew install kubectl` |
| helm | ✅ (`make cluster-create` dùng để cài Traefik) | `brew install helm` |
| **Gradle global** | ✅ | `brew install gradle` hoặc `sdk install gradle` |

⚠️ Khác với hướng dẫn chạy `quarkusDev` local (README), **toàn bộ target
build trong `Makefile` gọi lệnh `gradle` global, không phải `./gradlew`** —
phải có Gradle cài sẵn trên PATH. Nếu chỉ có `./gradlew`, sửa `Makefile` đổi
`gradle` → `./gradlew` (chạy trong thư mục `services/`), hoặc cài Gradle
global cho khớp file gốc.

---

## 1. Generate JWT dev key

```bash
cd services
./setup.sh
cd ..
```

Generate `dev-private.pem`/`dev-public.pem` (RS256) + copy public key vào
từng service. Dòng đầu script (`gradle wrapper --gradle-version 8.8`) chỉ để
bootstrap wrapper lần đầu — `./gradlew` (Gradle 9.5.1) đã có sẵn trong repo,
có thể bỏ qua dòng đó nếu nó lỗi.

---

## 2. Tạo kind cluster + cài Traefik

```bash
make cluster-create
```

`kind create cluster --config infra/kind-cluster.yaml` (map hostPort `8080` →
Traefik entrypoint, 1 control-plane + 2 worker) rồi `helm install traefik`
vào namespace `kube-system`.

---

## 3. Build image (JIB — không cần Dockerfile)

```bash
make build
```

Build cả 9 service/consumer 1 lệnh, tag `latest`, **tự detect kiến trúc host**
(arm64 trên Apple Silicon, amd64 nơi khác — xem biến `ARCH` đầu `Makefile`,
override bằng `make build ARCH=linux/amd64` nếu cần build khác kiến trúc máy
đang chạy). Muốn build lẻ từng nhóm: `make build-auth`, `make build-user`,
`make build-post`, `make build-interaction`, `make build-notification`,
`make build-websocket`.

---

## 4. Load image vào kind

```bash
make load
```

kind không tự pull image từ Docker local, phải load thủ công — target này
loop qua cả 9 image. Load lẻ: `make load-auth`, `make load-user`, ...

---

## 5. Deploy infra (Postgres, Redis, Kafka, LocalStack, LGTM)

```bash
make deploy-infra
```

Apply `k8s/namespaces.yaml` (tạo namespace `social` — **bước sau cần
namespace này tồn tại**) + 5 manifest trong `k8s/infra/`, rồi đợi
Postgres/Kafka `rollout status` xong (timeout 60-90s).

---

## 6. Tạo JWT secret trong cluster

```bash
make k8s-secrets
```

Target đọc `services/dev-private.pem` + `dev-public.pem` (bước 1) → tạo
Secret `jwt-keys` (`--dry-run=client | apply`, chạy lại vô hại). `make deploy`
và `make up` giờ tự chạy bước này đúng thứ tự (sau `deploy-infra`, trước
`deploy-services`) — chạy tay riêng như trên chỉ cần khi muốn rotate key
hoặc chạy `deploy-services` độc lập.

---

## 7. Deploy services

```bash
make deploy-services-sequential
```

Deploy **lần lượt từng service theo đúng thứ tự phụ thuộc** (xem
`specs/service-dependencies.md`), đợi mỗi service `Ready` mới sang cái tiếp
theo — **khuyên dùng thay `make deploy-services`** (apply cả 9 cùng lúc).
Trên máy resource hạn chế (vd Docker Desktop cấp <8GB), 9 JVM cold-start đồng
thời gây CPU/memory storm thật sự, dẫn tới Postgres connection timeout hàng
loạt dù code không có bug gì — đã gặp trực tiếp trong quá trình viết guide
này. `make deploy` / `make up` đã tự dùng bản tuần tự này, không cần gọi tay
trừ khi muốn chạy lại riêng bước 7.

Muốn apply song song kiểu cũ (máy khoẻ, không ngại storm): `make deploy-
services`.

Xem tiến độ trong lúc script chạy (terminal khác):

```bash
make status
```

Nếu pod nào `CrashLoopBackOff`/`CreateContainerConfigError` sau khi script
báo xong, xem log:

```bash
make logs-auth-service      # hoặc logs-post-api, logs-user-api, ...
```

---

## 8. Deploy Debezium + đăng ký connector

```bash
make deploy-debezium
```

Deploy `kafka-connect`, đợi ready, rồi 1 k8s `Job` (`register-debezium-
connector`) tự động `curl` đăng ký PostgreSQL connector (outbox tables:
`interaction.outbox`, `post.outbox`, `auth.outbox`) — **không cần chạy curl
thủ công** như README mô tả (bước 4 README trỏ tới file
`k8s/infra/debezium-connector.json` — file này **không tồn tại**, payload
thật nằm inline trong `k8s/infra/debezium.yaml`; README đang sai chỗ này,
chưa sửa).

Verify connector đã đăng ký:

```bash
kubectl port-forward -n social svc/kafka-connect 8083:8083 &
curl -s localhost:8083/connectors/social-outbox-connector/status
```

---

## 9. Route Traefik

```bash
make deploy-routes
```

Apply `k8s/traefik/ingressroute.yaml` — route `/api/*`, `/ws`, và (theo
`specs/decisions/0001-*.md`) `/q/*` + `/openapi/*` cho Swagger UI.

---

## Từ lần 2 trở đi: rebuild nhanh 1 service

Sau khi cluster đã lên, sửa code 1 service rồi muốn update:

```bash
make up-post      # build-post + load-post + restart đúng post-api, post-consumer
```

`up-%` dùng bảng `DEPLOYS_<nhóm>` trong `Makefile` để restart đúng tên
deployment thật (1 nhóm có thể ứng nhiều deployment, vd `post` →
`post-api` + `post-consumer`). Tương tự `up-auth`, `up-user`,
`up-interaction`, `up-notification`, `up-websocket`. Không cần lặp lại toàn
bộ 9 bước ở trên cho 1 thay đổi nhỏ.

`make up` (= `build` + `load` + `deploy` toàn bộ, đã gồm `k8s-secrets` đúng
thứ tự) dùng được ngay từ lần đầu, không cần chạy `k8s-secrets` riêng trước.

⚠️ **`make up-<x>` (hoặc bất kỳ `./gradlew build`/`make build-*` nào) cũng gây
lag y hệt tình huống ở bước 7, dù chỉ rebuild 1 service** — gặp trực tiếp
trong lúc viết guide này. Nguyên nhân: build Gradle/JIB chạy trên **host**,
không phải trong pod, nhưng vẫn dùng chung 1 Docker Desktop VM với toàn bộ
kind cluster (không tách biệt tài nguyên) — build tốn CPU nặng, cạnh tranh
trực tiếp với các pod đang chạy. Triệu chứng: `k9s` đơ, `kubectl`/`docker
stats` timeout ("TLS handshake timeout"). Cách xử lý:
- **Đừng chạy build khi cluster đang tự hồi phục** (vừa restart nhiều pod,
  đang crash-loop) — đợi `kubectl get pods` ổn định rồi mới rebuild tiếp.
- Nếu đang đơ thật và cần giải phóng ngay: dừng build đang chạy (`Ctrl+C`
  nếu chạy foreground, hoặc kill process gradle/java nếu chạy nền), đợi vài
  chục giây rồi thử lại `kubectl get pods`.
- Về lâu dài: tăng resource Docker Desktop (Settings → Resources) nếu máy
  còn dư — không bắt buộc (steady-state vẫn chạy được với cấu hình nhỏ, đã
  verify), nhưng giảm hẳn tần suất gặp tình huống này khi vừa build vừa dev.

---

## 10. Verify happy path

```bash
# Đăng ký + lấy JWT
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@test.com","password":"Passw0rd!"}'

# Swagger UI — test API bằng tay (specs/decisions/0001-*.md)
open http://localhost:8080/q/swagger-ui

# Traefik dashboard — 9090 KHÔNG map ra host qua kind (chỉ 8080), phải port-forward
make traefik-dashboard   # http://localhost:9090/dashboard/ — chiếm terminal, Ctrl+C để thoát

# Grafana (LGTM)
make grafana   # http://localhost:3000, admin/admin — chiếm terminal, Ctrl+C để thoát

# Kafdrop — xem topic/partition/message, test produce thủ công
make kafdrop   # http://localhost:9000 — chiếm terminal, Ctrl+C để thoát
```

`make dev-token` là tiện ích lấy JWT nhanh, nhưng hardcode gọi
`localhost:8081/dev/token` — chỉ dùng được khi `auth-service` chạy **local**
qua `quarkusDev` (README flow), không áp dụng trực tiếp cho luồng kind này
trừ khi tự `kubectl port-forward svc/auth-service 8081:8080` trước.

---

## Xem & debug cluster bằng k9s

TUI xem/debug cluster nhanh, không cần deploy gì thêm vào `social` (chỉ đọc
kubeconfig hiện có). Chọn k9s thay vì Kubernetes Dashboard chính thức —
project đó đã bị archive từ 1/2026, không còn maintain, tự README của họ
cũng khuyên chuyển sang [Headlamp](https://headlamp.dev/) nếu cần GUI.

### Cài đặt

```bash
brew install k9s
```

### Mở đúng namespace của project

```bash
k9s -n social
```

Mặc định vào thẳng view `pods` trong namespace `social` — không phải gõ
`kubectl -n social get pods` lặp lại nữa.

### Điều hướng cơ bản

| Phím | Chức năng |
|---|---|
| `:pods`, `:deploy`, `:svc`, `:ns` | Chuyển resource view (gõ `:` + tên, Enter) |
| `↑`/`↓` hoặc `j`/`k` | Di chuyển |
| `Enter` | Vào chi tiết (pod → xem container bên trong) |
| `l` | Xem log (live tail) của pod đang chọn |
| `d` | Describe — xem `Events` cuối, chỗ đầu tiên nên nhìn khi pod đỏ/vàng |
| `s` | Shell/exec vào container |
| `/` | Filter theo tên (fuzzy) |
| `Esc` | Quay lại view trước |
| `?` | Help đầy đủ |
| `Ctrl+c` hoặc `:q` | Thoát |

### Áp vào đúng các lỗi đã biết trong hướng dẫn này

- **Thiếu secret `jwt-keys`** (chỉ xảy ra nếu chạy `deploy-services` tay,
  tách rời khỏi `make deploy`/`make up` — 2 target đó giờ tự lo thứ tự đúng)
  → `:pods` → tìm pod đang `CreateContainerConfigError` (màu vàng/đỏ) →
  `Enter` → `d` (describe) → phần `Events` cuối cùng sẽ ghi rõ
  `secret "jwt-keys" not found`.
- **`CrashLoopBackOff`** → chọn đúng pod → `l` → xem log ngay trước lúc
  container chết (thường là exception lúc `quarkusBuild` app khởi động,
  vd sai `DB_URL`, JWT key lỗi).
- **Kiểm tra pod đang chạy image/tag nào** (hữu ích sau khi đổi `ARCH` build
  arm64/amd64) → chọn pod → `Enter` → xem cột `Image` của container.
- **`post-api`/`post-consumer` không thấy trong `:deploy`** → `up-post` chỉ
  build+load+restart, không tự tạo Deployment mới — Deployment chỉ được tạo
  bởi `make deploy-services` (bước 7). Chạy bước 7 ít nhất 1 lần trước khi
  dùng `up-<x>` để update.

---

## Shutdown an toàn

**Trước tiên — điểm quan trọng nhất:** không manifest infra nào trong
`k8s/infra/` (`postgres.yaml`, `kafka.yaml`, `redis.yaml`, `localstack.yaml`)
có `PersistentVolumeClaim`/`emptyDir`/`hostPath` — đã `grep` xác nhận. Nghĩa
là **không có data nào tồn tại qua pod restart cả**, dù bạn tắt kiểu gì.
"An toàn" ở đây là "không đụng gì ngoài phạm vi cluster `social`", không
phải "bảo toàn dữ liệu" — vì vốn dĩ chẳng có gì để giữ.

Cụ thể với Postgres — hay bị hỏi nhất: data thật (`/var/lib/postgresql/data`)
nằm ở **writable layer của container**, không phải volume riêng. `postgres.
yaml` chỉ mount 1 `ConfigMap` (`init-schemas.sql`, tạo schema rỗng lúc khởi
động lần đầu) — không mount gì cho thư mục data. Container Postgres biến mất
vì bất kỳ lý do gì (`make shutdown`, xoá pod, restart do OOM/crash) → **data
mất sạch**, lần sau lên lại chỉ có schema rỗng, không phải data cũ.

Đây là lựa chọn cố ý cho giai đoạn hiện tại (dev cluster chạy tạm để test,
không phải nơi giữ data thật). Nếu sau này cần data sống qua restart (test
data quan trọng, hoặc chuẩn bị lên production), cần thêm `PersistentVolume-
Claim` + `StorageClass` cho Postgres — đó là quyết định kiến trúc mới, cần
ghi ADR khi làm (`specs/decisions/`), hiện chưa có gì.

Trước khi tắt: `Ctrl+C` mọi `kubectl port-forward` hoặc `make grafana` đang
chạy nền ở terminal khác — không bắt buộc (process đó chỉ tự lỗi khi cluster
mất kết nối), nhưng dọn cho sạch process orphan.

Chọn 1 trong 3 mức tuỳ ý định:

**1) Tạm dừng, giữ cluster + Traefik, dọn app** — nhanh dựng lại nhất:
```bash
kubectl delete namespace social
```
Xoá toàn bộ Postgres/Kafka/Redis/LocalStack/service/IngressRoute (đều nằm
trong namespace `social`), **giữ nguyên kind cluster + Traefik controller**
(cài ở `kube-system`, ngoài namespace `social`). Dựng lại chỉ cần chạy lại
từ bước 5 (`make deploy-infra`) — bỏ qua bước 1-2 (đã có key + cluster rồi).

**2) Xoá cluster hoàn toàn** — dọn sạch nhất, khuyến nghị nếu thật sự xong việc:
```bash
make shutdown
```
Wrap quanh `scripts/shutdown-k8s.sh`: kiểm tra cluster `social` có tồn tại
không (tránh lỗi khó hiểu nếu đã xoá rồi), tự dừng mọi `kubectl port-forward`
chạy nền của project (`make grafana`/`make kafdrop`/port-forward
kafka-connect...) trước khi xoá, **hỏi xác nhận trước khi xoá thật** (bỏ qua
bằng `scripts/shutdown-k8s.sh -y` nếu cần chạy không tương tác), rồi mới chạy
`kind delete cluster --name social` — chỉ xoá đúng container/network gắn với
cluster tên `social`, không đụng cluster kind khác hay resource Docker không
liên quan. Xoá luôn Traefik, mọi image đã `kind load` vào node. **Image trên
Docker local (`docker images`) không bị xoá** — xem lưu ý dangling image ở
phần trước nếu build lại nhiều lần rồi muốn dọn `docker image prune`.

(`make cluster-delete` vẫn còn — chạy thẳng `kind delete cluster`, không hỏi
xác nhận, không dọn port-forward. Dùng khi gọi từ script/CI khác cần
non-interactive; `make shutdown` là lựa chọn khuyên dùng khi tự tay chạy.)

**3) Chỉ tắt Docker Desktop** — nhẹ nhất, không xoá gì, chỉ dừng:
Kind cluster là container Docker thường — tắt Docker Desktop = dừng toàn bộ
(cluster "đứng hình", không mất), mở lại Docker Desktop là cluster chạy tiếp
y nguyên trạng thái cũ. Không giải phóng dung lượng đĩa Docker VM đang chiếm,
chỉ dùng khi biết chắc sẽ quay lại sớm và không muốn dựng lại từ đầu.

Với dự án này (không có data cần giữ), **cách (2) là lựa chọn mặc định hợp
lý nhất** khi thật sự muốn "tắt hết" — không có lý do kỹ thuật nào để chọn
(1) hay (3) trừ khi muốn tiết kiệm thời gian dựng lại.

---

## Tình trạng thực tế (đọc kỹ trước khi báo cáo "không chạy được")

- **Traefik chưa từng thật sự lên được — `make cluster-create` cài fail âm
  thầm, đã fix, verify bằng cluster sống.** `helm status` cho thấy release ở
  `STATUS: failed` ngay từ lần cài đầu tiên: chart có port nội bộ `traefik`
  (dashboard/API) mặc định CŨNG `containerPort: 8080`, đụng thẳng port `web`
  mình set 8080 → Helm reject vì trùng containerPort, không pod nào lên,
  `curl localhost:8080` không phản hồi. Đã fix 3 việc trong `Makefile`:
  1. Dời dashboard sang `ports.traefik.port=9090` (đúng ý định ban đầu, hết
     đụng port với `web`)
  2. Ghim Traefik vào đúng node `social-control-plane` (`nodeSelector` +
     `toleration` — `extraPortMappings` của kind chỉ forward host:8080 tới
     đúng node đó, Traefik lỡ bị xếp lịch qua worker thì host:8080 không ai
     lắng nghe — đã gặp trực tiếp, verify bằng `docker port`)
  3. `ingressRoute.dashboard.enabled=true` — mặc định `false`, thiếu dòng
     này thì `/dashboard/` luôn 404 dù pod đã chạy đúng chỗ
  4. Đổi `helm install` → `helm upgrade --install` để idempotent, chạy lại
     được nếu release cũ kẹt ở trạng thái `failed`

  Verify thật: `curl localhost:8080/api/auth/signup` → `405` (đúng, route
  chỉ nhận POST — xác nhận route qua Traefik tới `auth-service` hoạt động),
  `make traefik-dashboard` → `curl .../dashboard/` → `200`.
- **`post-api` từng không build được, đã fix** — lịch sử: `quarkus-amazon-
  services-bom` gây lỗi "different platform streams" với `quarkus-bom:3.25.1`
  (`0001-*.md`, `0002-*.md`); thử đổi groupId BOM thất bại (`0003-*.md`);
  nguyên nhân gốc là `post-api` tự khai platform BOM trực tiếp một cách thừa
  (post-service đã khai + export transitive rồi) — xoá dòng thừa là xong
  (`0004-*.md`). `./gradlew build` hiện tại **19/19 module pass**.
- **Image build ra đúng kiến trúc host** — trước đây JIB mặc định build
  `linux/amd64` bất kể máy chạy arm64 (Apple Silicon), khiến container chạy
  qua emulation (chậm, không crash). `Makefile` đã thêm auto-detect
  `ARCH`/`quarkus.jib.platforms`, verify bằng `docker inspect` cho ra đúng
  `arm64` trên máy dev hiện tại. Không có ADR riêng — đây là build-config
  fix, không phải quyết định kiến trúc/schema/API.
- **`k8s-secrets` không nằm trong `deploy`/`up` — đã fix.** Giờ `deploy:
  deploy-infra k8s-secrets deploy-services deploy-debezium deploy-routes`,
  thứ tự đúng tự động, không cần chạy tay riêng nữa (trừ khi muốn rotate key).
- **`up-%` restart sai deployment — đã fix.** Thêm bảng `DEPLOYS_<nhóm>`
  trong `Makefile`, restart đúng tên deployment thật cho từng nhóm (kể cả
  nhóm ứng nhiều deployment như `post`, `user`, `notification`).
- **Đã thêm `make shutdown`** (`scripts/shutdown-k8s.sh`) — xoá cluster có
  xác nhận + tự dọn port-forward chạy nền, khuyên dùng thay `make
  cluster-delete` khi tự tay chạy. Xem mục "Shutdown an toàn".
- **Kafka pod đã chạy được thật — verify bằng cluster sống, không chỉ đọc
  log.** Fix 5 bug liên tiếp trong `k8s/infra/kafka.yaml`:
  1. Giá trị env chứa dấu phẩy trong YAML flow-style `{ }` bị cắt cụt vì
     thiếu quote (`KAFKA_PROCESS_ROLES` v.v.)
  2. `enableServiceLinks` mặc định của k8s tiêm biến `KAFKA_PORT` (trùng tên
     Service `kafka`) đụng biến deprecated-fatal của chính image
  3. Service `kafka` thiếu port 9093 (CONTROLLER)
  4. `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093` (qua Service) dính
     **hairpin NAT** trên kindnetd (pod tự gọi chính nó qua ClusterIP) —
     đổi sang `1@localhost:9093`
  5. `readinessProbe` dùng `kafka-topics --list` **cũng dính hairpin NAT**
     y hệt (client luôn redirect sang `advertised.listeners` sau bootstrap,
     kể cả khi bootstrap bằng `localhost`) — đổi sang `tcpSocket: port 9092`

  **Verify thật trên cluster sống**: `kubectl rollout status` →
  `successfully rolled out`, `kubectl get pods` → `1/1 Running` cả Kafka lẫn
  Kafdrop, gọi thẳng Kafdrop REST API qua port-forward → thấy `broker/1` —
  xác nhận Kafdrop connect được Kafka thật, không phải suy đoán từ log.
  Chưa verify tiếp outbox→Debezium→Kafka→consumer end-to-end.
- **Đã thêm Kafdrop 4.3.0** (`k8s/infra/kafdrop.yaml`, `make kafdrop`) — Kafka
  Web UI để xem topic/message, test produce thủ công khi cần, port-forward
  không qua Traefik.
- **k8s có thể còn 1 bug khác chưa fix, liên quan S3/LocalStack**: property
  `quarkus.s3.endpoint-override` trong `post-api/application.properties` chỉ
  scope `%dev.` — dù `k8s/services/post-api.yaml` set env `S3_ENDPOINT=
  http://localstack:4566`, ở prod profile property này không được áp dụng,
  S3 client có thể default sang AWS thật thay vì LocalStack trong cluster.
  Cách fix đã biết (đổi tên env var manifest thành
  `QUARKUS_S3_ENDPOINT_OVERRIDE` — runtime property, không cần rebuild image)
  nhưng **chưa áp dụng**, chưa có ADR.
- **Toàn bộ luồng trên chưa từng chạy thật end-to-end trên kind** — viết lại
  từ `Makefile` + k8s manifest + code đọc trực tiếp và build-verify từng
  phần, không phải từ 1 lần deploy thành công đã verify. Nếu gặp lỗi ở bước
  nào, đó là thông tin hữu ích cần ghi lại (ADR hoặc cập nhật `tracking.md`),
  không phải bạn làm sai.
- **README.md "Running Locally"** mô tả 1 luồng KHÁC (docker-compose +
  `quarkusDev` trên host, không qua kind) — 2 luồng không tương thích nhau,
  đừng trộn. Luồng docker-compose cũng thiếu Debezium/kafka-connect trong
  `infra/docker-compose.dev.yml`, nên outbox pattern chỉ test được qua kind
  (luồng trong file này), không test được qua docker-compose.
