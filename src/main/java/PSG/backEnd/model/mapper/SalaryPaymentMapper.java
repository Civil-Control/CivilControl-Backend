package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalaryPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    @Mapping(target = "documentSortOrder", defaultExpression = "java(0)")
    @Mapping(target = "ivaPercentage", source = "ivaPercentage", defaultExpression = "java(new java.math.BigDecimal(\"21.00\"))")
    SalaryPayment toEntity(SalaryPaymentDTO salaryPaymentDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    @Mapping(target = "transactionalDocument", source = "transactionalDocument", qualifiedByName = "documentToSummaryDto")
    @Mapping(target = "ivaPercentage", source = "ivaPercentage")
    @Mapping(target = "totalWithIva", expression = "java(computeTotalWithIva(salaryPayment))")
    SalaryPaymentResponseDTO toResponseDto(SalaryPayment salaryPayment);

    default java.math.BigDecimal computeTotalWithIva(SalaryPayment sp) {
        if (sp.getTransactionalDocument() == null || sp.getAmount() == null) return null;
        java.math.BigDecimal iva = sp.getIvaPercentage();
        if (iva == null || iva.compareTo(java.math.BigDecimal.ZERO) <= 0) return sp.getAmount();
        return sp.getAmount()
                .multiply(iva.divide(new java.math.BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP)
                        .add(java.math.BigDecimal.ONE))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    @Mapping(target = "ivaPercentage", source = "ivaPercentage")
    void partialUpdate(SalaryPaymentDTO updateDTO, @MappingTarget SalaryPayment salaryPayment);

    @Named("documentToSummaryDto")
    default TransactionalDocumentSummaryDTO documentToSummaryDto(TransactionalDocument doc) {
        if (doc == null) return null;
        return new TransactionalDocumentSummaryDTO(
                doc.getId(),
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getBranchCode(),
                doc.getDocumentNumber(),
                doc.getSupplier() != null ? doc.getSupplier().getLegalName() : null,
                doc.getTotal(),
                doc.getDate()
        );
    }
}

