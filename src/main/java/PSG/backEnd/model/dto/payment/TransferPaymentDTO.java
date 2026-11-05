package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Schema(description = "Pago por transferencia bancaria. La cuenta bancaria es obligatoria.")
public record TransferPaymentDTO(

        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Size(min = 6, max = 100, message = "{payment.transactionNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$",
                message = "{payment.transactionNumber.pattern}",
                groups = {OnCreate.class, OnUpdate.class}
        )
        String transactionNumber,

        @Schema(description = "Cuenta bancaria emisora de la transferencia. Reemplaza al antiguo `bankName`.")
        @NotNull(message = "{treasury.bankAccount.required}", groups = OnCreate.class)
        Long bankAccountId,

        boolean deleted
) {}
