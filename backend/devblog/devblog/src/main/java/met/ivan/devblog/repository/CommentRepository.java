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
}
