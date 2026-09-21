# Vì sao port-forward suông không đủ để local dev service nói chuyện với Kafka trong cluster

## Câu hỏi ban đầu

Muốn chạy 1 service local (`quarkusDev` trên máy host) nhưng vẫn cần nó nối
được vào Postgres/Redis/S3/Kafka thật đang chạy trong cluster k8s. Postgres,
Redis, S3 (localstack), và gọi REST sang service khác — chỉ cần
`kubectl port-forward` là đủ, service local vẫn nói chuyện được bình
thường. Kafka thì không — vì sao?

## Cấu hình hiện tại

```yaml
# k8s/infra/kafka.yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
```

Broker chỉ quảng bá hostname `kafka` — DNS nội bộ cluster, chỉ resolve được
từ bên trong cluster (namespace `social`).

## Vì sao port-forward đơn thuần không đủ

Kafka protocol hoạt động 2 bước, khác hẳn 1 kết nối TCP đơn giản kiểu
Postgres/Redis:

1. **Bootstrap**: client kết nối vào địa chỉ được cho ban đầu
   (`localhost:9092` qua port-forward — bước này thành công).
2. **Metadata redirect**: broker trả về metadata nói "leader của partition
   này ở địa chỉ `kafka:9092`" (đúng giá trị `KAFKA_ADVERTISED_LISTENERS`).
   Client **tự động ngắt kết nối bootstrap và reconnect** sang đúng địa chỉ
   đó để produce/consume thật.

Bước 2 mới là chỗ vỡ: hostname `kafka` không resolve được từ máy host (nó
chỉ tồn tại trong DNS nội bộ cluster) — port-forward xong vẫn treo ở bước
reconnect này, không phải hairpin NAT (khác bản chất bug hairpin NAT đã fix
trong `tracking.md` cho traffic NỘI BỘ cluster — đây là vấn đề DNS resolve
từ NGOÀI cluster).

Postgres/Redis/S3/REST API khác không gặp vấn đề này vì giao thức của chúng
không có bước "redirect sang địa chỉ khác sau kết nối ban đầu" — 1 lần kết
nối là xong.

## 2 cách xử lý thật (không cách nào miễn phí)

1. **Hack nhanh, 1 máy**: thêm `127.0.0.1 kafka` vào `/etc/hosts` +
   `kubectl port-forward svc/kafka 9092:9092` — hostname `kafka` giờ
   resolve đúng về port-forward trên chính máy đó. Sửa file hệ thống, cần
   `sudo`, chỉ áp dụng cho máy đang sửa.
2. **Đúng bài hơn**: thêm 1 listener ngoài riêng
   (`EXTERNAL://localhost:9092` song song `PLAINTEXT://kafka:9092`) — pattern
   chuẩn để expose Kafka-on-k8s ra ngoài cluster mà không cần hack DNS máy
   local. Là thay đổi hạ tầng có chủ đích (thêm port, thêm advertised
   listener), chưa làm.

**Chưa áp dụng** — ghi nhận cho lần cần dev 1 service có publish/consume
Kafka mà không muốn chạy nguyên cụm k8s.

## Bài học tổng quát

Không phải mọi giao thức TCP đều "port-forward là xong". Bất kỳ giao thức
nào có bước redirect-sau-kết-nối (Kafka, 1 số setup Redis Cluster, gRPC với
service mesh...) đều cần địa chỉ redirect đó **cũng resolve được từ phía
client**, không chỉ địa chỉ bootstrap ban đầu.
