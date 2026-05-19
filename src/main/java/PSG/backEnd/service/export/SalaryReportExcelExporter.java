package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.salary.*;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
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
import java.util.List;
import java.util.Map;

/**
 * Dedicated Excel exporter for salary reports.
 * Generates a workbook with 3 sheets:
 * 1. "Resumen por Área" - One row per area with totals and frequency subtotals
 * 2. "Resumen por Empleado" - One row per employee with area, totals, and frequency subtotals
 * 3. "Detalle de Pagos" - One row per payment with full detail
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SalaryReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<SalaryFrecuency> FREQUENCY_ORDER = List.of(
            SalaryFrecuency.MENSUAL, SalaryFrecuency.QUINCENAL, SalaryFrecuency.SEMANAL);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SalaryReportDTO report) {
        log.info("Exporting salary report to Excel: {} areas, {} total payments",
                report.areaGroups().size(), report.totalCount());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle titleCellStyle = createTitleStyle(workbook);
            CellStyle subtotalStyle = createSubtotalStyle(workbook);
            CellStyle subtotalLabelStyle = createSubtotalLabelStyle(workbook);

            // Determine which frequencies exist in the report
            List<SalaryFrecuency> activeFrequencies = FREQUENCY_ORDER.stream()
                    .filter(f -> report.totalsByFrequency().containsKey(f))
                    .toList();

            createAreaSummarySheet(workbook, report, activeFrequencies,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle);
            createEmployeeSummarySheet(workbook, report, activeFrequencies,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);
            createPaymentDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);

            workbook.write(baos);

            log.info("Salary Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating salary Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, SalaryReportDTO report,
                                         List<SalaryFrecuency> activeFrequencies,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleCellStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        // Title
        rowNum = addSheetHeader(sheet, "REPORTE DE SALARIOS - RESUMEN POR ÁREA", report, titleCellStyle,
                3 + activeFrequencies.size());
        rowNum++; // blank row

        // Headers: Área | Cant. Pagos | [freq columns...] | Total
        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Área", headerStyle);
        setCellWithStyle(headerRow, col++, "Cant. Pagos", headerStyle);
        for (SalaryFrecuency freq : activeFrequencies) {
            setCellWithStyle(headerRow, col++, "Total " + freq.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        // Data rows
        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(area.projectAreaName());
            row.createCell(c++).setCellValue(area.paymentCount());
            for (SalaryFrecuency freq : activeFrequencies) {
                Cell cell = row.createCell(c++);
                BigDecimal val = area.subtotalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(currencyStyle);
            }
            Cell totalCell = row.createCell(c);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
        }

        // Grand total row
        rowNum++; // blank
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(tc++);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        for (SalaryFrecuency freq : activeFrequencies) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        // Auto-size
        for (int i = 0; i <= tc; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Empleado
    // ═══════════════════════════════════════════════════════════════════════

    private void createEmployeeSummarySheet(Workbook workbook, SalaryReportDTO report,
                                             List<SalaryFrecuency> activeFrequencies,
                                             CellStyle headerStyle, CellStyle currencyStyle,
                                             CellStyle totalStyle, CellStyle totalLabelStyle,
                                             CellStyle titleCellStyle, CellStyle subtotalStyle,
                                             CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Empleado");
        int rowNum = 0;

        // Title
        int totalCols = 2 + activeFrequencies.size(); // Empleado, [freqs...], Total
        rowNum = addSheetHeader(sheet, "REPORTE DE SALARIOS - RESUMEN POR EMPLEADO", report, titleCellStyle,
                totalCols);
        rowNum++;

        // Headers: Empleado | [freq columns...] | Total
        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Empleado", headerStyle);
        for (SalaryFrecuency freq : activeFrequencies) {
            setCellWithStyle(headerRow, col++, "Total " + freq.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        // Area group style (bold, colored background)
        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        // Data rows grouped by area
        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            // Area header row
            int areaHeaderRowNum = rowNum;
            Row areaRow = sheet.createRow(rowNum++);
            Cell areaCell = areaRow.createCell(0);
            areaCell.setCellValue(area.projectAreaName());
            areaCell.setCellStyle(areaHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                areaRow.createCell(i).setCellStyle(areaHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaHeaderRowNum, areaHeaderRowNum, 0, totalCols - 1));

            // Employee rows (already sorted by backend)
            for (SalaryReportEmployeeGroupDTO emp : area.employeeGroups()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(emp.employeeLastName() + ", " + emp.employeeName());
                for (SalaryFrecuency freq : activeFrequencies) {
                    Cell cell = row.createCell(c++);
                    BigDecimal val = emp.subtotalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
                    cell.setCellValue(val.doubleValue());
                    cell.setCellStyle(currencyStyle);
                }
                Cell totalCell = row.createCell(c);
                totalCell.setCellValue(emp.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Area subtotal row
            int subtotalRowNum = rowNum;
            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + area.projectAreaName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            int sc = 1;
            for (SalaryFrecuency freq : activeFrequencies) {
                Cell cell = subtotalRow.createCell(sc++);
                BigDecimal val = area.subtotalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(subtotalStyle);
            }
            Cell subtotalAmount = subtotalRow.createCell(sc);
            subtotalAmount.setCellValue(area.subtotalAmount().doubleValue());
            subtotalAmount.setCellStyle(subtotalStyle);

            rowNum++; // blank row between areas
        }

        // Grand total row
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        for (SalaryFrecuency freq : activeFrequencies) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        // Auto-size
        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void createPaymentDetailSheet(Workbook workbook, SalaryReportDTO report,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle dateStyle,
                                           CellStyle titleCellStyle, CellStyle subtotalStyle,
                                           CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Pagos");
        int rowNum = 0;

        // Title
        rowNum = addSheetHeader(sheet, "REPORTE DE SALARIOS - DETALLE DE PAGOS", report, titleCellStyle, 6);
        rowNum++;

        // Headers: Área | Apellido y Nombre | Fecha | Frecuencia | Método de Pago | Monto
        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Apellido y Nombre", headerStyle);
        setCellWithStyle(headerRow, 2, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 3, "Frecuencia", headerStyle);
        setCellWithStyle(headerRow, 4, "Método de Pago", headerStyle);
        setCellWithStyle(headerRow, 5, "Monto", headerStyle);

        // Data rows - sorted by area, then employee (alpha), then frequency order, then date
        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            for (SalaryReportEmployeeGroupDTO emp : area.employeeGroups()) {
                for (SalaryReportPaymentDTO payment : emp.payments()) {
                    Row row = sheet.createRow(rowNum++);
                    String areaDisplay = area.projectAreaName();
                    if (payment.projectAreaTaskName() != null) {
                        areaDisplay += " - " + payment.projectAreaTaskName();
                    }
                    row.createCell(0).setCellValue(areaDisplay);
                    row.createCell(1).setCellValue(emp.employeeLastName() + ", " + emp.employeeName());

                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(payment.paymentDate().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    row.createCell(3).setCellValue(payment.salaryFrequency().getDisplayName());
                    row.createCell(4).setCellValue(
                            payment.paymentMethod() != null ? payment.paymentMethod().getDisplayName() : "-");

                    Cell amountCell = row.createCell(5);
                    amountCell.setCellValue((payment.totalWithIva() != null ? payment.totalWithIva() : payment.amount()).doubleValue());
                    amountCell.setCellStyle(currencyStyle);
                }

                // Employee subtotal row
                int empSubtotalRowNum = rowNum;
                Row empSubtotalRow = sheet.createRow(rowNum++);
                Cell empLabel = empSubtotalRow.createCell(0);
                empLabel.setCellValue("Subtotal " + emp.employeeLastName() + ", " + emp.employeeName());
                empLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 4; i++) {
                    empSubtotalRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(empSubtotalRowNum, empSubtotalRowNum, 0, 4));
                Cell empTotal = empSubtotalRow.createCell(5);
                empTotal.setCellValue(emp.totalAmount().doubleValue());
                empTotal.setCellStyle(subtotalStyle);
            }

            // Area subtotal row
            int areaSubtotalRowNum = rowNum;
            Row areaSubtotalRow = sheet.createRow(rowNum++);
            Cell areaLabel = areaSubtotalRow.createCell(0);
            areaLabel.setCellValue("Subtotal " + area.projectAreaName() + " (" + area.paymentCount() + " pagos)");
            areaLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 4; i++) {
                areaSubtotalRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaSubtotalRowNum, areaSubtotalRowNum, 0, 4));
            Cell areaTotal = areaSubtotalRow.createCell(5);
            areaTotal.setCellValue(area.subtotalAmount().doubleValue());
            areaTotal.setCellStyle(subtotalStyle);

            rowNum++; // blank row between areas
        }

        // Grand total
        int grandTotalRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " pagos)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 4; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(grandTotalRowNum, grandTotalRowNum, 0, 4));
        Cell grandTotal = totalRow.createCell(5);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        // Auto-size
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, SalaryReportDTO report,
                                CellStyle titleCellStyle, int mergeColCount) {
        int rowNum = 0;

        // Title row
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleCellStyle);
        if (mergeColCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeColCount - 1));
        }

        // Subtitle (company)
        Row companyRow = sheet.createRow(rowNum++);
        companyRow.createCell(0).setCellValue("ESEA S.A.");

        rowNum++; // blank

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

        rowNum++; // blank

        return rowNum;
    }

    private void setCellWithStyle(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

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
}
