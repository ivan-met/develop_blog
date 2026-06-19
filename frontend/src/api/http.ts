import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

// The base URL is intentionally empty so the Vite proxy handles /api/* → backend
const http: AxiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Track whether a refresh is in progress to prevent multiple concurrent refreshes
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

function notifyPending(token: string | null, err: unknown = null) {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (token) resolve(token)
    else reject(err)
  })
  pendingRequests = []
}

// Lazy import to avoid circular dependency (store imports http, http needs store)
async function getAuthStore() {
  const { useAuthStore } = await import('@/stores/auth')
  return useAuthStore()
}

// ─── Request interceptor: attach Bearer token ────────────────────────────────
http.interceptors.request.use(
  async (config) => {
    const store = await getAuthStore()
    const token = store.accessToken
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ─── Response interceptor: 401 → refresh once, then logout ──────────────────
http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & {
      _retry?: boolean
    }

    // Only handle 401 and don't retry auth endpoints themselves
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/register')
    ) {
      if (isRefreshing) {
        // Queue the request until refresh finishes
        return new Promise((resolve, reject) => {
          pendingRequests.push({
            resolve: (token: string) => {
              if (originalRequest.headers) {
                originalRequest.headers['Authorization'] = `Bearer ${token}`
              }
              resolve(http(originalRequest))
            },
            reject,
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const store = await getAuthStore()
        const newToken = await store.refresh()
        notifyPending(newToken)
        if (originalRequest.headers) {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
        }
        return http(originalRequest)
      } catch (refreshError) {
        notifyPending(null, refreshError)
        const store = await getAuthStore()
        await store.logout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

export default http
