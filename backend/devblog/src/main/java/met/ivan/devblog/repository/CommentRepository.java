package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Fetch comments for a post with comment author and post author eagerly loaded (avoids N+1).
     * Pageable controls ordering and pagination (caller should supply createdAt DESC sort).
     * Explicit countQuery avoids Hibernate problems counting fetch-joined results.
     */
    @Query(value = "SELECT c FROM Comment c " +
                   "JOIN FETCH c.author " +
                   "JOIN FETCH c.post p " +
                   "JOIN FETCH p.author " +
                   "WHERE c.post.id = :postId",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    Page<Comment> findByPostIdWithAuthor(@Param("postId") Long postId, Pageable pageable);

    /**
     * Admin global comment listing with optional search across content, author username, and
     * post title. JOIN FETCHes author and post (with post author) to avoid N+1.
     * Explicit countQuery avoids Hibernate problems counting fetch-joined results.
     * Pass {@code null} for search to return all comments.
     */
    @Query(value = "SELECT c FROM Comment c " +
                   "JOIN FETCH c.author a " +
                   "JOIN FETCH c.post p " +
                   "JOIN FETCH p.author " +
                   "WHERE (:search IS NULL OR " +
                   "       LOWER(c.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                   "       LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                   "       LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(c) FROM Comment c " +
                        "JOIN c.author a " +
                        "JOIN c.post p " +
                        "WHERE (:search IS NULL OR " +
                        "       LOWER(c.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "       LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "       LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Comment> findAllWithAuthorAndPost(@Param("search") String search, Pageable pageable);
}
