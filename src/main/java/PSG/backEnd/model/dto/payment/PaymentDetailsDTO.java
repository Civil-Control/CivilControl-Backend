package PSG.backEnd.model.dto.payment;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentDetailsDTO(
        @NotNull(message = "Payment date is required", groups = OnCreate.class)
        @PastOrPresent(message = "Payment date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
        LocalDate paymentDate,

        @NotNull(message = "Supplier is required", groups = OnCreate.class)
        Long supplierId,

        @NotNull(message = "Amount is required", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 12, fraction = 2, message = "Amount must have up to 12 digits and 2 decimals", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Size(max = 500, message = "Comment cannot exceed 500 characters", groups = {OnCreate.class, OnUpdate.class})
        String comment,

        // Ahora es opcional - puede ser null o lista vacía para pagos independientes
        List<Long> paidDocumentIds
) {}