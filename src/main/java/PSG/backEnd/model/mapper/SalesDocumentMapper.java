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
    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "projectAreaId", target = "projectArea.id")
    SalesDocument toEntity(SalesDocumentDTO dto);

    @Mapping(source = "client", target = "client")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    SalesDocumentResponseDTO toResponseDto(SalesDocument salesDocument);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "projectAreaId", target = "projectArea.id")
    void partialUpdate(SalesDocumentDTO dto, @MappingTarget SalesDocument salesDocument);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "salesDocument", ignore = true)
    @Mapping(source = "itemId", target = "item.id")
    @Mapping(target = "totalAmount", ignore = true)
    SalesItemDetail toItemDetailEntity(SalesItemDetailDTO dto);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    SalesItemDetailResponseDTO toItemDetailResponseDto(SalesItemDetail salesItemDetail);
}
