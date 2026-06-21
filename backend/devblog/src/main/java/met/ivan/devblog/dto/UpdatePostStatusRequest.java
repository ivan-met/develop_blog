package met.ivan.devblog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import met.ivan.devblog.entity.PostStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostStatusRequest {

    @NotNull(message = "Status is required")
    private PostStatus status;
}
