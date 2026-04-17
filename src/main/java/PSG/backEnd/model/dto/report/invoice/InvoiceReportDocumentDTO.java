package PSG.backEnd.model.dto.report.invoice;

import PSG.backEnd.model.enums.documents.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single transactional document entry within the invoice report")
public record InvoiceReportDocumentDTO(
    Long id,
    LocalDate date,
    DocumentType documentType,
    String branchCode,
    String documentNumber,
    BigDecimal totalAmount,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal otherTaxes,
    String supplierLegalName,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    Boolean paid,
    String projectAreaName
) {}
