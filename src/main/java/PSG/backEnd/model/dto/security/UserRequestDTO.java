package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * DTO to create or update a user.
 */
@Schema(description = "DTO to create or update a system user")
public record UserRequestDTO(

        @Schema(description = "User authentication credentials (username and password)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.required}")
        @Valid
        CredentialsDTO credentials,

        @Schema(description = "Unique user email",
                example = "juan.garcia@esea.com.ar",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email}")
        @Size(max = 100, message = "{validation.size}")
        String email,

        @Schema(description = "User first name",
                example = "Juan",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2,
                maxLength = 50)
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 50, message = "{validation.size}")
        @Pattern(regexp = "^[\\p{L}\\s.'-]+$",
                message = "{validation.pattern}")
        String firstName,

        @Schema(description = "User last name",
                example = "García",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2,
                maxLength = 50)
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 50, message = "{validation.size}")
        @Pattern(regexp = "^[\\p{L}\\s.'-]+$",
                message = "{validation.pattern}")
        String lastName,

        @Schema(description = "User job title or position (for display)",
                example = "Senior Architect",
                maxLength = 100)
        @Size(max = 100, message = "{validation.size}")
        String jobTitle,

        @Schema(description = "List of role IDs to assign to the user. Must contain at least one role.",
                example = "[1, 2]",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1)
        @NotEmpty(message = "{validation.required}")
        Set<Long> roleIds,

        @Schema(description = "Indicates if the user is enabled",
                example = "true",
                defaultValue = "true")
        Boolean enabled,

        @Schema(description = "User's default location for address autofill in forms")
        @Valid
        UserLocationDTO location
) {}

