package met.ivan.devblog.service;

import met.ivan.devblog.dto.AdminCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCommentService {
    /**
     * List all comments across all posts with optional search across
     * comment content, author username, and post title.
     *
     * @param search optional filter string (null means no filter)
     * @param pageable pagination and sort parameters
     */
    Page<AdminCommentResponse> list(String search, Pageable pageable);
}
