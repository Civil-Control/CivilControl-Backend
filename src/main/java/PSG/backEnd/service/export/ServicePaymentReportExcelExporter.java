package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.servicePayment.*;
import PSG.backEnd.model.enums.ServiceType;
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
 * Dedicated Excel exporter for service payment reports.
 * Generates a workbook with 3 sheets:
 * 1. "Resumen por Área" - One row per area with totals and service type subtotals
 * 2. "Resumen por Edificio" - One row per building grouped by area
 * 3. "Detalle de Pagos" - One row per payment with full detail
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServicePaymentReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<ServiceType> SERVICE_TYPE_ORDER = List.of(
            ServiceType.LUZ, ServiceType.AGUA, ServiceType.GAS, ServiceType.INTERNET,
            ServiceType.TELEFONIA, ServiceType.MUNICIPALES, ServiceType.PROVINCIALES,
            ServiceType.NACIONALES, ServiceType.OTRO);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(ServicePaymentReportDTO report) {
        log.info("Exporting service payment report to Excel: {} areas, {} total payments",
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

            // Determine which service types exist in the report
            List<ServiceType> activeServiceTypes = SERVICE_TYPE_ORDER.stream()
                    .filter(st -> report.totalsByServiceType().containsKey(st.name()))
                    .toList();

            createAreaSummarySheet(workbook, report, activeServiceTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle);
            createBuildingSummarySheet(workbook, report, activeServiceTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);
            createPaymentDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);

            workbook.write(baos);

            log.info("Service payment Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating service payment Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, ServicePaymentReportDTO report,
                                         List<ServiceType> activeServiceTypes,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleCellStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        rowNum = addSheetHeader(sheet, "REPORTE DE PAGO DE SERVICIOS - RESUMEN POR ÁREA", report, titleCellStyle,
                3 + activeServiceTypes.size());
        rowNum++;

        // Headers: Área | Cant. Pagos | [service type columns...] | Total
        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Área", headerStyle);
        setCellWithStyle(headerRow, col++, "Cant. Pagos", headerStyle);
        for (ServiceType st : activeServiceTypes) {
            setCellWithStyle(headerRow, col++, "Total " + st.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        // Data rows
        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(area.projectAreaName());
            row.createCell(c++).setCellValue(area.paymentCount());
            for (ServiceType st : activeServiceTypes) {
                Cell cell = row.createCell(c++);
                BigDecimal val = area.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(currencyStyle);
            }
            Cell totalCell = row.createCell(c);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
        }

        // Grand total row
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(tc++);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        for (ServiceType st : activeServiceTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        for (int i = 0; i <= tc; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Edificio
    // ═══════════════════════════════════════════════════════════════════════

    private void createBuildingSummarySheet(Workbook workbook, ServicePaymentReportDTO report,
                                             List<ServiceType> activeServiceTypes,
                                             CellStyle headerStyle, CellStyle currencyStyle,
                                             CellStyle totalStyle, CellStyle totalLabelStyle,
                                             CellStyle titleCellStyle, CellStyle subtotalStyle,
                                             CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Edificio");
        int rowNum = 0;

        int totalCols = 2 + activeServiceTypes.size(); // Edificio, [serviceTypes...], Total
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGO DE SERVICIOS - RESUMEN POR EDIFICIO", report, titleCellStyle,
                totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Edificio", headerStyle);
        for (ServiceType st : activeServiceTypes) {
            setCellWithStyle(headerRow, col++, "Total " + st.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        // Area group style
        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
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

            for (ServicePaymentReportBuildingGroupDTO building : area.buildingGroups()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(building.buildingName());
                for (ServiceType st : activeServiceTypes) {
                    Cell cell = row.createCell(c++);
                    BigDecimal val = building.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
                    cell.setCellValue(val.doubleValue());
                    cell.setCellStyle(currencyStyle);
                }
                Cell totalCell = row.createCell(c);
                totalCell.setCellValue(building.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Area subtotal row
            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + area.projectAreaName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            int sc = 1;
            for (ServiceType st : activeServiceTypes) {
                Cell cell = subtotalRow.createCell(sc++);
                BigDecimal val = area.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(subtotalStyle);
            }
            Cell subtotalAmount = subtotalRow.createCell(sc);
            subtotalAmount.setCellValue(area.subtotalAmount().doubleValue());
            subtotalAmount.setCellStyle(subtotalStyle);

            rowNum++;
        }

        // Grand total row
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        for (ServiceType st : activeServiceTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void createPaymentDetailSheet(Workbook workbook, ServicePaymentReportDTO report,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle dateStyle,
                                           CellStyle titleCellStyle, CellStyle subtotalStyle,
                                           CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Pagos");
        int rowNum = 0;

        int detailCols = 9;
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGO DE SERVICIOS - DETALLE DE PAGOS", report, titleCellStyle, detailCols);
        rowNum++;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Edificio", headerStyle);
        setCellWithStyle(headerRow, 2, "Proveedor", headerStyle);
        setCellWithStyle(headerRow, 3, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 4, "Tipo Servicio", headerStyle);
        setCellWithStyle(headerRow, 5, "Año/Período", headerStyle);
        setCellWithStyle(headerRow, 6, "Referencia", headerStyle);
        setCellWithStyle(headerRow, 7, "Método Pago", headerStyle);
        setCellWithStyle(headerRow, 8, "Monto", headerStyle);

        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
            for (ServicePaymentReportBuildingGroupDTO building : area.buildingGroups()) {
                for (ServicePaymentReportItemDTO payment : building.payments()) {
                    Row row = sheet.createRow(rowNum++);
                    String areaDisplay = area.projectAreaName();
                    if (payment.projectAreaTaskName() != null) {
                        areaDisplay += " - " + payment.projectAreaTaskName();
                    }
                    row.createCell(0).setCellValue(areaDisplay);
                    row.createCell(1).setCellValue(building.buildingName());
                    row.createCell(2).setCellValue(payment.supplierName());

                    Cell dateCell = row.createCell(3);
                    dateCell.setCellValue(payment.paymentDate().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    row.createCell(4).setCellValue(getServiceTypeDisplayName(payment.serviceType()));
                    row.createCell(5).setCellValue(formatYearPeriod(payment.year(), payment.period()));
                    row.createCell(6).setCellValue(payment.referenceNumber() != null ? payment.referenceNumber() : "-");
                    row.createCell(7).setCellValue(
                            payment.paymentMethod() != null ? payment.paymentMethod().getDisplayName() : "-");

                    Cell amountCell = row.createCell(8);
                    amountCell.setCellValue(payment.amount().doubleValue());
                    amountCell.setCellStyle(currencyStyle);
                }

                // Building subtotal row
                int bSubRowNum = rowNum;
                Row bSubRow = sheet.createRow(rowNum++);
                Cell bLabel = bSubRow.createCell(0);
                bLabel.setCellValue("Subtotal " + building.buildingName());
                bLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 7; i++) {
                    bSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(bSubRowNum, bSubRowNum, 0, 7));
                Cell bTotal = bSubRow.createCell(8);
                bTotal.setCellValue(building.totalAmount().doubleValue());
                bTotal.setCellStyle(subtotalStyle);
            }

            // Area subtotal row
            int aSubRowNum = rowNum;
            Row aSubRow = sheet.createRow(rowNum++);
            Cell aLabel = aSubRow.createCell(0);
            aLabel.setCellValue("Subtotal " + area.projectAreaName() + " (" + area.paymentCount() + " pagos)");
            aLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 7; i++) {
                aSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(aSubRowNum, aSubRowNum, 0, 7));
            Cell aTotal = aSubRow.createCell(8);
            aTotal.setCellValue(area.subtotalAmount().doubleValue());
            aTotal.setCellStyle(subtotalStyle);

            rowNum++;
        }

        // Grand total
        int gtRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " pagos)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 7; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(gtRowNum, gtRowNum, 0, 7));
        Cell grandTotal = totalRow.createCell(8);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < detailCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, ServicePaymentReportDTO report,
                                CellStyle titleCellStyle, int mergeColCount) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleCellStyle);
        if (mergeColCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeColCount - 1));
        }

        Row companyRow = sheet.createRow(rowNum++);
        companyRow.createCell(0).setCellValue("ESEA S.A.");

        rowNum++;

        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Período:");
        periodRow.createCell(1).setCellValue(report.periodDescription());

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Fecha generación:");
        dateRow.createCell(1).setCellValue(report.generatedAt().format(DATETIME_FORMATTER));

        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Total registros:");
        countRow.createCell(1).setCellValue(report.totalCount());

        rowNum++;

        return rowNum;
    }

    private String getServiceTypeDisplayName(String serviceType) {
        try {
            return ServiceType.valueOf(serviceType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return serviceType;
        }
    }

    private String formatYearPeriod(Integer year, Integer period) {
        if (year != null && period != null) {
            return String.format("%02d/%d", period, year);
        } else if (year != null) {
            return String.valueOf(year);
        }
        return "-";
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
