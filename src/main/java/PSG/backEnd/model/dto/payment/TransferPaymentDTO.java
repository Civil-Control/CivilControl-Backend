package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Schema(description = "Data Transfer Object for creating or updating bank transfer payments. " +
        "Represents payments made via electronic bank transfer, including transaction number and bank details. " +
        "This is one of the payment method options along with cash and checks.")
public record TransferPaymentDTO(

        @Schema(description = "Payment details including amount, date, supplier reference, and other common payment information.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Unique transaction number or reference provided by the bank. " +
                "Must contain only letters and digits. Length between 6 and 100 characters. " +
                "This number is used to track and verify the transfer.",
                example = "TRF20240315ABC123XYZ",
                pattern = "^[a-zA-Z0-9]{6,100}$",
                minLength = 6,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Transaction number is required", groups = OnCreate.class)
        @Pattern(
                regexp = "^[a-zA-Z0-9]{6,100}$",
                message = "Transaction number must contain only letters and digits, and be between 6 and 100 characters long",
                groups = {OnCreate.class, OnUpdate.class}
        )
        String transactionNumber,

        @Schema(description = "Name of the bank through which the transfer was made. Maximum 100 characters.",
                example = "Banco Nación Argentina",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Bank name is required", groups = OnCreate.class)
        @Size(max = 100, message = "Bank name cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        @Schema(description = "Soft deletion flag. When true, the payment is marked as deleted but remains in database.",
                example = "false")
        boolean deleted
) {}