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
        String checkNumber,

        @Size(max = 50, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
        String transferNumber,

        @Size(max = 255, message = "{validation.size}")
        String supplierName,

        @Size(max = 50, message = "{validation.size}")
        String supplierAlias,

        Long supplierId,

        /**
         * Coincidence (substring) filter on the payment amount. The amount is compared
         * as a string, so {@code "150"} matches {@code 150}, {@code 1500}, {@code 21500.00}, etc.
         */
        @Size(max = 20, message = "{validation.size}")
        String amount,

        /**
         * Generic search applied across the supplier name (legal/trade) and the payment amount
         * (substring match on either). Intended for the table's main search box.
         */
        @Size(max = 255, message = "{validation.size}")
        String search
) {}
