package PSG.backEnd.model.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairFilterDTO(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long vehicleId,
        String vehicleLicensePlate,
        BigDecimal minCost,
        BigDecimal maxCost,
        String employee,
        Long supplierId,
        String supplierLegalName,
        String repairType
) {}

