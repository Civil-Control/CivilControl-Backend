package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Complete sales report with hierarchical grouping: Area -> Client -> Rows (Invoices + orphan Certifications)")
public record SalesReportDTO(
        SalesReportFilterDTO filters,
        List<SalesReportAreaGroupDTO> areaGroups,
        BigDecimal totalAmount,
        int totalCount,
        Map<SalesDocumentType, BigDecimal> totalsByDocumentType,
        BigDecimal totalNet,
        BigDecimal totalIva,
        BigDecimal totalIvaExempt,
        BigDecimal totalOtherTaxes,
        BigDecimal totalInvoiced,
        BigDecimal totalCertifiedOnly,
        BigDecimal totalCertifiedLinked,
        BigDecimal totalPaidAmount,
        BigDecimal totalUnpaidAmount,
        int invoiceCount,
        int certificationOnlyCount,
        LocalDateTime generatedAt,
        String reportName,
        String periodDescription
) {}
