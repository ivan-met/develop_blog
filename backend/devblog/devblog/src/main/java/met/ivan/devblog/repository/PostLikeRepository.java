package met.ivan.devblog.repository;

import met.ivan.devblog.entity.PostLike;
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
}
