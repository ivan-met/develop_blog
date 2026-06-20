package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
