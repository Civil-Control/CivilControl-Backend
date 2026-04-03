package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.employee.AttendanceRecordDTO;
import PSG.backEnd.model.dto.employee.AttendanceRecordResponseDTO;
import PSG.backEnd.model.entity.employee.AttendanceRecord;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AttendanceRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "building", ignore = true)
    AttendanceRecord toEntity(AttendanceRecordDTO dto);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.name", target = "employeeName")
    @Mapping(source = "employee.lastName", target = "employeeLastName")
    @Mapping(source = "employee.dni", target = "employeeDni")
    @Mapping(source = "building.id", target = "buildingId")
    @Mapping(source = "building.name", target = "buildingName")
    @Mapping(source = "employee.projectArea.id", target = "projectAreaId")
    @Mapping(source = "employee.projectArea.name", target = "projectAreaName")
    @Mapping(source = "employee.projectArea.color", target = "projectAreaColor")
    AttendanceRecordResponseDTO toResponseDto(AttendanceRecord entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "building", ignore = true)
    void partialUpdate(AttendanceRecordDTO dto, @MappingTarget AttendanceRecord entity);
}
