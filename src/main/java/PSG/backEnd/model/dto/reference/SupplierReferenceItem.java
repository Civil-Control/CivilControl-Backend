package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reference item for supplier dropdowns / search modals.
 *
 * <p>Extends the basic {@link ReferenceItem} contract with the three
 * searchable supplier name variants so the frontend can match user
 * input against any of them (legal name, trade name, alias).</p>
 */
@Schema(description = "Reference item for supplier selections, with searchable name variants")
public record SupplierReferenceItem(
        @Schema(description = "Supplier id", example = "42")
        Long id,
        @Schema(description = "Display label (trade name when present, otherwise legal name)",
                example = "Servicios Andes S.A.")
        String label,
        @Schema(description = "Legal (registered) name", example = "Servicios Andes Sociedad Anónima",
                nullable = true)
        String legalName,
        @Schema(description = "Trade / commercial name", example = "Servicios Andes", nullable = true)
        String tradeName,
        @Schema(description = "Alias / short nickname", example = "SA", nullable = true)
        String alias,
        @Schema(description = "CUIT / fiscal identification number", example = "30-12345678-9", nullable = true)
        String cuit
) {}
