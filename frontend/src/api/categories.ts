import http from './http'
import type {
  CategoryResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from './types'

export const categoriesApi = {
  // ─── Public ──────────────────────────────────────────────────────────────

  list(): Promise<CategoryResponse[]> {
    return http.get<CategoryResponse[]>('/categories').then((r) => r.data)
  },

  getBySlug(slug: string): Promise<CategoryResponse> {
    return http
      .get<CategoryResponse>(`/categories/${slug}`)
      .then((r) => r.data)
  },

  // ─── Admin ────────────────────────────────────────────────────────────────

  create(data: CreateCategoryRequest): Promise<CategoryResponse> {
    return http
      .post<CategoryResponse>('/admin/categories', data)
      .then((r) => r.data)
  },

  update(id: number, data: UpdateCategoryRequest): Promise<CategoryResponse> {
    return http
      .put<CategoryResponse>(`/admin/categories/${id}`, data)
      .then((r) => r.data)
  },

  remove(id: number): Promise<void> {
    return http.delete(`/admin/categories/${id}`).then(() => undefined)
  },
}
