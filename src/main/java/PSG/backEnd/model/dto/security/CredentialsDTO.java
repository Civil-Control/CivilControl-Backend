package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for user credentials (username and password).
 *
 * <p>Note: structural password validation is intentionally minimal here
 * (length only). Full password policy (strength, denylist, HIBP breach
 * check) is enforced server-side by {@code PasswordPolicyService} at
 * create/update time. Login flows must accept any pre-existing password
 * a user may have set under an older policy; the strict policy only
 * applies when a NEW password is being chosen.
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

        @Schema(description = "User password. New passwords must be at least 12 characters long " +
                "and contain upper-case, lower-case, digit and a symbol; must not appear in known breach corpora.",
                example = "My$ecureP4ssphrase",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 8,
                maxLength = 100)
        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{user.password.size}")
        String password
) {}

