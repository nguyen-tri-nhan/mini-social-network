import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ArticleCard } from './ArticleCard'
import { useAuthStore } from '../../stores/authStore'
import { articlesApi, usersApi } from '../../api/articles'
import { votesApi } from '../../api/interactions'
import type { Article, UserProfile } from '../../types'

vi.mock('../../api/articles', () => ({
  articlesApi: { delete: vi.fn() },
  usersApi:    { getById: vi.fn() },
}))

vi.mock('../../api/interactions', () => ({
  votesApi: { cast: vi.fn() },
}))

vi.mock('../comment/CommentSection', () => ({
  CommentSection: () => <div data-testid="comment-section" />,
}))

const mockAuthor: UserProfile = {
  id: 'author-1',
  username: 'author',
  email: 'author@example.com',
  firstname: 'John',
  lastname: 'Doe',
  createdAt: '2024-01-01T00:00:00Z',
}

const mockArticle: Article = {
  id: 'article-1',
  authorId: 'author-1',
  description: 'Hello world',
  voteCount: 10,
  commentCount: 3,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

function renderCard(article = mockArticle) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <ArticleCard article={article} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ArticleCard', () => {
  beforeEach(() => {
    vi.mocked(usersApi.getById).mockResolvedValue(mockAuthor)
    vi.mocked(articlesApi.delete).mockResolvedValue(undefined as any)
    vi.mocked(votesApi.cast).mockResolvedValue({ id: 'v1', value: 1, userId: 'u1', targetId: 'a1', targetType: 'ARTICLE' })
  })

  afterEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ token: null, user: null })
  })

  describe('content rendering', () => {
    it('renders article description', () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      expect(screen.getByText('Hello world')).toBeInTheDocument()
    })

    it('renders vote count', () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      expect(screen.getByText('10 votes')).toBeInTheDocument()
    })

    it('renders comment count', () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      expect(screen.getByText('3 comments')).toBeInTheDocument()
    })

    it('renders author name after query resolves', async () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      expect(await screen.findByText('John Doe')).toBeInTheDocument()
    })

    it('does not render description when undefined', () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard({ ...mockArticle, description: undefined })
      expect(screen.queryByText('Hello world')).not.toBeInTheDocument()
    })
  })

  describe('delete button', () => {
    it('shows delete button when current user is article owner', () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })  // id: 'author-1' matches authorId
      renderCard()
      expect(screen.getByRole('button', { name: /trash/i })).toBeInTheDocument()
    })

    it('hides delete button when current user is not owner', () => {
      useAuthStore.setState({
        token: 'token',
        user: { ...mockAuthor, id: 'other-user' },
      })
      renderCard()
      expect(screen.queryByRole('button', { name: /trash/i })).not.toBeInTheDocument()
    })

    it('hides delete button when not logged in', () => {
      useAuthStore.setState({ token: null, user: null })
      renderCard()
      expect(screen.queryByRole('button', { name: /trash/i })).not.toBeInTheDocument()
    })
  })

  describe('comment toggle', () => {
    it('shows comment section when Comment button is clicked', async () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      fireEvent.click(screen.getByRole('button', { name: /comment/i }))
      expect(screen.getByTestId('comment-section')).toBeInTheDocument()
    })

    it('hides comment section when Comment button is clicked again', async () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      const btn = screen.getByRole('button', { name: /comment/i })
      fireEvent.click(btn)
      fireEvent.click(btn)
      expect(screen.queryByTestId('comment-section')).not.toBeInTheDocument()
    })
  })

  describe('vote counts', () => {
    it('updates vote count optimistically on upvote', async () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      fireEvent.click(screen.getByRole('button', { name: /like/i }))
      await waitFor(() => {
        expect(screen.getByText('11 votes')).toBeInTheDocument()
      })
    })

    it('updates vote count optimistically on downvote', async () => {
      useAuthStore.setState({ token: 'token', user: mockAuthor })
      renderCard()
      fireEvent.click(screen.getByRole('button', { name: /dislike/i }))
      await waitFor(() => {
        expect(screen.getByText('9 votes')).toBeInTheDocument()
      })
    })
  })
})
