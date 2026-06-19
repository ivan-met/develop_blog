import http from './http'
import type {
  AuthResponse,
  TokenRefreshResponse,
  RegisterRequest,
  LoginRequest,
  RefreshRequest,
  LogoutRequest,
} from './types'

export const authApi = {
  register(data: RegisterRequest): Promise<AuthResponse> {
    return http.post<AuthResponse>('/auth/register', data).then((r) => r.data)
  },

  login(data: LoginRequest): Promise<AuthResponse> {
    return http.post<AuthResponse>('/auth/login', data).then((r) => r.data)
  },

  refresh(data: RefreshRequest): Promise<TokenRefreshResponse> {
    return http
      .post<TokenRefreshResponse>('/auth/refresh', data)
      .then((r) => r.data)
  },

  logout(data: LogoutRequest): Promise<void> {
    return http.post('/auth/logout', data).then(() => undefined)
  },
}
