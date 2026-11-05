package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of sales rows for a project area (sector)")
public record SalesReportAreaGroupDTO(
        Long projectAreaId,
        String projectAreaName,
        String projectAreaColor,
        BigDecimal subtotalAmount,
        int rowCount,
        Map<SalesDocumentType, BigDecimal> subtotalsByDocumentType,
        BigDecimal subtotalNet,
        BigDecimal subtotalIva,
        BigDecimal subtotalInvoiced,
        BigDecimal subtotalCertifiedOnly,
        List<SalesReportClientGroupDTO> clientGroups
) {}
