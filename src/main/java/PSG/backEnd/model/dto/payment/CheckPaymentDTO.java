package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating check payments. " +
        "Represents payments made by check (cheque), including check number, bank, and due date. " +
        "This is one of the payment method options along with cash and transfers.")
public record CheckPaymentDTO(

        @Schema(description = "Payment details including amount, date, supplier reference, and other common payment information.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{payment.details.required}", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Due date of the check. Date when the check can be deposited or cashed.",
                example = "2025-12-31",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{payment.dueDate.required}", groups = OnCreate.class)
        LocalDate dueDate,

        @Schema(description = "Optional check number as printed on the physical check. " +
                "Can contain letters, digits, hyphens, and spaces. Maximum 50 characters.",
                example = "12345678",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 50, message = "{payment.checkNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String checkNumber,

        @Schema(description = "Name of the bank that issued the check. Maximum 100 characters.",
                example = "Banco Galicia",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{payment.bankName.required}", groups = OnCreate.class)
        @Size(max = 100, message = "{payment.bankName.size}", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        @Schema(description = "Soft deletion flag. When true, the payment is marked as deleted but remains in database.",
                example = "false")
        boolean deleted
) {}