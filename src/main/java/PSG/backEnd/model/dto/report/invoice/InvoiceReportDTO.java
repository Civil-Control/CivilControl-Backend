package PSG.backEnd.model.dto.report.invoice;

import PSG.backEnd.model.enums.documents.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Complete invoice report with hierarchical grouping: Area → Supplier → Documents")
public record InvoiceReportDTO(
    InvoiceReportFilterDTO filters,
    List<InvoiceReportAreaGroupDTO> areaGroups,
    BigDecimal totalAmount,
    BigDecimal totalNet,
    BigDecimal totalIva,
    BigDecimal totalIvaExempt,
    int totalCount,
    Map<DocumentType, BigDecimal> totalsByDocumentType,
    Map<String, BigDecimal> totalsByIvaRate,
    LocalDateTime generatedAt,
    String reportName,
    String periodDescription
) {}
