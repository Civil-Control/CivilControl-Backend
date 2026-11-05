package PSG.backEnd.model.dto.report.policyPayment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Insured vehicle information within a policy group")
public record PolicyPaymentReportVehicleDTO(

    @Schema(description = "Vehicle ID")
    Long vehicleId,

    @Schema(description = "License plate")
    String licensePlate,

    @Schema(description = "Vehicle brand")
    String brand,

    @Schema(description = "Vehicle model")
    String model,

    @Schema(description = "Monthly premium for this vehicle in the policy")
    BigDecimal premioMensual,

    @Schema(description = "Total premium for this vehicle in the policy")
    BigDecimal premioTotal,

    @Schema(description = "Sum insured for this vehicle")
    BigDecimal sumInsured,

    @Schema(description = "Project area name of the vehicle", nullable = true)
    String projectAreaName
) {}
