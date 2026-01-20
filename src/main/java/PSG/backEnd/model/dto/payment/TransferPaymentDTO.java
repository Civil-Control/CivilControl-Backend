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
        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
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
        @NotBlank(message = "{payment.transactionNumber.required}", groups = OnCreate.class)
        @Size(min = 6, max = 100, message = "{payment.transactionNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        @Pattern(
                regexp = "^[a-zA-Z0-9]+$",
                message = "{payment.transactionNumber.pattern}",
                groups = {OnCreate.class, OnUpdate.class}
        )
        String transactionNumber,

        @Schema(description = "Name of the bank through which the transfer was made. Maximum 100 characters.",
                example = "Banco Nacion Argentina",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{payment.bankName.required}", groups = OnCreate.class)
        @Size(max = 100, message = "{payment.bankName.size}", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        @Schema(description = "Soft deletion flag. When true, the payment is marked as deleted but remains in database.",
                example = "false")
        boolean deleted
) {}