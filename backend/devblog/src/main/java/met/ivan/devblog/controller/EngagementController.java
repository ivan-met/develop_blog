package met.ivan.devblog.controller;

import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.service.EngagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    // --- Likes ---

    /**
     * POST /api/posts/{slug}/like — idempotent like.
     */
    @PostMapping("/api/posts/{slug}/like")
    public LikeResponse like(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails principal) {
        return engagementService.like(slug, principal.getUsername());
    }

    /**
     * DELETE /api/posts/{slug}/like — idempotent unlike.
     */
    @DeleteMapping("/api/posts/{slug}/like")
    public LikeResponse unlike(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails principal) {
        return engagementService.unlike(slug, principal.getUsername());
    }

    // --- Bookmarks ---

    /**
     * POST /api/posts/{slug}/bookmark.
     */
    @PostMapping("/api/posts/{slug}/bookmark")
    public BookmarkResponse bookmark(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails principal) {
        return engagementService.bookmark(slug, principal.getUsername());
    }

    /**
     * DELETE /api/posts/{slug}/bookmark.
     */
    @DeleteMapping("/api/posts/{slug}/bookmark")
    public BookmarkResponse removeBookmark(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails principal) {
        return engagementService.removeBookmark(slug, principal.getUsername());
    }

    // --- User bookmarks list ---

    /**
     * GET /api/users/me/bookmarks — auth required.
     */
    @GetMapping("/api/users/me/bookmarks")
    public Page<PostSummaryResponse> listMyBookmarks(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {
        return engagementService.listBookmarks(principal.getUsername(), pageable);
    }
}
