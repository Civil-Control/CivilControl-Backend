package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.AddressDTO;
import PSG.backEnd.model.dto.AddressResponseDTO;
import PSG.backEnd.model.entity.Address;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toEntity(AddressDTO addressDTO);

    AddressResponseDTO toResponseDto(Address address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(AddressDTO updateDTO, @MappingTarget Address address);
}
