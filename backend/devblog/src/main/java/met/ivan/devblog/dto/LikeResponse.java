package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResponse {
    private final long likeCount;
    private final boolean liked;
}
