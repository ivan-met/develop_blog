package met.ivan.devblog.service;

import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.dto.UpdatePostRequest;
import met.ivan.devblog.dto.UpdatePostStatusRequest;
import met.ivan.devblog.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

public interface PostService {

    PostResponse create(String authorUsername, CreatePostRequest request);

    PostResponse update(Long id, UserDetails principal, UpdatePostRequest request);

    PostResponse changeStatus(Long id, UserDetails principal, UpdatePostStatusRequest request);

    void delete(Long id, UserDetails principal);

    /**
     * Get a published post by slug, enriching with engagement data for the given principal
     * (null principal → liked/bookmarked will be null/omitted).
     */
    PostResponse getPublishedBySlug(String slug, UserDetails principal);

    /**
     * List published posts, enriching summaries with like counts (batch, no N+1).
     * Principal is used for per-user flags — pass null for anonymous.
     */
    Page<PostSummaryResponse> listPublished(String categorySlug, String search, String sort,
                                            Pageable pageable, UserDetails principal);

    PostResponse getOwn(Long id, UserDetails principal);

    Page<PostSummaryResponse> listOwn(UserDetails principal, PostStatus status, Pageable pageable);
}
