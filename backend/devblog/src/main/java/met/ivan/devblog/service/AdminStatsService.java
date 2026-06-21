package met.ivan.devblog.service;

import met.ivan.devblog.dto.PlatformStatsResponse;

public interface AdminStatsService {
    /**
     * Aggregate platform statistics: headline totals, top posts by views and likes,
     * and the most recent registered users.
     */
    PlatformStatsResponse getStats();
}
