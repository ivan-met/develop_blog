import http from './http'
import type {
  UserResponse,
  Page,
  UpdateProfileRequest,
  ChangePasswordRequest,
  UpdateRolesRequest,
  UpdateStatusRequest,
} from './types'

export const usersApi = {
  // ─── Profile (authenticated) ─────────────────────────────────────────────

  getMe(): Promise<UserResponse> {
    return http.get<UserResponse>('/users/me').then((r) => r.data)
  },

  updateMe(data: UpdateProfileRequest): Promise<UserResponse> {
    return http.put<UserResponse>('/users/me', data).then((r) => r.data)
  },

  changePassword(data: ChangePasswordRequest): Promise<void> {
    return http.put('/users/me/password', data).then(() => undefined)
  },

  // ─── Admin user management (ROLE_ADMIN) ──────────────────────────────────

  listUsers(params: {
    page?: number
    size?: number
    search?: string
  }): Promise<Page<UserResponse>> {
    return http
      .get<Page<UserResponse>>('/admin/users', { params })
      .then((r) => r.data)
  },

  getUser(id: number): Promise<UserResponse> {
    return http
      .get<UserResponse>(`/admin/users/${id}`)
      .then((r) => r.data)
  },

  updateRoles(id: number, data: UpdateRolesRequest): Promise<UserResponse> {
    return http
      .put<UserResponse>(`/admin/users/${id}/roles`, data)
      .then((r) => r.data)
  },

  updateStatus(id: number, data: UpdateStatusRequest): Promise<UserResponse> {
    return http
      .put<UserResponse>(`/admin/users/${id}/status`, data)
      .then((r) => r.data)
  },
}
