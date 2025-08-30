package PSG.backEnd.model.dto.transactionalDocument;

import PSG.backEnd.model.dto.item.ItemDetailResponseDTO;

import java.math.BigDecimal;
import java.util.List;

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
    boolean deleted,
    List<ItemDetailResponseDTO> items
) {}
