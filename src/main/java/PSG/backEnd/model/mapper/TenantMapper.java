package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.tenant.TenantDTO;
import PSG.backEnd.model.dto.tenant.TenantResponseDTO;
import PSG.backEnd.model.entity.Tenant;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface TenantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", source = "active", defaultValue = "true")
    Tenant toEntity(TenantDTO tenantDTO);

    TenantResponseDTO toResponseDto(Tenant tenant);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(TenantDTO updateDTO, @MappingTarget Tenant tenant);
}
