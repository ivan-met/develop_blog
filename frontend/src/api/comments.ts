import http from './http'
import type { CommentResponse, Page } from './types'

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
}
