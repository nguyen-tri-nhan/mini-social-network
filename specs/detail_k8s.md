# Kubernetes — Chi tiết cấu hình & Lý do

---

## 1. Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: social
```

**Namespace là gì?**
Namespace là một "folder ảo" bên trong cluster. Mọi resource (Pod, Service, Secret...) đều thuộc về một namespace.

**Tại sao cần namespace `social`?**

| Không có namespace | Có namespace |
|---|---|
| Tất cả resource nằm chung `default` | Resource tách biệt theo domain |
| Khó phân biệt resource của project nào | `kubectl get pods -n social` chỉ thấy của mình |
| Xoá nhầm resource của app khác | `kubectl delete namespace social` xoá sạch 1 lần |

Trong project này, toàn bộ app — kể cả Kafka — đều chạy trong namespace `social`. Tất cả các biến env đều dùng tên ngắn:

```
# Cùng namespace social → tên ngắn cho tất cả
postgres:5432   ✅
redis:6379      ✅
kafka:9092      ✅
```

---

## 2. Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: post-api
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: post-api }
  template:
    metadata:
      labels: { app: post-api }
    spec:
      containers:
        - name: post-api
          image: nhan/post-api:latest
```

**Tại sao không tạo Pod trực tiếp?**

Pod là đơn vị chạy nhỏ nhất trong k8s. Nhưng Pod "chết là chết" — k8s không tự restart nó.

Deployment là controller bọc ngoài Pod, đảm bảo luôn có đúng số `replicas` Pod đang chạy:

```
Deployment (replicas: 1)
    │
    └─► Pod (post-api-7d9f8...)   ← nếu Pod này crash
              ↓ k8s phát hiện
    └─► Pod (post-api-4bc2a...)   ← Deployment tạo Pod mới thay thế
```

Deployment còn quản lý **rolling update**: khi deploy image mới, k8s start Pod mới trước, rồi mới kill Pod cũ — zero downtime.

**`replicas: 1` cho local dev** — trên production sẽ tăng lên 2-3 để high availability.

---

## 3. Service — Service Discovery & Load Balancing

```yaml
apiVersion: v1
kind: Service
metadata:
  name: post-service
  namespace: social
spec:
  selector: { app: post-api }   # ← match label của Pod
  ports:
    - port: 8080
```

**Vấn đề:** Pod có IP động — mỗi lần restart IP thay đổi. Service khác không thể hardcode IP của Pod.

**Service giải quyết bằng DNS ổn định:**

```
post-service.social.svc.cluster.local:8080
     ↑           ↑         ↑
  tên Service  namespace  suffix cố định của k8s
```

Traefik dùng tên Service này để route request. Các service gọi nhau cũng dùng tên này.

```
Traefik → "post-service:8080"
              ↓ k8s DNS resolve
         Service post-service
              ↓ selector match app=post-api
         Pod post-api-7d9f8... :8080
```

**Service type:**

| Type | Dùng khi |
|---|---|
| `ClusterIP` (default) | Internal — chỉ traffic trong cluster. Tất cả app service của mình dùng type này. |
| `NodePort` | Expose ra ngoài qua port của Node. Traefik dùng type này để nhận traffic từ host. |
| `LoadBalancer` | Cloud provider tạo LB thật (AWS ALB...). Dùng trên EKS prod. |

**Tại sao Service `post-service` nhưng Deployment tên `post-api`?**

Service name phải match tên khai báo trong `ingressroute.yaml`. Deployment name là nội bộ. Selector (`app: post-api`) là cầu nối giữa hai cái.

---

## 4. imagePullPolicy

```yaml
containers:
  - name: post-api
    image: nhan/post-api:latest
    imagePullPolicy: IfNotPresent   # ← quan trọng khi dùng kind
```

**3 giá trị:**

| Value | Hành vi |
|---|---|
| `Always` | Luôn pull từ registry (kể cả image đã có). Default khi tag là `latest`. |
| `IfNotPresent` | Chỉ pull nếu chưa có image trong node. Dùng khi đã `kind load`. |
| `Never` | Không bao giờ pull — báo lỗi nếu không có image. |

**Vấn đề với kind:** kind dùng containerd riêng, không share Docker daemon của máy host. Khi build image bằng JIB, image nằm trong Docker daemon local — kind không thấy.

```
Docker daemon (host)          kind cluster (containerd)
┌──────────────────┐          ┌──────────────────────┐
│ nhan/post-api    │          │ ??? không có image   │
│ :latest          │          │                      │
└──────────────────┘          └──────────────────────┘
         │                               ↑
         └─── kind load docker-image ────┘
```

Sau `kind load`, image có trong kind's containerd. Lúc này `IfNotPresent` sẽ dùng image đó thay vì pull từ internet (không có registry nào).

Nếu để `Always`, k8s sẽ cố pull `nhan/post-api:latest` từ Docker Hub → fail vì chưa push lên đó.

---

## 5. Environment Variables — Cấu hình qua env

```yaml
env:
  - name: DB_URL
    value: jdbc:postgresql://postgres:5432/social
  - name: REDIS_URL
    value: redis://redis:6379
```

**Tại sao không hardcode trong application.properties?**

`application.properties` đặt giá trị default:
```properties
quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/social}
```

Quarkus đọc theo thứ tự ưu tiên:
```
Env var DB_URL  >  System property  >  application.properties default
```

Khi chạy trong k8s Pod, env var `DB_URL=jdbc:postgresql://postgres:5432/social` override giá trị `localhost:5432` trong file. Cùng 1 binary JAR chạy được cả local dev lẫn k8s mà không cần build lại.

**Biến môi trường per service:**

| Env var | auth | user | post | interaction | notification |
|---|---|---|---|---|---|
| `DB_URL` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `REDIS_URL` | — | ✅ | ✅ | — | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS` | ✅ | — | ✅ | ✅ | ✅ |
| `S3_ENDPOINT` | — | — | ✅ | — | — |
| `JWT_PUBLIC_KEY_LOCATION` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `SMALLRYE_JWT_SIGN_KEY_LOCATION` | ✅ | — | — | — | — |

---

## 6. Secret & Volume Mount — JWT Keys

**Vấn đề:** Private key dùng để ký JWT là sensitive data — không được commit vào git, không được để trong ConfigMap (lưu dạng plain text).

**Secret khác ConfigMap như thế nào?**

| | ConfigMap | Secret |
|---|---|---|
| Dùng cho | Config bình thường (URL, feature flags) | Sensitive data (password, private key) |
| Lưu trữ | Plain text trong etcd | Base64 encoded trong etcd (có thể encrypt at rest) |
| RBAC | Ai có quyền đọc namespace đều đọc được | Có thể restrict riêng |
| Audit log | Không | Có |

**Tạo Secret từ PEM file:**

```bash
kubectl create secret generic jwt-keys \
  --from-file=private-key.pem=services/dev-private.pem \
  --from-file=public-key.pem=services/dev-public.pem \
  -n social
```

k8s tự base64-encode nội dung file. Secret trông như thế này trong etcd:
```yaml
data:
  private-key.pem: LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQ==  # base64
  public-key.pem:  LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0K            # base64
```

**Mount vào container:**

```yaml
volumeMounts:
  - name: jwt-keys
    mountPath: /etc/jwt    # ← file sẽ xuất hiện ở đây trong container
    readOnly: true

volumes:
  - name: jwt-keys
    secret:
      secretName: jwt-keys
```

Bên trong container, Quarkus đọc:
```
/etc/jwt/private-key.pem  ← SMALLRYE_JWT_SIGN_KEY_LOCATION
/etc/jwt/public-key.pem   ← JWT_PUBLIC_KEY_LOCATION
```

**Tại sao mount file thay vì env var?**

SmallRye JWT kỳ vọng đọc file PEM — format RSA key không phù hợp với env var (multiline, special chars). Mount file là cách tự nhiên nhất.

---

## 7. ConfigMap — Postgres Init SQL

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-init
  namespace: social
data:
  init-schemas.sql: |
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE SCHEMA IF NOT EXISTS users;
    ...
```

ConfigMap dùng để inject file config hoặc script vào container. Ở đây mount SQL init vào `/docker-entrypoint-initdb.d/` — Postgres tự chạy file này khi khởi động lần đầu.

---

## 8. Health Probes

```yaml
readinessProbe:
  httpGet: { path: /q/health/ready, port: 8080 }
  initialDelaySeconds: 15
  periodSeconds: 10

livenessProbe:
  httpGet: { path: /q/health/live, port: 8080 }
  initialDelaySeconds: 30
  periodSeconds: 15
```

**Hai probe khác nhau:**

| Probe | Hỏi gì? | Hành động khi fail |
|---|---|---|
| `readinessProbe` | "Pod sẵn sàng nhận traffic chưa?" | Tạm thời remove Pod khỏi Service (không route request tới nữa) |
| `livenessProbe` | "Pod có còn sống không?" | Restart Pod |

**Tại sao cần cả hai?**

```
Tình huống 1: Pod đang khởi động (Liquibase migration chạy)
  → readinessProbe fail → Service không route traffic → user không thấy 500
  → livenessProbe pass  → Pod không bị restart

Tình huống 2: Pod bị deadlock (không crash nhưng không xử lý được request)
  → readinessProbe fail → traffic không vào
  → livenessProbe fail  → k8s restart Pod → hết deadlock

Tình huống 3: Pod healthy
  → cả hai pass → traffic đi bình thường
```

**`initialDelaySeconds`:** Quarkus cần thời gian startup (chạy Liquibase, connect DB, Kafka...). `readinessProbe` đặt 15s, `livenessProbe` đặt 30s để tránh k8s restart Pod trước khi nó kịp khởi động.

**Quarkus health endpoint:** `quarkus-smallrye-health` extension tự expose:
- `/q/health/ready` — check DB connection, Kafka connection, Redis connection
- `/q/health/live` — chỉ check app process còn chạy

---

## 9. DNS trong Cluster — Service Discovery

k8s có CoreDNS built-in. Mỗi Service tự động có DNS record:

```
<service-name>.<namespace>.svc.cluster.local
```

Trong cùng namespace `social`, các service gọi nhau bằng tên ngắn:

```
postgres       → postgres.social.svc.cluster.local:5432
redis          → redis.social.svc.cluster.local:6379
post-service   → post-service.social.svc.cluster.local:8080
```

Kafka cũng ở namespace `social` nên dùng tên ngắn như các service khác:
```
kafka.social.svc.cluster.local:9092   # full form
kafka:9092                             # short form dùng trong env var
```

---

## 10. Rolling Update — Zero Downtime Deploy

Khi chạy `make up-post` (build mới + load + restart):

```bash
kubectl rollout restart deployment/post-api -n social
```

k8s thực hiện rolling update:

```
Ban đầu:
  Pod A (old image) — nhận traffic

Sau rollout restart:
  Pod B (new image) — start, chờ readinessProbe pass
  Pod A (old image) — vẫn nhận traffic trong khi B đang khởi động

Khi B ready:
  Pod B — nhận traffic
  Pod A — bị terminate

Kết quả: không có downtime
```

Với `replicas: 1` thì vẫn có downtime ngắn vì k8s phải terminate A trước khi B ready (không đủ resource để chạy song song trong cluster nhỏ). Để zero downtime cần `replicas: 2`.

---

## 11. Toàn bộ Flow — Request đến Pod

```
curl http://localhost:8080/api/articles
        │
        ▼
kind cluster port mapping (8080 → 8080)
        │
        ▼
Traefik Pod (kube-system namespace)
  - match PathPrefix(/api/articles)
  - forward to Service: post-service:8080
        │
        ▼
k8s Service "post-service" (social namespace)
  - selector: app=post-api
  - ClusterIP + kube-proxy routing
        │
        ▼
Pod post-api-xxxx (social namespace)
  - port 8080
  - Quarkus app xử lý request
        │
        ▼
postgres:5432 / redis:6379
  (trong cùng namespace, resolve bằng CoreDNS)
```

---

## 12. Kafka — KRaft Mode

```yaml
image: confluentinc/cp-kafka:7.6.0
env:
  KAFKA_PROCESS_ROLES: broker,controller        # 1 node vừa là broker vừa là controller
  KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
  KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

**Tại sao KRaft thay vì Strimzi?**

Strimzi là Kubernetes operator phù hợp cho production (HA, nhiều broker, tự động quản lý). Nhưng với local dev cluster, Strimzi cần cài CRD + operator riêng — thêm phức tạp không cần thiết.

KRaft (Kafka Raft) cho phép Kafka tự quản lý metadata mà không cần ZooKeeper. Chạy được với 1 container duy nhất, khởi động nhanh, phù hợp cho local dev.

```
Strimzi (production)          KRaft single-node (local dev)
┌─────────────────────┐       ┌──────────────────────────┐
│ Strimzi Operator    │       │                          │
│ ZooKeeper x3        │       │  cp-kafka:7.6.0          │
│ Kafka Broker x3     │       │  (broker + controller)   │
│ Topic Operator      │       │                          │
└─────────────────────┘       └──────────────────────────┘
```

Kafka chạy trong namespace `social` — tên Service là `kafka`, cùng namespace với app → env var dùng tên ngắn `kafka:9092`.

**readinessProbe** dùng `kafka-topics --list` thay vì HTTP vì Kafka không có HTTP health endpoint.

---

## 13. Debezium — Outbox CDC

`debezium.yaml` gồm 3 resource:

```
kafka-connect  (Deployment)   ← chạy Debezium connector framework
kafka-connect  (Service)      ← expose REST API port 8083
register-debezium-connector (Job) ← chạy 1 lần để đăng ký connector
```

**Flow:**

```
PostgreSQL outbox tables
  (interaction.outbox, post.outbox, auth.outbox)
        │  pgoutput replication slot
        ▼
Debezium PostgreSQL Connector
        │  CDC event
        ▼
Kafka topic: social.post / social.interaction / social.auth
        │
        ▼
Consumer services (post-consumer, notification-consumer, user-consumer)
```

**Job `register-debezium-connector`:**

Job chỉ chạy 1 lần (`restartPolicy: OnFailure`). Dùng `initContainer` để chờ kafka-connect sẵn sàng (health check `/connectors`), sau đó POST connector config qua REST API.

Config Debezium quan trọng:

| Field | Giá trị | Ý nghĩa |
|---|---|---|
| `table.include.list` | `interaction.outbox,post.outbox,auth.outbox` | Chỉ watch 3 bảng outbox |
| `transforms.outbox.type` | `EventRouter` | Dùng Debezium Outbox Event Router transform |
| `route.by.field` | `aggregate_type` | Route event theo cột `aggregate_type` (post/interaction/auth) |
| `route.topic.replacement` | `social.${routedByValue}` | Event từ bảng post → topic `social.post` |
| `tombstones.on.delete` | `false` | Không publish tombstone message khi row bị xóa |

---

## 14. LGTM — Observability Stack

```yaml
image: grafana/otel-lgtm:0.8.1
ports:
  - 3000   # Grafana UI
  - 4317   # OTLP gRPC
  - 4318   # OTLP HTTP
```

`otel-lgtm` là image all-in-one đóng gói toàn bộ observability stack:

| Component | Vai trò |
|---|---|
| **L**oki | Log aggregation |
| **G**rafana | Dashboard & visualization |
| **T**empo | Distributed tracing |
| **M**imir | Metrics (Prometheus-compatible) |
| OpenTelemetry Collector | Thu thập OTLP từ app → phân phối vào L/T/M |

**Các service gửi telemetry qua:**
```
OTEL_EXPORTER_OTLP_ENDPOINT=http://lgtm:4317
```

Quarkus tự export traces/metrics/logs qua OTLP khi có extension `quarkus-opentelemetry`. Không cần cấu hình thêm ở app level.

**Grafana:** `GF_AUTH_ANONYMOUS_ENABLED=true` + `ORG_ROLE=Admin` → vào thẳng dashboard mà không cần login (chỉ dùng cho local dev).

Truy cập: `http://localhost:3000` (qua NodePort hoặc port-forward).

---

## 15. Consumer Services

Consumer service (post-consumer, notification-consumer, user-consumer) khác API service ở chỗ:

| | API Service | Consumer Service |
|---|---|---|
| Nhận request từ | Traefik (HTTP) | Kafka topic |
| Expose ra ngoài | Có (qua IngressRoute) | Không — không có entry trong `ingressroute.yaml` |
| Scale | Theo HTTP traffic | Theo Kafka partition count |
| Health check | `/q/health/ready` (DB + Kafka) | `/q/health/ready` (DB + Kafka) |

Consumer vẫn có `containerPort: 8080` và health probe — Quarkus expose health endpoint dù không serve request thật.

Env var của consumer bao gồm `DB_URL` + `KAFKA_BOOTSTRAP_SERVERS` (cần ghi vào DB khi xử lý event), một số có thêm `REDIS_URL` (post-consumer cần Redis để ghi counter).

---

## 16. WebSocket Service

```yaml
- match: PathPrefix(`/ws`)
  kind: Rule
  services:
    - name: websocket-service
      port: 8080
```

WebSocket route **không có** middleware `jwt-verify` trong IngressRoute — auth được xử lý ở application level bên trong websocket-service (client gửi JWT trong query param hoặc subprotocol header khi upgrade).

Traefik hỗ trợ WebSocket upgrade tự động — không cần cấu hình thêm.

---

## 17. Cấu trúc file k8s

```
k8s/
├── namespaces.yaml               # tạo namespace social trước
├── infra/
│   ├── postgres.yaml             # ConfigMap init SQL + Deployment + Service
│   ├── redis.yaml                # Deployment + Service
│   ├── kafka.yaml                # Kafka KRaft single-node + Service
│   ├── debezium.yaml             # Kafka Connect + Debezium + Job đăng ký connector
│   ├── lgtm.yaml                 # Grafana OTEL-LGTM (Loki+Grafana+Tempo+Mimir)
│   ├── localstack.yaml           # LocalStack (S3 emulator) + Service
│   └── jwt-secret.yaml           # hướng dẫn tạo Secret (không commit key)
├── services/
│   ├── auth-service.yaml         # Deployment + Service
│   ├── user-api.yaml             # Deployment + Service
│   ├── user-consumer.yaml        # Deployment (no external route)
│   ├── post-api.yaml             # Deployment + Service
│   ├── post-consumer.yaml        # Deployment (no external route)
│   ├── interaction-service.yaml  # Deployment + Service
│   ├── notification-api.yaml     # Deployment + Service
│   ├── notification-consumer.yaml# Deployment (no external route)
│   └── websocket-service.yaml    # Deployment + Service
└── traefik/
    └── ingressroute.yaml         # routing rules + jwt-verify middleware
```

**Thứ tự apply:**
```bash
kubectl apply -f k8s/namespaces.yaml    # 1. namespace trước
make k8s-secrets                         # 2. Secret (jwt-keys)
kubectl apply -f k8s/infra/             # 3. infra (postgres, redis, kafka, lgtm, localstack)
# chờ postgres + kafka ready
kubectl apply -f k8s/services/          # 4. app services (Liquibase chạy migration)
kubectl apply -f k8s/traefik/           # 5. routing (sau khi services đã up)
```

Hoặc dùng: `make deploy` (Makefile xử lý thứ tự + chờ postgres ready tự động).

---

## 18. Annotated YAML — Giải thích từng dòng

> Mỗi file YAML được chú thích `# ←` giải thích ý nghĩa từng trường. Đọc file này sau khi đã nắm lý thuyết ở các section trên.

---

### 18.1 `k8s/namespaces.yaml`

```yaml
apiVersion: v1          # ← version của k8s API cho resource này
                        #   "v1" = core API group (Namespace, Pod, Service, ConfigMap, Secret)
                        #   "apps/v1" = apps group (Deployment, ReplicaSet)
                        #   "traefik.io/v1alpha1" = CRD của Traefik

kind: Namespace         # ← loại resource muốn tạo
                        #   k8s hiểu "đây là Namespace, không phải Deployment hay Service"

metadata:               # ← metadata của resource (không phải config của app)
  name: social          # ← tên namespace, dùng trong mọi lệnh:
                        #   kubectl get pods -n social
                        #   kubectl apply -f xxx.yaml -n social
```

**Tại sao `name: social`?** Tên namespace xuất hiện trong mọi DNS service name:
```
postgres.social.svc.cluster.local
         ^^^^^^ namespace
```

---

### 18.2 `k8s/infra/postgres.yaml`

```yaml
# ── Resource 1: ConfigMap (init SQL) ─────────────────────────────────────────

apiVersion: v1
kind: ConfigMap           # ← lưu config dạng plain text (không sensitive)
                          #   Khác Secret: không có RBAC đặc biệt, hiển thị plain text
metadata:
  name: postgres-init     # ← tên ConfigMap, dùng để reference trong volumes
  namespace: social
data:
  init-schemas.sql: |     # ← key = tên file sẽ được mount vào container
                          #   value (sau dấu |) = nội dung file
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE SCHEMA IF NOT EXISTS users;
    CREATE SCHEMA IF NOT EXISTS post;
    CREATE SCHEMA IF NOT EXISTS interaction;
    CREATE SCHEMA IF NOT EXISTS notification;
    # ← Tạo 5 schemas, mỗi schema cho 1 microservice
    # Postgres chạy file này khi khởi động lần đầu
    # Sau đó Liquibase trong từng service tạo tables trong đúng schema

---

# ── Resource 2: Deployment (postgres) ────────────────────────────────────────

apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: social
spec:
  replicas: 1             # ← chỉ 1 instance (single-node, không HA)
                          #   Production: dùng RDS Multi-AZ hoặc postgres Operator

  selector:               # ← Deployment dùng selector để "nhận ra" pods của mình
    matchLabels:
      app: postgres       # ← tìm pods có label "app=postgres"

  template:               # ← mẫu để tạo pods (Deployment clone template này)
    metadata:
      labels:
        app: postgres     # ← label gán cho pod, phải match selector ở trên
                          #   Service cũng dùng label này để route traffic

    spec:                 # ← spec của Pod (containers, volumes, etc.)
      containers:
        - name: postgres  # ← tên container trong pod (một pod có thể có nhiều container)
          image: postgres:15-alpine
          # ← postgres official image, alpine = nhỏ gọn hơn (~240MB vs ~350MB)
          # ← 15 = PostgreSQL major version (15.x)

          args:
            - -c
            - wal_level=logical     # ← BẮT BUỘC cho Debezium CDC
                                    #   WAL = Write-Ahead Log (transaction log của postgres)
                                    #   "logical" = WAL có đủ info để tái tạo từng row thay đổi
                                    #   Debezium đọc WAL để phát hiện INSERT/UPDATE/DELETE
            - -c
            - max_wal_senders=4     # ← tối đa 4 replication slot connections
                                    #   Debezium cần 1 slot, để dư cho debug
            - -c
            - max_replication_slots=4  # ← tối đa 4 replication slots
                                       #   Slot = "đánh dấu" vị trí WAL để không bị xóa
                                       #   trước khi Debezium đọc xong

          env:
            - name: POSTGRES_DB
              value: social         # ← tên database mặc định được tạo
            - name: POSTGRES_USER
              value: postgres       # ← superuser, dùng cho local dev
                                    #   Production: dùng user riêng, password từ Secret
            - name: POSTGRES_PASSWORD
              value: postgres       # ← plain text! OK cho dev, KHÔNG dùng prod

          ports:
            - containerPort: 5432   # ← port postgres lắng nghe trong container
                                    #   Không phải "expose ra ngoài" — chỉ là metadata
                                    #   Service mới thực sự expose

          volumeMounts:
            - name: init-sql        # ← tham chiếu đến volume tên "init-sql" (định nghĩa bên dưới)
              mountPath: /docker-entrypoint-initdb.d
              # ← postgres image tự chạy tất cả .sql files trong thư mục này khi start lần đầu

          readinessProbe:           # ← k8s hỏi "postgres đã sẵn sàng nhận query chưa?"
            exec:
              command: [pg_isready, -U, postgres]
              # ← pg_isready = utility check postgres có accept connections không
              # ← -U postgres = user postgres
            initialDelaySeconds: 5  # ← chờ 5 giây sau khi container start mới bắt đầu probe
            periodSeconds: 5        # ← probe mỗi 5 giây

      volumes:
        - name: init-sql            # ← tên volume (match với volumeMounts.name)
          configMap:
            name: postgres-init     # ← lấy dữ liệu từ ConfigMap "postgres-init"
                                    #   k8s mount từng key của ConfigMap thành 1 file

---

# ── Resource 3: Service (postgres) ───────────────────────────────────────────

apiVersion: v1
kind: Service
metadata:
  name: postgres          # ← tên DNS: "postgres" (trong cùng namespace)
                          #   Các service gọi: jdbc:postgresql://postgres:5432/social
  namespace: social
spec:
  selector:
    app: postgres         # ← route traffic đến pods có label "app=postgres"
  ports:
    - port: 5432          # ← port của Service (bên ngoài gọi vào đây)
                          #   targetPort mặc định = port (5432)
                          #   Service nhận :5432 → forward đến pod :5432
```

---

### 18.3 `k8s/infra/redis.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: redis }   # ← shorthand YAML: {key: value} = inline map
  template:
    metadata:
      labels: { app: redis }
    spec:
      containers:
        - name: redis
          image: redis:7-alpine   # ← Redis 7.x, alpine base

          args: [--maxmemory, 256mb, --maxmemory-policy, allkeys-lru]
          # ← truyền flags vào redis-server:
          #   --maxmemory 256mb      : giới hạn RAM Redis dùng
          #   --maxmemory-policy allkeys-lru : khi đầy memory, xóa key ít dùng nhất
          #   "allkeys" = xóa bất kỳ key nào (không chỉ key có TTL)
          #   "lru" = Least Recently Used algorithm
          #
          # Các policy khác:
          #   volatile-lru  : chỉ xóa keys có TTL set
          #   allkeys-lfu   : xóa key ít truy cập nhất (LFU algorithm)
          #   noeviction    : từ chối ghi mới khi đầy (dùng cho queue, không dùng cho cache)

          ports:
            - containerPort: 6379

          readinessProbe:
            exec:
              command: [redis-cli, ping]
              # ← redis-cli ping → server trả "PONG" nếu đang chạy bình thường
            initialDelaySeconds: 5
            periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: social
spec:
  selector: { app: redis }
  ports:
    - port: 6379   # ← Redis default port
                   # ← Services dùng: REDIS_URL=redis://redis:6379
```

**Tại sao `allkeys-lru`?**

Redis trong project dùng cho 2 mục đích:
- **Cache** (user profile, article counters): OK nếu bị evict — miss → query DB
- **Counter** (article:id:comment_count): evict mất data chưa flush về DB

Với `allkeys-lru`, Redis tự quản lý memory. Nếu counter bị evict trước khi flush 30s, giá trị sẽ sai. Production nên dùng `volatile-lru` + set TTL dài cho counters.

---

### 18.4 `k8s/infra/kafka.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: kafka }
  template:
    metadata:
      labels: { app: kafka }
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:7.6.0
          # ← Confluent Platform Kafka (enterprise distribution)
          # ← Không phải Apache Kafka official image (ít feature hơn)
          # ← 7.6.0 ~ Kafka 3.6.x

          env:
            - name: KAFKA_NODE_ID
              value: "1"
              # ← ID duy nhất của broker này trong cluster
              # ← KRaft mode: mỗi node cần 1 ID khác nhau
              # ← Với 1 node duy nhất: luôn là "1"

            - name: KAFKA_PROCESS_ROLES
              value: broker,controller
              # ← KRaft mode: 1 node vừa là broker vừa là controller
              # ← broker   : nhận/ghi messages, phục vụ producers/consumers
              # ← controller : quản lý cluster metadata (thay ZooKeeper)
              # ← Trước Kafka 2.8: bắt buộc có ZooKeeper riêng
              # ← Sau Kafka 3.x: KRaft stable, không cần ZooKeeper

            - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
              value: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
              # ← Map tên listener → protocol bảo mật
              # ← CONTROLLER:PLAINTEXT = internal controller communication không mã hóa
              # ← PLAINTEXT:PLAINTEXT  = client communication không mã hóa
              # ← Production: SASL_SSL hoặc SSL thay PLAINTEXT

            - name: KAFKA_LISTENERS
              value: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
              # ← Kafka lắng nghe trên những địa chỉ nào
              # ← PLAINTEXT://0.0.0.0:9092 : nhận message từ clients (tất cả interfaces)
              # ← CONTROLLER://0.0.0.0:9093 : internal cluster communication
              # ← 0.0.0.0 = lắng nghe tất cả network interfaces của container

            - name: KAFKA_ADVERTISED_LISTENERS
              value: PLAINTEXT://kafka:9092
              # ← ĐÂY LÀ ĐỊA CHỈ KAFKA QUẢNG CÁO VỚI CLIENTS
              # ← Khi client (producer/consumer) kết nối, Kafka bảo: "hãy connect vào kafka:9092"
              # ← "kafka" = tên Service k8s → DNS resolve trong cluster
              # ← BUG THƯỜNG GẶP: dùng "localhost:9092" → chỉ work trong cùng pod
              #   Docker-compose dev dùng "localhost:9092" vì services chạy ngoài container

            - name: KAFKA_CONTROLLER_LISTENER_NAMES
              value: CONTROLLER
              # ← tên listener nào dùng cho controller communication

            - name: KAFKA_CONTROLLER_QUORUM_VOTERS
              value: 1@kafka:9093
              # ← danh sách tất cả controller nodes trong KRaft cluster
              # ← format: nodeId@host:port
              # ← "1@kafka:9093" = node ID 1, host kafka (Service DNS), port 9093
              # ← Cluster lớn hơn: "1@kafka-0:9093,2@kafka-1:9093,3@kafka-2:9093"

            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
              # ← Topic nội bộ "__consumer_offsets" lưu offset của consumers
              # ← replication factor = 1 vì chỉ có 1 broker
              # ← Production (3 brokers): replication factor = 3

            - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
              value: "true"
              # ← Kafka tự tạo topic khi producer gửi message lần đầu
              # ← Tiện cho dev, không cần tạo topic thủ công
              # ← Production: false → phải tạo topic trước với config cụ thể (retention, partitions)

            - name: CLUSTER_ID
              value: MkU3OEVBNTcwNTJENDM2Qk
              # ← ID duy nhất của KRaft cluster (base64 encoded UUID)
              # ← Phải nhất quán khi restart (không thay đổi)
              # ← Generate: kafka-storage.sh random-uuid

          ports:
            - containerPort: 9092   # ← client port

          readinessProbe:
            exec:
              command: [kafka-topics, --bootstrap-server, localhost:9092, --list]
              # ← kafka-topics --list = liệt kê tất cả topics
              # ← Nếu command thành công (exit 0) = Kafka đang chạy
              # ← Dùng localhost vì probe chạy TRONG container
              # ← Không dùng HTTP vì Kafka không có HTTP endpoint
            initialDelaySeconds: 20   # ← Kafka khởi động chậm (~15-20s)
            periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: kafka   # ← DNS name "kafka" → các services dùng KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  namespace: social
spec:
  selector: { app: kafka }
  ports:
    - port: 9092
```

---

### 18.5 `k8s/infra/localstack.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: localstack
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: localstack }
  template:
    metadata:
      labels: { app: localstack }
    spec:
      containers:
        - name: localstack
          image: localstack/localstack:3
          # ← LocalStack = emulator cho AWS services (S3, SQS, DynamoDB...)
          # ← Chạy local thay AWS thật, tiết kiệm chi phí dev
          # ← version 3 = LocalStack 3.x (stable)

          env:
            - name: SERVICES
              value: s3
              # ← Chỉ khởi động S3, không bật tất cả services
              # ← Tiết kiệm RAM và startup time
              # ← Nếu cần thêm: value: s3,sqs,dynamodb

            - name: DEFAULT_REGION
              value: us-east-1
              # ← AWS region mặc định cho LocalStack
              # ← AWS SDK client cần region, LocalStack emulate region này

          ports:
            - containerPort: 4566
              # ← LocalStack expose TẤT CẢ services qua 1 port duy nhất
              # ← S3 endpoint: http://localstack:4566
              # ← Khác với AWS thật: mỗi service có endpoint riêng

---
apiVersion: v1
kind: Service
metadata:
  name: localstack
  namespace: social
spec:
  selector: { app: localstack }
  ports:
    - port: 4566
```

**Cách post-api dùng LocalStack:**
```yaml
# k8s/services/post-api.yaml
- name: S3_ENDPOINT
  value: http://localstack:4566   # ← override AWS S3 endpoint
- name: AWS_ACCESS_KEY_ID
  value: test                      # ← LocalStack chấp nhận bất kỳ credentials nào
- name: AWS_SECRET_ACCESS_KEY
  value: test
```

---

### 18.6 `k8s/infra/lgtm.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: lgtm
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: lgtm }
  template:
    metadata:
      labels: { app: lgtm }
    spec:
      containers:
        - name: lgtm
          image: grafana/otel-lgtm:0.8.1
          # ← All-in-one observability image từ Grafana Labs
          # ← LGTM = Loki + Grafana + Tempo + Mimir
          # ← "otel" = OpenTelemetry Collector tích hợp sẵn

          ports:
            - containerPort: 3000    # ← Grafana UI
              name: grafana          # ← đặt tên port để dễ reference
            - containerPort: 4317    # ← OTLP gRPC endpoint
              name: otlp-grpc        # ← Apps gửi telemetry qua đây
            - containerPort: 4318    # ← OTLP HTTP endpoint
              name: otlp-http        # ← Alternative nếu gRPC bị block

          env:
            - name: GF_AUTH_ANONYMOUS_ENABLED
              value: "true"
              # ← Cho phép truy cập Grafana không cần login
              # ← Chỉ dùng cho dev! Production: false + setup OAuth

            - name: GF_AUTH_ANONYMOUS_ORG_ROLE
              value: Admin
              # ← Anonymous user có quyền Admin (toàn quyền)
              # ← Dev: tiện, không cần config user
              # ← Production: Viewer hoặc tắt hoàn toàn

          readinessProbe:
            httpGet:
              path: /api/health    # ← Grafana health check endpoint
              port: 3000
            initialDelaySeconds: 15
            periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: lgtm
  namespace: social
spec:
  selector: { app: lgtm }
  ports:
    - name: grafana
      port: 3000    # ← kubectl port-forward svc/lgtm 3000:3000
    - name: otlp-grpc
      port: 4317    # ← OTEL_EXPORTER_OTLP_ENDPOINT=http://lgtm:4317
    - name: otlp-http
      port: 4318
```

**Flow observability:**
```
auth-service → gửi traces/metrics/logs qua OTLP gRPC → lgtm:4317
                                                              ↓
                                              OpenTelemetry Collector (in lgtm)
                                                    ↙      ↓       ↘
                                                 Loki   Tempo    Mimir
                                                 (logs) (traces) (metrics)
                                                    ↘      ↓       ↙
                                                       Grafana :3000
```

---

### 18.7 `k8s/infra/debezium.yaml`

```yaml
# ── Resource 1: Deployment (kafka-connect) ───────────────────────────────────

apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-connect
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: kafka-connect }
  template:
    metadata:
      labels: { app: kafka-connect }
    spec:
      containers:
        - name: kafka-connect
          image: debezium/connect:2.7
          # ← Debezium = platform CDC (Change Data Capture) chạy trên Kafka Connect
          # ← image này đã bao gồm Kafka Connect + Debezium PostgreSQL connector

          ports:
            - containerPort: 8083   # ← Kafka Connect REST API
                                    # ← Dùng để đăng ký connector, check status

          env:
            - name: BOOTSTRAP_SERVERS
              value: kafka:9092         # ← Kafka cluster để connect vào

            - name: GROUP_ID
              value: debezium-connect   # ← Consumer group ID của Kafka Connect cluster
                                        # ← Dùng để coordinate khi scale Kafka Connect

            - name: CONFIG_STORAGE_TOPIC
              value: debezium.configs   # ← Topic Kafka lưu connector configs
                                        # ← Persist qua restarts

            - name: OFFSET_STORAGE_TOPIC
              value: debezium.offsets   # ← Topic lưu vị trí đọc WAL hiện tại
                                        # ← Debezium biết đọc đến đâu rồi, restart không đọc lại

            - name: STATUS_STORAGE_TOPIC
              value: debezium.status    # ← Topic lưu trạng thái connector

            - name: KEY_CONVERTER
              value: org.apache.kafka.connect.json.JsonConverter
              # ← Format của message KEY trong Kafka topic (row identifier)

            - name: VALUE_CONVERTER
              value: org.apache.kafka.connect.json.JsonConverter
              # ← Format của message VALUE (row data)
              # ← Thay thế: io.confluent.kafka.serializers.KafkaAvroSerializer (cần Schema Registry)

            - name: KEY_CONVERTER_SCHEMAS_ENABLE
              value: "false"
              # ← Không nhúng schema vào mỗi message (nhỏ hơn, đủ cho project này)

            - name: VALUE_CONVERTER_SCHEMAS_ENABLE
              value: "false"

          readinessProbe:
            httpGet:
              path: /connectors    # ← Kafka Connect REST API list connectors
              port: 8083
            initialDelaySeconds: 30   # ← Kafka Connect khởi động chậm (connect Kafka, init topics)
            periodSeconds: 10

---

# ── Resource 2: Service (kafka-connect) ──────────────────────────────────────

apiVersion: v1
kind: Service
metadata:
  name: kafka-connect
  namespace: social
spec:
  selector: { app: kafka-connect }
  ports:
    - port: 8083   # ← Job "register-debezium-connector" gọi vào đây

---

# ── Resource 3: Job (register-debezium-connector) ────────────────────────────

apiVersion: batch/v1
kind: Job             # ← Job = task chạy 1 lần đến khi thành công, không phải Deployment
                      # ← Deployment: chạy mãi (long-running service)
                      # ← Job: chạy đến khi exit 0 (batch/one-shot task)
metadata:
  name: register-debezium-connector
  namespace: social
spec:
  template:
    spec:
      restartPolicy: OnFailure
      # ← Nếu job thất bại (exit non-0) → k8s retry
      # ← Deployment dùng restartPolicy: Always
      # ← Job dùng OnFailure hoặc Never

      initContainers:
        - name: wait-connect
          image: curlimages/curl:8.7.1
          # ← initContainer chạy TRƯỚC container chính
          # ← Container chính chỉ start khi TẤT CẢ initContainers exit 0
          # ← Dùng để chờ dependency sẵn sàng

          command:
            - sh
            - -c
            - |
              until curl -sf http://kafka-connect:8083/connectors; do
                echo "Waiting for kafka-connect..."; sleep 5
              done
              # ← Loop cho đến khi kafka-connect trả HTTP 2xx
              # ← curl -sf: -s = silent (không show progress), -f = fail on HTTP error
              # ← Vì readinessProbe của kafka-connect cũng check endpoint này,
              #   initContainer này đảm bảo "ready" trước khi gọi register

      containers:
        - name: register
          image: curlimages/curl:8.7.1
          command:
            - sh
            - -c
            - |
              curl -sf -X POST http://kafka-connect:8083/connectors \
                -H "Content-Type: application/json" \
                -d '{
                  "name": "social-outbox-connector",
                  "config": {
                    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                    # ← class của connector: PostgreSQL source connector

                    "database.hostname": "postgres",   # ← Service DNS
                    "database.port": "5432",
                    "database.user": "postgres",
                    "database.password": "postgres",
                    "database.dbname": "social",

                    "topic.prefix": "social",
                    # ← Prefix cho tên Kafka topics
                    # ← Topics sẽ là: social.interaction, social.post, social.auth

                    "plugin.name": "pgoutput",
                    # ← WAL decoder plugin
                    # ← pgoutput: built-in trong PostgreSQL 10+ (không cần cài thêm)
                    # ← Thay thế: decoderbufs (cần plugin riêng)

                    "table.include.list": "interaction.outbox,post.outbox,auth.outbox",
                    # ← CHỈ watch 3 bảng outbox (không watch toàn bộ DB)
                    # ← Format: schema.table

                    "transforms": "outbox",
                    # ← Tên của transform được apply

                    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
                    # ← Debezium Outbox Event Router: transform message từ outbox table
                    # ← thành event "thật" với đúng topic và payload

                    "transforms.outbox.route.by.field": "aggregate_type",
                    # ← Route event đến topic dựa vào cột "aggregate_type" trong outbox
                    # ← Nếu aggregate_type="interaction" → topic "social.interaction"

                    "transforms.outbox.route.topic.replacement": "social.${routedByValue}",
                    # ← Template tên topic: social.interaction, social.post, social.auth

                    "tombstones.on.delete": "false"
                    # ← Khi outbox row bị xóa, không publish tombstone message
                    # ← Tombstone = message với value=null (dùng cho log compaction)
                    # ← Không cần vì outbox rows không bao giờ bị update/delete
                  }
                }'
```

**Outbox flow qua Debezium:**
```
interaction-service INSERT vào outbox table
        ↓
PostgreSQL WAL (wal_level=logical)
        ↓
Debezium đọc WAL qua replication slot
        ↓
EventRouter transform → Kafka topic "social.interaction"
        ↓
notification-consumer CONSUME từ topic
```

---

### 18.8 `k8s/services/auth-service.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: social
spec:
  replicas: 1
  selector:
    matchLabels: { app: auth-service }
  template:
    metadata:
      labels: { app: auth-service }
    spec:
      containers:
        - name: auth-service
          image: nhan/auth-service:latest
          # ← "nhan" = group/namespace trong image name
          # ← ":latest" = tag, kind dùng được (IfNotPresent + kind load)
          # ← EKS: cần prefix ECR registry + imagePullPolicy: Always

          imagePullPolicy: IfNotPresent
          # ← Chỉ pull nếu image CHƯA có trong node
          # ← kind: dùng sau `kind load docker-image nhan/auth-service:latest`
          # ← EKS: phải đổi thành "Always" và dùng ECR URI

          ports:
            - containerPort: 8080   # ← Quarkus mặc định port 8080

          env:
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: http://lgtm:4317
              # ← OpenTelemetry: gửi traces/metrics/logs đến LGTM stack
              # ← "lgtm" = Service name của LGTM pod
              # ← :4317 = OTLP gRPC port
              # ← Quarkus tự export khi có quarkus-opentelemetry extension

            - name: DB_URL
              value: jdbc:postgresql://postgres:5432/social
              # ← JDBC URL cho Quarkus DataSource
              # ← "postgres" = Service DNS name trong namespace social
              # ← "social" = tên database (tạo bởi POSTGRES_DB env var)
              # ← Override application.properties: quarkus.datasource.jdbc.url=${DB_URL}

            - name: KAFKA_BOOTSTRAP_SERVERS
              value: kafka:9092
              # ← Kafka connection string cho SmallRye Reactive Messaging
              # ← "kafka" = Service DNS
              # ← auth-service dùng Kafka để publish user.created event

            - name: JWT_ISSUER
              value: https://social.nhan.dev
              # ← Claim "iss" trong JWT payload
              # ← Mọi service verify JWT đều check issuer này
              # ← Phải nhất quán giữa auth-service (sign) và các service khác (verify)

            - name: JWT_PUBLIC_KEY_LOCATION
              value: /etc/jwt/public-key.pem
              # ← Đường dẫn file public key để VERIFY JWT signature
              # ← File này được mount từ Secret "jwt-keys"
              # ← Quarkus SmallRye JWT đọc file này tự động

            - name: SMALLRYE_JWT_SIGN_KEY_LOCATION
              value: /etc/jwt/private-key.pem
              # ← Đường dẫn file private key để SIGN JWT
              # ← Chỉ auth-service có biến này (chỉ auth-service mới sign JWT)
              # ← Các service khác chỉ có JWT_PUBLIC_KEY_LOCATION

          volumeMounts:
            - name: jwt-keys          # ← tên volume (match với volumes[].name)
              mountPath: /etc/jwt      # ← thư mục trong container
                                       # ← sau mount: /etc/jwt/private-key.pem và public-key.pem
              readOnly: true           # ← container chỉ đọc, không ghi được
                                       # ← Best practice cho credentials

          readinessProbe:
            httpGet:
              path: /q/health/ready   # ← Quarkus SmallRye Health endpoint
                                      # ← Check: DB connection? Kafka connection?
              port: 8080
            initialDelaySeconds: 15   # ← Quarkus + Liquibase migration cần ~10-15s startup
            periodSeconds: 10         # ← probe mỗi 10 giây

          livenessProbe:
            httpGet:
              path: /q/health/live    # ← Chỉ check "app process còn chạy không?"
                                      # ← Không check DB/Kafka (tránh cascade restart)
              port: 8080
            initialDelaySeconds: 30   # ← Dài hơn readiness (pod phải ready trước khi check live)
            periodSeconds: 15

          resources:
            requests:
              memory: "256Mi"   # ← Minimum RAM cho scheduler allocate pod
              cpu: "250m"       # ← 250 millicores = 0.25 CPU core
                                # ← Scheduler cần node có ít nhất 0.25 core trống
            limits:
              memory: "512Mi"   # ← Container bị OOMKilled nếu vượt quá
              cpu: "500m"       # ← CPU bị throttled nếu vượt quá (không kill)
                                # ← Chú ý: Quarkus JVM cần memory để warm up

      volumes:
        - name: jwt-keys         # ← tên volume (match với volumeMounts[].name)
          secret:
            secretName: jwt-keys # ← lấy dữ liệu từ Secret "jwt-keys"
                                  # ← k8s mount từng key của Secret thành 1 file
                                  # ← jwt-keys có 2 keys: private-key.pem, public-key.pem

---
apiVersion: v1
kind: Service
metadata:
  name: auth-service    # ← Traefik dùng tên này: service.name: auth-service
  namespace: social
spec:
  selector: { app: auth-service }
  ports:
    - port: 8080         # ← Port của Service (bên trong cluster gọi auth-service:8080)
```

---

### 18.9 `k8s/services/user-api.yaml`

```yaml
# Pattern GIỐNG auth-service, trừ các điểm khác biệt:

env:
  - name: REDIS_URL
    value: redis://redis:6379
    # ← user-api dùng Redis để cache user profile
    # ← Khi user update profile → invalidate cache
    # ← post-api đọc cache này để hiển thị author info trong feed

  - name: JWT_PUBLIC_KEY_LOCATION
    value: /etc/jwt/public-key.pem
    # ← KHÔNG có SMALLRYE_JWT_SIGN_KEY_LOCATION
    # ← user-api chỉ VERIFY JWT, không tạo mới

# Tên Service = "user-service" (khác với Deployment name "user-api")
# ← Traefik route /api/users → service: user-service
# ← Deployment name và Service name có thể khác nhau — nối bằng selector
```

---

### 18.10 `k8s/services/user-consumer.yaml`

```yaml
# Consumer service — KHÔNG có Service resource, KHÔNG có route trong IngressRoute

apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-consumer
  namespace: social
spec:
  template:
    spec:
      containers:
        - name: user-consumer
          env:
            - name: KAFKA_BOOTSTRAP_SERVERS
              value: kafka:9092
              # ← user-consumer CONSUME từ Kafka
              # ← Event: "user.created" (publish từ auth-service sau signup)
              # ← user-consumer tạo UserProfile row trong user_db

            - name: REDIS_URL
              value: redis://redis:6379
              # ← user-consumer cần Redis để warm up cache
              # ← Sau khi tạo user profile → SET cache

          # Không có volumeMounts jwt-keys
          # ← Consumer không verify JWT (không có HTTP endpoint public)
          # ← Consumer nhận message từ Kafka, không từ HTTP clients

          resources:
            requests:
              memory: "192Mi"   # ← Consumer nhẹ hơn API service
              cpu: "100m"       # ← Không cần nhiều CPU (I/O bound, không compute heavy)
            limits:
              memory: "384Mi"
              cpu: "300m"

# KHÔNG có Service resource sau Deployment
# ← Consumer không cần Service (không ai gọi vào consumer qua HTTP)
# ← Consumer TỰ PULL message từ Kafka (không phải Kafka push vào)
```

---

### 18.11 `k8s/services/post-api.yaml`

```yaml
env:
  - name: S3_ENDPOINT
    value: http://localstack:4566
    # ← Override AWS S3 endpoint với LocalStack
    # ← Quarkus Amazon S3 extension đọc biến này
    # ← Production: xóa biến này → AWS SDK tự dùng s3.amazonaws.com

  - name: S3_BUCKET
    value: social-images
    # ← Tên S3 bucket lưu ảnh
    # ← LocalStack tự tạo bucket này khi nhận request đầu tiên
    # ← hoặc tạo sẵn qua infra/localstack/init.sh

  - name: S3_PUBLIC_URL
    value: http://localstack:4566/social-images
    # ← Base URL trả về cho client để hiển thị ảnh
    # ← Client dùng URL này để load ảnh trong browser
    # ← Production: https://cdn.social.nhan.dev hoặc S3 public URL

  - name: AWS_ACCESS_KEY_ID
    value: test           # ← LocalStack chấp nhận bất kỳ credentials
  - name: AWS_SECRET_ACCESS_KEY
    value: test           # ← Production: dùng IRSA (IAM Role for Service Account)

  # Không có SMALLRYE_JWT_SIGN_KEY_LOCATION
  # Không có REDIS_URL (post-api không ghi Redis trực tiếp)
  # ← post-consumer mới ghi Redis counter
```

---

### 18.12 `k8s/services/post-consumer.yaml`

```yaml
env:
  - name: REDIS_URL
    value: redis://redis:6379
    # ← post-consumer cần Redis để:
    # ← 1. INCR article:{id}:comment_count khi nhận event comment.created
    # ← 2. INCR article:{id}:vote_count khi nhận event vote.cast
    # ← CounterFlushJob (Quarkus @Scheduled) flush về DB mỗi 30s

  - name: KAFKA_BOOTSTRAP_SERVERS
    value: kafka:9092
    # ← post-consumer CONSUME từ topic "social.interaction"
    # ← Nhận: comment.created, vote.cast events
    # ← Publish bởi: interaction-service qua Debezium outbox

# Không có JWT_PUBLIC_KEY_LOCATION
# ← Consumer không expose HTTP endpoint (không verify JWT)
# ← Không có volumeMounts jwt-keys
```

---

### 18.13 `k8s/services/interaction-service.yaml`

```yaml
env:
  - name: KAFKA_BOOTSTRAP_SERVERS
    value: kafka:9092
    # ← interaction-service PUBLISH events (không consume)
    # ← Sau khi INSERT comment/vote → publish qua outbox → Debezium → Kafka

  - name: JWT_PUBLIC_KEY_LOCATION
    value: /etc/jwt/public-key.pem
    # ← Verify JWT của user request
    # ← Extract X-User-Id từ header (Traefik ForwardAuth đã set)

# Không có REDIS_URL
# ← interaction-service không đọc/ghi Redis trực tiếp
# ← Counter update xảy ra trong post-consumer (async, sau khi event đến Kafka)
```

---

### 18.14 `k8s/services/notification-api.yaml` và `notification-consumer.yaml`

```yaml
# notification-api
env:
  - name: REDIS_URL
    value: redis://redis:6379
    # ← Lưu "noti_unread:{userId}" counter
    # ← GET /api/notifications/unread-count → đọc từ Redis (không query DB)
    # ← Cực kỳ thường xuyên: mỗi lần user refresh page

  - name: KAFKA_BOOTSTRAP_SERVERS
    value: kafka:9092
    # ← notification-api CÓ consumer embedded (không tách riêng file)
    # ← Nhưng project này tách ra notification-consumer riêng

# notification-consumer
# ← CONSUME events từ Kafka: comment.created, vote.cast
# ← INSERT vào notification_db
# ← INCR Redis "noti_unread:{ownerId}"

# Tên Service trong notification-api.yaml:
# metadata.name: notification-service  (không phải notification-api)
# ← Traefik route /api/notifications → service: notification-service
```

---

### 18.15 `k8s/services/websocket-service.yaml`

```yaml
env:
  - name: REDIS_URL
    value: redis://redis:6379
    # ← WebSocket service dùng Redis Pub/Sub
    # ← notification-consumer publish vào Redis channel "ws:{userId}"
    # ← websocket-service subscribe và push real-time đến browser

  - name: KAFKA_BOOTSTRAP_SERVERS
    value: kafka:9092
    # ← websocket-service CONSUME từ Kafka trực tiếp
    # ← hoặc receive từ notification-consumer qua Redis Pub/Sub

# KHÔNG có JWT_PUBLIC_KEY_LOCATION và volumeMounts jwt-keys
# ← Auth xử lý ở application level (JWT trong query param hoặc subprotocol header)
# ← Traefik không apply jwt-verify middleware cho /ws routes

resources:
  requests:
    memory: "192Mi"
    cpu: "100m"   # ← WebSocket connections mostly idle (I/O bound)
  limits:
    memory: "384Mi"
    cpu: "300m"
```

---

### 18.16 `k8s/traefik/ingressroute.yaml`

```yaml
---
# ── Resource 1: Middleware (jwt-verify) ──────────────────────────────────────

apiVersion: traefik.io/v1alpha1
# ← CRD (Custom Resource Definition) của Traefik
# ← k8s không biết "Middleware" hay "IngressRoute" — Traefik operator dạy k8s
# ← Phải cài Traefik trước: helm install traefik traefik/traefik

kind: Middleware
metadata:
  name: jwt-verify    # ← tên middleware, reference trong IngressRoute
  namespace: social
spec:
  forwardAuth:
    address: http://auth-service:8080/api/auth/verify
    # ← Traefik forward request gốc đến endpoint này để verify
    # ← auth-service check JWT → 200 (valid) hoặc 401 (invalid)
    # ← Nếu 401 → Traefik trả 401 luôn, không forward đến service thật

    authResponseHeaders:
      - X-User-Id
      # ← Header từ auth-service response được forward xuống service tiếp theo
      # ← auth-service trả: X-User-Id: uuid-of-user
      # ← post-api nhận: request có header X-User-Id → biết user nào đang gọi
      # ← Các service KHÔNG cần parse JWT, chỉ đọc X-User-Id header

---

# ── Resource 2: IngressRoute (routing rules) ─────────────────────────────────

apiVersion: traefik.io/v1alpha1
kind: IngressRoute
metadata:
  name: social-routes
  namespace: social
spec:
  entryPoints: [web]
  # ← "web" = Traefik entry point tên "web" (port 80)
  # ← Traefik có nhiều entry points: web (80), websecure (443), dashboard (9090)
  # ← Định nghĩa trong Helm values hoặc Traefik static config

  routes:

    # ── Public route (không có middleware) ───────────────────────────────────
    - match: PathPrefix(`/api/auth`)
      # ← Rule matching: URL path bắt đầu bằng /api/auth
      # ← Backtick (`) bao quanh value là cú pháp của Traefik rule language
      # ← Các rule khác: Host(`example.com`), Method(`POST`), Headers(`X-Key`, `value`)
      kind: Rule
      services:
        - name: auth-service   # ← tên K8s Service (không phải Deployment)
          port: 8080
      # ← KHÔNG có middlewares → không verify JWT
      # ← /api/auth/signup và /api/auth/signin phải public!

    # ── Protected routes (có middleware jwt-verify) ───────────────────────────
    - match: PathPrefix(`/api/users`)
      kind: Rule
      middlewares:
        - name: jwt-verify     # ← apply middleware "jwt-verify" (định nghĩa ở trên)
          # ← namespace mặc định = namespace của IngressRoute (social)
      services:
        - name: user-service   # ← K8s Service name = "user-service" (trong user-api.yaml)
          port: 8080

    - match: PathPrefix(`/api/articles`)
      kind: Rule
      middlewares:
        - name: jwt-verify
      services:
        - name: post-service   # ← K8s Service name (trong post-api.yaml)
          port: 8080

    - match: PathPrefix(`/api/comments`)
      kind: Rule
      middlewares:
        - name: jwt-verify
      services:
        - name: interaction-service
          port: 8080

    - match: PathPrefix(`/api/votes`)
      kind: Rule
      middlewares:
        - name: jwt-verify
      services:
        - name: interaction-service   # ← cùng service với /api/comments
          port: 8080

    - match: PathPrefix(`/api/notifications`)
      kind: Rule
      middlewares:
        - name: jwt-verify
      services:
        - name: notification-service
          port: 8080

    - match: PathPrefix(`/ws`)
      kind: Rule
      # ← KHÔNG có middlewares → WebSocket không qua jwt-verify
      # ← Auth xử lý ở application level trong websocket-service
      # ← Traefik upgrade WebSocket tự động (không cần config thêm)
      services:
        - name: websocket-service
          port: 8080
```

**Tại sao Service name khác Deployment name?**

| Deployment name | Service name | Lý do |
|---|---|---|
| `user-api` | `user-service` | IngressRoute route đến `user-service` |
| `post-api` | `post-service` | IngressRoute route đến `post-service` |
| `notification-api` | `notification-service` | IngressRoute route đến `notification-service` |
| `auth-service` | `auth-service` | Tên trùng (tự nhiên hơn) |
| `interaction-service` | `interaction-service` | Tên trùng |

Deployment name = tên nội bộ. Service name = DNS name. Selector nối hai cái lại:
```yaml
# Service
metadata:
  name: user-service     # ← DNS name (ai gọi vào)
spec:
  selector:
    app: user-api        # ← match Deployment's pod label
```

---

### 18.17 Tóm tắt: So sánh các service manifests

| Service | DB | Redis | Kafka | JWT verify | JWT sign | S3 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| auth-service | ✅ | ❌ | ✅ (pub) | ✅ | ✅ | ❌ |
| user-api | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| user-consumer | ✅ | ✅ | ✅ (sub) | ❌ | ❌ | ❌ |
| post-api | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ |
| post-consumer | ✅ | ✅ | ✅ (sub) | ❌ | ❌ | ❌ |
| interaction-service | ✅ | ❌ | ✅ (pub) | ✅ | ❌ | ❌ |
| notification-api | ✅ | ✅ | ✅ (pub) | ✅ | ❌ | ❌ |
| notification-consumer | ✅ | ✅ | ✅ (sub) | ❌ | ❌ | ❌ |
| websocket-service | ❌ | ✅ | ✅ (sub) | ❌ | ❌ | ❌ |

**Pattern:** Consumer services không có JWT keys (không verify token), không expose HTTP route. API services có JWT public key để verify token từ Traefik header.
