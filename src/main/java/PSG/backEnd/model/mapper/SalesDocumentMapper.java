package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailResponseDTO;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SalesDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    SalesDocument toEntity(SalesDocumentDTO dto);

    @Mapping(source = "client", target = "client")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    SalesDocumentResponseDTO toResponseDto(SalesDocument salesDocument);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
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
