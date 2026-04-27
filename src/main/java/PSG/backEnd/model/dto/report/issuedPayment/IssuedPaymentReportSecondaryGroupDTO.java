package PSG.backEnd.model.dto.report.issuedPayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Layer-2 group of the Issued Payments Report. Sits inside a
 * {@link IssuedPaymentReportPrimaryGroupDTO} and contains the actual rows.
 */
@Builder
@Schema(description = "Layer-2 group inside an Issued-Payments Report primary group")
public record IssuedPaymentReportSecondaryGroupDTO(
    String groupKey,
    String groupLabel,
    String groupSubLabel,
    BigDecimal subtotalAmount,
    int paymentCount,
    List<IssuedPaymentReportItemDTO> payments
) {}
