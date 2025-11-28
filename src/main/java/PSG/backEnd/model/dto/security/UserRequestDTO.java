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
        @NotNull(message = "Credentials are required")
        @Valid
        CredentialsDTO credentials,

        @Schema(description = "Unique user email",
                example = "juan.garcia@esea.com.ar",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be in valid format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Schema(description = "User first name",
                example = "Juan",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2,
                maxLength = 50)
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        @Pattern(regexp = "^[\\p{L}\\s.'-]+$",
                message = "First name must contain only letters, spaces, dots, hyphens and apostrophes")
        String firstName,

        @Schema(description = "User last name",
                example = "García",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2,
                maxLength = 50)
        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        @Pattern(regexp = "^[\\p{L}\\s.'-]+$",
                message = "Last name must contain only letters, spaces, dots, hyphens and apostrophes")
        String lastName,

        @Schema(description = "User job title or position (for display)",
                example = "Senior Architect",
                maxLength = 100)
        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        @Schema(description = "List of role IDs to assign to the user. Must contain at least one role.",
                example = "[1, 2]",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1)
        @NotEmpty(message = "At least one role is required")
        Set<Long> roleIds,

        @Schema(description = "Indicates if the user is enabled",
                example = "true",
                defaultValue = "true")
        Boolean enabled
) {}

