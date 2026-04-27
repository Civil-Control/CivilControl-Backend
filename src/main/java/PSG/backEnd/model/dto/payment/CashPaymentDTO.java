package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Pago en efectivo. La caja es obligatoria si el tenant tiene al menos una caja activa.")
public record CashPaymentDTO(

        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Caja desde la que se egresa el efectivo. Obligatoria cuando existe " +
                "al menos una caja activa en el tenant; ignorada en caso contrario.")
        Long cashBoxId,

        boolean deleted
) {}
