import type { InfiniteData } from '@tanstack/react-query'
import type { ChatMessage, ChatUser, CursorPage } from '../types'

export type MessagesData = InfiniteData<CursorPage<ChatMessage>, string | undefined>

export interface PendingMessage {
  clientMessageId: string
  content: string
  failed: boolean
}

export const MAX_MESSAGE_LENGTH = 4096

export function chatUserName(u: ChatUser | undefined) {
  return u?.firstname ? `${u.firstname} ${u.lastname ?? ''}`.trim() : 'User'
}

// Chèn tin (từ WS hoặc response HTTP) vào trang mới nhất; đã có thì giữ nguyên —
// 2 nguồn có thể về cùng 1 tin, thứ tự nào cũng được (messaging-plan §14.3).
export function upsertMessage(data: MessagesData | undefined, msg: ChatMessage): MessagesData | undefined {
  if (!data || data.pages.length === 0) return data
  if (data.pages.some((p) => p.items.some((m) => m.id === msg.id))) return data
  const [first, ...rest] = data.pages
  return { ...data, pages: [{ ...first, items: [msg, ...first.items] }, ...rest] }
}

// id là UUIDv7 dạng chuỗi hex cùng độ dài → so chuỗi = so thời gian server.
export function chronological(data: MessagesData | undefined): ChatMessage[] {
  const byId = new Map<string, ChatMessage>()
  data?.pages.forEach((p) => p.items.forEach((m) => byId.set(m.id, m)))
  return [...byId.values()].sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
}

export function unconfirmed(pending: PendingMessage[], confirmed: ChatMessage[]): PendingMessage[] {
  const done = new Set(confirmed.map((m) => m.clientMessageId))
  return pending.filter((p) => !done.has(p.clientMessageId))
}
