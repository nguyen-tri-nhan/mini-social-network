import { describe, it, expect } from 'vitest'
import { chatUserName, chronological, unconfirmed, upsertMessage, type MessagesData } from './chat'
import type { ChatMessage } from '../types'

const msg = (id: string, clientMessageId = `c-${id}`): ChatMessage => ({
  id, clientMessageId, conversationId: 'conv', senderId: 'u1', content: id, createdAt: '2026-10-02T00:00:00Z',
})

const data = (...pages: ChatMessage[][]): MessagesData => ({
  pages: pages.map((items) => ({ items, hasMore: false })),
  pageParams: pages.map(() => undefined),
})

describe('chat helpers', () => {
  it('upsertMessage prepends a new message to the newest page', () => {
    const next = upsertMessage(data([msg('0002'), msg('0001')]), msg('0003'))
    expect(next?.pages[0].items.map((m) => m.id)).toEqual(['0003', '0002', '0001'])
  })

  it('upsertMessage ignores a message already present in any page', () => {
    const d = data([msg('0003')], [msg('0001')])
    expect(upsertMessage(d, msg('0001'))).toBe(d)
  })

  it('upsertMessage leaves an uncached thread alone', () => {
    expect(upsertMessage(undefined, msg('0001'))).toBeUndefined()
  })

  it('chronological flattens pages oldest first without duplicates', () => {
    const d = data([msg('0190a'), msg('0190b')], [msg('0180'), msg('0190a')])
    expect(chronological(d).map((m) => m.id)).toEqual(['0180', '0190a', '0190b'])
  })

  it('unconfirmed drops pending messages the server already returned', () => {
    const pending = [
      { clientMessageId: 'c-1', content: 'a', failed: false },
      { clientMessageId: 'c-2', content: 'b', failed: false },
    ]
    expect(unconfirmed(pending, [msg('1', 'c-1')]).map((p) => p.clientMessageId)).toEqual(['c-2'])
  })

  it('chatUserName falls back when user_ref is missing', () => {
    expect(chatUserName({ id: 'x' })).toBe('User')
    expect(chatUserName({ id: 'x', firstname: 'An', lastname: 'Le' })).toBe('An Le')
  })
})
