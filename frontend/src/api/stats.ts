import http from './http'
import type { PlatformStatsResponse } from './types'

export const statsApi = {
  getStats(): Promise<PlatformStatsResponse> {
    return http.get<PlatformStatsResponse>('/admin/stats').then((r) => r.data)
  },
}
