package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    /**
     * Batch count likes for a set of post IDs.
     * Returns pairs of [postId, count] to enrich list responses without N+1.
     */
    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countsByPostIds(@Param("postIds") Collection<Long> postIds);

    /**
     * Check which of the given post IDs the user has already liked.
     */
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    /**
     * Top N posts by like count. Returns pairs [Post, likeCount].
     * Post associations (author, category) must be accessed within a transaction so that
     * Hibernate can load them lazily, or the caller re-fetches the posts with JOIN FETCH.
     * Pageable limits to top N (e.g. 5).
     * Note: FETCH joins are not compatible with aggregate GROUP BY in JPQL; the post entity
     * is returned by reference and its lazy associations will be loaded within the same
     * transaction by the service layer.
     */
    @Query("SELECT pl.post, COUNT(pl) AS likeCount " +
           "FROM PostLike pl " +
           "GROUP BY pl.post " +
           "ORDER BY likeCount DESC")
    List<Object[]> findTopPostsByLikeCount(Pageable pageable);
}
