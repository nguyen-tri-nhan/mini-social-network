SHELL     := /bin/bash
CLUSTER   := social
NAMESPACE := social
GROUP     := nhan
TAG       := latest

SERVICES  := auth-service user-api post-api interaction-service notification-api
CONSUMERS := post-consumer user-consumer notification-consumer

# ── Build ─────────────────────────────────────────────────────────────────────

.PHONY: build build-auth build-user build-post build-interaction build-notification

## Build tất cả service + consumer images (JIB — không cần Dockerfile)
build:
	cd services && gradle \
		$(foreach svc,$(SERVICES) $(CONSUMERS),:$(svc):build) \
		-Dquarkus.container-image.build=true \
		-Dquarkus.container-image.tag=$(TAG)

## Build từng service riêng lẻ
build-auth:
	cd services && gradle :auth-service:build -Dquarkus.container-image.build=true

build-user:
	cd services && gradle :user-api:build :user-consumer:build -Dquarkus.container-image.build=true

build-post:
	cd services && gradle :post-api:build :post-consumer:build -Dquarkus.container-image.build=true

build-interaction:
	cd services && gradle :interaction-service:build -Dquarkus.container-image.build=true

build-notification:
	cd services && gradle :notification-api:build :notification-consumer:build \
		-Dquarkus.container-image.build=true

# ── Kind — load images ────────────────────────────────────────────────────────

.PHONY: load load-auth load-user load-post load-interaction load-notification

## Load tất cả images vào kind cluster (kind không pull từ local Docker tự động)
load:
	@for svc in $(SERVICES) $(CONSUMERS); do \
		echo "Loading $$svc..."; \
		kind load docker-image $(GROUP)/$$svc:$(TAG) --name $(CLUSTER); \
	done

load-auth:
	kind load docker-image $(GROUP)/auth-service:$(TAG) --name $(CLUSTER)

load-user:
	kind load docker-image $(GROUP)/user-api:$(TAG) --name $(CLUSTER)
	kind load docker-image $(GROUP)/user-consumer:$(TAG) --name $(CLUSTER)

load-post:
	kind load docker-image $(GROUP)/post-api:$(TAG) --name $(CLUSTER)
	kind load docker-image $(GROUP)/post-consumer:$(TAG) --name $(CLUSTER)

load-interaction:
	kind load docker-image $(GROUP)/interaction-service:$(TAG) --name $(CLUSTER)

load-notification:
	kind load docker-image $(GROUP)/notification-api:$(TAG) --name $(CLUSTER)
	kind load docker-image $(GROUP)/notification-consumer:$(TAG) --name $(CLUSTER)

# ── Deploy ────────────────────────────────────────────────────────────────────

.PHONY: deploy deploy-infra deploy-services deploy-routes

## Apply tất cả k8s manifests (thứ tự: namespace → secrets → infra → services → routes)
deploy: deploy-infra deploy-services deploy-routes

deploy-infra:
	kubectl apply -f k8s/namespaces.yaml
	kubectl apply -f k8s/infra/postgres.yaml
	kubectl apply -f k8s/infra/redis.yaml
	kubectl apply -f k8s/infra/kafka.yaml
	kubectl apply -f k8s/infra/localstack.yaml
	kubectl apply -f k8s/infra/lgtm.yaml
	@echo "Waiting for postgres and kafka to be ready..."
	kubectl rollout status deployment/postgres -n $(NAMESPACE) --timeout=60s
	kubectl rollout status deployment/kafka    -n $(NAMESPACE) --timeout=90s

## Grafana UI — port-forward để mở trên máy host
grafana:
	@echo "Grafana: http://localhost:3000  (admin/admin)"
	kubectl port-forward -n $(NAMESPACE) svc/lgtm 3000:3000

deploy-services:
	kubectl apply -f k8s/services/

deploy-routes:
	kubectl apply -f k8s/traefik/ingressroute.yaml

## Tạo Secret cho JWT keys từ file PEM local (idempotent)
k8s-secrets:
	kubectl create secret generic jwt-keys \
		--from-file=private-key.pem=services/dev-private.pem \
		--from-file=public-key.pem=services/dev-public.pem \
		-n $(NAMESPACE) \
		--dry-run=client -o yaml | kubectl apply -f -

## Rolling restart — áp dụng image mới đã load vào kind
restart:
	kubectl rollout restart deployment -n $(NAMESPACE)

restart-%:
	kubectl rollout restart deployment/$* -n $(NAMESPACE)

# ── Full workflow ─────────────────────────────────────────────────────────────

.PHONY: up up-% cluster-create cluster-delete

## Build + load + deploy tất cả (full workflow)
up: build load deploy
	@echo "✅ All services deployed to kind cluster '$(CLUSTER)'"

## Build + load + restart 1 service: make up-post
up-%: build-% load-%
	kubectl rollout restart deployment/$* -n $(NAMESPACE) 2>/dev/null || true
	@echo "✅ $* updated"

# ── Cluster lifecycle ─────────────────────────────────────────────────────────

cluster-create:
	kind create cluster --name $(CLUSTER) --config infra/kind-cluster.yaml
	helm repo add traefik https://helm.traefik.io/traefik && helm repo update
	helm install traefik traefik/traefik \
		--namespace kube-system \
		--set ports.web.port=8080 \
		--set ports.web.hostPort=8080 \
		--set service.type=NodePort \
		--wait

cluster-delete:
	kind delete cluster --name $(CLUSTER)

# ── Helpers ───────────────────────────────────────────────────────────────────

.PHONY: status logs-% dev-token

## Xem trạng thái tất cả pods
status:
	kubectl get pods -n $(NAMESPACE)

## Xem log 1 service: make logs-post-api
logs-%:
	kubectl logs -n $(NAMESPACE) deployment/$* -f --tail=100

## Lấy dev token (auth-service phải đang chạy local)
dev-token:
	@curl -s "http://localhost:8081/dev/token" | python3 -c \
		"import sys,json; d=json.load(sys.stdin)['data']; print('Token:',d['token'][:50]+'...'); print('UserId:',d['userId'])"

.PHONY: help
help:
	@grep -E '^##' Makefile | sed 's/## //'
