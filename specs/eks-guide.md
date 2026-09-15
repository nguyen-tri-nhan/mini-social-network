# EKS Deployment Guide — Test Full Flow

> Mục tiêu: dựng cluster EKS để test toàn bộ flow (signup → post → comment → vote → notification) với infrastructure giống prod nhất có thể.

---

## 1. Kind vs EKS — Khác biệt cần xử lý

| Thành phần | kind (local) | EKS (target) | Cần thay đổi |
|---|---|---|---|
| Image source | `kind load` từ Docker daemon local | Pull từ ECR registry | ✅ Bắt buộc |
| `imagePullPolicy` | `IfNotPresent` | `Always` hoặc digest tag | ✅ Bắt buộc |
| Traefik service type | `NodePort` | `LoadBalancer` (AWS NLB) | ✅ Bắt buộc |
| Port mapping | `kind-cluster.yaml` extraPortMappings | ALB/NLB DNS | ✅ Bắt buộc |
| Persistent storage | `emptyDir` (implicit) | EBS PersistentVolumeClaim | ⚠️ Cần nếu test kéo dài |
| Secrets | K8s Secret từ PEM file local | Giữ nguyên (hoặc Secrets Manager sau) | Giữ nguyên cho test |
| S3 | LocalStack in-cluster | Giữ nguyên LocalStack (test) hoặc S3 thật | Giữ nguyên cho test |
| Kafka | KRaft single-node in-cluster | Giữ nguyên (test) hoặc MSK (prod) | Giữ nguyên cho test |
| DNS | `localhost` | ELB DNS / custom domain | Cập nhật env vars |

---

## 2. Readiness Checklist

### ✅ Đã sẵn sàng (không cần thay đổi)
- [x] Tất cả k8s manifests hợp lệ YAML
- [x] Health probes (`/q/health/ready`, `/q/health/live`) trên mọi service
- [x] Resource requests/limits đã set
- [x] JWT Secret mount pattern (Secret → volume)
- [x] Env vars cho DB/Redis/Kafka dùng service DNS name (không hardcode IP)
- [x] Debezium connector config (`k8s/infra/debezium.yaml`)
- [x] Traefik IngressRoute + ForwardAuth middleware
- [x] `postgres.yaml` có `wal_level=logical` cho Debezium CDC
- [x] Makefile targets (`deploy-infra`, `deploy-services`, `deploy-debezium`, `deploy-routes`)

### ❌ Blocking — Phải làm trước khi lên EKS

- [ ] **ECR repositories** — tạo 1 repo cho mỗi service
- [ ] **Build + push images** lên ECR
- [ ] **Cập nhật image names** trong tất cả `k8s/services/*.yaml`
- [ ] **Thay `imagePullPolicy: IfNotPresent` → `Always`** (hoặc dùng sha256 digest)
- [ ] **Traefik Helm install** với `service.type=LoadBalancer`
- [ ] **EKS cluster** — tạo bằng `eksctl` hoặc AWS Console

### ⚠️ Nên làm (không blocking nhưng data mất nếu bỏ qua)
- [ ] PersistentVolumeClaim cho postgres (EBS gp3)
- [ ] PersistentVolumeClaim cho kafka

---

## 3. Bước 1 — Verify kind Cluster Trước (Khuyến nghị)

**Làm local trước, rẻ hơn và tìm bugs nhanh hơn.**

```bash
# Tạo kind cluster
make cluster-create

# Tạo JWT secret
make k8s-secrets

# Build tất cả images
make build

# Load vào kind
make load

# Deploy infra + services + debezium + routes
make deploy

# Verify
kubectl get pods -n social
kubectl logs -n social deployment/auth-service -f
```

Test happy path:
```bash
# Signup
curl -s http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@test.com","password":"pass1234","firstname":"Alice","lastname":"Test"}'

# Signin
TOKEN=$(curl -s http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass1234"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# Create post
curl -s -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"description":"Hello world"}'
```

Chạy k6 smoke test:
```bash
k6 run k6/smoke/smoke.js
```

---

## 4. Bước 2 — Tạo ECR Repositories

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=ap-southeast-1
ECR_BASE=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Tạo repo cho từng service
for svc in auth-service user-api user-consumer post-api post-consumer \
           interaction-service notification-api notification-consumer websocket-service; do
  aws ecr create-repository --repository-name nhan/$svc --region $AWS_REGION
done

# Login ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $ECR_BASE
```

---

## 5. Bước 3 — Build và Push Images lên ECR

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=ap-southeast-1
ECR_REGISTRY=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

cd services

# Build tất cả images với ECR registry
SERVICES="auth-service user-api user-consumer post-api post-consumer \
          interaction-service notification-api notification-consumer websocket-service"

for svc in $SERVICES; do
  ./gradlew :$svc:build \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.registry=$ECR_REGISTRY \
    -Dquarkus.container-image.group=nhan \
    -Dquarkus.container-image.tag=latest \
    -Dquarkus.container-image.push=true
done
```

---

## 6. Bước 4 — Cập nhật K8s Manifests cho EKS

### 6.1 Thay image names và imagePullPolicy

Cập nhật tất cả `k8s/services/*.yaml`. Thay:
```yaml
# Cũ (kind)
image: nhan/auth-service:latest
imagePullPolicy: IfNotPresent
```
thành:
```yaml
# Mới (EKS)
image: <ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com/nhan/auth-service:latest
imagePullPolicy: Always
```

> Làm nhanh bằng `sed`:
> ```bash
> ECR_BASE="<ACCOUNT_ID>.dkr.ecr.ap-southeast-1.amazonaws.com"
> for f in k8s/services/*.yaml; do
>   sed -i "s|image: nhan/|image: $ECR_BASE/nhan/|g" $f
>   sed -i "s|imagePullPolicy: IfNotPresent|imagePullPolicy: Always|g" $f
> done
> ```

### 6.2 Thêm imagePullSecrets (nếu EKS node chưa có ECR access qua IAM)

Cách đơn giản hơn: gắn IAM role cho node group với policy `AmazonEC2ContainerRegistryReadOnly` → không cần imagePullSecrets.

---

## 7. Bước 5 — Tạo EKS Cluster

### Option A — eksctl (nhanh nhất)

```bash
eksctl create cluster \
  --name social \
  --region ap-southeast-1 \
  --nodegroup-name standard \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed

# Verify
kubectl get nodes
```

Node type `t3.medium` (2 vCPU / 4GB): đủ chạy ~5-6 pods cho test. Dùng `t3.large` nếu muốn chạy đầy đủ cả observability stack.

### Option B — AWS Console / Terraform

Dùng khi cần VPC config phức tạp hơn.

---

## 8. Bước 6 — Cài Traefik với LoadBalancer

```bash
helm repo add traefik https://helm.traefik.io/traefik
helm repo update

# EKS: service.type=LoadBalancer thay NodePort
helm install traefik traefik/traefik \
  --namespace kube-system \
  --set service.type=LoadBalancer \
  --wait

# Lấy External IP / DNS name
kubectl get svc -n kube-system traefik
# NAME      TYPE           EXTERNAL-IP
# traefik   LoadBalancer   <aws-nlb-dns>.elb.amazonaws.com
```

> Lưu `EXTERNAL-IP` lại — đây là endpoint để gọi API thay vì `localhost:8080`.

---

## 9. Bước 7 — Deploy lên EKS

```bash
# 1. Namespace
kubectl apply -f k8s/namespaces.yaml

# 2. JWT Secret (dùng dev keys cho test)
make k8s-secrets

# 3. Infra (postgres, redis, kafka, localstack, lgtm)
kubectl apply -f k8s/infra/postgres.yaml
kubectl apply -f k8s/infra/redis.yaml
kubectl apply -f k8s/infra/kafka.yaml
kubectl apply -f k8s/infra/localstack.yaml
kubectl apply -f k8s/infra/lgtm.yaml

# Chờ postgres + kafka sẵn sàng
kubectl rollout status deployment/postgres -n social --timeout=60s
kubectl rollout status deployment/kafka    -n social --timeout=90s

# 4. Services
kubectl apply -f k8s/services/

# Chờ services up (Liquibase migration chạy)
kubectl rollout status deployment/auth-service -n social --timeout=120s

# 5. Debezium (sau khi services đã up — outbox tables phải tồn tại)
kubectl apply -f k8s/infra/debezium.yaml
kubectl rollout status deployment/kafka-connect -n social --timeout=120s

# 6. IngressRoute
kubectl apply -f k8s/traefik/ingressroute.yaml
```

---

## 10. Bước 8 — Verify trên EKS

```bash
# Tất cả pods phải Running
kubectl get pods -n social

# Lấy Traefik external DNS
TRAEFIK_HOST=$(kubectl get svc -n kube-system traefik \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Test signup
curl -s http://$TRAEFIK_HOST/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@test.com","password":"pass1234","firstname":"Alice","lastname":"Test"}'

# k6 smoke test
k6 run k6/smoke/smoke.js -e BASE_URL=http://$TRAEFIK_HOST
```

---

## 11. (Optional) Persistent Storage cho Postgres

Nếu muốn data không mất khi pod restart:

```yaml
# Thêm vào k8s/infra/postgres.yaml
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: social
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: gp2   # EKS default, dùng gp3 nếu có
  resources:
    requests:
      storage: 10Gi
```

Và mount vào Postgres Deployment:
```yaml
containers:
  - name: postgres
    volumeMounts:
      - name: postgres-data
        mountPath: /var/lib/postgresql/data
volumes:
  - name: postgres-data
    persistentVolumeClaim:
      claimName: postgres-pvc
```

---

## 12. Dọn dẹp sau khi test xong

```bash
# Xóa tất cả resources trong cluster
kubectl delete namespace social

# Xóa EKS cluster (tránh tốn tiền)
eksctl delete cluster --name social --region ap-southeast-1

# Xóa ECR images (optional)
for svc in auth-service user-api user-consumer post-api post-consumer \
           interaction-service notification-api notification-consumer websocket-service; do
  aws ecr delete-repository --repository-name nhan/$svc --force --region ap-southeast-1
done
```

---

## 13. Ước tính chi phí EKS test

| Thành phần | Instance | Giá/giờ (ap-southeast-1) |
|---|---|---|
| EKS control plane | — | $0.10/giờ |
| EC2 worker (t3.medium × 2) | 2 vCPU / 4GB | ~$0.084/giờ × 2 |
| NLB (Traefik LoadBalancer) | — | ~$0.008/giờ |
| **Tổng** | | **~$0.28/giờ** |

> Test 8 tiếng ≈ $2.2 — bật xong tắt ngay để tránh billing.

---

## 14. Khác biệt EKS test vs Production

| | EKS test (guide này) | Production (tương lai) |
|---|---|---|
| Database | Postgres pod in-cluster | AWS RDS PostgreSQL |
| Kafka | KRaft single-node pod | AWS MSK |
| S3 | LocalStack pod | AWS S3 thật |
| Secrets | K8s Secret từ PEM file | AWS Secrets Manager + ESO |
| Image tag | `latest` | Git SHA digest |
| Replicas | 1 | 2+ (HA) |
| Monitoring | LGTM pod | Grafana Cloud / AWS CloudWatch |
| TLS | Không | ACM Certificate + HTTPS |
