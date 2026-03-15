package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO containing complete information about a vehicle licence plate payment, " +
        "including related vehicle details.")
public record LicencePlatePaymentResponseDTO(

        @Schema(description = "Unique identifier of the licence plate payment record.",
                example = "42")
        Long id,

        @Schema(description = "Date when the payment was made.",
                example = "2024-05-15")
        LocalDate date,

        @Schema(description = "ID of the vehicle for which the payment was made.",
                example = "25")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle for which the payment was made.",
                example = "XYZ789")
        String vehicleLicensePlate,

        @Schema(description = "Total amount paid for the licence plate.",
                example = "3500.00")
        BigDecimal amount,

        @Schema(description = "Year for which the payment applies.",
                example = "2024")
        Integer year,

        @Schema(description = "Period (month) for which the payment applies. 1 = January, 12 = December.",
                example = "5")
        Integer period,

        @Schema(description = "Type of jurisdiction where the payment was made.",
                example = "PROVINCIAL",
                allowableValues = {"PROVINCIAL", "MUNICIPAL", "NATIONAL"})
        String jurisdictionType,

        @Schema(description = "ID of the project area associated with this payment.",
                example = "3")
        Long projectAreaId
) {}
