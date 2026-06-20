package met.ivan.devblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import met.ivan.devblog.entity.PostStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String contentMarkdown;

    @Size(max = 300, message = "Excerpt must not exceed 300 characters")
    private String excerpt;

    private Long categoryId;

    private PostStatus status;
}
