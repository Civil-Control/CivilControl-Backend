package PSG.backEnd.model.dto.report.invoice;

import PSG.backEnd.model.enums.documents.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of documents for a single supplier within an area")
public record InvoiceReportSupplierGroupDTO(
    Long supplierId,
    String supplierLegalName,
    String supplierTradeName,
    String supplierCuit,
    BigDecimal totalAmount,
    int documentCount,
    Map<DocumentType, BigDecimal> subtotalsByDocumentType,
    List<InvoiceReportDocumentDTO> documents
) {}
