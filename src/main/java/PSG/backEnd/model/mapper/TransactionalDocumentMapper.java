package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {SupplierMapper.class}
)
public interface TransactionalDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(source = "supplierId", target = "supplier.id")
    TransactionalDocument toEntity(TransactionalDocumentDTO dto);

    @Mapping(source = "supplier.id", target = "supplierId")
    TransactionalDocumentResponseDTO toResponseDto(TransactionalDocument entity);

    TransactionalDocumentDTO toDto(TransactionalDocument entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(source = "supplierId", target = "supplier.id")
    void partialUpdate(TransactionalDocumentDTO updateDTO, @MappingTarget TransactionalDocument entity);
}