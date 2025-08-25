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
        @Pattern(
                regexp = "^[a-zA-Z0-9]{6,100}$",
                message = "Transaction number must contain only letters and digits, and be between 6 and 100 characters long",
                groups = {OnCreate.class, OnUpdate.class}
        )

        @NotBlank(message = "Bank name is required", groups = OnCreate.class)
        @Size(max = 100, message = "Bank name cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        boolean deleted
) {}