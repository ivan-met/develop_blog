package met.ivan.devblog.service.impl;

import jakarta.persistence.criteria.JoinType;
import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.mapper.UserMapper;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthorServiceImpl implements AuthorService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    public AuthorServiceImpl(
            UserRepository userRepository,
            PostRepository postRepository,
            UserMapper userMapper,
            PostMapper postMapper) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
    }

    @Override
    public AuthorProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + username));
        long postCount = postRepository.countByAuthorUsername(username);
        return userMapper.toAuthorProfile(user, postCount);
    }

    @Override
    public Page<PostSummaryResponse> getPublishedPosts(String username, Pageable pageable) {
        // Verify the author exists first
        if (!userRepository.existsByUsername(username)) {
            throw new ResourceNotFoundException("Author not found: " + username);
        }
        Specification<Post> spec = buildAuthorPublishedSpec(username);
        return postRepository.findAll(spec, pageable)
                .map(postMapper::toSummary);
    }

    private Specification<Post> buildAuthorPublishedSpec(String username) {
        return (root, query, cb) -> {
            var authorJoin = root.join("author", JoinType.INNER);
            return cb.and(
                    cb.equal(authorJoin.get("username"), username),
                    cb.equal(root.get("status"), PostStatus.PUBLISHED)
            );
        };
    }
}
