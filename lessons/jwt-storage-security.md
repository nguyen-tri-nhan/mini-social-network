# localStorage vs httpOnly cookie cho JWT — đánh đổi bảo mật thật

## Bối cảnh

Sau khi fix bug thứ tự lưu token (`setToken` gọi sau `usersApi.me()`, xem
`tracking.md`), rà soát luôn: cách lưu JWT hiện tại (`localStorage`) đã
"chuẩn" chưa.

## Cơ chế hiện tại của project

```ts
// authStore.ts
setToken: (token) => {
  localStorage.setItem('jwt', token)
  set({ token })
}

// client.ts — interceptor gắn thủ công vào mọi request
const token = localStorage.getItem('jwt')
if (token) config.headers.Authorization = `Bearer ${token}`
```

Token nằm trong `localStorage`, gắn thủ công qua header `Authorization` —
không dùng cookie tự động gửi kèm request.

## Đánh giá

**Rủi ro thật của localStorage**: bất kỳ script nào chạy được trên trang
(XSS, kể cả qua 1 dependency npm bị compromise) đọc thẳng được token qua
`localStorage.getItem('jwt')`. Khác với cookie `httpOnly` — JS hoàn toàn
không đọc được cookie đó, kể cả khi có XSS.

**Điểm cộng bị bỏ qua nếu chỉ nhìn 1 chiều**: localStorage + header thủ
công **không dính CSRF** — vì token không tự động gửi kèm mọi request như
cookie thường làm. Cookie-based auth muốn an toàn tương đương phải tự làm
thêm chống CSRF (SameSite, CSRF token riêng...) — không phải "cookie luôn an
toàn hơn" một cách tuyệt đối, chỉ là đổi 1 loại rủi ro (XSS-token-theft) lấy
1 loại rủi ro khác (CSRF) nếu làm ẩu.

**Kiểm tra thực tế trong codebase**: grep toàn `frontend/src` cho
`dangerouslySetInnerHTML`/`innerHTML` → **0 kết quả**. React tự escape nội
dung theo mặc định — chưa có đường XSS rõ ràng nào tồn tại ngay bây giờ để
khai thác rủi ro localStorage nói trên.

## Kết luận đã chốt

**Không đổi sang httpOnly cookie** — đây sẽ là thay đổi kiến trúc lớn hơn
nhiều, không phải sửa 1 dòng:
- `auth-service` phải set cookie (`Set-Cookie`) thay vì trả token trong
  response body.
- Cần cấu hình CORS `credentials: true` xuyên các service.
- Cần tự làm chống CSRF (cookie không tự động an toàn hơn nếu thiếu bước
  này).
- Traefik phải forward cookie đúng qua các route.

Với quy mô project cá nhân/học tập hiện tại, chưa có XSS vector nào tồn
tại, localStorage + JWT RS256 7 ngày là chấp nhận được. Nếu sau này muốn
học sâu phần httpOnly cookie + CSRF, đáng làm ADR riêng (đây là quyết định
kiến trúc auth, không phải bug fix).

## Bài học tổng quát

Đánh giá bảo mật 1 cơ chế không nên dừng ở "X có rủi ro Y" — cần hỏi thêm:
(1) rủi ro Y có đường khai thác thật trong codebase hiện tại không (ở đây:
check `dangerouslySetInnerHTML` trước khi kết luận), và (2) phương án thay
thế có tự động an toàn hơn không, hay chỉ đổi loại rủi ro (XSS ↔ CSRF) và
cần thêm công sức để thật sự an toàn hơn.
