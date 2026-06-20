package met.ivan.devblog.service.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.dto.UpdatePostRequest;
import met.ivan.devblog.dto.UpdatePostStatusRequest;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.PostService;
import met.ivan.devblog.util.Slugs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostMapper postMapper;

    public PostServiceImpl(
            PostRepository postRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.postMapper = postMapper;
    }

    @Override
    public PostResponse create(String authorUsername, CreatePostRequest request) {
        User author = userRepository.findByUsernameWithRoles(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authorUsername));

        boolean isAdmin = author.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN);
        if (isAdmin) {
            throw new ForbiddenOperationException("Administrators are not permitted to author posts");
        }

        Category category = resolveCategory(request.getCategoryId());

        PostStatus status = request.getStatus() != null ? request.getStatus() : PostStatus.DRAFT;
        if (status == PostStatus.PUBLISHED) {
            requireCategory(category);
        }

        String slug = Slugs.uniqueSlug(request.getTitle(), postRepository::existsBySlug);

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(slug)
                .contentMarkdown(request.getContentMarkdown())
                .excerpt(request.getExcerpt())
                .status(status)
                .author(author)
                .category(category)
                .publishedAt(status == PostStatus.PUBLISHED ? Instant.now() : null)
                .build();

        post = postRepository.save(post);
        return postMapper.toResponse(post);
    }

    @Override
    public PostResponse update(Long id, UserDetails principal, UpdatePostRequest request) {
        Post post = loadPost(id);
        assertOwnerOrAdmin(post, principal);

        Category category = resolveCategory(request.getCategoryId());

        post.setTitle(request.getTitle());
        post.setContentMarkdown(request.getContentMarkdown());
        post.setExcerpt(request.getExcerpt());
        post.setCategory(category);
        // slug remains stable on update

        post = postRepository.save(post);
        return postMapper.toResponse(post);
    }

    @Override
    public PostResponse changeStatus(Long id, UserDetails principal, UpdatePostStatusRequest request) {
        Post post = loadPost(id);
        assertOwnerOrAdmin(post, principal);

        PostStatus newStatus = request.getStatus();
        if (newStatus == PostStatus.PUBLISHED) {
            requireCategory(post.getCategory());
            if (post.getPublishedAt() == null) {
                post.setPublishedAt(Instant.now());
            }
        }

        post.setStatus(newStatus);
        post = postRepository.save(post);
        return postMapper.toResponse(post);
    }

    @Override
    public void delete(Long id, UserDetails principal) {
        Post post = loadPost(id);
        assertOwnerOrAdmin(post, principal);
        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPublishedBySlug(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published post not found with slug: " + slug));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listPublished(String categorySlug, String search, Pageable pageable) {
        Specification<Post> spec = buildPublishedSpec(categorySlug, search);
        return postRepository.findAll(spec, pageable)
                .map(postMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getOwn(Long id, UserDetails principal) {
        Post post = loadPost(id);
        assertOwnerOrAdmin(post, principal);
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listOwn(UserDetails principal, PostStatus status, Pageable pageable) {
        Specification<Post> spec = buildOwnSpec(principal.getUsername(), status);
        return postRepository.findAll(spec, pageable)
                .map(postMapper::toSummary);
    }

    // --- helpers ---

    private Post loadPost(Long id) {
        return postRepository.findByIdWithAuthorAndCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Post post, UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        boolean isOwner = post.getAuthor().getUsername().equals(principal.getUsername());
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to modify this post");
        }
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private void requireCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("A category is required to publish a post");
        }
    }

    private Specification<Post> buildPublishedSpec(String categorySlug, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));

            if (categorySlug != null && !categorySlug.isBlank()) {
                Join<Post, Category> categoryJoin = root.join("category", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("slug"), categorySlug));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Post> buildOwnSpec(String username, PostStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Post, User> authorJoin = root.join("author", JoinType.INNER);
            predicates.add(cb.equal(authorJoin.get("username"), username));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
