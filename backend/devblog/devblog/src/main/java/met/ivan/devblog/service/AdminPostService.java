package met.ivan.devblog.service;

import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminPostService {
    /**
     * List all posts across all authors and statuses with optional filters.
     *
     * @param status       optional status filter (null means all statuses)
     * @param search       optional search across title and tags (null means no filter)
     * @param categorySlug optional category filter by slug (null means all categories)
     * @param pageable     pagination and sort parameters
     */
    Page<PostSummaryResponse> list(PostStatus status, String search, String categorySlug, Pageable pageable);
}
