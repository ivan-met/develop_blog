package met.ivan.devblog.repository;

import met.ivan.devblog.entity.RefreshToken;
import met.ivan.devblog.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository")
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = em.persistAndFlush(User.builder()
                .username("tokenuser")
                .email("token@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>())
                .build());
    }

    @Test
    @DisplayName("findByToken: returns token when found")
    void findByToken_found() {
        RefreshToken token = em.persistAndFlush(RefreshToken.builder()
                .token("my-refresh-token")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        Optional<RefreshToken> result = refreshTokenRepository.findByToken("my-refresh-token");

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getUsername()).isEqualTo("tokenuser");
    }

    @Test
    @DisplayName("findByToken: returns empty when not found")
    void findByToken_notFound() {
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("revokeAllByUser: marks all user tokens as revoked")
    void revokeAllByUser() {
        em.persistAndFlush(RefreshToken.builder()
                .token("token1")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());
        em.persistAndFlush(RefreshToken.builder()
                .token("token2")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());
        em.clear();

        refreshTokenRepository.revokeAllByUser(testUser);
        em.clear();

        // Verify tokens are revoked
        Optional<RefreshToken> t1 = refreshTokenRepository.findByToken("token1");
        Optional<RefreshToken> t2 = refreshTokenRepository.findByToken("token2");
        assertThat(t1).isPresent();
        assertThat(t1.get().isRevoked()).isTrue();
        assertThat(t2).isPresent();
        assertThat(t2.get().isRevoked()).isTrue();
    }
}
