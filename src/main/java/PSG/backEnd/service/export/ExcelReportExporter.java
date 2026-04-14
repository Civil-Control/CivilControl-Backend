package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel Report Exporter using Apache POI library.
 * Generates professional Excel reports with multiple sheets, formatting, and formulas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExcelReportExporter implements IReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final MessageSourceHelper messageSourceHelper;

    @Override
    public byte[] export(MoneyOutflowReportDTO report) {
        log.info("Exporting report to Excel with {} items", report.totalCount());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);

            // Create sheets
            createDataSheet(workbook, report, headerStyle, dateStyle, currencyStyle, totalStyle, totalLabelStyle);
            createSummarySheet(workbook, report, headerStyle, currencyStyle, totalStyle, totalLabelStyle);

            workbook.write(baos);

            log.info("Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new ReportGenerationException(messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    /**
     * Creates the main data sheet with all items.
     */
    private void createDataSheet(Workbook workbook, MoneyOutflowReportDTO report,
                                  CellStyle headerStyle, CellStyle dateStyle,
                                  CellStyle currencyStyle, CellStyle totalStyle,
                                  CellStyle totalLabelStyle) {
        Sheet sheet = workbook.createSheet("Datos");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE SALIDAS DE DINERO - ESEA S.A.");
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        rowNum++; // Empty row

        // Metadata
        addMetadataToSheet(sheet, report, rowNum);
        rowNum += 5;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Fecha", "Categoría", "Área", "Descripción", "Beneficiario", "Método Pago", "Referencia", "Monto"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (ReportItemDTO item : report.items()) {
            Row row = sheet.createRow(rowNum++);

            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(item.date().format(DATE_FORMATTER));
            dateCell.setCellStyle(dateStyle);

            row.createCell(1).setCellValue(item.category().getDisplayName());
            row.createCell(2).setCellValue(item.projectAreaName() != null ? item.projectAreaName() : "");
            row.createCell(3).setCellValue(item.description() != null ? item.description() : "");
            row.createCell(4).setCellValue(item.beneficiary() != null ? item.beneficiary() : "");
            row.createCell(5).setCellValue(item.paymentMethod() != null ? item.paymentMethod() : "");
            row.createCell(6).setCellValue(item.reference() != null ? item.reference() : "");

            Cell amountCell = row.createCell(7);
            amountCell.setCellValue(item.amount().doubleValue());
            amountCell.setCellStyle(currencyStyle);
        }

        // Total row
        rowNum++; // blank row before total
        Row totalRow = sheet.createRow(rowNum++);
        // Merge label across first columns
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("TOTAL GENERAL");
        totalLabelCell.setCellStyle(totalLabelStyle);
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 6));
        // Apply style to merged cells
        for (int i = 1; i <= 6; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }

        Cell totalValueCell = totalRow.createCell(7);
        totalValueCell.setCellValue(report.totalAmount().doubleValue());
        totalValueCell.setCellStyle(totalStyle);

        // Adjusted total row when there are duplicated amounts
        if (report.duplicatedAmount() != null && report.duplicatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            Row dupRow = sheet.createRow(rowNum++);
            Cell dupLabelCell = dupRow.createCell(0);
            dupLabelCell.setCellValue("VINCULADO");
            sheet.addMergedRegion(new CellRangeAddress(dupRow.getRowNum(), dupRow.getRowNum(), 0, 6));
            Cell dupValueCell = dupRow.createCell(7);
            dupValueCell.setCellValue(report.duplicatedAmount().negate().doubleValue());
            dupValueCell.setCellStyle(currencyStyle);

            BigDecimal adjustedTotal = report.totalAmount().subtract(report.duplicatedAmount());
            Row adjRow = sheet.createRow(rowNum++);
            Cell adjLabelCell = adjRow.createCell(0);
            adjLabelCell.setCellValue("TOTAL AJUSTADO");
            adjLabelCell.setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(adjRow.getRowNum(), adjRow.getRowNum(), 0, 6));
            for (int i = 1; i <= 6; i++) {
                adjRow.createCell(i).setCellStyle(totalLabelStyle);
            }
            Cell adjValueCell = adjRow.createCell(7);
            adjValueCell.setCellValue(adjustedTotal.doubleValue());
            adjValueCell.setCellStyle(totalStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Creates the summary sheet with category totals.
     */
    private void createSummarySheet(Workbook workbook, MoneyOutflowReportDTO report,
                                    CellStyle headerStyle, CellStyle currencyStyle,
                                    CellStyle totalStyle, CellStyle totalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RESUMEN POR CATEGORÍA");
        CellStyle titleStyleLocal = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyleLocal.setFont(titleFont);
        titleCell.setCellStyle(titleStyleLocal);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        rowNum += 2;

        // Metadata
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Período:");
        periodRow.createCell(1).setCellValue(report.periodDescription());

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Fecha generación:");
        dateRow.createCell(1).setCellValue(report.generatedAt().format(DATETIME_FORMATTER));

        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Total registros:");
        countRow.createCell(1).setCellValue(report.totalCount());

        // Add project area if filtered
        if (report.projectAreaName() != null) {
            Row projectAreaRow = sheet.createRow(rowNum++);
            projectAreaRow.createCell(0).setCellValue("Sector:");
            projectAreaRow.createCell(1).setCellValue(report.projectAreaName());
        }

        rowNum += 2;

        // Summary table headers
        Row headerRow = sheet.createRow(rowNum++);
        Cell catHeaderCell = headerRow.createCell(0);
        catHeaderCell.setCellValue("Categoría");
        catHeaderCell.setCellStyle(headerStyle);

        Cell countHeaderCell = headerRow.createCell(1);
        countHeaderCell.setCellValue("Cantidad");
        countHeaderCell.setCellStyle(headerStyle);

        Cell amountHeaderCell = headerRow.createCell(2);
        amountHeaderCell.setCellValue("Total");
        amountHeaderCell.setCellStyle(headerStyle);

        Cell percentHeaderCell = headerRow.createCell(3);
        percentHeaderCell.setCellValue("% del Total");
        percentHeaderCell.setCellStyle(headerStyle);

        // Summary data rows
        for (Map.Entry<MoneyOutflowCategory, BigDecimal> entry : report.summaryByCategory().entrySet()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(entry.getKey().getDisplayName());

            // Count items in this category
            long count = report.items().stream()
                    .filter(item -> item.category() == entry.getKey())
                    .count();
            row.createCell(1).setCellValue(count);

            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(entry.getValue().doubleValue());
            amountCell.setCellStyle(currencyStyle);

            // Percentage
            double percentage = entry.getValue()
                    .divide(report.totalAmount(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            Cell percentCell = row.createCell(3);
            percentCell.setCellValue(percentage);
            CellStyle percentStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            percentStyle.setDataFormat(format.getFormat("0.00%"));
            percentCell.setCellStyle(percentStyle);
        }

        rowNum++;

        // Grand total
        Row totalRow = sheet.createRow(rowNum++);
        Cell grandTotalLabelCell = totalRow.createCell(0);
        grandTotalLabelCell.setCellValue("TOTAL GENERAL");
        grandTotalLabelCell.setCellStyle(totalLabelStyle);

        Cell totalCountCell = totalRow.createCell(1);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);

        Cell totalValueCell = totalRow.createCell(2);
        totalValueCell.setCellValue(report.totalAmount().doubleValue());
        totalValueCell.setCellStyle(totalStyle);

        Cell totalPercentCell = totalRow.createCell(3);
        totalPercentCell.setCellValue(1.0);
        CellStyle percentStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        percentStyle.setDataFormat(format.getFormat("0.00%"));
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        percentStyle.setFont(boldFont);
        totalPercentCell.setCellStyle(percentStyle);

        // Adjusted total row when there are duplicated amounts
        if (report.duplicatedAmount() != null && report.duplicatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            rowNum++;
            Row dupRow = sheet.createRow(rowNum++);
            dupRow.createCell(0).setCellValue("MONTO VINCULADO (incluido en facturas):");
            Cell dupValueCell = dupRow.createCell(2);
            dupValueCell.setCellValue(report.duplicatedAmount().negate().doubleValue());
            dupValueCell.setCellStyle(currencyStyle);

            BigDecimal adjustedTotal = report.totalAmount().subtract(report.duplicatedAmount());
            Row adjRow = sheet.createRow(rowNum++);
            Cell adjLabelCell = adjRow.createCell(0);
            adjLabelCell.setCellValue("TOTAL AJUSTADO:");
            adjLabelCell.setCellStyle(totalStyle);
            Cell adjValueCell = adjRow.createCell(2);
            adjValueCell.setCellValue(adjustedTotal.doubleValue());
            adjValueCell.setCellStyle(totalStyle);
        }

        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Adds metadata information to the sheet.
     */
    private void addMetadataToSheet(Sheet sheet, MoneyOutflowReportDTO report, int startRow) {
        int currentRow = startRow;

        Row periodRow = sheet.createRow(currentRow++);
        periodRow.createCell(0).setCellValue("Período:");
        periodRow.createCell(1).setCellValue(report.periodDescription());

        Row dateRow = sheet.createRow(currentRow++);
        dateRow.createCell(0).setCellValue("Fecha generación:");
        dateRow.createCell(1).setCellValue(report.generatedAt().format(DATETIME_FORMATTER));

        Row countRow = sheet.createRow(currentRow++);
        countRow.createCell(0).setCellValue("Total registros:");
        countRow.createCell(1).setCellValue(report.totalCount());

        // Add project area if filtered
        if (report.projectAreaName() != null) {
            Row projectAreaRow = sheet.createRow(currentRow++);
            projectAreaRow.createCell(0).setCellValue("Sector:");
            projectAreaRow.createCell(1).setCellValue(report.projectAreaName());
        }

        if (report.filters().categories() != null && !report.filters().categories().isEmpty()) {
            Row categoryRow = sheet.createRow(currentRow++);
            categoryRow.createCell(0).setCellValue("Categorías:");
            String categoriesStr = report.filters().categories().stream()
                    .map(MoneyOutflowCategory::getDisplayName)
                    .collect(Collectors.joining(", "));
            categoryRow.createCell(1).setCellValue(categoriesStr);
        }
    }

    /**
     * Creates header cell style.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
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

    /**
     * Creates date cell style.
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Creates currency cell style.
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * Creates total cell style.
     */
    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.DOUBLE);
        return style;
    }

    /**
     * Creates total label cell style (left-aligned, no currency format).
     */
    private CellStyle createTotalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.DOUBLE);
        return style;
    }

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.EXCEL;
    }

    @Override
    public String getContentType() {
        return ReportFormat.EXCEL.getContentType();
    }

    @Override
    public String getFileExtension() {
        return ReportFormat.EXCEL.getFileExtension();
    }
}

