package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.PaymentSubjectType;
import PSG.backEnd.model.enums.ServiceCategory;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO for service payment, supporting both building-based and vehicle-based payments.")
public record ServicePaymentResponseDTO(

        Long id,

        PaymentSubjectType subjectType,

        // ——— Building-based fields (from service assignment) ———
        Long serviceAssignmentId,
        Long serviceSupplierId,
        String supplierName,
        String supplierTradeName,
        String supplierCuit,
        Long buildingId,
        String buildingName,
        ServiceType serviceType,
        ServiceCategory serviceCategory,
        String accountNumber,
        String accountHolder,

        // ——— Vehicle-based fields ———
        Long vehicleId,
        String vehicleLicensePlate,

        // ——— Common fields ———
        LocalDate paymentDate,
        BigDecimal amount,
        Integer year,
        Integer period,
        String referenceNumber,
        String comment,
        Long projectAreaId,
        String projectAreaName,
        String projectAreaColor,
        PaymentMethod paymentMethod
) {}

