package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CheckPaymentDTO(
        @NotNull(message = "Payment details cannot be null", groups = OnCreate.class)
        @Valid
        PaymentDetailsDTO paymentDetails,

        @NotNull(message = "Due date is required", groups = OnCreate.class)
        @Future(message = "Due date must be in the future", groups = {OnCreate.class, OnUpdate.class})
        LocalDate dueDate,

        @NotBlank(message = "Check number is required", groups = OnCreate.class)
        @Size(max = 50, message = "Check number cannot exceed 50 characters", groups = {OnCreate.class, OnUpdate.class})
        String checkNumber,

        @NotBlank(message = "Bank name is required", groups = OnCreate.class)
        @Size(max = 100, message = "Bank name cannot exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
        String bankName,

        boolean deleted
) {}