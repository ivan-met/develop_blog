package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
}
