import http from './http'
import type { AdminCommentResponse, CommentResponse, Page } from './types'

export const commentsApi = {
  list(
    slug: string,
    params: { page?: number; size?: number },
  ): Promise<Page<CommentResponse>> {
    return http
      .get<Page<CommentResponse>>(`/posts/${slug}/comments`, { params })
      .then((r) => r.data)
  },

  create(slug: string, data: { content: string }): Promise<CommentResponse> {
    return http
      .post<CommentResponse>(`/posts/${slug}/comments`, data)
      .then((r) => r.data)
  },

  remove(id: number): Promise<void> {
    return http.delete(`/comments/${id}`).then(() => undefined)
  },

  // ─── Admin ────────────────────────────────────────────────────────────────

  listAll(params: {
    search?: string
    page?: number
    size?: number
  }): Promise<Page<AdminCommentResponse>> {
    return http
      .get<Page<AdminCommentResponse>>('/admin/comments', { params })
      .then((r) => r.data)
  },
}
