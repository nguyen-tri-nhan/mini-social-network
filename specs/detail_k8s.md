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

Trong project này, Kafka chạy trong namespace `kafka` (do Strimzi operator quản lý), còn toàn bộ app chạy trong namespace `social`. Đây là lý do các biến env trỏ Kafka phải dùng full DNS:

```
# Cùng namespace → tên ngắn
postgres:5432       ✅ (postgres ở namespace social, service cũng ở social)

# Khác namespace → phải đủ <service>.<namespace>
social-kafka-bootstrap.kafka:9092   ✅
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

Kafka ở namespace khác (`kafka`) nên phải dùng full name:
```
social-kafka-bootstrap.kafka.svc.cluster.local:9092
```

Đây là lý do env vars trong deployment manifest dùng tên ngắn cho postgres/redis nhưng full name cho Kafka.

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

## 12. Cấu trúc file k8s

```
k8s/
├── namespaces.yaml          # tạo namespace social trước
├── infra/
│   ├── postgres.yaml        # ConfigMap init SQL + Deployment + Service
│   ├── redis.yaml           # Deployment + Service
│   ├── localstack.yaml      # Deployment + Service
│   └── jwt-secret.yaml      # hướng dẫn tạo Secret (không commit key)
├── services/
│   ├── auth-service.yaml    # Deployment + Service (port 8080)
│   ├── user-api.yaml
│   ├── post-api.yaml
│   ├── interaction-service.yaml
│   └── notification-api.yaml
└── traefik/
    └── ingressroute.yaml    # routing rules — áp dụng sau khi Traefik đã cài
```

**Thứ tự apply:**
```bash
kubectl apply -f k8s/namespaces.yaml    # 1. namespace trước
make k8s-secrets                         # 2. Secret (jwt-keys)
kubectl apply -f k8s/infra/             # 3. infra (postgres, redis, localstack)
# chờ postgres ready
kubectl apply -f k8s/services/          # 4. app services (Liquibase chạy migration)
kubectl apply -f k8s/traefik/           # 5. routing (sau khi services đã up)
```

Hoặc dùng: `make deploy` (Makefile xử lý thứ tự + chờ postgres ready tự động).
