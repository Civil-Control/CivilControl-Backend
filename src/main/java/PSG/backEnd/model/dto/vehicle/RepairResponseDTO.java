package PSG.backEnd.model.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepairResponseDTO(
        Long id,
        LocalDate date,
        Long vehicleId,
        String vehicleLicensePlate,
        BigDecimal cost,
        String description,
        String employee,
        Long supplierId,
        String supplierLegalName,
        String repairType
) {}

