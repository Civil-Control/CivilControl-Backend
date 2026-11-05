package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter DTO for searching service payments by various criteria.")
public record ServicePaymentFilterDTO(

        SubjectType subjectType,

        Long serviceAssignmentId,
        Long serviceSupplierId,
        Long buildingId,
        Long projectAreaId,
        ServiceType serviceType,
        Long vehicleId,
        Integer year,
        Integer period,

        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String referenceNumber,
        String supplierName,
        String search
) {}

