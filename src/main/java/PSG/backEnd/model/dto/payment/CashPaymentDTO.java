package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Data Transfer Object for creating or updating cash payments. " +
        "Represents payments made in physical currency (cash). " +
        "This is one of the payment method options along with transfers and checks.")
public record CashPaymentDTO(

        @Schema(description = "Payment details including amount, date, document reference, and other common payment information.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Soft deletion flag. When true, the payment is marked as deleted but remains in database.",
                example = "false")
        boolean deleted
) {}

