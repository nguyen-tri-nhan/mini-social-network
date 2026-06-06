import { describe, it, expect } from 'vitest'
import { qk } from './queryKeys'

describe('queryKeys', () => {
  it('article list key includes filter and sort', () => {
    const key = qk.articles.list('authorId=="abc"', 'createdAt,desc')
    expect(key).toEqual(['articles', 'list', 'authorId=="abc"', 'createdAt,desc'])
  })

  it('article detail key includes id', () => {
    expect(qk.articles.detail('123')).toEqual(['articles', '123'])
  })

  it('comment key includes targetId and targetType', () => {
    expect(qk.comments.byTarget('abc', 'ARTICLE')).toEqual(['comments', 'abc', 'ARTICLE'])
  })

  it('static keys are stable references', () => {
    expect(qk.notifications.list).toEqual(['notifications'])
    expect(qk.notifications.unread).toEqual(['notifications', 'unread'])
  })
})
