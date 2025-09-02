package PSG.backEnd.model.dto.transactionalDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionalDocumentFilterDTO(
    String documentNumber,
    String supplierCuit,
    String supplierName,
    Long projectAreaId,
    String projectAreaName,
    BigDecimal minTotalAmount,
    BigDecimal maxTotalAmount,
    BigDecimal totalAmount,
    LocalDate fromDate,
    LocalDate toDate,
    Boolean paid
) {}
