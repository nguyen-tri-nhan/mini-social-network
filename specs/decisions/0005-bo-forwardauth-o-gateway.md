# 0005: Bỏ ForwardAuth ở Traefik gateway, mỗi service tự verify JWT

Status: accepted

## Context
Middleware `jwt-verify` (ForwardAuth → `http://auth-service:8080/api/auth/verify`)
gắn trên mọi route protected, nhưng chưa từng chạy được từ đầu: (1) Traefik
chạy ở namespace `kube-system`, tên Service trần `auth-service` không resolve
được cross-namespace ("no such host"); (2) endpoint `/api/auth/verify` chưa
từng được implement ở `auth-service` (chỉ có `/signup`, `/signin`) — verify
bằng `curl` trực tiếp → 404. Mọi request qua route protected bị Traefik trả
500 trước khi tới service. Đồng thời phát hiện mọi service (vd `user-api`)
đã tự verify JWT cục bộ qua `mp.jwt.verify.publickey.location` +
`@RolesAllowed`/`JsonWebToken` — không service nào đọc header `X-User-Id` mà
ForwardAuth định forward xuống (grep toàn `services/` ra 0 kết quả).

## Decision
Xoá hẳn Middleware `jwt-verify` (ForwardAuth) khỏi `k8s/traefik/ingressroute.yaml`
và `infra/traefik/dynamic/routes.yaml`. Traefik chỉ route theo path; mỗi
service tiếp tục tự verify JWT bằng public key như đang làm — đây vốn đã là
cơ chế xác thực thật sự duy nhất hoạt động, ForwardAuth chỉ là lớp trùng lặp
chưa từng chạy.

## Consequences
Không còn double-check JWT ở gateway (giảm 1 network hop/request, nhưng cũng
mất layer "chặn sớm trước khi vào service" — chấp nhận được vì mỗi service
vẫn tự chặn đúng). Nếu sau này muốn có gateway-level auth thật (vd rate-limit
theo user, hoặc chuẩn hoá lỗi 401 tại 1 điểm), cần implement `/api/auth/verify`
thật ở `auth-service` và fix đúng tên DNS (`auth-service.social.svc.cluster.local`)
trước khi bật lại ForwardAuth.
