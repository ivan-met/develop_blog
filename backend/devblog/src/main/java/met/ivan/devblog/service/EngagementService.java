package met.ivan.devblog.service;

import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EngagementService {

    LikeResponse like(String slug, String username);

    LikeResponse unlike(String slug, String username);

    BookmarkResponse bookmark(String slug, String username);

    BookmarkResponse removeBookmark(String slug, String username);

    Page<PostSummaryResponse> listBookmarks(String username, Pageable pageable);
}
