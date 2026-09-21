SHELL     := /bin/bash
CLUSTER   := social
NAMESPACE := social
GROUP     := nhan
TAG       := latest

# JIB mặc định build linux/amd64 bất kể host arch — auto-detect theo uname,
# override: make build ARCH=linux/amd64
UNAME_ARCH := $(shell uname -m)
ifeq ($(UNAME_ARCH),$(filter $(UNAME_ARCH),arm64 aarch64))
ARCH := linux/arm64/v8
else
ARCH := linux/amd64
endif

SERVICES  := auth-service user-api post-api interaction-service notification-api websocket-service
CONSUMERS := post-consumer user-consumer notification-consumer

# make build RERUN=1 — ép Gradle chạy lại toàn bộ task graph, bỏ qua up-to-date
# check. Cần khi image trong Docker daemon bị mất (vd docker system prune, xoá
# cache JIB thủ công) nhưng Gradle vẫn thấy input/output nó tự theo dõi
# (jib-image.id, jib-image.digest trong build/) không đổi nên skip luôn bước
# containerize thật — báo "BUILD SUCCESSFUL, up-to-date" dù image không tồn
# tại. Gradle không biết verify side-effect ngoài nó (Docker daemon).
RERUN :=
ifeq ($(RERUN),1)
RERUN_FLAG := --rerun-tasks
endif

# ── Build ─────────────────────────────────────────────────────────────────────

.PHONY: build build-auth build-user build-post build-interaction build-notification build-websocket

## Build tất cả service + consumer images (JIB — không cần Dockerfile)
build:
	cd services && gradle \
		$(foreach svc,$(SERVICES) $(CONSUMERS),:$(svc):build) \
		-Dquarkus.container-image.build=true \
		-Dquarkus.container-image.tag=$(TAG) \
		-Dquarkus.jib.platforms=$(ARCH) \
		$(RERUN_FLAG)

## Build từng service riêng lẻ
build-auth:
	cd services && gradle :auth-service:build -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

build-user:
	cd services && gradle :user-api:build :user-consumer:build -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

build-post:
	cd services && gradle :post-api:build :post-consumer:build -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

build-interaction:
	cd services && gradle :interaction-service:build -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

build-notification:
	cd services && gradle :notification-api:build :notification-consumer:build \
		-Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

build-websocket:
	cd services && gradle :websocket-service:build -Dquarkus.container-image.build=true -Dquarkus.jib.platforms=$(ARCH) $(RERUN_FLAG)

# ── Kind — load images ────────────────────────────────────────────────────────

.PHONY: load load-auth load-user load-post load-interaction load-notification load-websocket

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

load-websocket:
	kind load docker-image $(GROUP)/websocket-service:$(TAG) --name $(CLUSTER)

# ── Deploy ────────────────────────────────────────────────────────────────────

.PHONY: deploy deploy-infra deploy-services deploy-services-sequential deploy-debezium deploy-routes k8s-secrets

## Apply tất cả k8s manifests (thứ tự: infra → secrets → services → debezium → routes)
## Dùng deploy-services-sequential (không phải deploy-services) — deploy 9
## service cùng lúc gây CPU/memory storm trên máy resource hạn chế, xem
## specs/service-dependencies.md
deploy: deploy-infra k8s-secrets deploy-services-sequential deploy-debezium deploy-routes

deploy-infra:
	kubectl apply -f k8s/namespaces.yaml
	kubectl apply -f k8s/infra/postgres.yaml
	kubectl apply -f k8s/infra/redis.yaml
	kubectl apply -f k8s/infra/kafka.yaml
	kubectl apply -f k8s/infra/localstack.yaml
	kubectl apply -f k8s/infra/lgtm.yaml
	kubectl apply -f k8s/infra/kafdrop.yaml
	@echo "Waiting for postgres and kafka to be ready..."
	kubectl rollout status deployment/postgres -n $(NAMESPACE) --timeout=60s
	kubectl rollout status deployment/kafka    -n $(NAMESPACE) --timeout=90s

## Grafana UI — port-forward để mở trên máy host (3001, không phải 3000 —
## đụng cổng Vite dev server của frontend)
grafana:
	@echo "Grafana: http://localhost:3001"
	kubectl port-forward -n $(NAMESPACE) svc/lgtm 3001:3000

## Kafdrop — xem/test produce message Kafka thủ công, port-forward khi cần
kafdrop:
	@echo "Kafdrop: http://localhost:9000"
	kubectl port-forward -n $(NAMESPACE) svc/kafdrop 9000:9000

## Port-forward Postgres ra host để đọc DB bằng psql/GUI client (TablePlus,
## DBeaver...) — connect: postgresql://postgres:postgres@localhost:5432/social
db-forward:
	@echo "Postgres: postgresql://postgres:postgres@localhost:5432/social"
	kubectl port-forward -n $(NAMESPACE) svc/postgres 5432:5432

## Mở psql thẳng vào DB qua port-forward đang chạy (chạy `make db-forward` ở terminal khác trước)
db-psql:
	PGPASSWORD=postgres psql -h localhost -p 5432 -U postgres -d social

## Traefik dashboard — port 9090 không có trên Service (chỉ web/websecure),
## port-forward thẳng vào Deployment; cũng không map ra host qua kind (chỉ 8080)
traefik-dashboard:
	@echo "Traefik dashboard: http://localhost:9090/dashboard/"
	kubectl port-forward -n kube-system deployment/traefik 9090:9090

# ── Port-forward, chạy nền (không chiếm terminal) ───────────────────────────

.PHONY: forward-up forward-down forward-status

FWD_DIR := .scratch/port-forward

## Bật tất cả port-forward hay dùng (Grafana, Kafdrop, Traefik dashboard,
## Postgres) chạy nền 1 lần — thay vì mở 4 terminal riêng. PID lưu ở
## .scratch/port-forward/*.pid, log ở *.log (cùng thư mục) để debug khi cần.
forward-up: forward-down
	@mkdir -p $(FWD_DIR)
	@nohup kubectl port-forward -n $(NAMESPACE)  svc/lgtm               3001:3000 > $(FWD_DIR)/grafana.log            2>&1 & echo $$! > $(FWD_DIR)/grafana.pid
	@nohup kubectl port-forward -n $(NAMESPACE)  svc/kafdrop            9000:9000 > $(FWD_DIR)/kafdrop.log             2>&1 & echo $$! > $(FWD_DIR)/kafdrop.pid
	@nohup kubectl port-forward -n kube-system   deployment/traefik     9090:9090 > $(FWD_DIR)/traefik-dashboard.log   2>&1 & echo $$! > $(FWD_DIR)/traefik-dashboard.pid
	@nohup kubectl port-forward -n $(NAMESPACE)  svc/postgres           5432:5432 > $(FWD_DIR)/postgres.log            2>&1 & echo $$! > $(FWD_DIR)/postgres.pid
	@sleep 1
	@echo "✅ Grafana:  http://localhost:3001"
	@echo "✅ Kafdrop:  http://localhost:9000"
	@echo "✅ Traefik:  http://localhost:9090/dashboard/"
	@echo "✅ Postgres: postgresql://postgres:postgres@localhost:5432/social"
	@echo "(make forward-status để xem lại, make forward-down để tắt hết)"

## Tắt hết port-forward đang chạy nền (idempotent, không lỗi nếu đã tắt sẵn)
forward-down:
	@mkdir -p $(FWD_DIR)
	@for f in $(FWD_DIR)/*.pid; do \
		[ -f "$$f" ] || continue; \
		pid=$$(cat "$$f"); \
		if kill $$pid 2>/dev/null; then echo "Stopped $$(basename $$f .pid) (pid $$pid)"; fi; \
		rm -f "$$f"; \
	done

## Xem port-forward nào đang chạy nền
forward-status:
	@found=0; \
	for f in $(FWD_DIR)/*.pid; do \
		[ -f "$$f" ] || continue; found=1; \
		pid=$$(cat "$$f"); \
		if kill -0 $$pid 2>/dev/null; then echo "✅ $$(basename $$f .pid) — running (pid $$pid)"; \
		else echo "❌ $$(basename $$f .pid) — chết nhưng còn pid file (chạy make forward-up lại)"; fi; \
	done; \
	[ $$found -eq 1 ] || echo "Không có port-forward nào đang chạy nền (make forward-up để bật)"

deploy-services:
	kubectl apply -f k8s/services/

## Deploy service lần lượt theo thứ tự phụ thuộc, đợi Ready mới sang cái tiếp
## theo — tránh CPU/memory storm (khuyên dùng thay deploy-services trên máy
## resource hạn chế). Xem specs/service-dependencies.md
deploy-services-sequential:
	@scripts/deploy-sequential.sh

## Deploy Debezium sau khi services đã up (outbox tables phải tồn tại trước)
deploy-debezium:
	kubectl apply -f k8s/infra/debezium.yaml
	@echo "Waiting for kafka-connect to be ready..."
	kubectl rollout status deployment/kafka-connect -n $(NAMESPACE) --timeout=120s

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

.PHONY: up up-% cluster-create cluster-delete shutdown

## Build + load + deploy tất cả (full workflow)
up: build load deploy
	@echo "✅ All services deployed to kind cluster '$(CLUSTER)'"

# Tên deployment thật cho từng nhóm build-%/load-% — dùng cho up-% restart.
DEPLOYS_auth         := auth-service
DEPLOYS_user         := user-api user-consumer
DEPLOYS_post         := post-api post-consumer
DEPLOYS_interaction  := interaction-service
DEPLOYS_notification := notification-api notification-consumer
DEPLOYS_websocket    := websocket-service

## Build + load + restart 1 service: make up-post
up-%: build-% load-%
	@for d in $(DEPLOYS_$*); do \
		kubectl rollout restart deployment/$$d -n $(NAMESPACE) 2>/dev/null || true; \
	done
	@echo "✅ $* updated"

# ── Cluster lifecycle ─────────────────────────────────────────────────────────

cluster-create:
	kind create cluster --name $(CLUSTER) --config infra/kind-cluster.yaml
	helm repo add traefik https://helm.traefik.io/traefik && helm repo update
	# Chart mặc định có port nội bộ "traefik" (dashboard/API) CŨNG default
	# containerPort=8080 — đụng thẳng port "web" mình set 8080, Helm reject
	# vì trùng containerPort (release status "failed", không pod nào lên,
	# im lặng không báo rõ). Dời dashboard sang 9090 (đúng ý định ban đầu —
	# xem docs/k8s-getting-started.md, infra/traefik/traefik.yaml bản local
	# cũng dùng :9090 cho dashboard) để hết đụng port.
	# upgrade --install thay vì install để idempotent, chạy lại được nếu
	# release cũ ở trạng thái failed.
	# extraPortMappings của kind (infra/kind-cluster.yaml) chỉ forward
	# host:8080 → đúng node control-plane — Traefik phải bị ghim vào node
	# đó (nodeSelector + toleration, vì control-plane có taint NoSchedule
	# mặc định) không thì host:8080 gõ vào node không ai lắng nghe.
	# ingressRoute.dashboard.enabled mặc định false — api.dashboard=true
	# (default chart) chỉ bật tính năng, chưa tạo route thật, thiếu dòng
	# này thì /dashboard/ luôn 404 dù đã port-forward đúng.
	helm upgrade --install traefik traefik/traefik \
		--namespace kube-system \
		--set ports.web.port=8080 \
		--set ports.web.containerPort=8080 \
		--set ports.web.hostPort=8080 \
		--set ports.traefik.port=9090 \
		--set service.type=NodePort \
		--set 'nodeSelector.kubernetes\.io/hostname=$(CLUSTER)-control-plane' \
		--set 'tolerations[0].key=node-role.kubernetes.io/control-plane' \
		--set 'tolerations[0].operator=Exists' \
		--set 'tolerations[0].effect=NoSchedule' \
		--set ingressRoute.dashboard.enabled=true \
		--wait

cluster-delete:
	kind delete cluster --name $(CLUSTER)

## Xoá hoàn toàn cluster, có xác nhận + dọn port-forward chạy nền trước (khuyên dùng thay cluster-delete)
shutdown:
	@scripts/shutdown-k8s.sh

# ── Frontend (local dev) ─────────────────────────────────────────────────────

.PHONY: frontend-install frontend-dev frontend-typecheck frontend-test frontend-build

## Cài dependencies FE — chỉ cần chạy lại khi package.json đổi
frontend-install:
	cd frontend && npm install

## Chạy FE dev server (Vite :3000) — proxy /api → Traefik gateway :8080
## (xem frontend/vite.config.ts), cần backend đã chạy trước (make status)
frontend-dev:
	cd frontend && npm run dev

## Type-check FE không build (tsc --noEmit)
frontend-typecheck:
	cd frontend && npm run typecheck

## Chạy FE unit test (vitest)
frontend-test:
	cd frontend && npm run test

## Build FE production bundle (static — phục vụ qua nginx/CDN, chưa có trong k8s/)
frontend-build:
	cd frontend && npm run build

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
