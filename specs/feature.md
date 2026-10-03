# Feature List

> Legend: ✅ Done · 🔧 Partial · ⬜ Planned

---

## 1. Authentication

| # | Feature | Status | Notes |
|---|---|---|---|
| 1.1 | Đăng ký tài khoản | ✅ | username (≤50) · email · password (BCrypt, ≥6) · firstname/lastname (≤100, khớp cột `user_profile`) · trả về JWT ngay · FE chờ tín hiệu `USER_READY` qua WS trước khi gọi `/me` |
| 1.2 | Đăng nhập | ✅ | `identifier` chấp nhận username hoặc email |
| 1.3 | JWT access token | ✅ | RS256 · 7 ngày · claims: sub, username, roles |
| 1.4 | Refresh token | ⬜ | `POST /api/auth/refresh` |
| 1.5 | Đăng xuất (revoke token) | ⬜ | Redis blacklist |
| 1.6 | Quên mật khẩu | ⬜ | — |

---

## 2. User Profile

| # | Feature | Status | Notes |
|---|---|---|---|
| 2.1 | Xem profile bản thân | ✅ | `GET /api/users/me` |
| 2.2 | Xem profile người khác | ✅ | `GET /api/users/{id}` · Redis cache 10 min |
| 2.3 | Cập nhật profile | ✅ | firstname · lastname · avatarUrl · bắn `USER_PROFILE_UPDATED` (outbox) để cache tên ở service khác cập nhật theo |
| 2.4 | Upload avatar | ⬜ | S3 presigned URL (tương tự post image) |
| 2.5 | Follow / Unfollow | ⬜ | — |

---

## 3. Bài đăng (Post)

| # | Feature | Status | Notes |
|---|---|---|---|
| 3.1 | Tạo bài đăng (text) | ✅ | `POST /api/articles` |
| 3.2 | Tạo bài đăng (text + ảnh) | ✅ | S3 presigned URL upload |
| 3.3 | Xem feed (tất cả bài) | ✅ | Paginated · count từ Redis, flush về DB mỗi 30s (comment count bị hỏng từ trước tới 29/9/2026 — post-consumer đọc sai key `targetId`, đã sửa thành `articleId`) |
| 3.4 | Xem chi tiết bài đăng | ✅ | `GET /api/articles/{id}` · FE route `/articles/:id` (`ArticleDetailPage.tsx`) |
| 3.5 | Xoá bài đăng | ✅ | Soft delete · chỉ owner · `DELETE /api/articles/{id}` |
| 3.6 | Sửa bài đăng | ⬜ | — |
| 3.7 | Feed cá nhân (của 1 user) | ⬜ | `GET /api/users/{id}/articles` |
| 3.8 | Tìm kiếm bài đăng | ⬜ | Full-text search — xem `specs/search-plan.md` |

---

## 4. Bình luận (Comment)

| # | Feature | Status | Notes |
|---|---|---|---|
| 4.1 | Thêm bình luận | ✅ | `POST /api/comments` (`targetId`, `targetType`, `description`) · max 1000 ký tự |
| 4.2 | Xem bình luận | ✅ | `GET /api/comments?targetId=&targetType=` · paginated · tên tác giả enrich sẵn từ backend (ADR 0006) |
| 4.3 | Xoá bình luận | ✅ | Soft delete · chỉ author · `DELETE /api/comments/{id}` |
| 4.4 | Sửa bình luận | ⬜ | — |
| 4.5 | Reply bình luận (nested) | ⬜ | — |

---

## 5. Vote (Like / Dislike)

| # | Feature | Status | Notes |
|---|---|---|---|
| 5.1 | Vote bài đăng | ✅ | `POST /api/votes` (`targetType=ARTICLE`) · +1 / 0 / -1 · UNIQUE per user |
| 5.2 | Vote bình luận | ✅ | `POST /api/votes` (`targetType=COMMENT`) · backend hỗ trợ, FE chưa có nút vote cho comment |
| 5.3 | Xem tổng vote | ✅ | Real-time từ Redis · flush về DB mỗi 30s |
| 5.4 | Rút vote | ✅ | value = 0 |

---

## 6. Thông báo (Notification)

| # | Feature | Status | Notes |
|---|---|---|---|
| 6.1 | Nhận thông báo khi có comment | ✅ | Kafka async · topic `social.interaction` |
| 6.2 | Nhận thông báo khi có vote | ✅ | Kafka async |
| 6.3 | Xem danh sách thông báo | ✅ | `GET /api/notifications` · paginated |
| 6.4 | Đếm chưa đọc (badge) | ✅ | `GET /api/notifications/unread-count` · Redis counter · poll 30s |
| 6.5 | Đánh dấu đã đọc (1 cái) | ✅ | `PATCH /api/notifications/{id}/seen` |
| 6.6 | Đánh dấu tất cả đã đọc | ✅ | `PATCH /api/notifications/seen-all` |
| 6.7 | Push notification (web) | ✅ | `websocket-service` push `NOTIFICATION` · FE `useNotificationSocket` (kết nối sống suốt session, tự reconnect) hiện toast realtime |
| 6.8 | Bấm vào notification → tới đích | ✅ | Toast (nút "View") và item trong list → `/articles/:id` (toast còn highlight đúng comment) · resolver dùng chung `lib/notificationTarget.ts` |
| 6.9 | Hiện tên thật của actor | ✅ | Denormalized lúc tạo notification (ADR 0006) · noti tạo trước 21/9/2026 hiện "Someone" |
| 6.10 | Xác thực WebSocket | ✅ | JWT qua `Sec-WebSocket-Protocol` · chỉ subscribe được `user_{x}_*` của chính mình (ADR 0009) · FE dùng chung 1 kết nối `lib/wsClient.ts` |

---

## 7. Image Upload

| # | Feature | Status | Notes |
|---|---|---|---|
| 7.1 | Upload ảnh cho bài đăng | ✅ | `POST /api/articles/images/presign` · browser PUT thẳng lên S3 |
| 7.2 | Xoá ảnh khi xoá bài | ⬜ | S3 cleanup |
| 7.3 | Resize / compress ảnh | ⬜ | Client-side trước khi upload |

---

## 8. Feed & Discovery

| # | Feature | Status | Notes |
|---|---|---|---|
| 8.1 | Feed tất cả bài (mới nhất) | ✅ | Sort by `created_at DESC` |
| 8.2 | Feed theo người đang follow | ⬜ | Cần feature Follow |
| 8.3 | Trending feed | ⬜ | `?sort=trending` · sort theo vote_count |
| 8.4 | Tìm kiếm người dùng | ⬜ | — |

---

## 9. Frontend UX

| # | Feature | Status | Notes |
|---|---|---|---|
| 9.1 | Inline comments trong card | ✅ | `ArticleCard.tsx` nhúng thẳng `CommentSection` (toggle `showComments`), không phải modal |
| 9.2 | Optimistic update (like/comment) | 🔧 | Vote có optimistic + rollback (`ArticleCard.tsx` `onMutate`/`onError`); comment chưa |
| 9.3 | Infinite scroll | ✅ | `FeedPage.tsx` dùng `useInfiniteQuery` (TanStack Query) + `useInView` (react-intersection-observer) |
| 9.4 | Relative timestamp | ✅ | `lib/utils.ts` `relativeTime()` (date-fns `formatDistanceToNow`), dùng trong `ArticleCard`/`CommentSection`/`NotificationsPage` |
| 9.5 | Loading & skeleton states | 🔧 | Có spinner (MUI `CircularProgress`), chưa có skeleton card |
| 9.6 | Empty states | ✅ | `FeedPage.tsx`, `ProfilePage.tsx` đã có empty state khi feed rỗng |
| 9.7 | Responsive mobile layout | ✅ | `Sidebar.tsx` có `BottomNav` (MUI, ẩn ở màn hình lớn), dùng trong `RootLayout.tsx` |
| 9.8 | Image drag & drop + preview | 🔧 | `CreatePost.tsx` có preview ảnh sau khi chọn, nhưng chỉ qua `<input type=file>`, chưa có onDrop/dragover |
| 9.9 | Image validation (client) | 🔧 | `CreatePost.tsx` check size ≤ 5MB có; check type chỉ qua `accept="image/*"` (lỏng hơn spec jpg/png/webp cụ thể) |
| 9.10 | Form validation (Zod) | ✅ | `LoginPage.tsx`, `SignUpPage.tsx`, `ProfilePage.tsx` dùng `useForm` + `zodResolver` + `z.object` |
| 9.11 | Dark mode | ⬜ | Không có gì liên quan "dark" trong `frontend/src` |
| 9.12 | PWA + offline support | ⬜ | Nice to have |
| 9.13 | Màn hình chờ khi tạo tài khoản | ✅ | `SignUpPage.tsx` hiện loading trong lúc chờ `USER_READY` (`lib/waitForUserReady.ts`, timeout 8s) |

---

## 10. Nhắn tin (Chat) — `specs/messaging-plan.md`

| # | Feature | Status | Notes |
|---|---|---|---|
| 10.1 | Nhắn tin 1:1 | ✅ | `chat-api` · `POST /api/conversations/{id}/messages` · tin ≤ 4.096 ký tự (ADR 0008) |
| 10.2 | Mở chat từ profile người khác | ✅ | FE route `/users/:id` + nút "Message" · tên/avatar tác giả ở bài viết/comment là link · conversation ID tất định, mở lại luôn ra cùng 1 |
| 10.3 | Danh sách conversation | ✅ | `/messages` · mới nhất trước · ẩn conversation chưa có tin · xem trước tin cuối |
| 10.4 | Nhận tin realtime | ✅ | WS topic `user_{id}_chat` · tải lại khi reconnect |
| 10.5 | Icon chat + badge chưa đọc | ✅ | Tách khỏi chuông notification · đếm số conversation chưa đọc · đánh dấu đã đọc khi mở thread / tab hiện lại |
| 10.6 | Mã hoá tin nhắn | ✅ | Envelope encryption qua KMS (LocalStack) · AES-256-GCM · chỉ bản mã trong DB lẫn trên Kafka (ADR 0007) |
| 10.7 | Chống gửi trùng + gửi lại | ✅ | `clientMessageId` · tin hiện ngay ở trạng thái "Sending…", lỗi thì nút retry |
| 10.8 | Read receipt ("đã xem") | ⬜ | Phase 2 |
| 10.9 | Typing indicator | ⬜ | Phase 2 |
| 10.10 | Group chat | ⬜ | Phase 2 |
| 10.11 | Đính kèm ảnh | ⬜ | Phase 2 |
| 10.12 | Chặn người dùng / tin nhắn chờ | ⬜ | Phase 2 — MVP ai cũng nhắn được cho ai |

---

## Summary

| | Tổng | Done | Partial | Planned |
|---|---|---|---|---|
| Authentication | 6 | 3 | 0 | 3 |
| User Profile | 5 | 3 | 0 | 2 |
| Post | 8 | 5 | 0 | 3 |
| Comment | 5 | 3 | 0 | 2 |
| Vote | 4 | 4 | 0 | 0 |
| Notification | 10 | 10 | 0 | 0 |
| Image Upload | 3 | 1 | 0 | 2 |
| Feed & Discovery | 4 | 1 | 0 | 3 |
| Frontend UX | 13 | 7 | 4 | 2 |
| Chat | 12 | 7 | 0 | 5 |
| **Total** | **70** | **44** | **4** | **22** |

> Cập nhật 2/10/2026 — thêm mục 10 (chat 1:1, messaging-plan Phase 1) và 6.10 (xác thực WS,
> ADR 0009), verify live trên kind cluster.
>
> Bản 29/9/2026 — đối chiếu lại với code thật sau đợt audit specs ↔
> implementation: sửa path cũ ở 4.1/4.2/5.1 (endpoint thật là `/api/comments`,
> `/api/votes`), topic 6.1 (`social.interaction`, không phải `social.events`),
> 9.2/9.4 hoá ra đã làm (bảng cũ ghi `⬜`), bỏ tham chiếu component/class
> Tailwind đã xoá khi migrate sang MUI (9.5, 9.7); thêm 6.8, 6.9, 9.13 cho các
> feature làm trong tháng 9. Bản trước: 16/9/2026.
