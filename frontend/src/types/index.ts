export interface AuthResponse {
  accessToken: string
  userId: string
  username: string
}

export interface UserProfile {
  id: string
  username: string
  email: string
  firstname: string
  lastname: string
  avatarUrl?: string
  createdAt: string
}

export interface Article {
  id: string
  description?: string
  imageUrl?: string
  authorId: string
  voteCount: number
  commentCount: number
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: string
  description: string
  targetId: string
  targetType: string
  authorId: string
  createdAt: string
}

export interface Vote {
  id: string
  value: number
  userId: string
  targetId: string
  targetType: string
}

export interface Notification {
  id: string
  type: string
  actorId: string
  ownerId: string
  articleId?: string
  seen: boolean
  createdAt: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
  hasNext: boolean
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: { errorCode: string; errorMessage: string; traceId: string }
}
