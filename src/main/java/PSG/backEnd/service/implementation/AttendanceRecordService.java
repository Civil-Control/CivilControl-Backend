package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.attendanceRecord.AttendanceRecordNotFoundException;
import PSG.backEnd.exception.attendanceRecord.AttendanceRecordNotValidException;
import PSG.backEnd.exception.attendanceRecord.DuplicateAttendanceRecordException;
import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.employee.*;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.employee.AttendanceRecord;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.mapper.AttendanceRecordMapper;
import PSG.backEnd.repository.AttendanceRecordRepository;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.service.port.IAttendanceRecordService;
import PSG.backEnd.service.importer.AttendanceExcelImporter;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceRecordService implements IAttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final BuildingRepository buildingRepository;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceExcelImporter attendanceExcelImporter;
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
    @Transactional
    public AttendanceImportResultDTO importFromExcel(MultipartFile file, boolean dryRun) {
        return attendanceExcelImporter.processFile(file, dryRun);
    }

    @Override
    public byte[] generateTemplate() {
        return attendanceExcelImporter.generateTemplate();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(AttendanceRecordFilterDTO filterDTO) {
        List<AttendanceRecordResponseDTO> records = getAllAttendanceRecordsNoPage(filterDTO);
        log.info("Exporting {} attendance records to Excel", records.size());

        if (records.size() > 10_000) {
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("attendanceRecord.export.tooManyRecords", 10_000));
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Registros de Asistencia");

            // Styles
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header row
            String[] headers = {"Empleado", "DNI", "Fecha", "Hora", "Tipo de Movimiento", "Edificio", "Área/Sector", "Observación"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            int rowNum = 1;
            for (AttendanceRecordResponseDTO r : records) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(
                        (r.employeeLastName() != null ? r.employeeLastName() : "") + " " +
                        (r.employeeName() != null ? r.employeeName() : ""));

                Cell dniCell = row.createCell(1);
                dniCell.setCellValue(r.employeeDni() != null ? r.employeeDni() : "");
                dniCell.setCellStyle(centerStyle);

                Cell dateCell = row.createCell(2);
                dateCell.setCellValue(r.date() != null ? r.date().format(dateFmt) : "");
                dateCell.setCellStyle(centerStyle);

                Cell timeCell = row.createCell(3);
                timeCell.setCellValue(r.time() != null ? r.time().format(timeFmt) : "");
                timeCell.setCellStyle(centerStyle);

                Cell typeCell = row.createCell(4);
                typeCell.setCellValue(r.movementType() != null ? r.movementType().name() : "");
                typeCell.setCellStyle(centerStyle);

                row.createCell(5).setCellValue(r.buildingName() != null ? r.buildingName() : "");
                row.createCell(6).setCellValue(r.projectAreaName() != null ? r.projectAreaName() : "");
                row.createCell(7).setCellValue(r.observation() != null ? r.observation() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            log.info("Attendance Excel export generated successfully ({} rows)", records.size());
            return baos.toByteArray();

        } catch (ReportGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating attendance Excel export", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    private CellStyle createExportHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
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
