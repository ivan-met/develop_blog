package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.service.impl.AdminPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPostServiceImpl")
class AdminPostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private AdminPostServiceImpl adminPostService;

    private User author;
    private Category category;
    private Post draftPost;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        Role userRole = TestDataFactory.userRole();
        author = TestDataFactory.userWithRole(userRole);
        category = TestDataFactory.category();
        draftPost = TestDataFactory.draftPost(author, category);
        publishedPost = TestDataFactory.publishedPost(author, category);
    }

    private PostSummaryResponse summaryOf(Post post) {
        return new PostSummaryResponse(
                post.getId(), post.getSlug(), post.getTitle(), post.getExcerpt(),
                post.getStatus(), null, null, post.getPublishedAt(), post.getCreatedAt(),
                post.getTags(), post.getViewCount(), 0L);
    }

    @Test
    @DisplayName("list: returns all posts across statuses when no filters given")
    void list_noFilters_returnsAllPosts() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postPage = new PageImpl<>(List.of(draftPost, publishedPost), pageable, 2);
        when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(postPage);
        when(postLikeRepository.countsByPostIds(any())).thenReturn(List.of());
        when(postMapper.toSummary(draftPost, 0L)).thenReturn(summaryOf(draftPost));
        when(postMapper.toSummary(publishedPost, 0L)).thenReturn(summaryOf(publishedPost));

        Page<PostSummaryResponse> result = adminPostService.list(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("list: filters by status when status param provided")
    void list_withStatusFilter_returnsFilteredResults() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postPage = new PageImpl<>(List.of(draftPost), pageable, 1);
        when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(postPage);
        when(postLikeRepository.countsByPostIds(any())).thenReturn(List.of());
        when(postMapper.toSummary(draftPost, 0L)).thenReturn(summaryOf(draftPost));

        Page<PostSummaryResponse> result = adminPostService.list(PostStatus.DRAFT, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    @DisplayName("list: returns empty page when no posts match")
    void list_noMatches_returnsEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postPage = new PageImpl<>(List.of(), pageable, 0);
        when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(postPage);

        Page<PostSummaryResponse> result = adminPostService.list(PostStatus.DRAFT, "nonexistent", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("list: like counts are batched and mapped correctly")
    void list_batchesLikeCounts() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postPage = new PageImpl<>(List.of(publishedPost), pageable, 1);
        when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(postPage);
        when(postLikeRepository.countsByPostIds(List.of(publishedPost.getId())))
                .thenReturn(List.<Object[]>of(new Object[]{publishedPost.getId(), 7L}));
        when(postMapper.toSummary(publishedPost, 7L)).thenReturn(
                new PostSummaryResponse(publishedPost.getId(), publishedPost.getSlug(),
                        publishedPost.getTitle(), publishedPost.getExcerpt(),
                        publishedPost.getStatus(), null, null, publishedPost.getPublishedAt(),
                        publishedPost.getCreatedAt(), publishedPost.getTags(),
                        publishedPost.getViewCount(), 7L));

        Page<PostSummaryResponse> result = adminPostService.list(null, null, null, pageable);

        assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(7L);
    }
}
