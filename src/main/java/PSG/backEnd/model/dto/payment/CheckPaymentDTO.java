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
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @Schema(description = "Due date of the check. Date when the check can be deposited or cashed. " +
                "Must be a future date relative to when the check is issued.",
                example = "2025-12-31",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Due date is required", groups = OnCreate.class)
        @Future(message = "Due date must be in the future", groups = {OnCreate.class, OnUpdate.class})
        LocalDate dueDate,

        @Schema(description = "Check number as printed on the physical check. " +
                "Can contain letters, digits, hyphens, and spaces. Maximum 50 characters. " +
                "This is the unique identifier of the check.",
                example = "12345678",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Check number is required", groups = OnCreate.class)
        @Size(max = 50, message = "Check number must not exceed 50 characters", groups = {OnCreate.class, OnUpdate.class})
        String checkNumber,

        @Schema(description = "Name of the bank that issued the check. Maximum 100 characters.",
                example = "Banco Galicia",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Bank name is required", groups = OnCreate.class)
        @Size(max = 100, message = "Bank name cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        @Schema(description = "Soft deletion flag. When true, the payment is marked as deleted but remains in database.",
                example = "false")
        boolean deleted
) {}