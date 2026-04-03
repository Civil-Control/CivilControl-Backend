package PSG.backEnd.service.importer;

import PSG.backEnd.exception.attendanceRecord.AttendanceImportException;
import PSG.backEnd.model.dto.employee.AttendanceImportResultDTO;
import PSG.backEnd.model.dto.employee.AttendanceImportRowResultDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.entity.employee.AttendanceRecord;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.enums.employee.MovementType;
import PSG.backEnd.model.enums.employee.RowStatus;
import PSG.backEnd.repository.AttendanceRecordRepository;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceExcelImporter {

    private static final int MAX_ROWS = 1000;
    private static final String SHEET_NAME = "Registros de Asistencia";
    private static final String[] EXPECTED_HEADERS = {"DNI", "Fecha", "Hora", "Tipo de Movimiento", "Edificio", "Observación"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final BuildingRepository buildingRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AttendanceImportResultDTO processFile(MultipartFile file, boolean dryRun) {
        validateFile(file);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = resolveSheet(workbook);
            validateHeaders(sheet);

            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                throw new AttendanceImportException("El archivo no contiene datos. Solo se encontraron los encabezados.");
            }
            if (lastRow > MAX_ROWS) {
                throw new AttendanceImportException(
                        String.format("El archivo supera el límite de %d filas (%d filas encontradas). Divida el archivo en partes más pequeñas.", MAX_ROWS, lastRow));
            }

            // Preload reference data
            Map<String, Employee> employeesByDni = preloadEmployees();
            Map<String, Building> buildingsByName = preloadBuildings();
            LocalDate today = LocalDate.now();

            // Parse all rows
            List<ParsedRow> parsedRows = new ArrayList<>();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) continue;
                parsedRows.add(parseRow(row, i + 1)); // +1 for 1-indexed display
            }

            if (parsedRows.isEmpty()) {
                throw new AttendanceImportException("El archivo no contiene datos válidos. Todas las filas están vacías.");
            }

            // Preload existing records for duplicate detection
            Set<String> existingKeys = preloadExistingRecordKeys(parsedRows, employeesByDni);

            // Validate each row
            Set<String> seenKeysInFile = new HashSet<>();
            List<AttendanceImportRowResultDTO> rowResults = new ArrayList<>();
            List<AttendanceRecord> recordsToSave = new ArrayList<>();

            // For coherence warnings, group by employee+date
            Map<String, List<ParsedRow>> rowsByEmployeeDate = new LinkedHashMap<>();

            for (ParsedRow parsed : parsedRows) {
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();
                Employee resolvedEmployee = null;
                Building resolvedBuilding = null;
                MovementType resolvedType = null;

                // E1/E2: DNI validation
                if (parsed.dni == null || parsed.dni.isBlank()) {
                    errors.add(String.format("Fila %d: El DNI es obligatorio", parsed.rowNumber));
                } else if (!parsed.dni.matches("\\d{7,8}")) {
                    errors.add(String.format("Fila %d: El DNI '%s' no tiene un formato válido (debe ser 7-8 dígitos)", parsed.rowNumber, parsed.dni));
                } else {
                    // E3: Employee lookup
                    resolvedEmployee = employeesByDni.get(parsed.dni);
                    if (resolvedEmployee == null) {
                        errors.add(String.format("Fila %d: No se encontró un empleado activo con DNI %s", parsed.rowNumber, parsed.dni));
                    }
                }

                // E4/E5: Date validation
                if (parsed.date == null) {
                    errors.add(String.format("Fila %d: La fecha es obligatoria y debe tener formato DD/MM/YYYY", parsed.rowNumber));
                } else if (parsed.date.isAfter(today)) {
                    errors.add(String.format("Fila %d: La fecha %s es posterior a hoy y no es válida",
                            parsed.rowNumber, parsed.date.format(DATE_FORMAT)));
                }

                // E6: Time validation
                if (parsed.time == null) {
                    errors.add(String.format("Fila %d: La hora es obligatoria y debe tener formato HH:mm (ej: 08:30)", parsed.rowNumber));
                }

                // E7: Movement type validation
                if (parsed.movementTypeRaw == null || parsed.movementTypeRaw.isBlank()) {
                    errors.add(String.format("Fila %d: El tipo de movimiento debe ser 'Entrada' o 'Salida'", parsed.rowNumber));
                } else {
                    resolvedType = resolveMovementType(parsed.movementTypeRaw);
                    if (resolvedType == null) {
                        errors.add(String.format("Fila %d: El tipo de movimiento '%s' no es válido. Debe ser 'Entrada' o 'Salida'",
                                parsed.rowNumber, parsed.movementTypeRaw));
                    }
                }

                // E8: Building validation (optional field)
                if (parsed.buildingName != null && !parsed.buildingName.isBlank()) {
                    resolvedBuilding = buildingsByName.get(parsed.buildingName.toLowerCase());
                    if (resolvedBuilding == null) {
                        errors.add(String.format("Fila %d: No se encontró el edificio '%s' en el sistema", parsed.rowNumber, parsed.buildingName));
                    }
                }

                // E9: Observation length
                if (parsed.observation != null && parsed.observation.length() > 500) {
                    errors.add(String.format("Fila %d: La observación supera el máximo de 500 caracteres", parsed.rowNumber));
                }

                // E10: Intra-file duplicate
                if (resolvedEmployee != null && parsed.date != null && parsed.time != null && resolvedType != null) {
                    String key = buildKey(resolvedEmployee.getId(), parsed.date, parsed.time, resolvedType);
                    if (!seenKeysInFile.add(key)) {
                        errors.add(String.format("Fila %d: Este registro es idéntico a una fila anterior (mismo empleado, fecha, hora y tipo)", parsed.rowNumber));
                    }

                    // E11: Database duplicate
                    if (errors.isEmpty() && existingKeys.contains(key)) {
                        errors.add(String.format("Fila %d: Ya existe un registro de %s para el empleado DNI %s el %s a las %s",
                                parsed.rowNumber, resolvedType.getDisplayName(), parsed.dni,
                                parsed.date.format(DATE_FORMAT), parsed.time.toString()));
                    }
                }

                // Warnings (only if no errors so data is valid enough to check)
                if (errors.isEmpty() && parsed.date != null && resolvedType != null && resolvedEmployee != null) {
                    // W4: Weekend check
                    DayOfWeek dow = parsed.date.getDayOfWeek();
                    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                        String dayName = dow == DayOfWeek.SATURDAY ? "sábado" : "domingo";
                        warnings.add(String.format("Fila %d: El %s es un día %s. Verifique si el registro es correcto",
                                parsed.rowNumber, parsed.date.format(DATE_FORMAT), dayName));
                    }

                    // Track for coherence checks
                    String empDateKey = resolvedEmployee.getId() + "_" + parsed.date;
                    rowsByEmployeeDate.computeIfAbsent(empDateKey, k -> new ArrayList<>()).add(parsed);
                }

                RowStatus status;
                List<String> messages;
                if (!errors.isEmpty()) {
                    status = RowStatus.ERROR;
                    messages = errors;
                } else if (!warnings.isEmpty()) {
                    status = RowStatus.WARNING;
                    messages = warnings;
                } else {
                    status = RowStatus.VALID;
                    messages = List.of();
                }

                String employeeName = resolvedEmployee != null
                        ? resolvedEmployee.getLastName() + " " + resolvedEmployee.getName()
                        : null;

                rowResults.add(new AttendanceImportRowResultDTO(
                        parsed.rowNumber, status, parsed.dni, employeeName,
                        parsed.date, parsed.time,
                        resolvedType != null ? resolvedType.getDisplayName() : parsed.movementTypeRaw,
                        resolvedBuilding != null ? resolvedBuilding.getName() : parsed.buildingName,
                        parsed.observation, messages
                ));

                if (errors.isEmpty() && resolvedEmployee != null) {
                    AttendanceRecord record = new AttendanceRecord();
                    record.setEmployee(resolvedEmployee);
                    record.setDate(parsed.date);
                    record.setTime(parsed.time);
                    record.setMovementType(resolvedType);
                    record.setBuilding(resolvedBuilding);
                    record.setObservation(parsed.observation);
                    recordsToSave.add(record);
                }
            }

            // Coherence warnings (W1, W2, W3) — apply after all rows parsed
            applyCoherenceWarnings(rowsByEmployeeDate, rowResults, existingKeys, employeesByDni);

            int errorCount = (int) rowResults.stream().filter(r -> r.status() == RowStatus.ERROR).count();
            int warningCount = (int) rowResults.stream().filter(r -> r.status() == RowStatus.WARNING).count();
            int validCount = (int) rowResults.stream().filter(r -> r.status() == RowStatus.VALID).count();

            boolean imported = false;
            if (!dryRun && errorCount == 0 && !recordsToSave.isEmpty()) {
                attendanceRecordRepository.saveAll(recordsToSave);
                imported = true;
                log.info("Imported {} attendance records successfully", recordsToSave.size());
            }

            return new AttendanceImportResultDTO(
                    rowResults.size(), validCount, warningCount, errorCount, imported, rowResults
            );

        } catch (AttendanceImportException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing attendance import file", e);
            throw new AttendanceImportException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Main data sheet
            Sheet dataSheet = workbook.createSheet(SHEET_NAME);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Example style (gray)
            CellStyle exampleStyle = workbook.createCellStyle();
            Font exampleFont = workbook.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleStyle.setFont(exampleFont);

            // Headers row
            Row headerRow = dataSheet.createRow(0);
            for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPECTED_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example row 1 (Entrada)
            Row example1 = dataSheet.createRow(1);
            String[] ex1 = {"12345678", "15/03/2026", "08:30", "Entrada", "Planta Central", "Ingreso normal"};
            for (int i = 0; i < ex1.length; i++) {
                Cell cell = example1.createCell(i);
                cell.setCellValue(ex1[i]);
                cell.setCellStyle(exampleStyle);
            }

            // Example row 2 (Salida, no building)
            Row example2 = dataSheet.createRow(2);
            String[] ex2 = {"12345678", "15/03/2026", "17:00", "Salida", "", ""};
            for (int i = 0; i < ex2.length; i++) {
                Cell cell = example2.createCell(i);
                cell.setCellValue(ex2[i]);
                cell.setCellStyle(exampleStyle);
            }

            // Column widths
            dataSheet.setColumnWidth(0, 14 * 256); // DNI
            dataSheet.setColumnWidth(1, 16 * 256); // Fecha
            dataSheet.setColumnWidth(2, 10 * 256); // Hora
            dataSheet.setColumnWidth(3, 22 * 256); // Tipo
            dataSheet.setColumnWidth(4, 28 * 256); // Edificio
            dataSheet.setColumnWidth(5, 42 * 256); // Observación

            // Instructions sheet
            Sheet instrSheet = workbook.createSheet("Instrucciones");
            CellStyle instrHeaderStyle = workbook.createCellStyle();
            Font instrHeaderFont = workbook.createFont();
            instrHeaderFont.setBold(true);
            instrHeaderFont.setFontHeightInPoints((short) 12);
            instrHeaderStyle.setFont(instrHeaderFont);

            CellStyle instrStyle = workbook.createCellStyle();
            instrStyle.setWrapText(true);

            int r = 0;
            addInstructionRow(instrSheet, r++, instrHeaderStyle, "Instrucciones para llenar la plantilla de asistencia");
            r++;
            addInstructionRow(instrSheet, r++, instrStyle, "1. DNI (obligatorio): Número de documento del empleado, 7 u 8 dígitos.");
            addInstructionRow(instrSheet, r++, instrStyle, "2. Fecha (obligatorio): Formato DD/MM/YYYY. No puede ser una fecha futura.");
            addInstructionRow(instrSheet, r++, instrStyle, "3. Hora (obligatorio): Formato HH:mm (ej: 08:30, 17:00).");
            addInstructionRow(instrSheet, r++, instrStyle, "4. Tipo de Movimiento (obligatorio): Escribir 'Entrada' o 'Salida'.");
            addInstructionRow(instrSheet, r++, instrStyle, "5. Edificio (opcional): Nombre del edificio tal como aparece en el sistema.");
            addInstructionRow(instrSheet, r++, instrStyle, "6. Observación (opcional): Texto libre, máximo 500 caracteres.");
            r++;
            addInstructionRow(instrSheet, r++, instrHeaderStyle, "Notas importantes:");
            addInstructionRow(instrSheet, r++, instrStyle, "- Las dos primeras filas de datos son ejemplos. Elimínelas antes de importar.");
            addInstructionRow(instrSheet, r++, instrStyle, "- El máximo de filas por importación es 1000.");
            addInstructionRow(instrSheet, r++, instrStyle, "- No se permiten registros duplicados (mismo empleado, fecha, hora y tipo).");
            addInstructionRow(instrSheet, r++, instrStyle, "- El sistema validará todos los datos antes de importar.");

            instrSheet.setColumnWidth(0, 80 * 256);

            var baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating attendance template", e);
            throw new AttendanceImportException("Error al generar la plantilla: " + e.getMessage(), e);
        }
    }

    // ======================== Private helpers ========================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AttendanceImportException("El archivo está vacío o no fue enviado.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new AttendanceImportException("El archivo debe ser un archivo Excel (.xlsx).");
        }
    }

    private Sheet resolveSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        if (sheet == null) {
            throw new AttendanceImportException("No se encontró ninguna hoja en el archivo Excel.");
        }
        return sheet;
    }

    private void validateHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new AttendanceImportException("La primera fila del archivo debe contener los encabezados: " + String.join(", ", EXPECTED_HEADERS));
        }
        // Flexible: check at least the first 4 required headers exist
        for (int i = 0; i < 4; i++) {
            String cellValue = getCellStringValue(headerRow.getCell(i));
            if (cellValue == null || !cellValue.trim().equalsIgnoreCase(EXPECTED_HEADERS[i])) {
                throw new AttendanceImportException(
                        String.format("Encabezado incorrecto en columna %s. Se esperaba '%s' pero se encontró '%s'. Descargue la plantilla para ver el formato correcto.",
                                (char) ('A' + i), EXPECTED_HEADERS[i], cellValue != null ? cellValue : "(vacío)"));
            }
        }
    }

    private Map<String, Employee> preloadEmployees() {
        return employeeRepository.findByDeletedFalse().stream()
                .filter(e -> e.getDni() != null)
                .collect(Collectors.toMap(
                        Employee::getDni,
                        e -> e,
                        (e1, e2) -> e1 // in case of duplicate DNI, take first
                ));
    }

    private Map<String, Building> preloadBuildings() {
        return buildingRepository.findByDeletedFalse(Pageable.unpaged()).getContent().stream()
                .collect(Collectors.toMap(
                        b -> b.getName().toLowerCase(),
                        b -> b,
                        (b1, b2) -> b1
                ));
    }

    private Set<String> preloadExistingRecordKeys(List<ParsedRow> parsedRows, Map<String, Employee> employeesByDni) {
        // Find date range from parsed rows
        LocalDate minDate = null;
        LocalDate maxDate = null;
        List<Long> employeeIds = new ArrayList<>();

        for (ParsedRow row : parsedRows) {
            if (row.date != null) {
                if (minDate == null || row.date.isBefore(minDate)) minDate = row.date;
                if (maxDate == null || row.date.isAfter(maxDate)) maxDate = row.date;
            }
            if (row.dni != null) {
                Employee emp = employeesByDni.get(row.dni);
                if (emp != null) employeeIds.add(emp.getId());
            }
        }

        if (minDate == null || employeeIds.isEmpty()) return Set.of();

        return attendanceRecordRepository
                .findByEmployeeIdInAndDateBetweenOrderByDateAscTimeAsc(employeeIds.stream().distinct().toList(), minDate, maxDate)
                .stream()
                .map(ar -> buildKey(ar.getEmployee().getId(), ar.getDate(), ar.getTime(), ar.getMovementType()))
                .collect(Collectors.toSet());
    }

    private void applyCoherenceWarnings(
            Map<String, List<ParsedRow>> rowsByEmployeeDate,
            List<AttendanceImportRowResultDTO> rowResults,
            Set<String> existingKeys,
            Map<String, Employee> employeesByDni) {

        Map<Integer, AttendanceImportRowResultDTO> resultsByRow = new LinkedHashMap<>();
        for (AttendanceImportRowResultDTO r : rowResults) {
            resultsByRow.put(r.rowNumber(), r);
        }

        for (var entry : rowsByEmployeeDate.entrySet()) {
            List<ParsedRow> dayRows = entry.getValue();
            if (dayRows.isEmpty()) continue;

            String dni = dayRows.get(0).dni;
            Employee emp = employeesByDni.get(dni);
            if (emp == null) continue;

            // Get existing records for this employee+date from DB
            LocalDate date = dayRows.get(0).date;
            List<AttendanceRecord> existingDayRecords = attendanceRecordRepository
                    .findByEmployeeIdAndDateOrderByTimeAsc(emp.getId(), date);

            // Combine existing + new rows to check coherence
            List<SimpleMovement> allMovements = new ArrayList<>();
            for (AttendanceRecord ar : existingDayRecords) {
                allMovements.add(new SimpleMovement(ar.getTime(), ar.getMovementType(), -1));
            }
            for (ParsedRow pr : dayRows) {
                MovementType mt = resolveMovementType(pr.movementTypeRaw);
                if (mt != null && pr.time != null) {
                    allMovements.add(new SimpleMovement(pr.time, mt, pr.rowNumber));
                }
            }
            allMovements.sort(Comparator.comparing(m -> m.time));

            LocalTime lastEntrada = null;
            boolean hasOpenEntrada = false;

            for (SimpleMovement mv : allMovements) {
                if (mv.rowNumber < 0) {
                    // Existing record, update state
                    if (mv.type == MovementType.ENTRADA) {
                        lastEntrada = mv.time;
                        hasOpenEntrada = true;
                    } else {
                        hasOpenEntrada = false;
                    }
                    continue;
                }

                // New row from import
                AttendanceImportRowResultDTO current = resultsByRow.get(mv.rowNumber);
                if (current == null || current.status() == RowStatus.ERROR) continue;

                List<String> newWarnings = new ArrayList<>(current.messages());

                if (mv.type == MovementType.SALIDA) {
                    if (!hasOpenEntrada) {
                        // W1: SALIDA without prior ENTRADA
                        newWarnings.add(String.format("Fila %d: Se registra una SALIDA para DNI %s el %s pero no hay una ENTRADA previa ese día",
                                mv.rowNumber, dni, date.format(DATE_FORMAT)));
                    } else if (lastEntrada != null && mv.time.isBefore(lastEntrada)) {
                        // W3: SALIDA time before ENTRADA time
                        newWarnings.add(String.format("Fila %d: La hora de SALIDA (%s) es anterior a la última ENTRADA (%s) del mismo día",
                                mv.rowNumber, mv.time, lastEntrada));
                    }
                    hasOpenEntrada = false;
                } else if (mv.type == MovementType.ENTRADA) {
                    if (hasOpenEntrada && lastEntrada != null) {
                        // W2: ENTRADA without prior SALIDA
                        newWarnings.add(String.format("Fila %d: Se registra otra ENTRADA para DNI %s el %s pero la entrada anterior (%s) no tiene salida registrada",
                                mv.rowNumber, dni, date.format(DATE_FORMAT), lastEntrada));
                    }
                    lastEntrada = mv.time;
                    hasOpenEntrada = true;
                }

                if (newWarnings.size() > current.messages().size()) {
                    RowStatus newStatus = current.status() == RowStatus.VALID ? RowStatus.WARNING : current.status();
                    resultsByRow.put(mv.rowNumber, new AttendanceImportRowResultDTO(
                            current.rowNumber(), newStatus, current.dni(), current.employeeName(),
                            current.date(), current.time(), current.movementType(),
                            current.buildingName(), current.observation(), newWarnings
                    ));
                }
            }
        }

        // Replace results with updated versions
        rowResults.clear();
        rowResults.addAll(resultsByRow.values());
    }

    private String buildKey(Long employeeId, LocalDate date, LocalTime time, MovementType type) {
        return employeeId + "|" + date + "|" + time + "|" + type;
    }

    private MovementType resolveMovementType(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase();
        if (normalized.equals("entrada") || normalized.equals("entry") || normalized.equals("in")) {
            return MovementType.ENTRADA;
        }
        if (normalized.equals("salida") || normalized.equals("exit") || normalized.equals("out")) {
            return MovementType.SALIDA;
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 6; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }

    private ParsedRow parseRow(Row row, int rowNumber) {
        String dni = sanitize(getCellStringValue(row.getCell(0)));
        // Remove decimal from numeric DNI (e.g. 12345678.0 -> 12345678)
        if (dni != null && dni.contains(".")) {
            try {
                dni = String.valueOf((long) Double.parseDouble(dni));
            } catch (NumberFormatException ignored) {}
        }

        LocalDate date = parseCellDate(row.getCell(1));
        LocalTime time = parseCellTime(row.getCell(2));
        String movementType = sanitize(getCellStringValue(row.getCell(3)));
        String buildingName = sanitize(getCellStringValue(row.getCell(4)));
        String observation = sanitize(getCellStringValue(row.getCell(5)));

        return new ParsedRow(rowNumber, dni, date, time, movementType, buildingName, observation);
    }

    private LocalDate parseCellDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String val = getCellStringValue(cell);
            if (val == null || val.isBlank()) return null;
            return LocalDate.parse(val.trim(), DATE_FORMAT);
        } catch (DateTimeParseException | IllegalStateException e) {
            return null;
        }
    }

    private LocalTime parseCellTime(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                // Excel stores time as a fraction of day
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalTime();
                }
                double numericValue = cell.getNumericCellValue();
                long totalSeconds = Math.round(numericValue * 24 * 60 * 60);
                int hours = (int) (totalSeconds / 3600) % 24;
                int minutes = (int) ((totalSeconds % 3600) / 60);
                return LocalTime.of(hours, minutes);
            }
            String val = getCellStringValue(cell);
            if (val == null || val.isBlank()) return null;
            val = val.trim();
            // Handle HH:mm or HH:mm:ss
            if (val.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
                String[] parts = val.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                return LocalTime.of(h, m);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }

    private String sanitize(String value) {
        if (value == null) return null;
        // Remove control characters and trim
        return value.replaceAll("[\\p{Cc}&&[^\\t\\n]]", "").trim();
    }

    private void addInstructionRow(Sheet sheet, int rowNum, CellStyle style, String text) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    // ======================== Inner classes ========================

    private record ParsedRow(int rowNumber, String dni, LocalDate date, LocalTime time,
                             String movementTypeRaw, String buildingName, String observation) {}

    private record SimpleMovement(LocalTime time, MovementType type, int rowNumber) {}
}
