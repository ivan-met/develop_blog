package met.ivan.devblog.service;

import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorService {

    AuthorProfileResponse getPublicProfile(String username);

    Page<PostSummaryResponse> getPublishedPosts(String username, Pageable pageable);
}
