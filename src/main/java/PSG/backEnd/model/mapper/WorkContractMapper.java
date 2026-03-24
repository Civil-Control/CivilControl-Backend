package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.contracts.WorkContractDTO;
import PSG.backEnd.model.dto.contracts.WorkContractResponseDTO;
import PSG.backEnd.model.entity.contracts.WorkContract;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WorkContractMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "projectAreaId", target = "projectArea.id")
    WorkContract toEntity(WorkContractDTO dto);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.businessName", target = "clientBusinessName")
    @Mapping(source = "client.cuit", target = "clientCuit")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    WorkContractResponseDTO toResponseDto(WorkContract entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "projectAreaId", target = "projectArea.id")
    void partialUpdate(WorkContractDTO dto, @MappingTarget WorkContract entity);
}
