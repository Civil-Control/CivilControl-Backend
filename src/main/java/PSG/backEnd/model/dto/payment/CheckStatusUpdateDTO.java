package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.enums.payment.CheckStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for {@code PATCH /api/v1/payments/check/{id}/status}.
 * <p>
 * Backend rules:
 * <ul>
 *   <li>{@code VENCIDO} cannot be set manually — it is a derived state.</li>
 *   <li>{@code settledDate} is required when transitioning to {@link CheckStatus#COBRADO}
 *       or {@link CheckStatus#RECHAZADO} and may not be earlier than the original payment date.</li>
 *   <li>Transitions out of {@link CheckStatus#COBRADO} or {@link CheckStatus#CANCELADO}
 *       (terminal states) are forbidden.</li>
 * </ul>
 */
@Schema(description = "Update payload for the operational status of a check payment")
public record CheckStatusUpdateDTO(

        @Schema(description = "New operational status (VENCIDO is not allowed — it is derived)",
                example = "COBRADO")
        @NotNull
        CheckStatus status,

        @Schema(description = "Settled / rejection date — required for COBRADO and RECHAZADO",
                example = "2026-04-22")
        LocalDate settledDate,

        @Schema(description = "Free-form note explaining the status change", maxLength = 500)
        @Size(max = 500)
        String statusComment
) {}
