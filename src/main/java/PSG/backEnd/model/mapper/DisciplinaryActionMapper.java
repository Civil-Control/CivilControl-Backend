package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import PSG.backEnd.model.entity.employee.DisciplinaryAction;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DisciplinaryActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    DisciplinaryAction toEntity(DisciplinaryActionDTO disciplinaryActionDTO);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    DisciplinaryActionResponseDTO toResponseDto(DisciplinaryAction disciplinaryAction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee.id", source = "employeeId")
    void partialUpdate(DisciplinaryActionDTO updateDTO, @MappingTarget DisciplinaryAction disciplinaryAction);
}

