# 0009: Xác thực WebSocket bằng JWT qua Sec-WebSocket-Protocol, chỉ subscribe topic của chính mình

Status: accepted

## Context
`websocket-service` public từ đầu (`websocket-plan.md`: "topic name không chứa data nhạy cảm"). Tiền đề
đó đã sai: userId lộ trong mọi `ArticleDto`/`CommentDto`, topic `user_{id}_notification` đoán được,
payload có tên actor (ADR 0006) → ai cũng nghe được noti của người khác, kể cả ai vote bài họ. Chat
sắp làm khiến việc này thành blocker: `websocket-service` là nơi giải mã tin nhắn (ADR 0007) và
conversationId tính được từ 2 userId (ADR 0008). Trình duyệt không set được header `Authorization`
cho WebSocket.

## Decision
- **Xác thực lúc HTTP upgrade:** client gửi token qua `Sec-WebSocket-Protocol`:
  `new WebSocket(url, ["bearer-token-carrier", encodeURIComponent("quarkus-http-upgrade#Authorization#Bearer " + token)])`.
  Server bật `quarkus.websockets-next.server.supported-subprotocols=bearer-token-carrier` +
  `quarkus.websockets-next.server.propagate-subprotocol-headers=true` (build-time, đã kiểm có trong
  Quarkus 3.25.1 — tính năng có từ 3.19) + `@Authenticated` trên `WsEndpoint`. Verify JWT giống mọi
  service khác (`quarkus-smallrye-jwt`, public key từ secret `jwt-keys`).
- **Phân quyền SUBSCRIBE:**
  - `user_{x}_*` → chỉ khi `x == jwt.subject` (dùng `subject`, không dùng `principal.name`);
  - `article_{id}_comment_added` → mọi user đã xác thực (comment vốn đã công khai với họ);
  - topic khác → từ chối.
- **FE:** 1 WebSocket client dùng chung (1 kết nối/tab, subscribe/unsubscribe, reconnect + subscribe
  lại), `useNotificationSocket` và `waitForUserReady` chuyển sang dùng nó; đóng kết nối khi logout.
- Không bắt buộc hạ log subscribe nữa: khi có xác thực, tên topic không còn là bí mật.

## Consequences
- Vá lỗ hổng notification hiện có — nên làm **trước** chat, độc lập với chat.
- `websocket-service` thêm dependency `quarkus-smallrye-jwt` + mount secret `jwt-keys` + env verify
  như các service khác; `make k8s-secrets` phải chạy trước (đã là prerequisite của deploy).
- Không kiểm lại token giữa chừng: kết nối mở trước khi token hết hạn (7 ngày) vẫn sống tới khi đóng
  — chấp nhận ở MVP; logout thì FE chủ động đóng.
- Token nằm trong header `Sec-WebSocket-Protocol` của request upgrade — Traefik mặc định không log
  header, khác cách để token trong query string (bị log nguyên URL).

## Nguồn
- Quarkus WebSockets Next — bearer token từ trình duyệt qua Sec-WebSocket-Protocol: https://quarkus.io/guides/websockets-next-reference
