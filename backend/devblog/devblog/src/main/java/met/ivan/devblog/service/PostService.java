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

    PostResponse getPublishedBySlug(String slug);

    Page<PostSummaryResponse> listPublished(String categorySlug, String search, String sort, Pageable pageable);

    PostResponse getOwn(Long id, UserDetails principal);

    Page<PostSummaryResponse> listOwn(UserDetails principal, PostStatus status, Pageable pageable);
}
