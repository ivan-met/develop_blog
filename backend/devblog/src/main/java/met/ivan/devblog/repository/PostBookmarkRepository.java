package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    /**
     * Return a page of bookmarked posts for a user, with author and category eagerly loaded.
     * The explicit countQuery avoids Hibernate trying to count the fetch-joined result.
     */
    @Query(value = "SELECT pb.post FROM PostBookmark pb " +
                   "JOIN FETCH pb.post.author " +
                   "LEFT JOIN FETCH pb.post.category " +
                   "WHERE pb.user.id = :userId",
           countQuery = "SELECT COUNT(pb) FROM PostBookmark pb WHERE pb.user.id = :userId")
    Page<Post> findBookmarkedPostsByUserId(@Param("userId") Long userId, Pageable pageable);
}
