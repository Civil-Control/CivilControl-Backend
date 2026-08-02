package PSG.backEnd.model.dto.transactionalDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionalDocumentFilterDTO(
    String documentNumber,
    String documentType,
    Long supplierId,
    String supplierCuit,
    String supplierName,
    String supplierAlias,
    Long projectAreaId,
    String projectAreaName,
    BigDecimal minTotalAmount,
    BigDecimal maxTotalAmount,
    BigDecimal totalAmount,
    LocalDate fromDate,
    LocalDate toDate,
    Boolean paid,
    String search
) {}
