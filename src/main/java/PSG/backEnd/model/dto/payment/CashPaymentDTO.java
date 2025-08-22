package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CashPaymentDTO(
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        boolean deleted
) {}

