# K8s Getting Started — từ lúc vừa `git clone`

Hướng dẫn đưa toàn bộ stack (6 service REST/WS + 3 consumer, Kafka, Debezium,
Postgres, Redis, LocalStack, LGTM, Traefik) chạy trên **kind** (Kubernetes
local qua Docker), dùng `Makefile` có sẵn trong repo thay vì gõ tay `kubectl`.

> **Đọc trước khi bắt đầu:** luồng này **chưa từng được verify end-to-end**
> trên máy nào (xem `specs/decisions/0001-*.md` → `0004-*.md` và mục "Tình
> trạng thực tế" cuối file). Tất cả 19 Gradle module (kể cả `post-api`) build
> được, image build đúng kiến trúc host (arm64/amd64 tự detect), nhưng chưa
> ai thật sự deploy + chạy thử trên kind.

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

⚠️ **Bắt buộc chạy bước này ở đúng vị trí — sau bước 5, trước bước 7.**
`k8s-secrets` **không nằm trong `make deploy` hay `make up`** — đây là gap có
thật trong `Makefile`, không phải bạn đọc thiếu bước. Bỏ qua bước này thì pod
`auth-service`/`post-api` (mount secret `jwt-keys` vào `/etc/jwt`) sẽ đứng ở
`CreateContainerConfigError` vì secret chưa tồn tại — `kubectl apply` của
bước 7 vẫn "thành công" (exit code 0), chỉ pod là fail âm thầm.

Target đọc `services/dev-private.pem` + `dev-public.pem` (bước 1) → tạo
Secret `jwt-keys` (`--dry-run=client | apply`, chạy lại vô hại).

---

## 7. Deploy services

```bash
make deploy-services
```

Apply toàn bộ `k8s/services/*.yaml`, kể cả `post-api.yaml` — image đã build +
load đúng kiến trúc ở bước 3-4 nên pod khởi động bình thường như các service
khác (không còn giới hạn build như trước `specs/decisions/0004-*.md`). Xem
tiến độ:

```bash
make status
```

Nếu pod nào `CrashLoopBackOff`/`CreateContainerConfigError`, xem log:

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
make up-post      # build-post + load-post (build + load image mới)
```

⚠️ **Phần "restart" của `up-%` không hoạt động đúng — bug có thật trong
`Makefile`, đã verify tĩnh, không phải hiểu nhầm.** `up-post` set stem
`%=post` rồi chạy `kubectl rollout restart deployment/post` — nhưng deployment
thật tên là `post-api`/`post-consumer`, không phải `post`. Lệnh restart fail,
bị `2>/dev/null || true` nuốt lỗi, rồi vẫn in `✅ post updated` như thành
công. Tương tự cho `up-auth`, `up-user`, ... (không stem nào khớp tên
deployment thật). Nên sau `make up-<x>`, tự restart đúng tên:

```bash
make restart-post-api restart-post-consumer   # đúng tên deployment, target restart-% không có bug này
# hoặc chắc ăn nhất, restart hết:
make restart
```

Không cần lặp lại toàn bộ 9 bước ở trên cho 1 thay đổi nhỏ, chỉ cần build +
load + restart đúng deployment.

`make up` (= `build` + `load` + `deploy` toàn bộ, không gồm `k8s-secrets`) chỉ
dùng được cho lần đầu **nếu đã chạy `make k8s-secrets` trước đó ít nhất 1
lần** (secret nằm trong cluster, còn nguyên qua các lần `deploy` sau).

---

## 10. Verify happy path

```bash
# Đăng ký + lấy JWT
curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@test.com","password":"Passw0rd!"}'

# Swagger UI — test API bằng tay (specs/decisions/0001-*.md)
open http://localhost:8080/q/swagger-ui

# Traefik dashboard
open http://localhost:9090

# Grafana (LGTM)
make grafana   # http://localhost:3000, admin/admin — chiếm terminal, Ctrl+C để thoát
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

- **Thiếu secret `jwt-keys`** (quên chạy `make k8s-secrets` ở bước 6 trước
  bước 7 — xem cảnh báo ở bước 6) → `:pods` → tìm pod đang
  `CreateContainerConfigError` (màu vàng/đỏ) → `Enter` → `d` (describe) →
  phần `Events` cuối cùng sẽ ghi rõ `secret "jwt-keys" not found`.
- **`CrashLoopBackOff`** → chọn đúng pod → `l` → xem log ngay trước lúc
  container chết (thường là exception lúc `quarkusBuild` app khởi động,
  vd sai `DB_URL`, JWT key lỗi).
- **Kiểm tra pod đang chạy image/tag nào** (hữu ích sau khi đổi `ARCH` build
  arm64/amd64) → chọn pod → `Enter` → xem cột `Image` của container.
- **`post-api`/`post-consumer` không thấy trong `:deploy`** → nhắc lại bug
  ở mục "Từ lần 2 trở đi": `up-post` build/load đúng nhưng KHÔNG tạo
  Deployment lần đầu — Deployment chỉ được tạo bởi `make deploy-services`
  (bước 7), `up-post` chỉ dùng để update sau khi đã deploy ít nhất 1 lần.

---

## Shutdown an toàn

**Trước tiên — điểm quan trọng nhất:** không manifest infra nào trong
`k8s/infra/` (`postgres.yaml`, `kafka.yaml`, `redis.yaml`, `localstack.yaml`)
có `PersistentVolumeClaim`/`emptyDir`/`hostPath` — đã `grep` xác nhận. Nghĩa
là **không có data nào tồn tại qua pod restart cả**, dù bạn tắt kiểu gì.
"An toàn" ở đây là "không đụng gì ngoài phạm vi cluster `social`", không
phải "bảo toàn dữ liệu" — vì vốn dĩ chẳng có gì để giữ.

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
make cluster-delete
```
Chạy `kind delete cluster --name social` — chỉ xoá đúng container/network
gắn với cluster tên `social` (kind namespace hoá theo tên cluster), không
đụng cluster kind khác hay resource Docker không liên quan. Xoá luôn Traefik,
mọi image đã `kind load` vào node. **Image trên Docker local (`docker images`)
không bị xoá** — xem lưu ý dangling image ở phần trước nếu build lại nhiều
lần rồi muốn dọn `docker image prune`.

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
- **`k8s-secrets` không nằm trong `deploy`/`up`** — xem cảnh báo ở bước 6.
  Đây là gap thật trong `Makefile`, chưa fix (chưa được yêu cầu fix), chỉ
  mới ghi lại ở đây để không ai bị bất ngờ.
- **`up-%` restart sai deployment, fail âm thầm** — `up-post`/`up-auth`/...
  chạy `kubectl rollout restart deployment/<stem>` nhưng deployment thật tên
  dài hơn (`post-api`, `auth-service`,...), không khớp. Lỗi bị
  `2>/dev/null || true` nuốt, vẫn in "✅ updated" dù không restart gì cả. Xem
  cách lách ở mục "Từ lần 2 trở đi". Chưa fix Makefile, chưa có ADR (build
  tooling bug, không phải quyết định kiến trúc).
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
