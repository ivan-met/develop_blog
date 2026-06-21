package met.ivan.devblog.service;

import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

public interface CommentService {

    Page<CommentResponse> list(String slug, UserDetails principal, Pageable pageable);

    CommentResponse create(String slug, String username, CreateCommentRequest request);

    void delete(Long commentId, UserDetails principal);
}
