package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    Item toEntity(ItemDTO dto);

    ItemResponseDTO toResponse(Item entity);
}
