package PSG.backEnd.model.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AvailableCreditNoteDTO(
    Long id,
    String documentNumber,
    LocalDate date,
    BigDecimal total,
    BigDecimal appliedAmount,
    BigDecimal availableAmount
) {}
