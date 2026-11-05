package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.sales.SalesCreditNoteApplicationResponseDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailResponseDTO;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SalesDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "manuallyApplied", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "creditNoteApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    SalesDocument toEntity(SalesDocumentDTO dto);

    @Mapping(source = "client", target = "client")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creditApplied", ignore = true)
    @Mapping(target = "pendingAmount", ignore = true)
    @Mapping(target = "remainingBalance", ignore = true)
    @Mapping(target = "creditApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    SalesDocumentResponseDTO toResponseDto(SalesDocument salesDocument);

    /**
     * Builds a fully enriched response DTO including derived business state. The derived fields
     * are computed in the service layer and passed in here. Mirrors
     * TransactionalDocumentMapper.toEnrichedResponseDto.
     */
    default SalesDocumentResponseDTO toEnrichedResponseDto(
            SalesDocument entity,
            String status,
            BigDecimal creditApplied,
            BigDecimal pendingAmount,
            BigDecimal remainingBalance,
            List<SalesCreditNoteApplicationResponseDTO> creditApplications,
            List<SalesCreditNoteApplicationResponseDTO> appliedCredits) {
        SalesDocumentResponseDTO base = toResponseDto(entity);
        return new SalesDocumentResponseDTO(
                base.id(), base.documentType(), base.branchCode(), base.documentNumber(), base.date(),
                base.client(), base.purchaseOrderReference(), base.netTotal(), base.ivaTotal(),
                base.ivaExemptTotal(), base.otherTaxes(), base.total(), base.discountPercentage(),
                base.projectAreaId(), base.projectAreaName(), base.projectAreaTaskId(), base.projectAreaTaskName(),
                base.items(), base.paid(), base.comment(), base.deleted(),
                status, creditApplied, pendingAmount, remainingBalance, creditApplications, appliedCredits,
                Boolean.TRUE.equals(entity.getManuallyApplied()));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "manuallyApplied", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "creditNoteApplications", ignore = true)
    @Mapping(target = "appliedCredits", ignore = true)
    void partialUpdate(SalesDocumentDTO dto, @MappingTarget SalesDocument salesDocument);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salesDocument", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    SalesItemDetail toItemDetailEntity(SalesItemDetailDTO dto);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    SalesItemDetailResponseDTO toItemDetailResponseDto(SalesItemDetail salesItemDetail);
}
