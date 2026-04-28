package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.forecast.BudgetForecastNotValidException;
import PSG.backEnd.model.dto.forecast.BudgetForecastImportResultDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastImportResultDTO.BudgetForecastImportRowErrorDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastImportResultDTO.BudgetForecastImportRowWarningDTO;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.StockRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.repository.forecast.BudgetForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Importador y exportador de items de previsión en formato Excel (.xlsx).
 *
 * <h3>Esquema del archivo</h3>
 * Hoja: <i>Items Previsión</i>. Encabezados (orden estricto):
 * Tipo | Descripción | Fecha (dd/MM/yyyy) | Monto | Empleado(CUIL) | Proveedor(CUIT) |
 * AsignaciónId | Vehículo(Patente) | Stock(Nombre) | Cantidad
 *
 * <h3>Códigos de error/advertencia</h3>
 * <ul>
 *   <li>E1: tipo desconocido</li>
 *   <li>E2: descripción vacía</li>
 *   <li>E3: fecha inválida o vacía</li>
 *   <li>E4: monto inválido o &lt;=0</li>
 *   <li>E5: referencia obligatoria faltante para el tipo</li>
 *   <li>E6: referencia no encontrada</li>
 *   <li>W1: cantidad de stock omitida (asume 1)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetForecastExcelService {

    private static final String SHEET_NAME = "Items Previsión";
    private static final int MAX_ROWS = 1000;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
            "Tipo", "Descripción", "Fecha", "Monto",
            "Empleado(CUIL)", "Proveedor(CUIT)", "AsignaciónId",
            "Vehículo(Patente)", "Stock(Nombre)", "Cantidad"
    };

    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final StockRepository stockRepository;
    private final BudgetForecastRepository forecastRepository;

    // ─────────────────────────── Import ───────────────────────────

    public BudgetForecastImportResultDTO importItems(BudgetForecast forecast, MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new BudgetForecastNotValidException("El archivo está vacío.");
        }
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) sheet = workbook.getSheetAt(0);
            validateHeaders(sheet);

            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                throw new BudgetForecastNotValidException("El archivo no contiene filas de datos.");
            }
            if (lastRow > MAX_ROWS) {
                throw new BudgetForecastNotValidException(
                        "El archivo supera el límite de " + MAX_ROWS + " filas.");
            }

            List<BudgetForecastItem> toAdd = new ArrayList<>();
            List<BudgetForecastImportRowErrorDTO> errors = new ArrayList<>();
            List<BudgetForecastImportRowWarningDTO> warnings = new ArrayList<>();
            int totalRows = 0;

            int baseOrder = forecast.getItems().stream()
                    .mapToInt(i -> i.getRowOrder() == null ? 0 : i.getRowOrder())
                    .max().orElse(-1) + 1;

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (isRowEmpty(row)) continue;
                totalRows++;
                int displayRow = r + 1;
                try {
                    BudgetForecastItem item = parseRow(row, displayRow, forecast, warnings);
                    item.setRowOrder(baseOrder++);
                    toAdd.add(item);
                } catch (RowParseException ex) {
                    errors.add(new BudgetForecastImportRowErrorDTO(displayRow, ex.code, ex.getMessage()));
                }
            }

            int imported = 0;
            if (errors.isEmpty() && !dryRun) {
                forecast.getItems().addAll(toAdd);
                BigDecimal newTotal = forecast.getItems().stream()
                        .map(BudgetForecastItem::getExpectedAmount)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                forecast.setTotalAmount(newTotal);
                forecastRepository.save(forecast);
                imported = toAdd.size();
            }

            return new BudgetForecastImportResultDTO(
                    dryRun, forecast.getId(), totalRows, imported, errors.size(), errors, warnings);

        } catch (BudgetForecastNotValidException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error procesando Excel de previsión", e);
            throw new BudgetForecastNotValidException("Error procesando el archivo Excel: " + e.getMessage());
        }
    }

    private void validateHeaders(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new BudgetForecastNotValidException("La hoja no tiene encabezados.");
        }
        for (int i = 0; i < HEADERS.length; i++) {
            String actual = getStringCell(header.getCell(i));
            if (actual == null || !actual.trim().equalsIgnoreCase(HEADERS[i])) {
                throw new BudgetForecastNotValidException(
                        "Encabezado inválido en columna " + (i + 1) + ". Esperado: \"" + HEADERS[i] + "\".");
            }
        }
    }

    private BudgetForecastItem parseRow(Row row, int displayRow, BudgetForecast forecast,
                                        List<BudgetForecastImportRowWarningDTO> warnings) {
        // Col 0: Tipo
        String typeRaw = getStringCell(row.getCell(0));
        if (typeRaw == null || typeRaw.isBlank()) throw new RowParseException("E1", "Tipo vacío.");
        BudgetForecastItemType itemType;
        try { itemType = BudgetForecastItemType.valueOf(typeRaw.trim().toUpperCase()); }
        catch (Exception e) { throw new RowParseException("E1", "Tipo desconocido: " + typeRaw); }

        // Col 1: Descripción
        String description = getStringCell(row.getCell(1));
        if (description == null || description.isBlank()) {
            throw new RowParseException("E2", "Descripción obligatoria.");
        }

        // Col 2: Fecha
        LocalDate date = parseDate(row.getCell(2));
        if (date == null) throw new RowParseException("E3", "Fecha inválida o vacía.");

        // Col 3: Monto
        BigDecimal amount = parseAmount(row.getCell(3));
        if (amount == null || amount.signum() <= 0) {
            throw new RowParseException("E4", "Monto inválido. Debe ser > 0.");
        }

        BudgetForecastItem item = BudgetForecastItem.builder()
                .budgetForecast(forecast)
                .itemType(itemType)
                .description(description.trim())
                .expectedDate(date)
                .expectedAmount(amount)
                .applicationStatus(BudgetForecastItemApplicationStatus.PENDIENTE)
                .build();

        // Refs
        String cuil = getStringCell(row.getCell(4));
        String cuit = getStringCell(row.getCell(5));
        Long assignmentId = parseLong(row.getCell(6));
        String plate = getStringCell(row.getCell(7));
        String stockName = getStringCell(row.getCell(8));
        BigDecimal qty = parseAmount(row.getCell(9));

        if (cuil != null && !cuil.isBlank()) {
            Employee e = employeeRepository.findByCuilAndDeletedFalse(cuil.trim())
                    .orElseThrow(() -> new RowParseException("E6", "Empleado no encontrado por CUIL: " + cuil));
            item.setEmployee(e);
        }
        if (cuit != null && !cuit.isBlank()) {
            Supplier s = supplierRepository.findByCuitAndDeletedFalse(cuit.trim())
                    .orElseThrow(() -> new RowParseException("E6", "Proveedor no encontrado por CUIT: " + cuit));
            item.setSupplier(s);
        }
        if (assignmentId != null) {
            ServiceAssignment sa = serviceAssignmentRepository.findByIdAndDeletedFalse(assignmentId)
                    .orElseThrow(() -> new RowParseException("E6", "Asignación no encontrada: " + assignmentId));
            item.setServiceAssignment(sa);
        }
        if (plate != null && !plate.isBlank()) {
            Vehicle v = vehicleRepository.findByLicensePlateAndDeletedFalse(plate.trim().toUpperCase())
                    .orElseThrow(() -> new RowParseException("E6", "Vehículo no encontrado por patente: " + plate));
            item.setVehicle(v);
        }
        if (stockName != null && !stockName.isBlank()) {
            Stock st = stockRepository.findByNameAndDeletedFalse(stockName.trim())
                    .orElseThrow(() -> new RowParseException("E6", "Stock no encontrado: " + stockName));
            item.setStock(st);
            if (qty == null) {
                warnings.add(new BudgetForecastImportRowWarningDTO(displayRow, "W1", "Cantidad omitida; se asume 1."));
                qty = BigDecimal.ONE;
            }
            item.setStockQuantity(qty);
        }

        // Validación cruzada por tipo
        validateTypeReferences(itemType, item);
        return item;
    }

    private void validateTypeReferences(BudgetForecastItemType type, BudgetForecastItem item) {
        switch (type) {
            case SALARIO -> requireRef(item.getEmployee(), "SALARIO requiere Empleado(CUIL).");
            case SERVICIO, PATENTE -> requireRef(item.getServiceAssignment(),
                    type + " requiere AsignaciónId.");
            case REPARACION -> requireRef(item.getVehicle(), "REPARACION requiere Vehículo(Patente).");
            case COMPRA_STOCK -> {
                requireRef(item.getStock(), "COMPRA_STOCK requiere Stock(Nombre).");
                if (item.getStockQuantity() == null || item.getStockQuantity().signum() <= 0) {
                    throw new RowParseException("E5", "COMPRA_STOCK requiere Cantidad > 0.");
                }
            }
            case OTRO -> { /* sin requisitos */ }
        }
    }

    private void requireRef(Object ref, String msg) {
        if (ref == null) throw new RowParseException("E5", msg);
    }

    // ─────────────────────────── Export ───────────────────────────

    public byte[] export(BudgetForecast forecast) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(SHEET_NAME);
            CellStyle headerStyle = headerStyle(wb);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            List<BudgetForecastItem> items = new ArrayList<>(forecast.getItems());
            items.sort(Comparator.comparing(i -> i.getRowOrder() == null ? 0 : i.getRowOrder()));
            for (BudgetForecastItem item : items) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(item.getItemType().name());
                row.createCell(1).setCellValue(nullSafe(item.getDescription()));
                row.createCell(2).setCellValue(item.getExpectedDate() != null
                        ? item.getExpectedDate().format(DATE_FMT) : "");
                if (item.getExpectedAmount() != null)
                    row.createCell(3).setCellValue(item.getExpectedAmount().doubleValue());
                row.createCell(4).setCellValue(item.getEmployee() != null
                        ? nullSafe(item.getEmployee().getCuil()) : "");
                row.createCell(5).setCellValue(item.getSupplier() != null
                        ? nullSafe(item.getSupplier().getCuit()) : "");
                if (item.getServiceAssignment() != null)
                    row.createCell(6).setCellValue(item.getServiceAssignment().getId());
                row.createCell(7).setCellValue(item.getVehicle() != null
                        ? nullSafe(item.getVehicle().getLicensePlate()) : "");
                row.createCell(8).setCellValue(item.getStock() != null
                        ? nullSafe(item.getStock().getName()) : "");
                if (item.getStockQuantity() != null)
                    row.createCell(9).setCellValue(item.getStockQuantity().doubleValue());
            }

            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error exportando previsión a Excel", e);
            throw new BudgetForecastNotValidException("Error exportando a Excel: " + e.getMessage());
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(f);
        cs.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        return cs;
    }

    // ─────────────────────────── Helpers cell ───────────────────────────

    private String getStringCell(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getDateCellValue().toInstant().toString();
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v)) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String s = getStringCell(cell);
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), DATE_FMT); }
        catch (Exception e) { return null; }
    }

    private BigDecimal parseAmount(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String s = getStringCell(cell);
            if (s == null || s.isBlank()) return null;
            return new BigDecimal(s.trim().replace(",", "."));
        } catch (Exception e) { return null; }
    }

    private Long parseLong(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return (long) cell.getNumericCellValue();
            String s = getStringCell(cell);
            if (s == null || s.isBlank()) return null;
            return Long.parseLong(s.trim());
        } catch (Exception e) { return null; }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = 0; c < HEADERS.length; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String s = getStringCell(cell);
                if (s != null && !s.isBlank()) return false;
            }
        }
        return true;
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    /** Excepción interna para parseo fila a fila. No escapa al cliente. */
    private static class RowParseException extends RuntimeException {
        final String code;
        RowParseException(String code, String msg) { super(msg); this.code = code; }
    }
}
