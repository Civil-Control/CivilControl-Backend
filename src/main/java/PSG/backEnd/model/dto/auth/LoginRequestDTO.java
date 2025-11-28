package PSG.backEnd.model.dto.auth;

import PSG.backEnd.model.dto.security.CredentialsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for login request.
 */
@Schema(description = "Login credentials")
public record LoginRequestDTO(

        @Schema(description = "User authentication credentials", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Credentials are required")
        @Valid
        CredentialsDTO credentials
) {}
