package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.crewAssignment.CrewAssignmentNotFoundException;
import PSG.backEnd.exception.crewAssignment.DuplicateCrewAssignmentException;
import PSG.backEnd.exception.crewAssignment.DuplicateDriverException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotFoundException;
import PSG.backEnd.model.dto.crewAssignment.*;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.vehicle.CrewAssignment;
import PSG.backEnd.model.entity.vehicle.DailyCrewReport;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.vehicle.CrewReportType;
import PSG.backEnd.model.mapper.CrewAssignmentMapper;
import PSG.backEnd.repository.CrewAssignmentRepository;
import PSG.backEnd.repository.DailyCrewReportRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.ICrewAssignmentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrewAssignmentService implements ICrewAssignmentService {

    private final CrewAssignmentRepository crewAssignmentRepository;
    private final DailyCrewReportRepository dailyCrewReportRepository;
    private final EmployeeRepository employeeRepository;
    private final VehicleRepository vehicleRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final CrewAssignmentMapper crewAssignmentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public CrewAssignmentResponseDTO createCrewAssignment(CrewAssignmentDTO dto) {
        Employee employee = resolveEmployee(dto.employeeId());
        Vehicle vehicle = resolveVehicle(dto.vehicleId());
        ProjectArea projectArea = resolveProjectArea(dto.projectAreaId());
        ProjectAreaTask projectAreaTask = resolveProjectAreaTask(dto.projectAreaTaskId());

        validateDriverUniqueness(dto.vehicleId(), dto.date(), dto.isDriver());

        CrewAssignment entity = crewAssignmentMapper.toEntity(dto);
        entity.setEmployee(employee);
        entity.setVehicle(vehicle);
        entity.setProjectArea(projectArea);
        entity.setProjectAreaTask(projectAreaTask);

        CrewAssignment saved = crewAssignmentRepository.save(entity);
        syncVehicleKm(vehicle, dto.km());
        return crewAssignmentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public CrewAssignmentBatchResponseDTO createBatchCrewAssignments(CrewAssignmentBatchDTO batchDTO) {
        List<CrewAssignmentResponseDTO> created = new ArrayList<>();
        List<CrewAssignmentWarningDTO> warnings = new ArrayList<>();

        // Track vehicles already warned about duplicate to avoid repeated warnings
        var warnedEmployees = new java.util.HashSet<Long>();
        var warnedVehicles = new java.util.HashSet<Long>();

        for (CrewAssignmentDTO dto : batchDTO.assignments()) {
            Employee employee = resolveEmployee(dto.employeeId());
            Vehicle vehicle = resolveVehicle(dto.vehicleId());
            ProjectArea projectArea = resolveProjectArea(dto.projectAreaId());

            // Warning W3: employee already has assignment for this date
            if (!warnedEmployees.contains(dto.employeeId()) &&
                    crewAssignmentRepository.existsByEmployeeIdAndDateAndDeletedFalse(dto.employeeId(), dto.date())) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W3",
                        messageSourceHelper.getMessage("crewAssignment.warning.duplicateEmployee",
                                employee.getName() + " " + employee.getLastName(), dto.date()),
                        employee.getId(),
                        vehicle.getId()
                ));
                warnedEmployees.add(dto.employeeId());
            }

            // Warning W4: vehicle already has assignments for this date
            if (!warnedVehicles.contains(dto.vehicleId()) &&
                    crewAssignmentRepository.existsByVehicleIdAndDateAndDeletedFalse(dto.vehicleId(), dto.date())) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W4",
                        messageSourceHelper.getMessage("crewAssignment.warning.duplicateVehicle",
                                vehicle.getLicensePlate(), dto.date()),
                        employee.getId(),
                        vehicle.getId()
                ));
                warnedVehicles.add(dto.vehicleId());
            }

            // Warning W1: employee assigned as driver but doesn't have CHOFER role
            if (Boolean.TRUE.equals(dto.isDriver()) &&
                    !employee.getEmployeeRoles().contains(EmployeeRole.CHOFER)) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W1",
                        messageSourceHelper.getMessage("crewAssignment.warning.notDriver",
                                employee.getName() + " " + employee.getLastName()),
                        employee.getId(),
                        vehicle.getId()
                ));
            }

            // Warning W2: vehicle already has 2+ members for this date
            long currentCount = crewAssignmentRepository.countByVehicleIdAndDateAndDeletedFalse(
                    dto.vehicleId(), dto.date());
            if (currentCount >= 2) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W2",
                        messageSourceHelper.getMessage("crewAssignment.warning.capacityExceeded",
                                vehicle.getLicensePlate(), currentCount + 1, dto.date()),
                        employee.getId(),
                        vehicle.getId()
                ));
            }

            // Validate driver uniqueness — in batch, if isDriver and another driver exists, error
            validateDriverUniqueness(dto.vehicleId(), dto.date(), dto.isDriver());

            CrewAssignment entity = crewAssignmentMapper.toEntity(dto);
            entity.setEmployee(employee);
            entity.setVehicle(vehicle);
            entity.setProjectArea(projectArea);

            CrewAssignment saved = crewAssignmentRepository.save(entity);
            syncVehicleKm(vehicle, dto.km());
            created.add(crewAssignmentMapper.toResponseDto(saved));
        }

        return new CrewAssignmentBatchResponseDTO(created, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CrewAssignmentResponseDTO> getAllCrewAssignments(CrewAssignmentFilterDTO filterDTO, Pageable pageable) {
        return crewAssignmentRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.employeeName(),
                filterDTO.employeeLastName(),
                filterDTO.employeeDni(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.dateExact(),
                filterDTO.isDriver(),
                filterDTO.search(),
                pageable
        ).map(crewAssignmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CrewAssignmentResponseDTO getCrewAssignmentById(Long id) {
        return crewAssignmentRepository.findByIdAndDeletedFalse(id)
                .map(crewAssignmentMapper::toResponseDto)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyCrewSummaryDTO getDailyCrewSummary(LocalDate date) {
        List<CrewAssignment> assignments = crewAssignmentRepository.findByDateAndDeletedFalseOrdered(date);

        // Group by vehicle, preserving order
        Map<Long, List<CrewAssignment>> byVehicle = assignments.stream()
                .collect(Collectors.groupingBy(
                        ca -> ca.getVehicle().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<VehicleCrewDTO> vehicles = byVehicle.entrySet().stream()
                .map(entry -> {
                    List<CrewAssignment> vehicleAssignments = entry.getValue();
                    CrewAssignment first = vehicleAssignments.get(0);
                    Vehicle v = first.getVehicle();
                    ProjectArea pa = first.getProjectArea();

                    List<CrewMemberDTO> members = vehicleAssignments.stream()
                            .map(ca -> new CrewMemberDTO(
                                    ca.getId(),
                                    ca.getEmployee().getId(),
                                    ca.getEmployee().getName(),
                                    ca.getEmployee().getLastName(),
                                    ca.getEmployee().getDni(),
                                    ca.getEmployee().getEmployeeRoles(),
                                    ca.isDriver(),
                                    ca.getObservation(),
                                    ca.getDepartureTime(),
                                    ca.getReturnTime()
                            ))
                            .toList();

                    return new VehicleCrewDTO(
                            v.getId(),
                            v.getLicensePlate(),
                            v.getNickName(),
                            v.getBrand(),
                            v.getModel(),
                            pa.getId(),
                            pa.getName(),
                            pa.getColor(),
                            first.getKm(),
                            members
                    );
                })
                .toList();

        return new DailyCrewSummaryDTO(date, vehicles);
    }

    @Override
    @Transactional(readOnly = true)
    public CrewCalendarDTO getCalendarSummary(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Object[]> rawData = crewAssignmentRepository.countByDateRange(startDate, endDate);

        List<CrewCalendarDayDTO> days = rawData.stream()
                .map(row -> new CrewCalendarDayDTO(
                        (LocalDate) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()
                ))
                .toList();

        return new CrewCalendarDTO(year, month, days);
    }

    @Override
    @Transactional
    public CrewAssignmentResponseDTO updateCrewAssignment(Long id, CrewAssignmentDTO dto) {
        CrewAssignment existing = crewAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(id));

        crewAssignmentMapper.partialUpdate(dto, existing);

        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = resolveProjectArea(dto.projectAreaId());
            existing.setProjectArea(projectArea);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = resolveProjectAreaTask(dto.projectAreaTaskId());
            existing.setProjectAreaTask(task);
        } else {
            existing.setProjectAreaTask(null);
        }

        CrewAssignment updated = crewAssignmentRepository.save(existing);
        return crewAssignmentMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public CrewAssignmentResponseDTO toggleDriver(Long id) {
        CrewAssignment existing = crewAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(id));

        if (!existing.isDriver()) {
            // Turning ON driver — check no other driver exists for this vehicle/date
            crewAssignmentRepository.findDriverByVehicleAndDate(
                    existing.getVehicle().getId(), existing.getDate()
            ).ifPresent(otherDriver -> {
                if (!otherDriver.getId().equals(existing.getId())) {
                    throw new DuplicateDriverException(
                            messageSourceHelper.getMessage("crewAssignment.duplicateDriver",
                                    existing.getVehicle().getLicensePlate(),
                                    existing.getDate(),
                                    otherDriver.getEmployee().getName() + " " + otherDriver.getEmployee().getLastName())
                    );
                }
            });
        }

        existing.setDriver(!existing.isDriver());
        CrewAssignment updated = crewAssignmentRepository.save(existing);
        return crewAssignmentMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCrewAssignment(Long id) {
        CrewAssignment existing = crewAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(id));
        existing.setDeleted(true);
        crewAssignmentRepository.save(existing);
    }

    @Override
    @Transactional
    public int deleteDailyAssignments(LocalDate date) {
        return crewAssignmentRepository.softDeleteByDate(date);
    }

    // ── Report-level operations ────────────────────────────────────

    @Override
    @Transactional
    public DailyCrewReportResponseDTO saveReport(DailyCrewReportSaveDTO dto) {
        ProjectArea projectArea = dto.projectAreaId() != null ? resolveProjectArea(dto.projectAreaId()) : null;

        DailyCrewReport report = DailyCrewReport.builder()
                .date(dto.date())
                .projectArea(projectArea)
                .type(dto.type())
                .departureTime(dto.departureTime())
                .returnTime(dto.returnTime())
                .deleted(false)
                .build();

        report = dailyCrewReportRepository.save(report);
        return saveReportAssignments(report, dto, null);
    }

    @Override
    @Transactional
    public DailyCrewReportResponseDTO updateReport(Long reportId, DailyCrewReportSaveDTO dto) {
        DailyCrewReport report = dailyCrewReportRepository.findByIdAndDeletedFalse(reportId)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(reportId));

        if (dto.projectAreaId() != null) {
            report.setProjectArea(resolveProjectArea(dto.projectAreaId()));
        } else {
            report.setProjectArea(null);
        }
        report.setType(dto.type());
        report.setDepartureTime(dto.departureTime());
        report.setReturnTime(dto.returnTime());
        report = dailyCrewReportRepository.save(report);

        // Delete old assignments, then re-create
        crewAssignmentRepository.softDeleteByReportId(reportId);
        return saveReportAssignments(report, dto, reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyCrewReportResponseDTO getReportById(Long reportId) {
        DailyCrewReport report = dailyCrewReportRepository.findByIdAndDeletedFalse(reportId)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(reportId));

        List<CrewAssignment> assignments = crewAssignmentRepository.findByReportIdAndDeletedFalseOrdered(reportId);
        List<VehicleCrewDTO> vehicles = buildVehicleCrewList(assignments);

        ProjectArea pa = report.getProjectArea();
        return new DailyCrewReportResponseDTO(
                report.getId(),
                report.getDate(),
                pa != null ? pa.getId() : null,
                pa != null ? pa.getName() : null,
                pa != null ? pa.getColor() : null,
                report.getType(),
                report.getDepartureTime(),
                report.getReturnTime(),
                vehicles,
                List.of()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DailyReportsSummaryDTO getDailyReportsSummary(LocalDate date) {
        List<DailyCrewReport> reports = dailyCrewReportRepository.findByDateAndDeletedFalse(date);

        List<DailyReportSummaryItemDTO> items = reports.stream().map(report -> {
            List<CrewAssignment> assignments = crewAssignmentRepository
                    .findByReportIdAndDeletedFalseOrdered(report.getId());

            long vehicleCount = assignments.stream()
                    .map(ca -> ca.getVehicle().getId())
                    .distinct().count();
            int employeeCount = assignments.size();

            ProjectArea pa = report.getProjectArea();
            return new DailyReportSummaryItemDTO(
                    report.getId(),
                    pa != null ? pa.getId() : null,
                    pa != null ? pa.getName() : null,
                    pa != null ? pa.getColor() : null,
                    report.getType(),
                    report.getDepartureTime(),
                    report.getReturnTime(),
                    (int) vehicleCount,
                    employeeCount
            );
        }).toList();

        return new DailyReportsSummaryDTO(date, items);
    }

    @Override
    @Transactional
    public void deleteReport(Long reportId) {
        DailyCrewReport report = dailyCrewReportRepository.findByIdAndDeletedFalse(reportId)
                .orElseThrow(() -> new CrewAssignmentNotFoundException(reportId));
        crewAssignmentRepository.softDeleteByReportId(reportId);
        report.setDeleted(true);
        dailyCrewReportRepository.save(report);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private Employee resolveEmployee(Long employeeId) {
        return employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
    }

    private Vehicle resolveVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        if (!vehicle.isActive()) {
            throw new VehicleNotFoundException(vehicleId);
        }
        return vehicle;
    }

    private ProjectArea resolveProjectArea(Long projectAreaId) {
        ProjectArea pa = projectAreaRepository.findByIdAndDeletedFalse(projectAreaId)
                .orElseThrow(() -> new ProjectAreaNotFoundException(projectAreaId));
        if (Boolean.FALSE.equals(pa.getActive())) {
            throw new ProjectAreaNotFoundException(projectAreaId);
        }
        return pa;
    }

    private ProjectAreaTask resolveProjectAreaTask(Long projectAreaTaskId) {
        if (projectAreaTaskId == null) return null;
        return projectAreaTaskRepository.findByIdAndDeletedFalse(projectAreaTaskId)
                .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + projectAreaTaskId));
    }

    private void validateNoDuplicate(Long employeeId, LocalDate date) {
        if (crewAssignmentRepository.existsByEmployeeIdAndDateAndDeletedFalse(employeeId, date)) {
            Employee emp = employeeRepository.findByIdAndDeletedFalse(employeeId).orElse(null);
            String empName = emp != null ? emp.getName() + " " + emp.getLastName() : String.valueOf(employeeId);
            throw new DuplicateCrewAssignmentException(
                    messageSourceHelper.getMessage("crewAssignment.duplicate", empName, date));
        }
    }

    private void syncVehicleKm(Vehicle vehicle, Integer km) {
        if (km != null) {
            vehicle.setKm(km);
            vehicleRepository.save(vehicle);
        }
    }

    private void validateDriverUniqueness(Long vehicleId, LocalDate date, Boolean isDriver) {
        if (!Boolean.TRUE.equals(isDriver)) return;

        crewAssignmentRepository.findDriverByVehicleAndDate(vehicleId, date)
                .ifPresent(existingDriver -> {
                    Vehicle v = existingDriver.getVehicle();
                    Employee driverEmp = existingDriver.getEmployee();
                    throw new DuplicateDriverException(
                            messageSourceHelper.getMessage("crewAssignment.duplicateDriver",
                                    v.getLicensePlate(), date,
                                    driverEmp.getName() + " " + driverEmp.getLastName()));
                });
    }

    private DailyCrewReportResponseDTO saveReportAssignments(
            DailyCrewReport report, DailyCrewReportSaveDTO dto, Long excludeReportIdForWarnings) {

        List<CrewAssignmentWarningDTO> warnings = new ArrayList<>();
        var warnedEmployees = new HashSet<Long>();
        var warnedVehicles = new HashSet<Long>();

        // Collect employee/vehicle IDs already in OTHER reports for this date (for cross-report duplicate warnings)
        Set<Long> employeesInOtherReports = new HashSet<>();
        Set<Long> vehiclesInOtherReports = new HashSet<>();
        if (excludeReportIdForWarnings != null) {
            employeesInOtherReports.addAll(crewAssignmentRepository
                    .findAssignedEmployeeIdsByDateExcludingReport(dto.date(), excludeReportIdForWarnings));
            vehiclesInOtherReports.addAll(crewAssignmentRepository
                    .findAssignedVehicleIdsByDateExcludingReport(dto.date(), excludeReportIdForWarnings));
        } else {
            // New report: check all existing assignments for this date
            List<Long> existingEmpIds = crewAssignmentRepository.findAssignedEmployeeIdsByDate(dto.date());
            employeesInOtherReports.addAll(existingEmpIds);
            // For vehicles, get from existing assignments
            List<CrewAssignment> existingAssignments = crewAssignmentRepository
                    .findByDateAndDeletedFalseOrdered(dto.date());
            existingAssignments.forEach(ca -> vehiclesInOtherReports.add(ca.getVehicle().getId()));
        }

        LocalTime reportDeparture = dto.departureTime();
        LocalTime reportReturn = dto.returnTime();

        for (CrewReportAssignmentDTO aDto : dto.assignments()) {
            Employee employee = resolveEmployee(aDto.employeeId());
            Vehicle vehicle = resolveVehicle(aDto.vehicleId());
            ProjectArea projectArea = resolveProjectArea(aDto.projectAreaId());
            ProjectAreaTask projectAreaTask = resolveProjectAreaTask(aDto.projectAreaTaskId());

            // Warning W3: employee already in another report for this date
            if (!warnedEmployees.contains(aDto.employeeId())
                    && employeesInOtherReports.contains(aDto.employeeId())) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W3",
                        messageSourceHelper.getMessage("crewAssignment.warning.duplicateEmployee",
                                employee.getName() + " " + employee.getLastName(), dto.date()),
                        employee.getId(), vehicle.getId()));
                warnedEmployees.add(aDto.employeeId());
            }

            // Warning W4: vehicle already in another report for this date
            if (!warnedVehicles.contains(aDto.vehicleId())
                    && vehiclesInOtherReports.contains(aDto.vehicleId())) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W4",
                        messageSourceHelper.getMessage("crewAssignment.warning.duplicateVehicle",
                                vehicle.getLicensePlate(), dto.date()),
                        employee.getId(), vehicle.getId()));
                warnedVehicles.add(aDto.vehicleId());
            }

            // Warning W1: driver without CHOFER role
            if (Boolean.TRUE.equals(aDto.isDriver())
                    && !employee.getEmployeeRoles().contains(EmployeeRole.CHOFER)) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W1",
                        messageSourceHelper.getMessage("crewAssignment.warning.notDriver",
                                employee.getName() + " " + employee.getLastName()),
                        employee.getId(), vehicle.getId()));
            }

            // Warning W2: vehicle capacity >= 3
            long currentCount = crewAssignmentRepository
                    .countByVehicleIdAndCrewReportIdAndDeletedFalse(aDto.vehicleId(), report.getId());
            if (currentCount >= 2) {
                warnings.add(new CrewAssignmentWarningDTO(
                        "W2",
                        messageSourceHelper.getMessage("crewAssignment.warning.capacityExceeded",
                                vehicle.getLicensePlate(), currentCount + 1, dto.date()),
                        employee.getId(), vehicle.getId()));
            }

            // Validate driver uniqueness within this report
            if (Boolean.TRUE.equals(aDto.isDriver())) {
                crewAssignmentRepository.findDriverByVehicleAndReportId(aDto.vehicleId(), report.getId())
                        .ifPresent(existingDriver -> {
                            throw new DuplicateDriverException(
                                    messageSourceHelper.getMessage("crewAssignment.duplicateDriver",
                                            vehicle.getLicensePlate(), dto.date(),
                                            existingDriver.getEmployee().getName() + " " + existingDriver.getEmployee().getLastName()));
                        });
            }

            CrewAssignment entity = CrewAssignment.builder()
                    .crewReport(report)
                    .employee(employee)
                    .vehicle(vehicle)
                    .projectArea(projectArea)
                    .projectAreaTask(projectAreaTask)
                    .date(dto.date())
                    .driver(Boolean.TRUE.equals(aDto.isDriver()))
                    .observation(aDto.observation())
                    .km(aDto.km())
                    .departureTime(aDto.departureTime() != null ? aDto.departureTime() : reportDeparture)
                    .returnTime(aDto.returnTime() != null ? aDto.returnTime() : reportReturn)
                    .deleted(false)
                    .build();

            crewAssignmentRepository.save(entity);
            syncVehicleKm(vehicle, aDto.km());
        }

        // Build response
        List<CrewAssignment> savedAssignments = crewAssignmentRepository
                .findByReportIdAndDeletedFalseOrdered(report.getId());
        List<VehicleCrewDTO> vehicles = buildVehicleCrewList(savedAssignments);

        ProjectArea pa = report.getProjectArea();
        return new DailyCrewReportResponseDTO(
                report.getId(),
                report.getDate(),
                pa != null ? pa.getId() : null,
                pa != null ? pa.getName() : null,
                pa != null ? pa.getColor() : null,
                report.getType(),
                report.getDepartureTime(),
                report.getReturnTime(),
                vehicles,
                warnings
        );
    }

    private List<VehicleCrewDTO> buildVehicleCrewList(List<CrewAssignment> assignments) {
        Map<Long, List<CrewAssignment>> byVehicle = assignments.stream()
                .collect(Collectors.groupingBy(
                        ca -> ca.getVehicle().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return byVehicle.entrySet().stream()
                .map(entry -> {
                    List<CrewAssignment> vehicleAssignments = entry.getValue();
                    CrewAssignment first = vehicleAssignments.get(0);
                    Vehicle v = first.getVehicle();
                    ProjectArea pa = first.getProjectArea();

                    List<CrewMemberDTO> members = vehicleAssignments.stream()
                            .map(ca -> new CrewMemberDTO(
                                    ca.getId(),
                                    ca.getEmployee().getId(),
                                    ca.getEmployee().getName(),
                                    ca.getEmployee().getLastName(),
                                    ca.getEmployee().getDni(),
                                    ca.getEmployee().getEmployeeRoles(),
                                    ca.isDriver(),
                                    ca.getObservation(),
                                    ca.getDepartureTime(),
                                    ca.getReturnTime()
                            ))
                            .toList();

                    return new VehicleCrewDTO(
                            v.getId(), v.getLicensePlate(), v.getNickName(),
                            v.getBrand(), v.getModel(),
                            pa != null ? pa.getId() : null,
                            pa != null ? pa.getName() : null,
                            pa != null ? pa.getColor() : null,
                            first.getKm(),
                            members
                    );
                })
                .toList();
    }
}
