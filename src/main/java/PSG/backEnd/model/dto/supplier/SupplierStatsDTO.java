package PSG.backEnd.model.dto.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Aggregated invoicing statistics for a specific supplier.")
public record SupplierStatsDTO(
    BigDecimal totalInvoiced,
    BigDecimal totalPaid,
    BigDecimal totalCredited,
    BigDecimal totalPending
) {}
