// ─── API DTO types (mirrors the backend contract) ───────────────────────────

export interface UserResponse {
  id: number
  username: string
  email: string
  displayName: string | null
  bio: string | null
  avatarUrl: string | null
  roles: string[]
  active: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserResponse
}

export interface TokenRefreshResponse {
  accessToken: string
  refreshToken: string
}

// ─── Request bodies ──────────────────────────────────────────────────────────

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface UpdateProfileRequest {
  displayName?: string
  bio?: string
  avatarUrl?: string
  email?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface UpdateRolesRequest {
  roles: string[]
}

export interface UpdateStatusRequest {
  active: boolean
}

// ─── Pagination ──────────────────────────────────────────────────────────────

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

// ─── Error response ──────────────────────────────────────────────────────────

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  fieldErrors?: Record<string, string>
}
