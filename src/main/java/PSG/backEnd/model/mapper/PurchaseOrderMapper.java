package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.purchaseOrder.PurchaseOrderRequestDTO;
import PSG.backEnd.model.dto.purchaseOrder.PurchaseOrderResponseDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.entity.PurchaseOrder;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PurchaseOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    PurchaseOrder toEntity(PurchaseOrderRequestDTO dto);

    @Mapping(
        target = "createdByUserName",
        expression = "java(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getFirstName() + \" \" + entity.getCreatedByUser().getLastName() : null)"
    )
    @Mapping(target = "transactionalDocument", source = "transactionalDocument", qualifiedByName = "toSummaryDTO")
    PurchaseOrderResponseDTO toResponseDto(PurchaseOrder entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "transactionalDocument", ignore = true)
    void partialUpdate(@MappingTarget PurchaseOrder entity, PurchaseOrderRequestDTO dto);

    @Named("toSummaryDTO")
    default TransactionalDocumentSummaryDTO toSummaryDTO(TransactionalDocument doc) {
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
