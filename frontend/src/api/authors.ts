import http from './http'
import type { AuthorProfileResponse, PostSummaryResponse, Page } from './types'

export const authorsApi = {
  getProfile(username: string): Promise<AuthorProfileResponse> {
    return http
      .get<AuthorProfileResponse>(`/authors/${username}`)
      .then((r) => r.data)
  },

  getPosts(
    username: string,
    params: { page?: number; size?: number },
  ): Promise<Page<PostSummaryResponse>> {
    return http
      .get<Page<PostSummaryResponse>>(`/authors/${username}/posts`, { params })
      .then((r) => r.data)
  },
}
