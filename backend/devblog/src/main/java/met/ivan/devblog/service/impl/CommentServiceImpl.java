package met.ivan.devblog.service.impl;

import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import met.ivan.devblog.entity.Comment;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.CommentMapper;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> list(String slug, UserDetails principal, Pageable pageable) {
        Post post = loadPublishedPost(slug);
        // Enforce newest-first ordering regardless of what the caller passes
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"))
        );
        return commentRepository.findByPostIdWithAuthor(post.getId(), sorted)
                .map(c -> commentMapper.toResponse(c, canDelete(c, principal)));
    }

    @Override
    public CommentResponse create(String slug, String username, CreateCommentRequest request) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published post not found with slug: " + slug));
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .author(author)
                .build();
        comment = commentRepository.save(comment);
        return commentMapper.toResponse(comment, true); // author can always delete their own
    }

    @Override
    public void delete(Long commentId, UserDetails principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (!canDelete(comment, principal)) {
            throw new ForbiddenOperationException("You do not have permission to delete this comment");
        }
        commentRepository.delete(comment);
    }

    // --- helpers ---

    private Post loadPublishedPost(String slug) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published post not found with slug: " + slug));
    }

    private boolean canDelete(Comment comment, UserDetails principal) {
        if (principal == null) {
            return false;
        }
        boolean isAdmin = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isCommentAuthor = comment.getAuthor().getUsername().equals(principal.getUsername());
        boolean isPostAuthor = comment.getPost().getAuthor().getUsername().equals(principal.getUsername());
        return isAdmin || isCommentAuthor || isPostAuthor;
    }
}
