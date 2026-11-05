package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.batch.BatchResponseDTO;
import PSG.backEnd.model.dto.employee.*;
import PSG.backEnd.model.enums.employee.MovementType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IAttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/attendance-records")
@RequiredArgsConstructor
@Tag(name = "Attendance Records", description = "API for managing employee attendance records (clock-in/clock-out).")
public class AttendanceRecordController {

    private final IAttendanceRecordService iAttendanceRecordService;

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_WRITE + "')")
    @PostMapping
    @Operation(summary = "Create a new attendance record",
            description = "Registers a new clock-in or clock-out event for an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attendance record successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Employee or building not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate attendance record")
    })
    public ResponseEntity<AttendanceRecordResponseDTO> createAttendanceRecord(
            @Validated(OnCreate.class) @RequestBody AttendanceRecordDTO dto) {
        AttendanceRecordResponseDTO created = iAttendanceRecordService.createAttendanceRecord(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_WRITE + "')")
    @PostMapping("/batch")
    @Operation(summary = "Create multiple attendance records in one request")
    public ResponseEntity<BatchResponseDTO<AttendanceRecordResponseDTO>> createBatchAttendanceRecords(
            @Validated @RequestBody AttendanceRecordBatchDTO batchDTO) {
        BatchResponseDTO<AttendanceRecordResponseDTO> result = iAttendanceRecordService.createBatchAttendanceRecords(batchDTO);
        HttpStatus status = result.totalSuccessful() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(result, status);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_READ + "')")
    @GetMapping
    @Operation(summary = "Get all attendance records",
            description = "Retrieves a paginated list of attendance records with optional filtering.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance records list")
    public ResponseEntity<Page<AttendanceRecordResponseDTO>> getAttendanceRecords(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by employee first name (case-insensitive partial match)") @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by employee last name (case-insensitive partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Filter by employee DNI") @RequestParam(required = false) String dni,
            @Parameter(description = "Filter by movement type (ENTRADA, SALIDA)") @RequestParam(required = false) MovementType movementType,
            @Parameter(description = "Filter by building ID") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Filter by minimum date") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Filter by maximum date") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Filter by minimum time") @RequestParam(required = false) LocalTime timeFrom,
            @Parameter(description = "Filter by maximum time") @RequestParam(required = false) LocalTime timeTo,
            @Parameter(description = "Generic search across employee name and lastName (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        String mappedSortBy = mapSortField(sortBy);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        AttendanceRecordFilterDTO filterDTO = new AttendanceRecordFilterDTO(
                employeeId, firstName, lastName, dni, movementType, buildingId,
                projectAreaId, dateFrom, dateTo, timeFrom, timeTo, search
        );

        return ResponseEntity.ok(iAttendanceRecordService.getAllAttendanceRecords(filterDTO, pageable));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_READ + "')")
    @GetMapping("/count")
    @Operation(summary = "Count attendance records matching filters")
    public ResponseEntity<Long> countAttendanceRecords(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) LocalTime timeFrom,
            @RequestParam(required = false) LocalTime timeTo,
            @RequestParam(required = false) String search
    ) {
        AttendanceRecordFilterDTO filterDTO = new AttendanceRecordFilterDTO(
                employeeId, firstName, lastName, dni, movementType, buildingId,
                projectAreaId, dateFrom, dateTo, timeFrom, timeTo, search
        );
        return ResponseEntity.ok(iAttendanceRecordService.countWithFilters(filterDTO));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_READ + "')")
    @GetMapping("/export")
    @Operation(summary = "Export attendance records to Excel",
            description = "Generates an Excel file with attendance records matching the given filters.")
    @ApiResponse(responseCode = "200", description = "Excel file generated successfully")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) LocalTime timeFrom,
            @RequestParam(required = false) LocalTime timeTo,
            @RequestParam(required = false) String search
    ) {
        AttendanceRecordFilterDTO filterDTO = new AttendanceRecordFilterDTO(
                employeeId, firstName, lastName, dni, movementType, buildingId,
                projectAreaId, dateFrom, dateTo, timeFrom, timeTo, search
        );
        byte[] excelBytes = iAttendanceRecordService.exportToExcel(filterDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "asistencia.xlsx");
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "employeeName" -> "employee.name";
            case "employeeLastName" -> "employee.lastName";
            case "employeeDni" -> "employee.dni";
            case "buildingName" -> "building.name";
            case "employeeId" -> "employee.id";
            default -> sortBy;
        };
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_READ + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get attendance record by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance record found"),
            @ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    public ResponseEntity<AttendanceRecordResponseDTO> getAttendanceRecordById(
            @Parameter(description = "Attendance record unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iAttendanceRecordService.getAttendanceRecordById(id));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_WRITE + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update attendance record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance record successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    public ResponseEntity<AttendanceRecordResponseDTO> updateAttendanceRecord(
            @Parameter(description = "Attendance record unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody AttendanceRecordDTO dto) {
        return ResponseEntity.ok(iAttendanceRecordService.updateAttendanceRecord(id, dto));
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_DELETE + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attendance record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attendance record successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    public ResponseEntity<Void> deleteAttendanceRecord(
            @Parameter(description = "Attendance record unique identifier", required = true) @PathVariable Long id) {
        iAttendanceRecordService.deleteAttendanceRecord(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_WRITE + "')")
    @PostMapping("/import")
    @Operation(summary = "Import attendance records from Excel file",
            description = "Uploads an .xlsx file with attendance records. Use dryRun=true to validate without persisting. Use excludeRows to skip specific row numbers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or validation errors")
    })
    public ResponseEntity<AttendanceImportResultDTO> importFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) List<Integer> excludeRows) {
        Set<Integer> excluded = excludeRows != null ? new HashSet<>(excludeRows) : Set.of();
        AttendanceImportResultDTO result = iAttendanceRecordService.importFromExcel(file, dryRun, excluded);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ATTENDANCE_RECORD_READ + "')")
    @GetMapping("/import/template")
    @Operation(summary = "Download empty Excel template for attendance import")
    @ApiResponse(responseCode = "200", description = "Template file generated successfully")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = iAttendanceRecordService.generateTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "plantilla_asistencia.xlsx");
        return new ResponseEntity<>(template, headers, HttpStatus.OK);
    }
}
