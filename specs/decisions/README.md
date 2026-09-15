# Decisions log (ADR)

Mỗi quyết định kiến trúc/schema/dependency/API contract đáng kể → 1 file ở
đây. Xem `specs/constitution.md` § "Phạm vi của specs/decisions/" cho tiêu
chí khi nào cần ghi.

## Đặt tên file

```
NNNN-slug-ngan-gon.md
```
`NNNN` = số thứ tự tăng dần, 4 chữ số (`0001`, `0002`, ...). Project cá nhân,
1 người sửa → không cần đổi sang date-slug để tránh conflict (chỉ cần khi có
nhiều người cùng tạo ADR song song trên các branch khác nhau).

## Format

```markdown
# NNNN: Tiêu đề ngắn, dạng quyết định (không phải câu hỏi)

Status: accepted | superseded by NNNN

## Context
Vấn đề/tình huống buộc phải quyết định. 1-3 câu.

## Decision
Đã chọn gì. 1-3 câu.

## Consequences
Đánh đổi/hệ quả cần nhớ sau này. 1-3 câu.
```

Ngắn có chủ đích — đây là log tra cứu nhanh, không phải design doc đầy đủ
(design doc dài đã có sẵn trong các file khác của `specs/`).

## Khi 1 quyết định bị thay thế

Đừng sửa/xoá file cũ. Tạo file mới, ghi `Status: accepted`; sửa file cũ thành
`Status: superseded by NNNN`. Giữ lịch sử nguyên vẹn.
