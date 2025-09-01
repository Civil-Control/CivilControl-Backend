package PSG.backEnd.model.dto.transactionalDocument;

import PSG.backEnd.model.dto.item.ItemDetailResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record TransactionalDocumentResponseDTO(

    Long id,
    String date,
    Long supplierId,
    String supplierName,
    String documentType,
    String branchCode,
    String documentNumber,
    List<ItemDetailResponseDTO> items,
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
