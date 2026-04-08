package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {SupplierMapper.class, ItemDetailMapper.class}
)
public interface TransactionalDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)  // Ignora items - se procesan manualmente en el service
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    TransactionalDocument toEntity(TransactionalDocumentDTO dto);

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.legalName", target = "supplierName")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    @Mapping(expression = "java(entity.getDocumentType().getDisplayName())", target = "documentType")
    TransactionalDocumentResponseDTO toResponseDto(TransactionalDocument entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)  // Ignora items - se procesan manualmente en el service
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    void partialUpdate(TransactionalDocumentDTO updateDTO, @MappingTarget TransactionalDocument entity);

    // Ya no necesitamos @AfterMapping porque los items se procesan manualmente
}