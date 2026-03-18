package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.EmergencyContactDTO;
import PSG.backEnd.model.dto.employee.EmergencyContactResponseDTO;
import PSG.backEnd.model.entity.employee.EmergencyContact;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmergencyContactMapper {

    EmergencyContact toEntity(EmergencyContactDTO emergencyContactDTO);

    EmergencyContactResponseDTO toResponseDto(EmergencyContact emergencyContact);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    void partialUpdate(EmergencyContactDTO updateDTO, @MappingTarget EmergencyContact emergencyContact);
}

