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
| 5.2 | Vote bình luận | ✅ | `POST /api/comments/{id}/vote` · cùng cơ chế |
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
| 6.7 | Push notification (web) | ⬜ | SSE hoặc WebSocket |

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
| 9.1 | Inline comments trong card | ⬜ | Không cần mở modal |
| 9.2 | Optimistic update (like/comment) | ⬜ | +1 ngay · rollback nếu lỗi |
| 9.3 | Infinite scroll | ⬜ | Thay pagination thủ công |
| 9.4 | Relative timestamp | ⬜ | "2 giờ trước" thay "2-6-2026" |
| 9.5 | Loading & skeleton states | ⬜ | Skeleton cards · spinner |
| 9.6 | Empty states | ⬜ | Feed trống · không có notification |
| 9.7 | Responsive mobile layout | ⬜ | Bottom nav bar · collapse sidebar |
| 9.8 | Image drag & drop + preview | ⬜ | Preview trước khi upload |
| 9.9 | Image validation (client) | ⬜ | Size ≤ 5MB · type: jpg/png/webp |
| 9.10 | Form validation (Zod) | ⬜ | Login · SignUp |
| 9.11 | Dark mode | ⬜ | Nice to have |
| 9.12 | PWA + offline support | ⬜ | Nice to have |

---

## Summary

| | Tổng | Done | Planned |
|---|---|---|---|
| Authentication | 6 | 3 | 3 |
| User Profile | 5 | 3 | 2 |
| Post | 8 | 5 | 3 |
| Comment | 5 | 3 | 2 |
| Vote | 4 | 4 | 0 |
| Notification | 7 | 6 | 1 |
| Image Upload | 3 | 1 | 2 |
| Feed & Discovery | 4 | 1 | 3 |
| Frontend UX | 12 | 0 | 12 |
| **Total** | **54** | **26** | **28** |
