package met.ivan.devblog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolesRequest {

    @NotNull(message = "Roles list is required")
    private List<String> roles;
}
