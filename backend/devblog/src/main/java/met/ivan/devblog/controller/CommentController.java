package met.ivan.devblog.controller;

import jakarta.validation.Valid;
import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import met.ivan.devblog.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * GET /api/posts/{slug}/comments — public; newest first.
     */
    @GetMapping("/api/posts/{slug}/comments")
    public Page<CommentResponse> listComments(
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal) {
        return commentService.list(slug, principal, pageable);
    }

    /**
     * POST /api/posts/{slug}/comments — auth required; returns 201.
     */
    @PostMapping("/api/posts/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable String slug,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return commentService.create(slug, principal.getUsername(), request);
    }

    /**
     * DELETE /api/comments/{id} — auth required; comment author, post author, or admin.
     */
    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        commentService.delete(id, principal);
    }
}
