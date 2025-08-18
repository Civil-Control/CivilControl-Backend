package PSG.backEnd.model.dto;

import java.math.BigDecimal;

public record TransactionalDocumentResponseDTO(

    Long id,
    String date,
    String documentType,
    String branchCode,
    String documentNumber,
    Long supplierId,
    BigDecimal otherTaxes,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal total,
    BigDecimal discountPercentage,
    String comment,
    boolean deleted
) {}
