package met.ivan.devblog.service.impl;

import met.ivan.devblog.dto.AdminCommentResponse;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.entity.Comment;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.service.AdminCommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminCommentServiceImpl implements AdminCommentService {

    private final CommentRepository commentRepository;

    public AdminCommentServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public Page<AdminCommentResponse> list(String search, Pageable pageable) {
        // Normalize blank search to null so the JPQL :search IS NULL branch is taken
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return commentRepository.findAllWithAuthorAndPost(normalizedSearch, pageable)
                .map(this::toAdminResponse);
    }

    private AdminCommentResponse toAdminResponse(Comment comment) {
        AuthorSummary authorSummary = new AuthorSummary(
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getDisplayName()
        );
        return new AdminCommentResponse(
                comment.getId(),
                comment.getContent(),
                authorSummary,
                comment.getPost().getSlug(),
                comment.getPost().getTitle(),
                comment.getCreatedAt()
        );
    }
}
