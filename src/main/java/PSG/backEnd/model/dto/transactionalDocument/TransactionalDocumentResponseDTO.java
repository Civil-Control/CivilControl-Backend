package PSG.backEnd.model.dto.transactionalDocument;

import java.math.BigDecimal;

public record TransactionalDocumentResponseDTO(

    Long id,
    String date,
    String documentType,
    String branchCode,
    String documentNumber,
    Long supplierId,
    String supplierName,
    BigDecimal otherTaxes,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal total,
    BigDecimal discountPercentage,
    String comment,
    boolean paid,
    boolean deleted
) {}
