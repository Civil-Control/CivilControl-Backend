package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.client.ClientDTO;
import PSG.backEnd.model.dto.client.ClientResponseDTO;
import PSG.backEnd.model.dto.client.ClientSummaryDTO;
import PSG.backEnd.model.entity.Client;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, ContactInfoMapper.class})
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    Client toEntity(ClientDTO dto);

    ClientResponseDTO toResponseDto(Client client);

    ClientSummaryDTO toSummaryDto(Client client);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    void partialUpdate(ClientDTO dto, @MappingTarget Client client);
}
