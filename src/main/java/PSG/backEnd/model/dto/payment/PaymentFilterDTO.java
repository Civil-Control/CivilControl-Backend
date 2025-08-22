package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.enums.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentFilterDTO(
        PaymentMethod paymentMethod,

        LocalDate startDate,

        LocalDate endDate,

        @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "Minimum amount must have up to 12 digits and 2 decimals", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal minAmount,

        @DecimalMin(value = "0.00", message = "Maximum amount cannot be negative", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "Maximum amount must have up to 12 digits and 2 decimals", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal maxAmount,

        @Size(max = 50, message = "Transaction number cannot exceed 50 characters", groups = {OnCreate.class, OnUpdate.class})
        String transactionNumber,

        Long supplierId
) {}
