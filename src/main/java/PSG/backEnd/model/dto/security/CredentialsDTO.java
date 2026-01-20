package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for user credentials (username and password).
 */
@Schema(description = "User authentication credentials")
public record CredentialsDTO(

        @Schema(description = "Unique username for login",
                example = "jgarcia",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 50)
        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 50, message = "{user.username.size}")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "{validation.pattern}")
        String username,

        @Schema(description = "User password. Must have at least 8 characters, " +
                "one uppercase letter, one lowercase letter and one digit",
                example = "SecurePass123",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8,
                maxLength = 100)
        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{user.password.size}")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "{user.password.pattern}")
        String password
) {}

