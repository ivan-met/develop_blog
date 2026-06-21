package met.ivan.devblog.dto;

import java.util.List;

/**
 * Aggregated platform statistics returned by GET /api/admin/stats.
 */
public record PlatformStatsResponse(
        StatsTotals totals,
        List<PostSummaryResponse> topPostsByViews,
        List<TopPostResponse> topPostsByLikes,
        List<RecentUserResponse> recentUsers
) {

    /**
     * Headline counts across all entities.
     */
    public record StatsTotals(
            long users,
            long activeUsers,
            long posts,
            long publishedPosts,
            long draftPosts,
            long comments,
            long categories,
            long likes,
            long bookmarks
    ) {}
}
