# 0001: Expose OpenAPI/Swagger UI gộp qua Traefik, dùng aggregator có sẵn của Quarkus

Status: accepted

## Context

Cần 1 cách test nhanh REST API của 5 service (auth, user, post, interaction,
notification) qua Swagger UI, dùng chung 1 cổng cho cả 2 môi trường (docker-
compose local và kind/k8s), mà không phải nhớ port riêng từng service.

## Decision

Thêm `quarkus-smallrye-openapi` vào 5 service REST. `auth-service` đóng vai
trang tổng hợp: `quarkus.swagger-ui.urls.*` trỏ tới `/q/openapi` của chính nó
và `/openapi/{users,post,interaction,notifications}` của 4 service kia (path
riêng từng service để tránh trùng `/q/openapi` khi gộp qua 1 gateway). Dùng
tính năng `swagger-ui.urls.*` có sẵn của Quarkus thay vì dựng thêm container
`swagger-ui` độc lập. Traefik (`infra/traefik/dynamic/routes.yaml` local và
`k8s/traefik/ingressroute.yaml` k8s) thêm router public — không qua middleware
`jwt-verify` — cho prefix `/q` và `/openapi/*`.

## Consequences

- Docs endpoint (`/q/swagger-ui`, `/q/openapi`, `/openapi/*`) không yêu cầu
  JWT để load — chỉ "Try it out" mới cần paste token vào nút Authorize. Chấp
  nhận được vì đây là dev tooling, không phải API nghiệp vụ; không dùng cho
  production thật.
- Mỗi service REST cần thêm dependency `quarkus-smallrye-openapi` +
  `quarkus.smallrye-openapi.path` riêng — nếu thêm REST service mới sau này
  phải nhớ đặt path không trùng và thêm router Traefik tương ứng, không tự
  động.
- Đã verify compile + `quarkusDev` + unit test (`./gradlew :auth-service:test`
  và compileKotlin cả 5 service) pass. **Chưa verify được qua
  `./gradlew build`** (JIB packaging) — gặp bug BOM version có sẵn từ trước
  (`quarkus-bom:3.25.1` vs `quarkus-amazon-services-bom:3.20.1` khác platform
  stream, từ commit `b71f464`), không liên quan tới thay đổi này, chặn
  packaging của **mọi** service. Chưa có quyết định fix — xem việc cần làm
  tiếp trong `tracking.md`.
