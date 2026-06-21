package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.slug = :slug")
    Optional<Post> findBySlug(@Param("slug") String slug);

    @Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.slug = :slug AND p.status = :status")
    Optional<Post> findBySlugAndStatus(@Param("slug") String slug, @Param("status") PostStatus status);

    @Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Post> findByIdWithAuthorAndCategory(@Param("id") Long id);

    boolean existsBySlug(String slug);

    boolean existsByCategory(Category category);

    long countByAuthorUsername(String username);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /** Count posts by status — used for admin stats totals. */
    long countByStatus(PostStatus status);

    /**
     * Top N published posts ordered by viewCount descending — used for admin stats.
     * Derived query; Pageable limits to 5.
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.category " +
           "WHERE p.status = :status ORDER BY p.viewCount DESC")
    List<Post> findTopByStatusOrderByViewCountDesc(@Param("status") PostStatus status, Pageable pageable);
}
