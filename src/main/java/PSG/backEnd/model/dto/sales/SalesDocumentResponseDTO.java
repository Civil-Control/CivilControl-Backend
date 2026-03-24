package PSG.backEnd.model.dto.sales;

import PSG.backEnd.model.dto.client.ClientSummaryDTO;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Full sales document data returned from the API.")
public record SalesDocumentResponseDTO(
    Long id,
    SalesDocumentType documentType,
    String branchCode,
    String documentNumber,
    LocalDate date,
    ClientSummaryDTO client,
    String purchaseOrderReference,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal otherTaxes,
    BigDecimal total,
    BigDecimal discountPercentage,
    Long projectAreaId,
    String projectAreaName,
    List<SalesItemDetailResponseDTO> items,
    Boolean paid,
    String comment,
    Boolean deleted
) {}
