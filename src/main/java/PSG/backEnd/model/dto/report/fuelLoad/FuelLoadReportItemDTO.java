package PSG.backEnd.model.dto.report.fuelLoad;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single fuel load entry within the report")
public record FuelLoadReportItemDTO(

    @Schema(description = "Fuel load ID")
    Long id,

    @Schema(description = "Date of the fuel load")
    LocalDate date,

    @Schema(description = "Fuel type (e.g. INFINIA, SUPER)")
    String fuelType,

    @Schema(description = "Liters loaded")
    BigDecimal liters,

    @Schema(description = "Price per liter")
    BigDecimal pricePerLiter,

    @Schema(description = "Total amount")
    BigDecimal totalAmount,

    @Schema(description = "Vehicle license plate", nullable = true)
    String vehicleLicensePlate,

    @Schema(description = "Vehicle description (brand + model)", nullable = true)
    String vehicleDescription,

    @Schema(description = "Gas station name (supplier legal name)")
    String gasStationName,

    @Schema(description = "Branch code")
    String branchCode,

    @Schema(description = "Ticket number")
    String ticketNumber,

    @Schema(description = "Project area task ID", nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Project area task name", nullable = true)
    String projectAreaTaskName,

    @Schema(description = "Project area name")
    String projectAreaName
) {}
