import { describe, it, expect } from 'vitest'
import { initials } from './utils'

describe('initials', () => {
  it('returns uppercased first letters', () => {
    expect(initials('Nhan', 'Nguyen')).toBe('NN')
  })

  it('handles empty strings gracefully', () => {
    expect(initials('', '')).toBe('')
  })
})
