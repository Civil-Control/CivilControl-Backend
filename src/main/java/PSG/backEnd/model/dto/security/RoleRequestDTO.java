package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * DTO to create or update a role.
 * The frontend sends the name, description and list of permission IDs.
 */
@Schema(description = "DTO to create or update a system role")
public record RoleRequestDTO(

        @Schema(description = "Role name. Must be unique and descriptive.",
                example = "Senior Architect",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 100)
        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 100, message = "{role.name.size}")
        String name,

        @Schema(description = "Role description and responsibilities",
                example = "Experienced architect who supervises projects and approves designs",
                maxLength = 500)
        @Size(max = 500, message = "{validation.size}")
        String description,

        @Schema(description = "List of permission IDs to assign to the role. Must contain at least one permission.",
                example = "[1, 2, 5, 10]",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1)
        @NotEmpty(message = "{validation.required}")
        Set<Long> permissionIds,

        @Schema(description = "Indicates if the role is active",
                example = "true",
                defaultValue = "true")
        Boolean active
) {}

