package PSG.backEnd.model.dto.report.invoice;

import PSG.backEnd.model.enums.documents.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filter criteria for the invoice report")
public record InvoiceReportFilterDTO(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> projectAreaIds,
    DocumentType documentType,
    Boolean paid,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    Boolean includeUnassigned
) {}
