package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Pago en efectivo. La caja es opcional: si no se especifica, no se registra movimiento de caja.")
public record CashPaymentDTO(

        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Caja desde la que se egresa el efectivo. Opcional: si es null no se registra " +
                "movimiento de caja. Si se provee, debe corresponder a una caja activa.")
        Long cashBoxId,

        boolean deleted
) {}
