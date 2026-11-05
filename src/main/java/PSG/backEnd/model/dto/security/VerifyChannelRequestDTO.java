package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "OTP code to confirm channel ownership")
public record VerifyChannelRequestDTO(

        @Schema(description = "6-digit OTP code sent to the channel", example = "483920")
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^\\d{6}$", message = "{channel.verification.code.format}")
        String code
) {}
