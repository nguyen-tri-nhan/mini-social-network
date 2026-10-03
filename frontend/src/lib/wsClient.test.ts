import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { WsClient } from './wsClient'

class FakeWebSocket {
  static OPEN = 1
  static instances: FakeWebSocket[] = []

  readyState = 0
  sent: string[] = []
  onopen: (() => void) | null = null
  onmessage: ((e: { data: string }) => void) | null = null
  onclose: (() => void) | null = null
  onerror: (() => void) | null = null

  constructor(public url: string, public protocols: string[]) {
    FakeWebSocket.instances.push(this)
  }

  send(data: string) { this.sent.push(data) }
  close() { this.readyState = 3; this.onclose?.() }
  open() { this.readyState = FakeWebSocket.OPEN; this.onopen?.() }
  receive(msg: object) { this.onmessage?.({ data: JSON.stringify(msg) }) }
  sentOfType(type: string) { return this.sent.map((s) => JSON.parse(s)).filter((m) => m.type === type) }
}

const latest = () => FakeWebSocket.instances[FakeWebSocket.instances.length - 1]

describe('WsClient', () => {
  let client: WsClient

  beforeEach(() => {
    FakeWebSocket.instances = []
    vi.stubGlobal('WebSocket', FakeWebSocket)
    vi.useFakeTimers()
    localStorage.setItem('jwt', 'tok')
    client = new WsClient()
  })

  afterEach(() => {
    client.close()
    vi.useRealTimers()
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('sends the JWT through the Sec-WebSocket-Protocol subprotocols', () => {
    client.subscribe('t', () => {})

    expect(latest().protocols).toEqual([
      'bearer-token-carrier',
      encodeURIComponent('quarkus-http-upgrade#Authorization#Bearer tok'),
    ])
  })

  it('does not connect without a token', () => {
    localStorage.clear()
    client.subscribe('t', () => {})

    expect(FakeWebSocket.instances).toHaveLength(0)
  })

  it('subscribes on open and dispatches messages by topic', () => {
    const h1 = vi.fn()
    const h2 = vi.fn()
    client.subscribe('t1', h1)
    client.subscribe('t2', h2)
    latest().open()

    expect(latest().sentOfType('SUBSCRIBE').map((m) => m.topic)).toEqual(['t1', 't2'])

    latest().receive({ topic: 't1', type: 'X' })
    expect(h1).toHaveBeenCalledWith({ topic: 't1', type: 'X' })
    expect(h2).not.toHaveBeenCalled()
  })

  it('sends one SUBSCRIBE per topic and UNSUBSCRIBE only after the last handler leaves', () => {
    client.subscribe('warmup', () => {})
    latest().open()

    const off1 = client.subscribe('t', () => {})
    const off2 = client.subscribe('t', () => {})
    expect(latest().sentOfType('SUBSCRIBE').filter((m) => m.topic === 't')).toHaveLength(1)

    off1()
    expect(latest().sentOfType('UNSUBSCRIBE')).toHaveLength(0)
    off2()
    expect(latest().sentOfType('UNSUBSCRIBE')).toEqual([{ type: 'UNSUBSCRIBE', topic: 't' }])
  })

  it('reconnects after a drop, re-subscribes and notifies reconnect listeners', () => {
    const onReconnect = vi.fn()
    client.onReconnect(onReconnect)
    client.subscribe('t', () => {})
    latest().open()
    expect(onReconnect).not.toHaveBeenCalled()

    latest().close()
    vi.advanceTimersByTime(3000)

    expect(FakeWebSocket.instances).toHaveLength(2)
    latest().open()
    expect(latest().sentOfType('SUBSCRIBE')).toEqual([{ type: 'SUBSCRIBE', topic: 't' }])
    expect(onReconnect).toHaveBeenCalledTimes(1)
  })

  it('backs off between failed reconnect attempts', () => {
    client.subscribe('t', () => {})
    latest().close()               // drop before ever opening → next try in 3s
    vi.advanceTimersByTime(3000)
    expect(FakeWebSocket.instances).toHaveLength(2)

    latest().close()               // second failure → 6s
    vi.advanceTimersByTime(3000)
    expect(FakeWebSocket.instances).toHaveLength(2)
    vi.advanceTimersByTime(3000)
    expect(FakeWebSocket.instances).toHaveLength(3)
  })

  it('stops reconnecting after close()', () => {
    client.subscribe('t', () => {})
    latest().open()

    client.close()
    vi.advanceTimersByTime(60_000)

    expect(FakeWebSocket.instances).toHaveLength(1)
  })
})
