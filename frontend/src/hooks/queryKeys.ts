export const qk = {
  articles: {
    all: ['articles'] as const,
    list: (filter?: string, sort?: string) => ['articles', 'list', filter, sort] as const,
    detail: (id: string) => ['articles', id] as const,
  },
  comments: {
    byTarget: (targetId: string, targetType: string) => ['comments', targetId, targetType] as const,
  },
  notifications: {
    list: ['notifications'] as const,
    unread: ['notifications', 'unread'] as const,
  },
  users: {
    me: ['users', 'me'] as const,
    detail: (id: string) => ['users', id] as const,
  },
}
