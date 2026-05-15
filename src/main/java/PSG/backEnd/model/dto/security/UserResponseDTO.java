package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Response DTO for User.
 * Does not include password for security reasons.
 */
@Schema(description = "Represents a system user (without password)")
public record UserResponseDTO(

        @Schema(description = "Unique user ID", example = "1")
        Long id,

        @Schema(description = "Username", example = "jgarcia")
        String username,

        @Schema(description = "User email", example = "juan.garcia@esea.com.ar")
        String email,

        @Schema(description = "User first name", example = "Juan")
        String firstName,

        @Schema(description = "User last name", example = "García")
        String lastName,

        @Schema(description = "User full name", example = "Juan García")
        String fullName,

        @Schema(description = "User job title", example = "Senior Architect")
        String jobTitle,

        @Schema(description = "List of roles assigned to the user")
        Set<RoleSimpleDTO> roles,

        @Schema(description = "Indicates if the user is enabled", example = "true")
        Boolean enabled,

        @Schema(description = "User's default location for address autofill")
        UserLocationDTO location,

        @Schema(description = "WhatsApp number in E.164 format", example = "+5491112345678", nullable = true)
        String whatsappNumber,

        @Schema(description = "Whether the email channel has been verified via OTP", example = "false")
        Boolean emailVerified,

        @Schema(description = "Whether the WhatsApp channel has been verified via OTP", example = "false")
        Boolean whatsappVerified
) {}

