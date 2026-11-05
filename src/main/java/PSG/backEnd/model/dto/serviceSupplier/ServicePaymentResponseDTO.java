package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO for service payment. Always linked to a service assignment.")
public record ServicePaymentResponseDTO(

        Long id,

        // ——— Fields from service assignment ———
        Long serviceAssignmentId,
        SubjectType subjectType,
        Long serviceSupplierId,
        String supplierName,
        String supplierTradeName,
        String supplierCuit,
        Long buildingId,
        String buildingName,
        Long vehicleId,
        String vehicleLicensePlate,
        ServiceType serviceType,
        String accountNumber,
        String accountHolder,

        // ——— Payment fields ———
        LocalDate paymentDate,
        BigDecimal amount,
        Integer year,
        Integer period,
        String referenceNumber,
        String comment,
        Long projectAreaId,
        String projectAreaName,
        String projectAreaColor,
        Long projectAreaTaskId,
        String projectAreaTaskName,
        PaymentMethod paymentMethod
) {}

