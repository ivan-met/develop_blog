import http from './http'
import type { LikeResponse, BookmarkResponse, PostSummaryResponse, Page } from './types'

export const engagementApi = {
  like(slug: string): Promise<LikeResponse> {
    return http.post<LikeResponse>(`/posts/${slug}/like`).then((r) => r.data)
  },

  unlike(slug: string): Promise<LikeResponse> {
    return http.delete<LikeResponse>(`/posts/${slug}/like`).then((r) => r.data)
  },

  bookmark(slug: string): Promise<BookmarkResponse> {
    return http
      .post<BookmarkResponse>(`/posts/${slug}/bookmark`)
      .then((r) => r.data)
  },

  removeBookmark(slug: string): Promise<BookmarkResponse> {
    return http
      .delete<BookmarkResponse>(`/posts/${slug}/bookmark`)
      .then((r) => r.data)
  },

  listMyBookmarks(params: {
    page?: number
    size?: number
  }): Promise<Page<PostSummaryResponse>> {
    return http
      .get<Page<PostSummaryResponse>>('/users/me/bookmarks', { params })
      .then((r) => r.data)
  },
}
