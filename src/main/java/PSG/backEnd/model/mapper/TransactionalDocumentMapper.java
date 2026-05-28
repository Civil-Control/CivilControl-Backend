package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.transactionalDocument.CreditNoteApplicationResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.OnAccountApplicationResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {SupplierMapper.class, ItemDetailMapper.class}
)
public interface TransactionalDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "manuallyApplied", ignore = true)
    @Mapping(target = "items", ignore = true)  // Ignora items - se procesan manualmente en el service
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "creditNoteApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    TransactionalDocument toEntity(TransactionalDocumentDTO dto);

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.legalName", target = "supplierName")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    @Mapping(expression = "java(entity.getDocumentType().getDisplayName())", target = "documentType")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creditApplied", ignore = true)
    @Mapping(target = "pendingAmount", ignore = true)
    @Mapping(target = "remainingBalance", ignore = true)
    @Mapping(target = "creditApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    @Mapping(target = "onAccountApplications", ignore = true)
    @Mapping(target = "availableOnAccountBalance", ignore = true)
    TransactionalDocumentResponseDTO toResponseDto(TransactionalDocument entity);

    /**
     * Builds a fully enriched response DTO including derived business state.
     * The derived fields ({@code status}, {@code creditApplied}, {@code pendingAmount},
     * {@code creditApplications}, {@code appliedCredits}) are computed in the service layer
     * and passed in here.
     */
    default TransactionalDocumentResponseDTO toEnrichedResponseDto(
            TransactionalDocument entity,
            String status,
            BigDecimal creditApplied,
            BigDecimal pendingAmount,
            BigDecimal remainingBalance,
            List<CreditNoteApplicationResponseDTO> creditApplications,
            List<CreditNoteApplicationResponseDTO> appliedCredits,
            List<OnAccountApplicationResponseDTO> onAccountApplications,
            BigDecimal availableOnAccountBalance) {
        TransactionalDocumentResponseDTO base = toResponseDto(entity);
        return new TransactionalDocumentResponseDTO(
                base.id(), base.date(), base.supplierId(), base.supplierName(),
                base.documentType(), base.branchCode(), base.documentNumber(), base.items(),
                base.otherTaxes(), base.iibbPerception(), base.netTotal(), base.ivaTotal(), base.ivaExemptTotal(),
                base.total(), base.discountPercentage(),
                base.projectAreaId(), base.projectAreaName(), base.projectAreaColor(),
                base.projectAreaTaskId(), base.projectAreaTaskName(),
                base.comment(), base.paid(), base.deleted(),
                status, creditApplied, pendingAmount, remainingBalance, creditApplications, appliedCredits,
                Boolean.TRUE.equals(entity.getManuallyApplied()),
                onAccountApplications, availableOnAccountBalance);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "manuallyApplied", ignore = true)
    @Mapping(target = "items", ignore = true)  // Ignora items - se procesan manualmente en el service
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "creditNoteApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    void partialUpdate(TransactionalDocumentDTO updateDTO, @MappingTarget TransactionalDocument entity);

    // Ya no necesitamos @AfterMapping porque los items se procesan manualmente
}