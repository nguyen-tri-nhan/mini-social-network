import { describe, it, expect } from 'vitest'
import { cn, initials } from './utils'

describe('cn', () => {
  it('merges class names', () => {
    expect(cn('foo', 'bar')).toBe('foo bar')
  })

  it('resolves tailwind conflicts — last wins', () => {
    expect(cn('p-2', 'p-4')).toBe('p-4')
  })

  it('ignores falsy values', () => {
    expect(cn('foo', false && 'bar', undefined, null, '')).toBe('foo')
  })
})

describe('initials', () => {
  it('returns uppercased first letters', () => {
    expect(initials('Nhan', 'Nguyen')).toBe('NN')
  })

  it('handles empty strings gracefully', () => {
    expect(initials('', '')).toBe('')
  })
})
