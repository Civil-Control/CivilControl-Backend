package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.crewAssignment.CrewAssignmentDTO;
import PSG.backEnd.model.dto.crewAssignment.CrewAssignmentResponseDTO;
import PSG.backEnd.model.entity.vehicle.CrewAssignment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CrewAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "driver", source = "isDriver", defaultValue = "false")
    CrewAssignment toEntity(CrewAssignmentDTO dto);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "employee.dni", target = "employeeDni")
    @Mapping(source = "employee.employeeRoles", target = "employeeRoles")
    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "vehicle.licensePlate", target = "vehicleLicensePlate")
    @Mapping(source = "vehicle.nickName", target = "vehicleNickName")
    @Mapping(source = "vehicle.brand", target = "vehicleBrand")
    @Mapping(source = "vehicle.model", target = "vehicleModel")
    @Mapping(source = "projectArea.id", target = "projectAreaId")
    @Mapping(source = "projectArea.name", target = "projectAreaName")
    @Mapping(source = "projectArea.color", target = "projectAreaColor")
    @Mapping(source = "projectAreaTask.id", target = "projectAreaTaskId")
    @Mapping(source = "projectAreaTask.name", target = "projectAreaTaskName")
    @Mapping(source = "driver", target = "isDriver")
    CrewAssignmentResponseDTO toResponseDto(CrewAssignment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "projectArea", ignore = true)
    @Mapping(target = "projectAreaTask", ignore = true)
    @Mapping(target = "driver", source = "isDriver")
    void partialUpdate(CrewAssignmentDTO dto, @MappingTarget CrewAssignment entity);
}
