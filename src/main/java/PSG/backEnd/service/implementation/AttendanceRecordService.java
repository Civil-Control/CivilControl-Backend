package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.attendanceRecord.AttendanceRecordNotFoundException;
import PSG.backEnd.exception.attendanceRecord.AttendanceRecordNotValidException;
import PSG.backEnd.exception.attendanceRecord.DuplicateAttendanceRecordException;
import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.model.dto.employee.*;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.employee.AttendanceRecord;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.mapper.AttendanceRecordMapper;
import PSG.backEnd.repository.AttendanceRecordRepository;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.service.port.IAttendanceRecordService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceRecordService implements IAttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final BuildingRepository buildingRepository;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public AttendanceRecordResponseDTO createAttendanceRecord(AttendanceRecordDTO dto) {
        validateEmployeeExists(dto.employeeId());
        validateDateNotFuture(dto.date());
        validateDuplicate(dto.employeeId(), dto.date(), dto.time(), dto.movementType());

        Employee employee = employeeRepository.findByIdAndDeletedFalse(dto.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(dto.employeeId()));

        AttendanceRecord record = attendanceRecordMapper.toEntity(dto);
        record.setEmployee(employee);
        record.setBuilding(resolveBuilding(dto.buildingId()));

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return attendanceRecordMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public List<AttendanceRecordResponseDTO> createBatchAttendanceRecords(AttendanceRecordBatchDTO batchDTO) {
        return batchDTO.records().stream()
                .map(this::createAttendanceRecord)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponseDTO> getAllAttendanceRecords(AttendanceRecordFilterDTO filterDTO, Pageable pageable) {
        return attendanceRecordRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.dni(),
                filterDTO.movementType(),
                filterDTO.buildingId(),
                filterDTO.projectAreaId(),
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.timeFrom(),
                filterDTO.timeTo(),
                filterDTO.search(),
                pageable
        ).map(attendanceRecordMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithFilters(AttendanceRecordFilterDTO filterDTO) {
        return attendanceRecordRepository.countWithFilters(
                filterDTO.employeeId(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.dni(),
                filterDTO.movementType(),
                filterDTO.buildingId(),
                filterDTO.projectAreaId(),
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.timeFrom(),
                filterDTO.timeTo(),
                filterDTO.search()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getAllAttendanceRecordsNoPage(AttendanceRecordFilterDTO filterDTO) {
        return attendanceRecordRepository.findAllWithFiltersNoPage(
                filterDTO.employeeId(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.dni(),
                filterDTO.movementType(),
                filterDTO.buildingId(),
                filterDTO.projectAreaId(),
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.timeFrom(),
                filterDTO.timeTo(),
                filterDTO.search()
        ).stream().map(attendanceRecordMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponseDTO getAttendanceRecordById(Long id) {
        return attendanceRecordRepository.findByIdAndEmployeeDeletedFalse(id)
                .map(attendanceRecordMapper::toResponseDto)
                .orElseThrow(() -> new AttendanceRecordNotFoundException(id));
    }

    @Override
    @Transactional
    public AttendanceRecordResponseDTO updateAttendanceRecord(Long id, AttendanceRecordDTO dto) {
        AttendanceRecord existing = attendanceRecordRepository.findByIdAndEmployeeDeletedFalse(id)
                .orElseThrow(() -> new AttendanceRecordNotFoundException(id));

        if (dto.employeeId() != null) {
            validateEmployeeExists(dto.employeeId());
        }

        if (dto.date() != null) {
            validateDateNotFuture(dto.date());
        }

        if (dto.employeeId() != null &&
            !existing.getEmployee().getId().equals(dto.employeeId())) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(dto.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(dto.employeeId()));
            existing.setEmployee(employee);
        }

        attendanceRecordMapper.partialUpdate(dto, existing);

        if (dto.employeeId() != null) {
            existing.setBuilding(resolveBuilding(dto.buildingId()));
        }

        AttendanceRecord updated = attendanceRecordRepository.save(existing);
        return attendanceRecordMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteAttendanceRecord(Long id) {
        AttendanceRecord record = attendanceRecordRepository.findByIdAndEmployeeDeletedFalse(id)
                .orElseThrow(() -> new AttendanceRecordNotFoundException(id));
        attendanceRecordRepository.delete(record);
    }

    @Override
    public byte[] generateTemplate() {
        // Will be implemented in Phase 3 (Import Excel)
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public byte[] exportToExcel(AttendanceRecordFilterDTO filterDTO) {
        // Will be implemented in Phase 5 (Export Excel)
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void validateEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateDateNotFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new AttendanceRecordNotValidException(
                    messageSourceHelper.getMessage("attendanceRecord.date.future"));
        }
    }

    private void validateDuplicate(Long employeeId, LocalDate date, java.time.LocalTime time,
                                   PSG.backEnd.model.enums.employee.MovementType movementType) {
        if (attendanceRecordRepository.existsByEmployeeIdAndDateAndTimeAndMovementType(
                employeeId, date, time, movementType)) {
            throw new DuplicateAttendanceRecordException(
                    messageSourceHelper.getMessage("attendanceRecord.duplicate"));
        }
    }

    private Building resolveBuilding(Long buildingId) {
        if (buildingId == null) return null;
        return buildingRepository.findByIdAndDeletedFalse(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException(buildingId));
    }
}
