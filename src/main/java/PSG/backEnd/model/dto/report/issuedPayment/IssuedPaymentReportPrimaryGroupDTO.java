package PSG.backEnd.model.dto.report.issuedPayment;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Layer-1 group of the Issued Payments Report.
 *
 * <p>Depending on {@code groupBy}:
 * <ul>
 *   <li><b>METHOD</b>:   {@code groupKey} = "CASH"/"TRANSFER"/"CHECK", {@code groupLabel} = display name.
 *       {@link #subtotalsByMethod()} is {@code null}.</li>
 *   <li><b>SUPPLIER</b>: {@code groupKey} = supplierId.toString(), {@code groupLabel} = legal name,
 *       {@code groupSubLabel} = CUIT.
 *       {@link #subtotalsByMethod()} is populated for the segmented bar visualization.</li>
 * </ul>
 */
@Builder
@Schema(description = "Layer-1 group of the Issued-Payments Report")
public record IssuedPaymentReportPrimaryGroupDTO(
    String groupKey,
    String groupLabel,
    String groupSubLabel,
    BigDecimal subtotalAmount,
    int paymentCount,
    Map<PaymentMethod, BigDecimal> subtotalsByMethod,
    List<IssuedPaymentReportSecondaryGroupDTO> secondaryGroups
) {}
