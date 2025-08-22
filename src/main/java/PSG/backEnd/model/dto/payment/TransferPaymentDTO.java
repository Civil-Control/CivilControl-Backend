package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record TransferPaymentDTO(
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @NotBlank(message = "Transaction number is required", groups = OnCreate.class)
        @Size(max = 100, message = "Transaction number cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String transactionNumber,

        @NotBlank(message = "Bank name is required", groups = OnCreate.class)
        @Size(max = 100, message = "Bank name cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        boolean deleted
) {}