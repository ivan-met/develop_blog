package met.ivan.devblog.controller;

import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    /**
     * GET /api/authors/{username} — public author profile.
     */
    @GetMapping("/{username}")
    public AuthorProfileResponse getProfile(@PathVariable String username) {
        return authorService.getPublicProfile(username);
    }

    /**
     * GET /api/authors/{username}/posts — public; published posts only.
     */
    @GetMapping("/{username}/posts")
    public Page<PostSummaryResponse> getAuthorPosts(
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return authorService.getPublishedPosts(username, pageable);
    }
}
