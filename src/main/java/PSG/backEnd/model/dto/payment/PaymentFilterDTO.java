package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentFilterDTO(
        PaymentMethod paymentMethod,

        LocalDate startDate,

        LocalDate endDate,

        @DecimalMin(value = "0.00", message = "{validation.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal minAmount,

        @DecimalMin(value = "0.00", message = "{validation.min}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal maxAmount,

        @Size(max = 50, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String transactionNumber,

        Long supplierId
) {}
