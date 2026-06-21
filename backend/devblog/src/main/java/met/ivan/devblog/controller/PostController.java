package met.ivan.devblog.controller;

import jakarta.validation.Valid;
import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.dto.UpdatePostRequest;
import met.ivan.devblog.dto.UpdatePostStatusRequest;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // --- Public read ---

    @GetMapping
    public Page<PostSummaryResponse> listPublished(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.listPublished(category, search, sort, pageable, principal);
    }

    @GetMapping("/{slug}")
    public PostResponse getBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.getPublishedBySlug(slug, principal);
    }

    // --- Authenticated authoring ---

    @GetMapping("/mine")
    public Page<PostSummaryResponse> listMine(
            @RequestParam(required = false) PostStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.listOwn(principal, status, pageable);
    }

    @GetMapping("/mine/{id}")
    public PostResponse getOwn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.getOwn(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.create(principal.getUsername(), request);
    }

    @PutMapping("/{id}")
    public PostResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.update(id, principal, request);
    }

    @PutMapping("/{id}/status")
    public PostResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostStatusRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return postService.changeStatus(id, principal, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        postService.delete(id, principal);
    }
}
