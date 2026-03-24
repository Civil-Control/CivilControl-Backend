package PSG.backEnd.model.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Aggregated statistics for a specific client.")
public record ClientStatsDTO(
    BigDecimal totalInvoiced,
    BigDecimal totalCollected,
    BigDecimal totalPending
) {}
