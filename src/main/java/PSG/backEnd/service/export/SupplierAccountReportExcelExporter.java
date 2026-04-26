package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountMovementDTO;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountReportDTO;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountReportSupplierGroupDTO;
import PSG.backEnd.model.enums.report.SupplierAccountMovementType;
import PSG.backEnd.model.enums.report.SupplierAccountStatus;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Excel exporter for the Supplier Current-Account Report.
 *
 * <p>Generates two sheets:
 * <ol>
 *     <li><b>Resumen por Proveedor</b> — one row per supplier with previous balance,
 *     period debits, payments, credit notes, final balance and status.</li>
 *     <li><b>Detalle de Movimientos</b> — chronological timeline grouped by supplier,
 *     with a "saldo anterior" header row and a "saldo final" subtotal row per supplier.</li>
 * </ol>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SupplierAccountReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Standard colors (mirrored across all reports for visual consistency)
    private static final Color PENDING_RGB = new Color(243, 156, 18);   // orange
    private static final Color SETTLED_RGB = new Color(120, 180, 120);  // light green
    private static final Color SUPPLIER_HEADER_RGB = new Color(189, 215, 238); // light blue

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SupplierAccountReportDTO report) {
        log.info("Exporting supplier-account report to Excel: {} suppliers", report.supplierCount());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle subtotalStyle = createSubtotalStyle(workbook);
            CellStyle subtotalLabelStyle = createSubtotalLabelStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle pendingStatusStyle = createStatusStyle(workbook, PENDING_RGB);
            CellStyle settledStatusStyle = createStatusStyle(workbook, SETTLED_RGB);
            CellStyle supplierGroupStyle = createSupplierGroupStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            createSummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleStyle, pendingStatusStyle, settledStatusStyle);

            createMovementDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    subtotalStyle, subtotalLabelStyle, titleStyle, supplierGroupStyle, dateStyle,
                    pendingStatusStyle, settledStatusStyle);

            workbook.write(baos);
            log.info("Supplier-account Excel report generated successfully");
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating supplier-account Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Sheet 1: Resumen por Proveedor
    // ════════════════════════════════════════════════════════════════════════

    private void createSummarySheet(XSSFWorkbook workbook, SupplierAccountReportDTO report,
                                    CellStyle headerStyle, CellStyle currencyStyle,
                                    CellStyle totalStyle, CellStyle totalLabelStyle,
                                    CellStyle titleStyle,
                                    CellStyle pendingStatusStyle, CellStyle settledStatusStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Proveedor");
        int rowNum = addSheetHeader(sheet, "REPORTE CTA. CTE. PROVEEDORES - RESUMEN", report, titleStyle, 9);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Proveedor (Razón Social)", headerStyle);
        setCellWithStyle(headerRow, 1, "CUIT", headerStyle);
        setCellWithStyle(headerRow, 2, "Saldo Anterior", headerStyle);
        setCellWithStyle(headerRow, 3, "Débitos Período", headerStyle);
        setCellWithStyle(headerRow, 4, "Pagos", headerStyle);
        setCellWithStyle(headerRow, 5, "NC Aplicadas", headerStyle);
        setCellWithStyle(headerRow, 6, "Saldo Final", headerStyle);
        setCellWithStyle(headerRow, 7, "Estado", headerStyle);
        setCellWithStyle(headerRow, 8, "Cant. Mov.", headerStyle);

        for (SupplierAccountReportSupplierGroupDTO group : report.supplierGroups()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(group.supplierLegalName());
            row.createCell(1).setCellValue(group.supplierCuit() != null ? group.supplierCuit() : "-");

            createCurrencyCell(row, 2, group.previousBalance(), currencyStyle);
            createCurrencyCell(row, 3, group.totalDebited(), currencyStyle);
            createCurrencyCell(row, 4, group.totalPaid(), currencyStyle);
            createCurrencyCell(row, 5, group.totalCreditNotes(), currencyStyle);
            createCurrencyCell(row, 6, group.finalBalance(), currencyStyle);

            Cell statusCell = row.createCell(7);
            statusCell.setCellValue(group.status() == SupplierAccountStatus.PENDIENTE ? "PENDIENTE" : "CANCELADO");
            statusCell.setCellStyle(group.status() == SupplierAccountStatus.PENDIENTE
                    ? pendingStatusStyle : settledStatusStyle);

            row.createCell(8).setCellValue(group.movementCount());
        }

        // Grand total row
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        totalRow.createCell(1).setCellStyle(totalLabelStyle);
        createCurrencyCell(totalRow, 2, report.totalPreviousBalance(), totalStyle);
        createCurrencyCell(totalRow, 3, report.totalDebited(), totalStyle);
        createCurrencyCell(totalRow, 4, nullSafe(sumPayments(report)), totalStyle);
        createCurrencyCell(totalRow, 5, nullSafe(sumCreditNotes(report)), totalStyle);
        createCurrencyCell(totalRow, 6, report.totalPendingBalance(), totalStyle);
        Cell countLabel = totalRow.createCell(7);
        countLabel.setCellValue(report.supplierCount() + " prov.");
        countLabel.setCellStyle(totalLabelStyle);
        totalRow.createCell(8).setCellStyle(totalLabelStyle);

        for (int i = 0; i < 9; i++) sheet.autoSizeColumn(i);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Sheet 2: Detalle de Movimientos
    // ════════════════════════════════════════════════════════════════════════

    private void createMovementDetailSheet(XSSFWorkbook workbook, SupplierAccountReportDTO report,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle subtotalStyle, CellStyle subtotalLabelStyle,
                                           CellStyle titleStyle, CellStyle supplierGroupStyle,
                                           CellStyle dateStyle,
                                           CellStyle pendingStatusStyle, CellStyle settledStatusStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Movimientos");
        int rowNum = addSheetHeader(sheet, "REPORTE CTA. CTE. PROVEEDORES - MOVIMIENTOS", report, titleStyle, 8);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 1, "Tipo", headerStyle);
        setCellWithStyle(headerRow, 2, "Referencia", headerStyle);
        setCellWithStyle(headerRow, 3, "Concepto", headerStyle);
        setCellWithStyle(headerRow, 4, "Débito", headerStyle);
        setCellWithStyle(headerRow, 5, "Crédito", headerStyle);
        setCellWithStyle(headerRow, 6, "Saldo Acumulado", headerStyle);
        setCellWithStyle(headerRow, 7, "M. Pago", headerStyle);

        for (SupplierAccountReportSupplierGroupDTO group : report.supplierGroups()) {
            // Supplier group banner row
            int groupRowNum = rowNum;
            Row groupRow = sheet.createRow(rowNum++);
            String groupLabel = group.supplierLegalName()
                    + (group.supplierCuit() != null ? " — CUIT " + group.supplierCuit() : "");
            Cell groupCell = groupRow.createCell(0);
            groupCell.setCellValue(groupLabel);
            groupCell.setCellStyle(supplierGroupStyle);
            for (int i = 1; i < 8; i++) {
                groupRow.createCell(i).setCellStyle(supplierGroupStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(groupRowNum, groupRowNum, 0, 7));

            // Previous balance row
            Row prevRow = sheet.createRow(rowNum++);
            Cell prevLabel = prevRow.createCell(0);
            prevLabel.setCellValue("Saldo anterior al " + report.filters().startDate().format(DATE_FORMATTER));
            prevLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 5; i++) prevRow.createCell(i).setCellStyle(subtotalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));
            createCurrencyCell(prevRow, 6, group.previousBalance(), subtotalStyle);
            prevRow.createCell(7).setCellStyle(subtotalLabelStyle);

            // Movements
            for (SupplierAccountMovementDTO m : group.movements()) {
                Row row = sheet.createRow(rowNum++);
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(m.date().format(DATE_FORMATTER));
                dateCell.setCellStyle(dateStyle);
                row.createCell(1).setCellValue(movementTypeLabel(m.type()));
                row.createCell(2).setCellValue(m.reference() != null ? m.reference() : "");
                row.createCell(3).setCellValue(m.description() != null ? m.description() : "");
                createCurrencyCell(row, 4, m.debit(), currencyStyle);
                createCurrencyCell(row, 5, m.credit(), currencyStyle);
                createCurrencyCell(row, 6, m.accumulatedBalance(), currencyStyle);
                row.createCell(7).setCellValue(m.paymentMethod() != null ? m.paymentMethod() : "");
            }

            // Final balance row
            Row finalRow = sheet.createRow(rowNum++);
            Cell finalLabel = finalRow.createCell(0);
            finalLabel.setCellValue("Saldo final al " + report.filters().endDate().format(DATE_FORMATTER));
            CellStyle statusStyle = group.status() == SupplierAccountStatus.PENDIENTE
                    ? pendingStatusStyle : settledStatusStyle;
            finalLabel.setCellStyle(statusStyle);
            for (int i = 1; i <= 5; i++) finalRow.createCell(i).setCellStyle(statusStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));
            createCurrencyCell(finalRow, 6, group.finalBalance(), statusStyle);
            Cell statusTag = finalRow.createCell(7);
            statusTag.setCellValue(group.status() == SupplierAccountStatus.PENDIENTE ? "PENDIENTE" : "CANCELADO");
            statusTag.setCellStyle(statusStyle);

            rowNum++;
        }

        for (int i = 0; i < 8; i++) sheet.autoSizeColumn(i);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private String movementTypeLabel(SupplierAccountMovementType type) {
        return switch (type) {
            case INVOICE -> "Factura";
            case DEBIT_NOTE -> "Nota de Débito";
            case CREDIT_NOTE -> "Nota de Crédito";
            case PAYMENT -> "Pago";
        };
    }

    private BigDecimal sumPayments(SupplierAccountReportDTO report) {
        return report.supplierGroups().stream()
                .map(g -> g.totalPaid() != null ? g.totalPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCreditNotes(SupplierAccountReportDTO report) {
        return report.supplierGroups().stream()
                .map(g -> g.totalCreditNotes() != null ? g.totalCreditNotes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nullSafe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private void createCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private int addSheetHeader(Sheet sheet, String title, SupplierAccountReportDTO report,
                               CellStyle titleCellStyle, int mergeColCount) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleCellStyle);
        if (mergeColCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeColCount - 1));
        }
        sheet.createRow(rowNum++).createCell(0).setCellValue("ESEA S.A.");
        rowNum++;
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Período:");
        periodRow.createCell(1).setCellValue(report.periodDescription());
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Fecha generación:");
        dateRow.createCell(1).setCellValue(report.generatedAt().format(DATETIME_FORMATTER));
        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Proveedores:");
        countRow.createCell(1).setCellValue(report.supplierCount() + " (" + report.pendingSupplierCount()
                + " pendientes / " + report.settledSupplierCount() + " cancelados)");
        rowNum++;
        return rowNum;
    }

    private void setCellWithStyle(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ── Styles ──────────────────────────────────────────────────────────────

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

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

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

    private CellStyle createSubtotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSubtotalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createStatusStyle(XSSFWorkbook workbook, Color rgb) {
        XSSFCellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSupplierGroupStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(SUPPLIER_HEADER_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }
}
