interface NotificationLike {
  type: string
  articleId?: string
  commentId?: string
}

interface NotificationTarget {
  path: string
  highlightCommentId?: string
}

// Thêm loại notification mới (follow, message,...) → thêm 1 entry ở đây,
// không cần sửa NotificationsPage/useNotificationSocket.
const RESOLVERS: Record<string, (n: NotificationLike) => NotificationTarget | null> = {
  COMMENT_CREATED: (n) => (n.articleId ? { path: `/articles/${n.articleId}`, highlightCommentId: n.commentId } : null),
  VOTE_CAST:       (n) => (n.articleId ? { path: `/articles/${n.articleId}` } : null),
}

export function resolveNotificationTarget(n: NotificationLike): NotificationTarget | null {
  return RESOLVERS[n.type]?.(n) ?? null
}
