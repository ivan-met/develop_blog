package met.ivan.devblog.controller;

import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.service.AdminPostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    /**
     * GET /api/admin/posts
     * Returns a page of posts across all authors and all statuses.
     *
     * @param status       optional status filter (DRAFT | PUBLISHED)
     * @param search       optional search across post title and tags
     * @param categorySlug optional category filter by slug
     * @param pageable     page/size/sort; defaults to size=20, sort=createdAt DESC
     */
    @GetMapping
    public Page<PostSummaryResponse> listPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categorySlug,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminPostService.list(status, search, categorySlug, pageable);
    }
}
