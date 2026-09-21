# Mini Social Network

Trước khi đưa ra quyết định kiến trúc/schema/dependency/API contract, đọc
`specs/constitution.md` và `specs/decisions/` (log các quyết định đã chốt).
Quyết định đáng kể mới → ghi 1 ADR ngắn vào `specs/decisions/` theo format
trong `specs/decisions/README.md`.

`specs/` còn chứa các design doc khác (feature tracker, plan, api-contract,
k8s study...) — đó là tài liệu thiết kế, không phải ADR log; giữ nguyên vai
trò riêng, không trộn vào `specs/decisions/`.

## Testing workflow

Sau khi sửa code frontend, **không tự động chạy Playwright**. Dùng
`npm run test` (Vitest) trong `frontend/` để verify thường ngày. Chỉ chạy
Playwright khi: (a) được yêu cầu rõ ràng test E2E/visual, hoặc (b) trước khi
kết thúc 1 feature UI lớn — không phải sau mỗi lần edit nhỏ. Điều này ghi đè
default "Browser testing → Playwright MCP > unit tests" của framework, chỉ
áp dụng cho project này.
