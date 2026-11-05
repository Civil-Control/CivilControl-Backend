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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importador y exportador de items de previsión en formato Excel (.xlsx).
 *
 * <h3>Layout matricial (espejo de la tabla del frontend)</h3>
 * <pre>
 *  Tipo │ Identificador │ &lt;día 1&gt; │ &lt;día 2&gt; │ … │ &lt;día N&gt; │ Monto │ Cantidad │ Proveedor (CUIT) │ Estado
 * </pre>
 * <ul>
 *   <li>Hay una columna por cada día del período de la previsión.</li>
 *   <li>Cada fila representa un item; la celda del día correspondiente contiene la
 *       <b>descripción</b> del item (las restantes celdas de día se dejan vacías).</li>
 *   <li>El <b>Identificador</b> es la clave canónica según el tipo:
 *     <ul>
 *       <li>SALARIO → CUIL del empleado</li>
 *       <li>SERVICIO / PATENTE → ID de la asignación de servicio</li>
 *       <li>REPARACION → patente del vehículo</li>
 *       <li>COMPRA_STOCK → nombre del stock</li>
 *       <li>OTRO → vacío</li>
 *     </ul>
 *   </li>
 *   <li><b>Cantidad</b> aplica únicamente a COMPRA_STOCK.</li>
 *   <li><b>Proveedor (CUIT)</b> es opcional para REPARACION y COMPRA_STOCK.</li>
 *   <li><b>Estado</b> se exporta como informativo y se ignora en la importación
 *       (los items importados quedan siempre en PENDIENTE).</li>
 * </ul>
 *
 * <h3>Códigos de error/advertencia</h3>
 * <ul>
 *   <li>E1: tipo desconocido</li>
 *   <li>E2: descripción/celda de día no encontrada</li>
 *   <li>E3: fecha fuera del período de la previsión</li>
 *   <li>E4: monto inválido o &lt;= 0</li>
 *   <li>E5: identificador obligatorio faltante o cantidad inválida</li>
 *   <li>E6: referencia (empleado, vehículo, etc.) no encontrada</li>
 *   <li>W1: cantidad de stock omitida (asume 1)</li>
 *   <li>W2: descripción presente en más de una celda de día (se usa la primera)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetForecastExcelService {

    private static final String SHEET_NAME = "Items Previsión";
    private static final int MAX_ROWS = 1000;
    private static final int MAX_DAYS = 366;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Locale ES_AR = new Locale("es", "AR");
    private static final Pattern HEADER_DATE_PATTERN = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");

    // Posición de las columnas fijas del lado izquierdo. Las del lado derecho se calculan
    // dinámicamente en función del rango de días (que es variable por previsión).
    private static final int COL_TYPE = 0;
    private static final int COL_IDENTIFIER = 1;
    private static final int FIXED_LEFT_COLS = 2;        // Tipo + Identificador
    private static final int FIXED_RIGHT_COLS = 4;       // Monto + Cantidad + Proveedor + Estado
    private static final String HEADER_TYPE = "Tipo";
    private static final String HEADER_IDENTIFIER = "Identificador";
    private static final String HEADER_AMOUNT = "Monto";
    private static final String HEADER_QUANTITY = "Cantidad";
    private static final String HEADER_SUPPLIER = "Proveedor (CUIT)";
    private static final String HEADER_STATUS = "Estado";

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
        if (forecast.getPeriodFrom() == null || forecast.getPeriodTo() == null) {
            throw new BudgetForecastNotValidException("La previsión no tiene un período definido.");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) sheet = workbook.getSheetAt(0);

            HeaderLayout layout = parseHeaders(sheet, forecast);

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
                if (isRowEmpty(row, layout)) continue;
                totalRows++;
                int displayRow = r + 1;
                try {
                    BudgetForecastItem item = parseRow(row, displayRow, forecast, layout, warnings);
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

    /**
     * Lee la fila de encabezados y reconstruye el layout matricial: detecta las columnas
     * fijas (Tipo/Identificador a la izquierda; Monto/Cantidad/Proveedor/Estado a la derecha)
     * y mapea cada columna intermedia a la fecha que representa.
     * <p>
     * Esto permite que el archivo importado tenga un período distinto al exportado siempre
     * que las fechas estén dentro del período de la previsión.
     */
    private HeaderLayout parseHeaders(Sheet sheet, BudgetForecast forecast) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new BudgetForecastNotValidException("La hoja no tiene encabezados.");
        }

        if (!equalsIgnoreCaseTrim(getStringCell(header.getCell(COL_TYPE)), HEADER_TYPE)) {
            throw new BudgetForecastNotValidException(
                    "Encabezado inválido en columna 1. Esperado: \"" + HEADER_TYPE + "\".");
        }
        if (!equalsIgnoreCaseTrim(getStringCell(header.getCell(COL_IDENTIFIER)), HEADER_IDENTIFIER)) {
            throw new BudgetForecastNotValidException(
                    "Encabezado inválido en columna 2. Esperado: \"" + HEADER_IDENTIFIER + "\".");
        }

        int lastCol = header.getLastCellNum() - 1;
        if (lastCol < FIXED_LEFT_COLS + FIXED_RIGHT_COLS) {
            throw new BudgetForecastNotValidException("La hoja no tiene la cantidad mínima de columnas esperadas.");
        }

        int amountCol = lastCol - 3;
        int quantityCol = lastCol - 2;
        int supplierCol = lastCol - 1;
        int statusCol = lastCol;
        if (!equalsIgnoreCaseTrim(getStringCell(header.getCell(amountCol)), HEADER_AMOUNT)
                || !equalsIgnoreCaseTrim(getStringCell(header.getCell(quantityCol)), HEADER_QUANTITY)
                || !equalsIgnoreCaseTrim(getStringCell(header.getCell(supplierCol)), HEADER_SUPPLIER)
                || !equalsIgnoreCaseTrim(getStringCell(header.getCell(statusCol)), HEADER_STATUS)) {
            throw new BudgetForecastNotValidException(
                    "Las últimas 4 columnas deben ser, en orden: \"" + HEADER_AMOUNT + "\", \""
                            + HEADER_QUANTITY + "\", \"" + HEADER_SUPPLIER + "\", \"" + HEADER_STATUS + "\".");
        }

        Map<Integer, LocalDate> dateColumns = new HashMap<>();
        for (int c = FIXED_LEFT_COLS; c < amountCol; c++) {
            String text = getStringCell(header.getCell(c));
            LocalDate d = extractHeaderDate(text);
            if (d == null) {
                throw new BudgetForecastNotValidException(
                        "Encabezado de día inválido en columna " + (c + 1)
                                + ". Se esperaba una fecha en formato dd/MM/yyyy.");
            }
            if (d.isBefore(forecast.getPeriodFrom()) || d.isAfter(forecast.getPeriodTo())) {
                throw new BudgetForecastNotValidException(
                        "La columna " + (c + 1) + " (" + text + ") está fuera del período de la previsión ("
                                + forecast.getPeriodFrom().format(DATE_FMT) + " — "
                                + forecast.getPeriodTo().format(DATE_FMT) + ").");
            }
            dateColumns.put(c, d);
        }
        if (dateColumns.isEmpty()) {
            throw new BudgetForecastNotValidException("La hoja no contiene columnas de día.");
        }

        return new HeaderLayout(amountCol, quantityCol, supplierCol, statusCol, dateColumns);
    }

    private LocalDate extractHeaderDate(String text) {
        if (text == null) return null;
        Matcher m = HEADER_DATE_PATTERN.matcher(text);
        if (!m.find()) return null;
        try {
            return LocalDate.parse(m.group(1), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private BudgetForecastItem parseRow(Row row, int displayRow, BudgetForecast forecast,
                                        HeaderLayout layout,
                                        List<BudgetForecastImportRowWarningDTO> warnings) {
        // Tipo
        String typeRaw = getStringCell(row.getCell(COL_TYPE));
        if (typeRaw == null || typeRaw.isBlank()) throw new RowParseException("E1", "Tipo vacío.");
        BudgetForecastItemType itemType;
        try { itemType = BudgetForecastItemType.valueOf(typeRaw.trim().toUpperCase()); }
        catch (Exception e) { throw new RowParseException("E1", "Tipo desconocido: " + typeRaw); }

        // Localizar la celda de día con descripción.
        LocalDate date = null;
        String description = null;
        boolean duplicate = false;
        for (Map.Entry<Integer, LocalDate> e : layout.dateColumns.entrySet()) {
            String txt = getStringCell(row.getCell(e.getKey()));
            if (txt != null && !txt.isBlank()) {
                if (date == null) {
                    date = e.getValue();
                    description = txt.trim();
                } else {
                    duplicate = true;
                }
            }
        }
        if (date == null || description == null) {
            throw new RowParseException("E2",
                    "No se encontró una celda de día con descripción para esta fila.");
        }
        if (duplicate) {
            warnings.add(new BudgetForecastImportRowWarningDTO(displayRow, "W2",
                    "La fila tiene descripción en más de una celda de día; se usó la primera ("
                            + date.format(DATE_FMT) + ")."));
        }
        // Cobertura defensiva (los encabezados ya fueron validados).
        if (date.isBefore(forecast.getPeriodFrom()) || date.isAfter(forecast.getPeriodTo())) {
            throw new RowParseException("E3", "Fecha fuera del período de la previsión: " + date.format(DATE_FMT));
        }

        // Monto
        BigDecimal amount = parseAmount(row.getCell(layout.amountCol));
        if (amount == null || amount.signum() <= 0) {
            throw new RowParseException("E4", "Monto inválido. Debe ser > 0.");
        }

        BudgetForecastItem item = BudgetForecastItem.builder()
                .budgetForecast(forecast)
                .itemType(itemType)
                .description(description)
                .expectedDate(date)
                .expectedAmount(amount)
                .applicationStatus(BudgetForecastItemApplicationStatus.PENDIENTE)
                .build();

        String identifier = trimToNull(getStringCell(row.getCell(COL_IDENTIFIER)));
        BigDecimal quantity = parseAmount(row.getCell(layout.quantityCol));
        String supplierCuit = trimToNull(getStringCell(row.getCell(layout.supplierCol)));

        resolveIdentifier(itemType, identifier, quantity, item, displayRow, warnings);
        resolveOptionalSupplier(itemType, supplierCuit, item);

        return item;
    }

    /**
     * Resuelve el identificador contra la entidad correspondiente y aplica
     * las validaciones de obligatoriedad por tipo.
     */
    private void resolveIdentifier(BudgetForecastItemType type, String identifier, BigDecimal quantity,
                                   BudgetForecastItem item, int displayRow,
                                   List<BudgetForecastImportRowWarningDTO> warnings) {
        switch (type) {
            case SALARIO -> {
                if (identifier == null) {
                    throw new RowParseException("E5", "SALARIO requiere CUIL del empleado en \"Identificador\".");
                }
                Employee e = employeeRepository.findByCuilAndDeletedFalse(identifier)
                        .orElseThrow(() -> new RowParseException("E6", "Empleado no encontrado por CUIL: " + identifier));
                item.setEmployee(e);
            }
            case SERVICIO, PATENTE -> {
                if (identifier == null) {
                    throw new RowParseException("E5", type + " requiere ID de asignación en \"Identificador\".");
                }
                long assignmentId;
                try { assignmentId = Long.parseLong(identifier); }
                catch (NumberFormatException ex) {
                    throw new RowParseException("E5", "Identificador de asignación inválido: " + identifier);
                }
                ServiceAssignment sa = serviceAssignmentRepository.findByIdAndDeletedFalse(assignmentId)
                        .orElseThrow(() -> new RowParseException("E6", "Asignación no encontrada: " + assignmentId));
                item.setServiceAssignment(sa);
            }
            case REPARACION -> {
                if (identifier == null) {
                    throw new RowParseException("E5", "REPARACION requiere patente del vehículo en \"Identificador\".");
                }
                Vehicle v = vehicleRepository.findByLicensePlateAndDeletedFalse(identifier.toUpperCase())
                        .orElseThrow(() -> new RowParseException("E6", "Vehículo no encontrado por patente: " + identifier));
                item.setVehicle(v);
            }
            case COMPRA_STOCK -> {
                if (identifier == null) {
                    throw new RowParseException("E5", "COMPRA_STOCK requiere nombre del stock en \"Identificador\".");
                }
                Stock st = stockRepository.findByNameAndDeletedFalse(identifier)
                        .orElseThrow(() -> new RowParseException("E6", "Stock no encontrado: " + identifier));
                item.setStock(st);
                if (quantity == null) {
                    warnings.add(new BudgetForecastImportRowWarningDTO(displayRow, "W1", "Cantidad omitida; se asume 1."));
                    quantity = BigDecimal.ONE;
                }
                if (quantity.signum() <= 0) {
                    throw new RowParseException("E5", "Cantidad de stock debe ser > 0.");
                }
                item.setStockQuantity(quantity);
            }
            case OTRO -> { /* sin requisitos */ }
        }
    }

    private void resolveOptionalSupplier(BudgetForecastItemType type, String cuit, BudgetForecastItem item) {
        if (cuit == null) return;
        if (type != BudgetForecastItemType.REPARACION && type != BudgetForecastItemType.COMPRA_STOCK) {
            return; // ignoramos el proveedor en tipos donde no aplica.
        }
        Supplier s = supplierRepository.findByCuitAndDeletedFalse(cuit)
                .orElseThrow(() -> new RowParseException("E6", "Proveedor no encontrado por CUIT: " + cuit));
        item.setSupplier(s);
    }

    // ─────────────────────────── Export ───────────────────────────

    public byte[] export(BudgetForecast forecast) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            List<LocalDate> dateRange = buildDateRange(forecast);
            Sheet sheet = wb.createSheet(SHEET_NAME);
            CellStyle headerStyle = headerStyle(wb);
            CellStyle dayHeaderStyle = dayHeaderStyle(wb);
            CellStyle dayCellStyle = dayCellStyle(wb);
            CellStyle moneyStyle = moneyStyle(wb);

            writeHeaderRow(sheet, dateRange, headerStyle, dayHeaderStyle);

            int amountCol = FIXED_LEFT_COLS + dateRange.size();
            int quantityCol = amountCol + 1;
            int supplierCol = amountCol + 2;
            int statusCol = amountCol + 3;
            int colCount = statusCol + 1;

            List<BudgetForecastItem> items = new ArrayList<>(forecast.getItems());
            items.sort(Comparator.comparing(i -> i.getRowOrder() == null ? 0 : i.getRowOrder()));

            int r = 1;
            for (BudgetForecastItem item : items) {
                Row row = sheet.createRow(r++);
                row.createCell(COL_TYPE).setCellValue(item.getItemType().name());
                row.createCell(COL_IDENTIFIER).setCellValue(buildIdentifier(item));

                for (int i = 0; i < dateRange.size(); i++) {
                    Cell c = row.createCell(FIXED_LEFT_COLS + i);
                    c.setCellStyle(dayCellStyle);
                    if (item.getExpectedDate() != null && item.getExpectedDate().equals(dateRange.get(i))) {
                        c.setCellValue(nullSafe(item.getDescription()));
                    }
                }

                if (item.getExpectedAmount() != null) {
                    Cell ca = row.createCell(amountCol);
                    ca.setCellValue(item.getExpectedAmount().doubleValue());
                    ca.setCellStyle(moneyStyle);
                }
                if (item.getStockQuantity() != null) {
                    row.createCell(quantityCol).setCellValue(item.getStockQuantity().doubleValue());
                }
                row.createCell(supplierCol).setCellValue(
                        item.getSupplier() != null ? nullSafe(item.getSupplier().getCuit()) : "");
                row.createCell(statusCol).setCellValue(
                        item.getApplicationStatus() != null ? item.getApplicationStatus().name() : "");
            }

            applyColumnWidths(sheet, dateRange.size(), colCount);
            sheet.createFreezePane(FIXED_LEFT_COLS, 1);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error exportando previsión a Excel", e);
            throw new BudgetForecastNotValidException("Error exportando a Excel: " + e.getMessage());
        }
    }

    // ─────────────────────────── Plantilla ───────────────────────────

    /**
     * Genera una plantilla Excel para la previsión indicada: misma matriz que el export,
     * con una fila de ejemplo por cada tipo y una hoja de instrucciones.
     */
    public byte[] generateImportTemplate(BudgetForecast forecast) {
        if (forecast.getPeriodFrom() == null || forecast.getPeriodTo() == null) {
            throw new BudgetForecastNotValidException("La previsión no tiene un período definido.");
        }
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            List<LocalDate> dateRange = buildDateRange(forecast);
            Sheet sheet = wb.createSheet(SHEET_NAME);

            CellStyle headerStyle = headerStyle(wb);
            CellStyle dayHeaderStyle = dayHeaderStyle(wb);
            CellStyle dayCellStyle = dayCellStyle(wb);
            CellStyle moneyStyle = moneyStyle(wb);

            Font exampleFont = wb.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            CellStyle exampleStyle = wb.createCellStyle();
            exampleStyle.setFont(exampleFont);
            CellStyle exampleDayStyle = mergeStyles(wb, dayCellStyle, exampleFont);
            CellStyle exampleMoneyStyle = mergeStyles(wb, moneyStyle, exampleFont);

            writeHeaderRow(sheet, dateRange, headerStyle, dayHeaderStyle);

            int amountCol = FIXED_LEFT_COLS + dateRange.size();
            int quantityCol = amountCol + 1;
            int supplierCol = amountCol + 2;
            int statusCol = amountCol + 3;
            int colCount = statusCol + 1;
            int sampleDayCol = FIXED_LEFT_COLS; // primer día del período

            String[][] examples = {
                    {"SALARIO",      "20-12345678-9",  "Pago mensual operario",     "150000",  "",     ""},
                    {"SERVICIO",     "5",              "Edenor — oficina central",  "85000",   "",     ""},
                    {"PATENTE",      "7",              "Patente camión utilitario", "42000",   "",     ""},
                    {"REPARACION",   "AB123CD",        "Cambio de frenos",          "60000",   "",     "30-71112222-3"},
                    {"COMPRA_STOCK", "Tornillo M8x40", "Reposición tornillos",      "12500",   "100",  "30-71112222-3"},
                    {"OTRO",         "",               "Imprevisto del mes",        "20000",   "",     ""},
            };
            for (int i = 0; i < examples.length; i++) {
                String[] vals = examples[i];
                Row row = sheet.createRow(i + 1);
                writeCell(row, COL_TYPE, vals[0], exampleStyle);
                writeCell(row, COL_IDENTIFIER, vals[1], exampleStyle);

                for (int d = 0; d < dateRange.size(); d++) {
                    Cell c = row.createCell(FIXED_LEFT_COLS + d);
                    c.setCellStyle(dayCellStyle);
                }
                Cell descCell = row.getCell(sampleDayCol);
                descCell.setCellValue(vals[2]);
                descCell.setCellStyle(exampleDayStyle);

                Cell amountCell = row.createCell(amountCol);
                amountCell.setCellValue(Double.parseDouble(vals[3]));
                amountCell.setCellStyle(exampleMoneyStyle);
                if (!vals[4].isEmpty()) writeCell(row, quantityCol, vals[4], exampleStyle);
                if (!vals[5].isEmpty()) writeCell(row, supplierCol, vals[5], exampleStyle);
                writeCell(row, statusCol, "PENDIENTE", exampleStyle);
            }

            applyColumnWidths(sheet, dateRange.size(), colCount);
            sheet.createFreezePane(FIXED_LEFT_COLS, 1);

            buildInstructionsSheet(wb, forecast);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando plantilla de importación de previsión", e);
            throw new BudgetForecastNotValidException("Error generando la plantilla: " + e.getMessage());
        }
    }

    private void buildInstructionsSheet(Workbook wb, BudgetForecast forecast) {
        Sheet instr = wb.createSheet("Instrucciones");
        CellStyle instrHeader = wb.createCellStyle();
        Font ihf = wb.createFont();
        ihf.setBold(true);
        ihf.setFontHeightInPoints((short) 12);
        instrHeader.setFont(ihf);
        CellStyle instrStyle = wb.createCellStyle();
        instrStyle.setWrapText(true);

        int r = 0;
        addInstr(instr, r++, instrHeader, "Plantilla de importación — Previsión \"" + nullSafe(forecast.getName()) + "\"");
        addInstr(instr, r++, instrStyle, "Período: " + forecast.getPeriodFrom().format(DATE_FMT)
                + " — " + forecast.getPeriodTo().format(DATE_FMT));
        r++;
        addInstr(instr, r++, instrHeader, "Cómo completar la plantilla");
        addInstr(instr, r++, instrStyle, "1. Cada fila = un item de previsión.");
        addInstr(instr, r++, instrStyle,
                "2. Una sola celda de día por fila debe contener la DESCRIPCIÓN del item; "
                        + "el resto se deja vacío. Esa celda define la fecha del item.");
        addInstr(instr, r++, instrStyle, "3. \"Identificador\" depende del Tipo:");
        addInstr(instr, r++, instrStyle, "    • SALARIO → CUIL del empleado");
        addInstr(instr, r++, instrStyle, "    • SERVICIO / PATENTE → ID de la asignación de servicio");
        addInstr(instr, r++, instrStyle, "    • REPARACION → patente del vehículo");
        addInstr(instr, r++, instrStyle, "    • COMPRA_STOCK → nombre exacto del stock (y \"Cantidad\" > 0)");
        addInstr(instr, r++, instrStyle, "    • OTRO → dejar vacío");
        addInstr(instr, r++, instrStyle, "4. \"Monto\" obligatorio (> 0).");
        addInstr(instr, r++, instrStyle,
                "5. \"Proveedor (CUIT)\" es opcional y solo se considera para REPARACION y COMPRA_STOCK.");
        addInstr(instr, r++, instrStyle, "6. \"Estado\" se ignora en la importación: los items quedan en PENDIENTE.");
        r++;
        addInstr(instr, r++, instrHeader, "Notas importantes");
        addInstr(instr, r++, instrStyle, "- Las primeras filas son ejemplos. Elimínelas antes de importar.");
        addInstr(instr, r++, instrStyle, "- Máximo " + MAX_ROWS + " filas por importación.");
        addInstr(instr, r++, instrStyle,
                "- No agregue ni elimine columnas: el orden y nombre de las columnas debe respetarse.");
        addInstr(instr, r++, instrStyle,
                "- Las fechas (encabezados de día) deben coincidir con días dentro del período de la previsión.");
        instr.setColumnWidth(0, 110 * 256);
    }

    private void addInstr(Sheet sheet, int rowIdx, CellStyle style, String text) {
        Row row = sheet.createRow(rowIdx);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(style);
    }

    // ─────────────────────────── Layout helpers ───────────────────────────

    private List<LocalDate> buildDateRange(BudgetForecast forecast) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate cur = forecast.getPeriodFrom();
        LocalDate end = forecast.getPeriodTo();
        for (int i = 0; i < MAX_DAYS && !cur.isAfter(end); i++) {
            out.add(cur);
            cur = cur.plusDays(1);
        }
        return out;
    }

    private void writeHeaderRow(Sheet sheet, List<LocalDate> dateRange,
                                CellStyle headerStyle, CellStyle dayHeaderStyle) {
        Row header = sheet.createRow(0);
        writeHeaderCell(header, COL_TYPE, HEADER_TYPE, headerStyle);
        writeHeaderCell(header, COL_IDENTIFIER, HEADER_IDENTIFIER, headerStyle);
        for (int i = 0; i < dateRange.size(); i++) {
            LocalDate d = dateRange.get(i);
            String weekday = capitalize(d.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, ES_AR).replace(".", ""));
            writeHeaderCell(header, FIXED_LEFT_COLS + i, weekday + " " + d.format(DATE_FMT), dayHeaderStyle);
        }
        int amountCol = FIXED_LEFT_COLS + dateRange.size();
        writeHeaderCell(header, amountCol,     HEADER_AMOUNT,     headerStyle);
        writeHeaderCell(header, amountCol + 1, HEADER_QUANTITY,   headerStyle);
        writeHeaderCell(header, amountCol + 2, HEADER_SUPPLIER,   headerStyle);
        writeHeaderCell(header, amountCol + 3, HEADER_STATUS,     headerStyle);
        header.setHeightInPoints(28);
    }

    private void writeHeaderCell(Row row, int col, String text, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(text);
        c.setCellStyle(style);
    }

    private void writeCell(Row row, int col, String text, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(text);
        if (style != null) c.setCellStyle(style);
    }

    private void applyColumnWidths(Sheet sheet, int dayCount, int colCount) {
        sheet.setColumnWidth(COL_TYPE, 16 * 256);
        sheet.setColumnWidth(COL_IDENTIFIER, 28 * 256);
        for (int i = 0; i < dayCount; i++) sheet.setColumnWidth(FIXED_LEFT_COLS + i, 18 * 256);
        int rightStart = FIXED_LEFT_COLS + dayCount;
        sheet.setColumnWidth(rightStart,     14 * 256); // Monto
        sheet.setColumnWidth(rightStart + 1, 12 * 256); // Cantidad
        sheet.setColumnWidth(rightStart + 2, 22 * 256); // Proveedor
        sheet.setColumnWidth(rightStart + 3, 14 * 256); // Estado
        // colCount es informativo; se mantiene en la firma para reflejar el ancho total.
        if (colCount < rightStart + FIXED_RIGHT_COLS) {
            log.debug("colCount={} no cubre todas las columnas esperadas", colCount);
        }
    }

    /**
     * Construye el identificador canónico por tipo, espejando el getter del frontend.
     */
    private String buildIdentifier(BudgetForecastItem item) {
        return switch (item.getItemType()) {
            case SALARIO -> item.getEmployee() != null ? nullSafe(item.getEmployee().getCuil()) : "";
            case SERVICIO, PATENTE -> item.getServiceAssignment() != null
                    ? String.valueOf(item.getServiceAssignment().getId()) : "";
            case REPARACION -> item.getVehicle() != null ? nullSafe(item.getVehicle().getLicensePlate()) : "";
            case COMPRA_STOCK -> item.getStock() != null ? nullSafe(item.getStock().getName()) : "";
            case OTRO -> "";
        };
    }

    // ─────────────────────────── Estilos ───────────────────────────

    private CellStyle headerStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(f);
        cs.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN);
        return cs;
    }

    private CellStyle dayHeaderStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 9);
        cs.setFont(f);
        cs.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN);
        return cs;
    }

    private CellStyle dayCellStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setBorderLeft(BorderStyle.HAIR);
        cs.setBorderRight(BorderStyle.HAIR);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setWrapText(true);
        return cs;
    }

    private CellStyle moneyStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cs.setDataFormat(df.getFormat("#,##0.00"));
        cs.setAlignment(HorizontalAlignment.RIGHT);
        return cs;
    }

    private CellStyle mergeStyles(Workbook wb, CellStyle base, Font font) {
        CellStyle cs = wb.createCellStyle();
        cs.cloneStyleFrom(base);
        cs.setFont(font);
        return cs;
    }

    // ─────────────────────────── Helpers cell ───────────────────────────

    private String getStringCell(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getDateCellValue().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(ISO_FMT);
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
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

    private boolean isRowEmpty(Row row, HeaderLayout layout) {
        if (row == null) return true;
        if (!isCellBlank(row.getCell(COL_TYPE))) return false;
        if (!isCellBlank(row.getCell(layout.amountCol))) return false;
        for (Integer c : layout.dateColumns.keySet()) {
            if (!isCellBlank(row.getCell(c))) return false;
        }
        return true;
    }

    private boolean isCellBlank(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return true;
        String s = getStringCell(cell);
        return s == null || s.isBlank();
    }

    private boolean equalsIgnoreCaseTrim(String a, String b) {
        return a != null && a.trim().equalsIgnoreCase(b);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(ES_AR) + s.substring(1);
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    /** Layout descubierto al leer los encabezados del Excel a importar. */
    private record HeaderLayout(int amountCol, int quantityCol, int supplierCol, int statusCol,
                                Map<Integer, LocalDate> dateColumns) { }

    /** Excepción interna para parseo fila a fila. No escapa al cliente. */
    private static class RowParseException extends RuntimeException {
        final String code;
        RowParseException(String code, String msg) { super(msg); this.code = code; }
    }
}
