# Feature List

> Legend: ✅ Done · 🔧 Partial · ⬜ Planned

---

## 1. Authentication

| # | Feature | Status | Notes |
|---|---|---|---|
| 1.1 | Đăng ký tài khoản | ✅ | username · email · password (BCrypt) · trả về JWT ngay |
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
| 2.3 | Cập nhật profile | ✅ | firstname · lastname · avatarUrl |
| 2.4 | Upload avatar | ⬜ | S3 presigned URL (tương tự post image) |
| 2.5 | Follow / Unfollow | ⬜ | — |

---

## 3. Bài đăng (Post)

| # | Feature | Status | Notes |
|---|---|---|---|
| 3.1 | Tạo bài đăng (text) | ✅ | `POST /api/articles` |
| 3.2 | Tạo bài đăng (text + ảnh) | ✅ | S3 presigned URL upload |
| 3.3 | Xem feed (tất cả bài) | ✅ | Paginated · real-time count từ Redis |
| 3.4 | Xem chi tiết bài đăng | ✅ | `GET /api/articles/{id}` |
| 3.5 | Xoá bài đăng | ✅ | Soft delete · chỉ owner · `DELETE /api/articles/{id}` |
| 3.6 | Sửa bài đăng | ⬜ | — |
| 3.7 | Feed cá nhân (của 1 user) | ⬜ | `GET /api/users/{id}/articles` |
| 3.8 | Tìm kiếm bài đăng | ⬜ | Full-text search |

---

## 4. Bình luận (Comment)

| # | Feature | Status | Notes |
|---|---|---|---|
| 4.1 | Thêm bình luận | ✅ | `POST /api/articles/{id}/comments` · max 1000 ký tự |
| 4.2 | Xem bình luận | ✅ | `GET /api/articles/{id}/comments` · paginated |
| 4.3 | Xoá bình luận | ✅ | Soft delete · chỉ author · `DELETE /api/comments/{id}` |
| 4.4 | Sửa bình luận | ⬜ | — |
| 4.5 | Reply bình luận (nested) | ⬜ | — |

---

## 5. Vote (Like / Dislike)

| # | Feature | Status | Notes |
|---|---|---|---|
| 5.1 | Vote bài đăng | ✅ | `POST /api/articles/{id}/vote` · +1 / 0 / -1 · UNIQUE per user |
| 5.2 | Vote bình luận | ✅ | `POST /api/votes` (payload chung article/comment, không phải `/api/comments/{id}/vote` như ghi trước — đã sửa theo code thật `InteractionResource.kt`) · cùng cơ chế |
| 5.3 | Xem tổng vote | ✅ | Real-time từ Redis · flush về DB mỗi 30s |
| 5.4 | Rút vote | ✅ | value = 0 |

---

## 6. Thông báo (Notification)

| # | Feature | Status | Notes |
|---|---|---|---|
| 6.1 | Nhận thông báo khi có comment | ✅ | Kafka async · topic: social.events |
| 6.2 | Nhận thông báo khi có vote | ✅ | Kafka async |
| 6.3 | Xem danh sách thông báo | ✅ | `GET /api/notifications` · paginated |
| 6.4 | Đếm chưa đọc (badge) | ✅ | `GET /api/notifications/unread-count` · Redis counter |
| 6.5 | Đánh dấu đã đọc (1 cái) | ✅ | `PATCH /api/notifications/{id}/seen` |
| 6.6 | Đánh dấu tất cả đã đọc | ✅ | `PATCH /api/notifications/seen-all` |
| 6.7 | Push notification (web) | ✅ | `websocket-service` (`WsEndpoint.kt` — `@WebSocket(path="/ws")`), consume Kafka `social.interaction`, push qua `WsPushService`/`TopicRegistry` |

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
| 9.2 | Optimistic update (like/comment) | ⬜ | Không thấy pattern `onMutate`/rollback trong mutation hooks |
| 9.3 | Infinite scroll | ✅ | `FeedPage.tsx` dùng `useInfiniteQuery` (TanStack Query) + `useInView` (react-intersection-observer) |
| 9.4 | Relative timestamp | ⬜ | `date-fns` có trong `package.json` nhưng không thấy dùng (`formatDistanceToNow`) trong pages/components |
| 9.5 | Loading & skeleton states | 🔧 | Có `Spinner` (`components/ui`), chưa có skeleton card |
| 9.6 | Empty states | ✅ | `FeedPage.tsx`, `ProfilePage.tsx` đã có empty state khi feed rỗng |
| 9.7 | Responsive mobile layout | ✅ | `Sidebar.tsx` có `BottomNav` (`fixed bottom-0 ... lg:hidden`), dùng trong `RootLayout.tsx` |
| 9.8 | Image drag & drop + preview | 🔧 | `CreatePost.tsx` có preview ảnh sau khi chọn, nhưng chỉ qua `<input type=file>`, chưa có onDrop/dragover |
| 9.9 | Image validation (client) | 🔧 | `CreatePost.tsx` check size ≤ 5MB có; check type chỉ qua `accept="image/*"` (lỏng hơn spec jpg/png/webp cụ thể) |
| 9.10 | Form validation (Zod) | ✅ | `LoginPage.tsx`, `SignUpPage.tsx` dùng `useForm` + `zodResolver` + `z.object` |
| 9.11 | Dark mode | ⬜ | Không có gì liên quan "dark" trong `frontend/src` |
| 9.12 | PWA + offline support | ⬜ | Nice to have |

---

## Summary

| | Tổng | Done | Partial | Planned |
|---|---|---|---|---|
| Authentication | 6 | 3 | 0 | 3 |
| User Profile | 5 | 3 | 0 | 2 |
| Post | 8 | 5 | 0 | 3 |
| Comment | 5 | 3 | 0 | 2 |
| Vote | 4 | 4 | 0 | 0 |
| Notification | 7 | 7 | 0 | 0 |
| Image Upload | 3 | 1 | 0 | 2 |
| Feed & Discovery | 4 | 1 | 0 | 3 |
| Frontend UX | 12 | 5 | 3 | 4 |
| **Total** | **54** | **32** | **3** | **19** |

> Cập nhật 16/9/2026 — đối chiếu lại toàn bộ bảng với code thật (`grep @Path`
> trong `*Resource.kt`, review `frontend/src/pages`+`components`), không phải
> chỉnh tay theo cảm tính. Thay đổi so với bản gốc (đầu 6/2026): 6.7 (push
> notification qua `websocket-service`) và 5/12 mục Frontend UX (9.1, 9.3,
> 9.6, 9.7, 9.10) hoá ra đã làm xong nhưng bảng cũ ghi `⬜`; thêm 3 mục
> `🔧 Partial` (9.5, 9.8, 9.9) trước đó gộp chung vào `⬜`.
