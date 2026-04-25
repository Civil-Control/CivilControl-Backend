package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of sales rows for a single client within an area")
public record SalesReportClientGroupDTO(
        Long clientId,
        String clientBusinessName,
        String clientTradeName,
        String clientCuit,
        IvaCondition clientIvaCondition,
        BigDecimal totalAmount,
        int rowCount,
        Map<SalesDocumentType, BigDecimal> subtotalsByDocumentType,
        BigDecimal subtotalNet,
        BigDecimal subtotalIva,
        BigDecimal subtotalInvoiced,
        BigDecimal subtotalCertifiedOnly,
        List<SalesReportRowDTO> rows
) {}
