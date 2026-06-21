package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorSummary {
    private final Long id;
    private final String username;
    private final String displayName;
}
