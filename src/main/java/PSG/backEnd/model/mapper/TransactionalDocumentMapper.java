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
    @Mapping(source = "supplierId", target = "supplier.id")
    TransactionalDocument toEntity(TransactionalDocumentDTO dto);

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.legalName", target = "supplierName")
    @Mapping(expression = "java(entity.getDocumentType().getDisplayName())", target = "documentType")
    TransactionalDocumentResponseDTO toResponseDto(TransactionalDocument entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)  // Ignora items - se procesan manualmente en el service
    @Mapping(source = "supplierId", target = "supplier.id")
    void partialUpdate(TransactionalDocumentDTO updateDTO, @MappingTarget TransactionalDocument entity);

    // Ya no necesitamos @AfterMapping porque los items se procesan manualmente
}