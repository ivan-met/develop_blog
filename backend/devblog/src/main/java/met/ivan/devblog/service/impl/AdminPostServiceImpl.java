package met.ivan.devblog.service.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.service.AdminPostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminPostServiceImpl implements AdminPostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostLikeRepository postLikeRepository;

    public AdminPostServiceImpl(
            PostRepository postRepository,
            PostMapper postMapper,
            PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.postLikeRepository = postLikeRepository;
    }

    @Override
    public Page<PostSummaryResponse> list(PostStatus status, String search, String categorySlug, Pageable pageable) {
        Specification<Post> spec = buildAdminSpec(status, search, categorySlug);
        Page<Post> postPage = postRepository.findAll(spec, pageable);

        // Batch like counts — one query for the entire page
        List<Long> postIds = postPage.getContent().stream().map(Post::getId).toList();
        Map<Long, Long> likeCountsByPostId = batchLikeCounts(postIds);

        return postPage.map(post -> postMapper.toSummary(post, likeCountsByPostId.getOrDefault(post.getId(), 0L)));
    }

    private Specification<Post> buildAdminSpec(PostStatus status, String search, String categorySlug) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter — if null, all statuses are returned
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Category filter
            if (categorySlug != null && !categorySlug.isBlank()) {
                var categoryJoin = root.join("category", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("slug"), categorySlug));
            }

            // Search across title and tags (mirrors public endpoint, excludes content for admin context)
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                var titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Join<Post, String> tagJoin = root.join("tags", JoinType.LEFT);
                var tagLike = cb.like(cb.lower(tagJoin), pattern);
                query.distinct(true);
                predicates.add(cb.or(titleLike, tagLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Long, Long> batchLikeCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = postLikeRepository.countsByPostIds(postIds);
        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
