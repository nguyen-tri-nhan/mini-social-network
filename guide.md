# Local Development Guide

---

## 1. Chạy local với Docker Compose (nhanh nhất)

Khởi động toàn bộ infrastructure:

```bash
docker compose -f infra/docker-compose.dev.yml up -d
```

Chạy từng service ở terminal riêng:

```bash
cd services

# Terminal 1
./gradlew :auth-service:quarkusDev

# Terminal 2
./gradlew :user-api:quarkusDev

# Terminal 3
./gradlew :post-api:quarkusDev

# Terminal 4
./gradlew :interaction-service:quarkusDev

# Terminal 5
./gradlew :notification-api:quarkusDev
```

API Gateway (Traefik) đang chạy trên `:8080`, FE gọi `http://localhost:8080/api/*`.

---

## 2. Chạy local với Kubernetes (kind)

### 2.1 Prerequisites

```bash
# macOS
brew install kind kubectl helm

# Verify
kind version        # >= 0.23
kubectl version     # >= 1.29
helm version        # >= 3.14
```

### 2.2 Tạo cluster

```bash
kind create cluster --name social --config - <<'EOF'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 80
        hostPort: 80       # Traefik HTTP — FE gọi localhost
      - containerPort: 443
        hostPort: 443
  - role: worker
  - role: worker
EOF
```

Xác nhận cluster:

```bash
kubectl cluster-info --context kind-social
kubectl get nodes
```

### 2.3 Cài Traefik (API Gateway)

```bash
helm repo add traefik https://helm.traefik.io/traefik
helm repo update

helm install traefik traefik/traefik \
  --namespace kube-system \
  --set ports.web.port=8080 \
  --set ports.web.hostPort=8080 \
  --set service.type=NodePort \
  --wait
```

Xác nhận:

```bash
kubectl get pods -n kube-system -l app.kubernetes.io/name=traefik
```

### 2.4 Tạo namespace

```bash
kubectl apply -f k8s/namespaces.yaml
```

### 2.5 Deploy infrastructure

#### PostgreSQL

```bash
kubectl apply -n social -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
spec:
  replicas: 1
  selector:
    matchLabels: { app: postgres }
  template:
    metadata:
      labels: { app: postgres }
    spec:
      containers:
        - name: postgres
          image: postgres:15-alpine
          env:
            - { name: POSTGRES_DB,       value: social }
            - { name: POSTGRES_USER,     value: postgres }
            - { name: POSTGRES_PASSWORD, value: postgres }
          ports: [{ containerPort: 5432 }]
          volumeMounts:
            - name: init-sql
              mountPath: /docker-entrypoint-initdb.d
      volumes:
        - name: init-sql
          configMap:
            name: postgres-init
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
spec:
  selector: { app: postgres }
  ports: [{ port: 5432 }]
EOF

# Upload init script
kubectl create configmap postgres-init \
  --from-file=init-schemas.sql=infra/postgres/init-schemas.sql \
  -n social
```

#### Redis

```bash
kubectl apply -n social -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels: { app: redis }
  template:
    metadata:
      labels: { app: redis }
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          args: ["--maxmemory", "256mb", "--maxmemory-policy", "allkeys-lru"]
          ports: [{ containerPort: 6379 }]
---
apiVersion: v1
kind: Service
metadata:
  name: redis
spec:
  selector: { app: redis }
  ports: [{ port: 6379 }]
EOF
```

#### Kafka

```bash
# Cài Strimzi operator
kubectl create namespace kafka
kubectl apply -f https://strimzi.io/install/latest?namespace=kafka -n kafka

# Tạo Kafka cluster (single-node cho dev)
kubectl apply -n kafka -f - <<'EOF'
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: social
spec:
  kafka:
    replicas: 1
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    config:
      offsets.topic.replication.factor: 1
      auto.create.topics.enable: "true"
    storage:
      type: ephemeral
  zookeeper:
    replicas: 1
    storage:
      type: ephemeral
  entityOperator:
    topicOperator: {}
EOF

# Chờ Kafka ready (2-3 phút)
kubectl wait kafka/social -n kafka \
  --for=condition=Ready --timeout=300s
```

#### LocalStack (S3)

```bash
kubectl apply -n social -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: localstack
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
          env:
            - { name: SERVICES,        value: s3 }
            - { name: DEFAULT_REGION,  value: us-east-1 }
          ports: [{ containerPort: 4566 }]
---
apiVersion: v1
kind: Service
metadata:
  name: localstack
spec:
  selector: { app: localstack }
  ports: [{ port: 4566 }]
EOF
```

### 2.6 Build và load image vào kind

```bash
cd services

# Build image (JIB — không cần Docker daemon)
gradle :auth-service:build -Dquarkus.container-image.build=true
gradle :user-api:build     -Dquarkus.container-image.build=true
gradle :post-api:build     -Dquarkus.container-image.build=true
gradle :interaction-service:build -Dquarkus.container-image.build=true
gradle :notification-api:build    -Dquarkus.container-image.build=true

# Load vào kind cluster (không cần push lên registry)
kind load docker-image nhan/auth-service:latest        --name social
kind load docker-image nhan/user-api:latest            --name social
kind load docker-image nhan/post-api:latest            --name social
kind load docker-image nhan/interaction-service:latest --name social
kind load docker-image nhan/notification-api:latest    --name social
```

### 2.7 Deploy services

Mỗi service cần 1 `Deployment` + `Service`. Ví dụ auth-service:

```bash
kubectl apply -n social -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
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
          imagePullPolicy: Never          # dùng image đã load từ kind
          ports: [{ containerPort: 8080 }]
          env:
            - { name: DB_URL,                  value: "jdbc:postgresql://postgres:5432/social" }
            - { name: KAFKA_BOOTSTRAP_SERVERS, value: "social-kafka-bootstrap.kafka:9092" }
          readinessProbe:
            httpGet: { path: /q/health/ready, port: 8080 }
            initialDelaySeconds: 10
          livenessProbe:
            httpGet: { path: /q/health/live, port: 8080 }
            initialDelaySeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector: { app: auth-service }
  ports: [{ port: 8080 }]
EOF
```

Lặp lại tương tự cho các service còn lại, thay image name và env vars theo từng service.

### 2.8 Apply IngressRoute

```bash
kubectl apply -f k8s/traefik/ingressroute.yaml
```

### 2.9 Verify

```bash
# Tất cả pods running
kubectl get pods -n social

# Logs service
kubectl logs -n social deployment/auth-service -f

# Test API qua gateway
curl http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"pass123","firstname":"Test","lastname":"User"}'

# Traefik dashboard
kubectl port-forward -n kube-system deployment/traefik 9090:9090
# Mở http://localhost:9090/dashboard
```

### 2.10 Xoá cluster

```bash
kind delete cluster --name social
```

---

## 3. Chạy k6 Smoke Tests

### Prerequisites

```bash
# macOS
brew install k6

# Verify
k6 version
```

### Chạy smoke test

```bash
# Local (docker-compose hoặc kind)
k6 run k6/smoke/smoke.js

# Chỉ định base URL khác
k6 run k6/smoke/smoke.js -e BASE_URL=http://localhost

# Staging
k6 run k6/smoke/smoke.js -e BASE_URL=https://api.social.nhan.dev

# Output chi tiết
k6 run k6/smoke/smoke.js --out json=k6/results.json
```

### Kết quả mẫu

```
  ✓ signup alice → 201
  ✓ signup bob → 201
  ✓ signin alice → 200 (implicit via signup response)
  ✓ createArticle → 201
  ✓ article has id
  ✓ listArticles → 200
  ✓ addComment → 201
  ✓ castVote ARTICLE → 200
  ✓ castVote COMMENT → 200
  ✓ listNotifications → 200
  ✓ unreadCount is 0 after markAllSeen

  checks.........................: 100.00%
  http_req_duration..............: avg=45ms  p(95)=120ms
  http_req_failed................: 0.00%
```

---

## 4. Tóm tắt ports (local dev)

| Service | Dev port | k8s port |
|---|---|---|
| Traefik (API Gateway) | `:8080` | `:8080` |
| Traefik Dashboard | `:9090` | port-forward |
| auth-service | `:8081` | `:8080` (internal) |
| user-api | `:8082` | `:8080` (internal) |
| post-api | `:8083` | `:8080` (internal) |
| interaction-service | `:8084` | `:8080` (internal) |
| notification-api | `:8085` | `:8080` (internal) |
| PostgreSQL | `:5432` | `:5432` (internal) |
| Redis | `:6379` | `:6379` (internal) |
| Kafka | `:9092` | `:9092` (internal) |
| LocalStack (S3) | `:4566` | `:4566` (internal) |
