import http from './http'
import type {
  PostSummaryResponse,
  PostResponse,
  CreatePostRequest,
  UpdatePostRequest,
  UpdatePostStatusRequest,
  PostStatus,
  Page,
} from './types'

export const postsApi = {
  // ─── Public (no auth required) ───────────────────────────────────────────

  listPublished(params: {
    category?: string
    search?: string
    sort?: 'latest' | 'popular'
    page?: number
    size?: number
  }): Promise<Page<PostSummaryResponse>> {
    return http
      .get<Page<PostSummaryResponse>>('/posts', { params })
      .then((r) => r.data)
  },

  getBySlug(slug: string): Promise<PostResponse> {
    return http.get<PostResponse>(`/posts/${slug}`).then((r) => r.data)
  },

  // ─── Authoring (authenticated) ────────────────────────────────────────────

  listMine(params: {
    status?: PostStatus
    page?: number
    size?: number
  }): Promise<Page<PostSummaryResponse>> {
    return http
      .get<Page<PostSummaryResponse>>('/posts/mine', { params })
      .then((r) => r.data)
  },

  getMine(id: number): Promise<PostResponse> {
    return http.get<PostResponse>(`/posts/mine/${id}`).then((r) => r.data)
  },

  create(data: CreatePostRequest): Promise<PostResponse> {
    return http.post<PostResponse>('/posts', data).then((r) => r.data)
  },

  update(id: number, data: UpdatePostRequest): Promise<PostResponse> {
    return http.put<PostResponse>(`/posts/${id}`, data).then((r) => r.data)
  },

  changeStatus(
    id: number,
    data: UpdatePostStatusRequest,
  ): Promise<PostResponse> {
    return http
      .put<PostResponse>(`/posts/${id}/status`, data)
      .then((r) => r.data)
  },

  remove(id: number): Promise<void> {
    return http.delete(`/posts/${id}`).then(() => undefined)
  },
}
