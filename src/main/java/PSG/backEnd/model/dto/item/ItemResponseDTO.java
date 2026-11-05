package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.enums.ItemType;

import java.util.Set;

public record ItemResponseDTO(
    Long id,
    String name,
    String description,
    Set<ItemType> itemTypes
) {}
