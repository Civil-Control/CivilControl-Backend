package PSG.backEnd.model.dto.recovery;

import PSG.backEnd.model.enums.recovery.RecoveryBase;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Input payload to create or update a {@link PSG.backEnd.model.entity.recovery.RecoverySupplierConfig}.
 * The owning project area is taken from the URL path so this DTO does not carry it.
 */
@Schema(description = "Configuración de un proveedor dentro del esquema de recupero (Feature 18). " +
        "Define el porcentaje a recuperar, la base de cálculo (neto o total) y la caja destino.")
public record RecoverySupplierConfigDTO(

    @Schema(description = "Identificador del proveedor.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = {OnCreate.class})
    Long supplierId,

    @Schema(description = "Porcentaje a recuperar. Rango [0.00, 100.00].",
            example = "90.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = {OnCreate.class})
    @DecimalMin(value = "0.00", message = "{recovery.percentage.range}", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.00", message = "{recovery.percentage.range}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal recoveryPercentage,

    @Schema(description = "Base de cálculo para el porcentaje. NET aplica el % al neto y recupera el IVA al 100 % " +
            "(default). TOTAL aplica el % al total (neto + iva). Si se omite en una creación, se asume NET.",
            nullable = true)
    RecoveryBase recoveryBase,

    @Schema(description = "Identificador de la caja destino del recupero.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{recovery.cashBox.required}", groups = {OnCreate.class})
    Long cashBoxId,

    @Schema(description = "Si la configuración está activa. Por defecto true.", nullable = true)
    Boolean active
) {}
