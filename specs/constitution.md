# Constitution

Nguyên tắc chỉ đạo cho project này. File ngắn, hiếm khi đổi. Đọc trước khi ra
quyết định kiến trúc lớn. Xem `specs/decisions/` cho log các quyết định cụ thể
đã chốt theo thời gian.

## Mục đích project

Theo README: đây là project **cố tình over-engineer** để học công nghệ, không
nhắm production best-practice. Mỗi công nghệ được thêm vào vì tác giả muốn
hiểu nó hoạt động thế nào dưới lớp vỏ — không phải vì use case thật sự cần.

Hệ quả cho cách ra quyết định: **ưu tiên "hiểu sâu 1 công nghệ" hơn "đường tắt
đơn giản"** khi 2 lựa chọn đó xung đột — ngược lại với constitution của 1
project production thông thường.

## Nguyên tắc

1. **Mỗi service là 1 cơ hội học, không phải chỉ để chạy được.** Nếu 1 cách
   làm đơn giản hơn nhưng bỏ qua thứ đang muốn học (vd Debezium CDC, Outbox
   pattern) → chọn cách phức tạp hơn có chủ đích.
2. **Outbox pattern là bắt buộc cho mọi event** — service không publish thẳng
   vào Kafka, luôn qua outbox table + Debezium.
3. **Không thêm dependency/service mới chỉ vì tiện** — phải gắn với 1 câu hỏi
   kỹ thuật cụ thể muốn trả lời (xem README "What's Inside" cho danh sách đã
   có).
4. **`specs/feature.md` là nguồn sự thật cho trạng thái feature** — không
   duplicate trạng thái đó ở đây hay trong decisions/.

## Phạm vi của specs/decisions/

Ghi ADR khi: đổi/thêm công nghệ cốt lõi, đổi schema DB xuyên service, đổi
event contract (Kafka topic/payload), đổi cách 2 service giao tiếp.

Không ghi ADR cho: sửa bug, đổi implementation nội bộ 1 service không ảnh
hưởng service khác, refactor không đổi hành vi.
