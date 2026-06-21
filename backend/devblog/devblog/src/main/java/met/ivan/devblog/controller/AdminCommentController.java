package met.ivan.devblog.controller;

import met.ivan.devblog.dto.AdminCommentResponse;
import met.ivan.devblog.service.AdminCommentService;
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
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    /**
     * GET /api/admin/comments
     * Returns a page of all comments across all posts.
     *
     * @param search   optional filter across comment content, author username, and post title
     * @param pageable page/size/sort; defaults to size=20, sort=createdAt DESC
     */
    @GetMapping
    public Page<AdminCommentResponse> listComments(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminCommentService.list(search, pageable);
    }
}
