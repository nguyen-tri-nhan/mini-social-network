import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { MessageThread } from './MessageThread'
import { chatApi } from '../../api/chat'
import { useAuthStore } from '../../stores/authStore'
import type { ChatMessage } from '../../types'

vi.mock('../../api/chat', () => ({
  chatApi: {
    get: vi.fn(),
    messages: vi.fn(),
    markRead: vi.fn(),
    send: vi.fn(),
  },
}))

const conversation = {
  id: 'conv-1',
  otherUser: { id: 'bob', firstname: 'Bob', lastname: 'Tran' },
  unread: false,
}

const serverMsg = (id: string, clientMessageId: string, content: string, senderId = 'me'): ChatMessage => ({
  id, clientMessageId, conversationId: 'conv-1', senderId, content, createdAt: new Date().toISOString(),
})

function renderThread() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <MessageThread conversationId="conv-1" />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function type(text: string) {
  fireEvent.change(screen.getByLabelText('Message'), { target: { value: text } })
}

describe('MessageThread', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 't', user: { id: 'me', username: 'me', email: '', firstname: 'Me', lastname: 'X', createdAt: '' } })
    vi.mocked(chatApi.get).mockResolvedValue(conversation)
    vi.mocked(chatApi.markRead).mockResolvedValue({} as never)
  })

  afterEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ token: null, user: null })
  })

  it('shows history oldest first and marks the newest message read', async () => {
    vi.mocked(chatApi.messages).mockResolvedValue({
      items: [serverMsg('0002', 'c2', 'second', 'bob'), serverMsg('0001', 'c1', 'first', 'bob')],
      hasMore: false,
    })
    renderThread()

    const first = await screen.findByText('first')
    const second = screen.getByText('second')
    expect(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    await waitFor(() => expect(chatApi.markRead).toHaveBeenCalledWith('conv-1', '0002'))
  })

  it('shows a sent message once: pending first, then replaced by the server copy', async () => {
    vi.mocked(chatApi.messages).mockResolvedValue({ items: [], hasMore: false })
    let resolveSend!: () => void
    vi.mocked(chatApi.send).mockImplementation((_, clientMessageId, content) =>
      new Promise((r) => { resolveSend = () => r(serverMsg('0009', clientMessageId, content)) }))
    renderThread()
    await screen.findByText(/Say hi to Bob Tran/)

    type('hello')
    fireEvent.click(screen.getByLabelText('Send'))

    expect(await screen.findByText('Sending…')).toBeInTheDocument()
    resolveSend()
    await waitFor(() => expect(screen.queryByText('Sending…')).not.toBeInTheDocument())
    expect(screen.getAllByText('hello')).toHaveLength(1)
  })

  it('retries a failed message with the same clientMessageId', async () => {
    vi.mocked(chatApi.messages).mockResolvedValue({ items: [], hasMore: false })
    vi.mocked(chatApi.send).mockRejectedValueOnce(new Error('offline'))
    renderThread()
    await screen.findByText(/Say hi/)

    type('again')
    fireEvent.click(screen.getByLabelText('Send'))
    fireEvent.click(await screen.findByLabelText('Retry'))

    await waitFor(() => expect(chatApi.send).toHaveBeenCalledTimes(2))
    const [first, second] = vi.mocked(chatApi.send).mock.calls
    expect(second[1]).toBe(first[1])
  })

  it('rejects blank and over-limit messages', async () => {
    vi.mocked(chatApi.messages).mockResolvedValue({ items: [], hasMore: false })
    renderThread()
    await screen.findByText(/Say hi/)

    type('   ')
    expect(screen.getByLabelText('Send')).toBeDisabled()
    type('x'.repeat(4097))
    expect(screen.getByLabelText('Send')).toBeDisabled()
    type('x'.repeat(4096))
    expect(screen.getByLabelText('Send')).toBeEnabled()
  })
})
