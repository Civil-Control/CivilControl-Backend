package PSG.backEnd.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionalDocumentFilterDTO(
    String documentNumber,
    String supplierCuit,
    String supplierName,
    BigDecimal minTotalAmount,
    BigDecimal maxTotalAmount,
    BigDecimal totalAmount,
    LocalDate fromDate,
    LocalDate toDate
) {}
