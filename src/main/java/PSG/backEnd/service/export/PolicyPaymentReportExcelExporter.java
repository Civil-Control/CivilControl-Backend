package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.policyPayment.*;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class PolicyPaymentReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(PolicyPaymentReportDTO report) {
        log.info("Exporting policy payment report to Excel: {} type groups, {} total payments",
                report.typeGroups().size(), report.totalPaymentCount());

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
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle totalNumberStyle = createTotalNumberStyle(workbook);
            CellStyle subtotalNumberStyle = createSubtotalNumberStyle(workbook);

            createTypeSummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle,
                    numberStyle, totalNumberStyle);
            createPolicySummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle,
                    numberStyle, totalNumberStyle, subtotalNumberStyle);
            createPaymentDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle,
                    numberStyle, totalNumberStyle, subtotalNumberStyle);

            // Sheet 4: Vehicles (only if AUTOMOTOR policies with vehicles exist)
            boolean hasVehicles = report.typeGroups().stream()
                    .flatMap(tg -> tg.policyGroups().stream())
                    .anyMatch(pg -> pg.insuredVehicles() != null && !pg.insuredVehicles().isEmpty());
            if (hasVehicles) {
                createVehiclesSheet(workbook, report,
                        headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                        titleCellStyle, subtotalStyle, subtotalLabelStyle);
            }

            workbook.write(baos);

            log.info("Policy payment Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating policy payment Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Tipo
    // ═══════════════════════════════════════════════════════════════════════

    private void createTypeSummarySheet(Workbook workbook, PolicyPaymentReportDTO report,
                                        CellStyle headerStyle, CellStyle currencyStyle,
                                        CellStyle totalStyle, CellStyle totalLabelStyle,
                                        CellStyle titleCellStyle,
                                        CellStyle numberStyle, CellStyle totalNumberStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Tipo");
        int rowNum = 0;

        int totalCols = 5;
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGOS DE PÓLIZA - RESUMEN POR TIPO", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Tipo de Póliza", headerStyle);
        setCellWithStyle(headerRow, 1, "Cant. Pagos", headerStyle);
        setCellWithStyle(headerRow, 2, "Total Pagado ($)", headerStyle);
        setCellWithStyle(headerRow, 3, "Premio Esperado ($)", headerStyle);
        setCellWithStyle(headerRow, 4, "Diferencia ($)", headerStyle);

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(typeGroup.policyTypeName());
            Cell countCell = row.createCell(1);
            countCell.setCellValue(typeGroup.paymentCount());
            countCell.setCellStyle(numberStyle);
            Cell paidCell = row.createCell(2);
            paidCell.setCellValue(typeGroup.subtotalPaid().doubleValue());
            paidCell.setCellStyle(currencyStyle);
            Cell expectedCell = row.createCell(3);
            expectedCell.setCellValue(typeGroup.subtotalExpected().doubleValue());
            expectedCell.setCellStyle(currencyStyle);
            Cell diffCell = row.createCell(4);
            diffCell.setCellValue(typeGroup.subtotalDifference().doubleValue());
            diffCell.setCellStyle(currencyStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(1);
        totalCountCell.setCellValue(report.totalPaymentCount());
        totalCountCell.setCellStyle(totalNumberStyle);
        Cell grandPaid = totalRow.createCell(2);
        grandPaid.setCellValue(report.totalPaidAmount().doubleValue());
        grandPaid.setCellStyle(totalStyle);
        Cell grandExpected = totalRow.createCell(3);
        grandExpected.setCellValue(report.totalExpectedAmount().doubleValue());
        grandExpected.setCellStyle(totalStyle);
        Cell grandDiff = totalRow.createCell(4);
        grandDiff.setCellValue(report.totalDifference().doubleValue());
        grandDiff.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Póliza
    // ═══════════════════════════════════════════════════════════════════════

    private void createPolicySummarySheet(Workbook workbook, PolicyPaymentReportDTO report,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle titleCellStyle, CellStyle subtotalStyle,
                                           CellStyle subtotalLabelStyle,
                                           CellStyle numberStyle, CellStyle totalNumberStyle,
                                           CellStyle subtotalNumberStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Póliza");
        int rowNum = 0;

        int totalCols = 8;
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGOS DE PÓLIZA - RESUMEN POR PÓLIZA", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Póliza", headerStyle);
        setCellWithStyle(headerRow, 1, "Estado", headerStyle);
        setCellWithStyle(headerRow, 2, "Frecuencia", headerStyle);
        setCellWithStyle(headerRow, 3, "Premio Mensual ($)", headerStyle);
        setCellWithStyle(headerRow, 4, "Cant. Pagos", headerStyle);
        setCellWithStyle(headerRow, 5, "Total Pagado ($)", headerStyle);
        setCellWithStyle(headerRow, 6, "Esperado ($)", headerStyle);
        setCellWithStyle(headerRow, 7, "Diferencia ($)", headerStyle);

        CellStyle typeHeaderStyle = workbook.createCellStyle();
        Font typeFont = workbook.createFont();
        typeFont.setBold(true);
        typeHeaderStyle.setFont(typeFont);
        typeHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        typeHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        typeHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            int typeHeaderRowNum = rowNum;
            Row typeRow = sheet.createRow(rowNum++);
            Cell typeCell = typeRow.createCell(0);
            typeCell.setCellValue(typeGroup.policyTypeName());
            typeCell.setCellStyle(typeHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                typeRow.createCell(i).setCellStyle(typeHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(typeHeaderRowNum, typeHeaderRowNum, 0, totalCols - 1));

            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(pg.policyNumber());
                row.createCell(1).setCellValue(pg.policyStatus() != null ? pg.policyStatus() : "");
                row.createCell(2).setCellValue(pg.paymentFrequency() != null ? pg.paymentFrequency() : "");
                Cell premioCell = row.createCell(3);
                premioCell.setCellValue(pg.premioMensual().doubleValue());
                premioCell.setCellStyle(currencyStyle);
                Cell countCell = row.createCell(4);
                countCell.setCellValue(pg.paymentCount());
                countCell.setCellStyle(numberStyle);
                Cell paidCell = row.createCell(5);
                paidCell.setCellValue(pg.totalPaid().doubleValue());
                paidCell.setCellStyle(currencyStyle);
                Cell expectedCell = row.createCell(6);
                expectedCell.setCellValue(pg.expectedAmount().doubleValue());
                expectedCell.setCellStyle(currencyStyle);
                Cell diffCell = row.createCell(7);
                diffCell.setCellValue(pg.difference().doubleValue());
                diffCell.setCellStyle(currencyStyle);
            }

            // Type subtotal
            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + typeGroup.policyTypeName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 3; i++) {
                subtotalRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            Cell subCount = subtotalRow.createCell(4);
            subCount.setCellValue(typeGroup.paymentCount());
            subCount.setCellStyle(subtotalNumberStyle);
            Cell subPaid = subtotalRow.createCell(5);
            subPaid.setCellValue(typeGroup.subtotalPaid().doubleValue());
            subPaid.setCellStyle(subtotalStyle);
            Cell subExpected = subtotalRow.createCell(6);
            subExpected.setCellValue(typeGroup.subtotalExpected().doubleValue());
            subExpected.setCellStyle(subtotalStyle);
            Cell subDiff = subtotalRow.createCell(7);
            subDiff.setCellValue(typeGroup.subtotalDifference().doubleValue());
            subDiff.setCellStyle(subtotalStyle);

            rowNum++;
        }

        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 3; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        Cell totalCountCell = totalRow.createCell(4);
        totalCountCell.setCellValue(report.totalPaymentCount());
        totalCountCell.setCellStyle(totalNumberStyle);
        Cell grandPaid = totalRow.createCell(5);
        grandPaid.setCellValue(report.totalPaidAmount().doubleValue());
        grandPaid.setCellStyle(totalStyle);
        Cell grandExpected = totalRow.createCell(6);
        grandExpected.setCellValue(report.totalExpectedAmount().doubleValue());
        grandExpected.setCellStyle(totalStyle);
        Cell grandDiff = totalRow.createCell(7);
        grandDiff.setCellValue(report.totalDifference().doubleValue());
        grandDiff.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void createPaymentDetailSheet(Workbook workbook, PolicyPaymentReportDTO report,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle dateStyle,
                                           CellStyle titleCellStyle, CellStyle subtotalStyle,
                                           CellStyle subtotalLabelStyle,
                                           CellStyle numberStyle, CellStyle totalNumberStyle,
                                           CellStyle subtotalNumberStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Pagos");
        int rowNum = 0;

        int detailCols = 9;
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGOS DE PÓLIZA - DETALLE", report, titleCellStyle, detailCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Tipo", headerStyle);
        setCellWithStyle(headerRow, 1, "Póliza", headerStyle);
        setCellWithStyle(headerRow, 2, "Fecha Pago", headerStyle);
        setCellWithStyle(headerRow, 3, "Período Desde", headerStyle);
        setCellWithStyle(headerRow, 4, "Período Hasta", headerStyle);
        setCellWithStyle(headerRow, 5, "Monto ($)", headerStyle);
        setCellWithStyle(headerRow, 6, "Premio Esp. ($)", headerStyle);
        setCellWithStyle(headerRow, 7, "Diferencia ($)", headerStyle);
        setCellWithStyle(headerRow, 8, "Notas", headerStyle);

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                for (PolicyPaymentReportPaymentDTO payment : pg.payments()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(typeGroup.policyTypeName());
                    row.createCell(1).setCellValue(pg.policyNumber());

                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(payment.paymentDate() != null
                            ? payment.paymentDate().format(DATE_FORMATTER) : "");
                    dateCell.setCellStyle(dateStyle);

                    Cell periodFromCell = row.createCell(3);
                    periodFromCell.setCellValue(payment.periodFrom() != null
                            ? payment.periodFrom().format(DATE_FORMATTER) : "");
                    periodFromCell.setCellStyle(dateStyle);

                    Cell periodToCell = row.createCell(4);
                    periodToCell.setCellValue(payment.periodTo() != null
                            ? payment.periodTo().format(DATE_FORMATTER) : "");
                    periodToCell.setCellStyle(dateStyle);

                    Cell amountCell = row.createCell(5);
                    amountCell.setCellValue(payment.amount() != null ? payment.amount().doubleValue() : 0);
                    amountCell.setCellStyle(currencyStyle);

                    Cell premioCell = row.createCell(6);
                    premioCell.setCellValue(payment.premioMensual() != null ? payment.premioMensual().doubleValue() : 0);
                    premioCell.setCellStyle(currencyStyle);

                    Cell diffCell = row.createCell(7);
                    diffCell.setCellValue(payment.difference() != null ? payment.difference().doubleValue() : 0);
                    diffCell.setCellStyle(currencyStyle);

                    row.createCell(8).setCellValue(payment.notes() != null ? payment.notes() : "");
                }

                // Policy subtotal
                int pSubRowNum = rowNum;
                Row pSubRow = sheet.createRow(rowNum++);
                Cell pLabel = pSubRow.createCell(0);
                pLabel.setCellValue("Subtotal " + pg.policyNumber() + " (" + pg.paymentCount() + " pagos)");
                pLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 4; i++) {
                    pSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(pSubRowNum, pSubRowNum, 0, 4));
                Cell pPaid = pSubRow.createCell(5);
                pPaid.setCellValue(pg.totalPaid().doubleValue());
                pPaid.setCellStyle(subtotalStyle);
                Cell pExpected = pSubRow.createCell(6);
                pExpected.setCellValue(pg.expectedAmount().doubleValue());
                pExpected.setCellStyle(subtotalStyle);
                Cell pDiff = pSubRow.createCell(7);
                pDiff.setCellValue(pg.difference().doubleValue());
                pDiff.setCellStyle(subtotalStyle);
                pSubRow.createCell(8).setCellStyle(subtotalLabelStyle);
            }

            // Type subtotal
            int tSubRowNum = rowNum;
            Row tSubRow = sheet.createRow(rowNum++);
            Cell tLabel = tSubRow.createCell(0);
            tLabel.setCellValue("Subtotal " + typeGroup.policyTypeName() + " (" + typeGroup.paymentCount() + " pagos)");
            tLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 4; i++) {
                tSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(tSubRowNum, tSubRowNum, 0, 4));
            Cell tPaid = tSubRow.createCell(5);
            tPaid.setCellValue(typeGroup.subtotalPaid().doubleValue());
            tPaid.setCellStyle(subtotalStyle);
            Cell tExpected = tSubRow.createCell(6);
            tExpected.setCellValue(typeGroup.subtotalExpected().doubleValue());
            tExpected.setCellStyle(subtotalStyle);
            Cell tDiff = tSubRow.createCell(7);
            tDiff.setCellValue(typeGroup.subtotalDifference().doubleValue());
            tDiff.setCellStyle(subtotalStyle);
            tSubRow.createCell(8).setCellStyle(subtotalLabelStyle);

            rowNum++;
        }

        // Grand total
        int gtRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalPaymentCount() + " pagos)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 4; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(gtRowNum, gtRowNum, 0, 4));
        Cell grandPaid = totalRow.createCell(5);
        grandPaid.setCellValue(report.totalPaidAmount().doubleValue());
        grandPaid.setCellStyle(totalStyle);
        Cell grandExpected = totalRow.createCell(6);
        grandExpected.setCellValue(report.totalExpectedAmount().doubleValue());
        grandExpected.setCellStyle(totalStyle);
        Cell grandDiff = totalRow.createCell(7);
        grandDiff.setCellValue(report.totalDifference().doubleValue());
        grandDiff.setCellStyle(totalStyle);
        totalRow.createCell(8).setCellStyle(totalLabelStyle);

        for (int i = 0; i < detailCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 4: Vehículos Asegurados
    // ═══════════════════════════════════════════════════════════════════════

    private void createVehiclesSheet(Workbook workbook, PolicyPaymentReportDTO report,
                                      CellStyle headerStyle, CellStyle currencyStyle,
                                      CellStyle totalStyle, CellStyle totalLabelStyle,
                                      CellStyle titleCellStyle, CellStyle subtotalStyle,
                                      CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Vehículos Asegurados");
        int rowNum = 0;

        int totalCols = 6;
        rowNum = addSheetHeader(sheet, "REPORTE DE PAGOS DE PÓLIZA - VEHÍCULOS ASEGURADOS", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Póliza", headerStyle);
        setCellWithStyle(headerRow, 1, "Patente", headerStyle);
        setCellWithStyle(headerRow, 2, "Marca/Modelo", headerStyle);
        setCellWithStyle(headerRow, 3, "Área", headerStyle);
        setCellWithStyle(headerRow, 4, "Suma Asegurada ($)", headerStyle);
        setCellWithStyle(headerRow, 5, "Premio Mensual ($)", headerStyle);

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                if (pg.insuredVehicles() == null || pg.insuredVehicles().isEmpty()) {
                    continue;
                }
                for (PolicyPaymentReportVehicleDTO vehicle : pg.insuredVehicles()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(pg.policyNumber());
                    row.createCell(1).setCellValue(vehicle.licensePlate() != null ? vehicle.licensePlate() : "");

                    String brandModel = "";
                    if (vehicle.brand() != null) brandModel += vehicle.brand();
                    if (vehicle.model() != null) {
                        if (!brandModel.isEmpty()) brandModel += " ";
                        brandModel += vehicle.model();
                    }
                    row.createCell(2).setCellValue(brandModel);
                    row.createCell(3).setCellValue(vehicle.projectAreaName() != null ? vehicle.projectAreaName() : "");

                    Cell sumCell = row.createCell(4);
                    sumCell.setCellValue(vehicle.sumInsured() != null ? vehicle.sumInsured().doubleValue() : 0);
                    sumCell.setCellStyle(currencyStyle);

                    Cell premioCell = row.createCell(5);
                    premioCell.setCellValue(vehicle.premioMensual() != null ? vehicle.premioMensual().doubleValue() : 0);
                    premioCell.setCellStyle(currencyStyle);
                }
            }
        }

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, PolicyPaymentReportDTO report,
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
        countRow.createCell(0).setCellValue("Total pagos:");
        countRow.createCell(1).setCellValue(report.totalPaymentCount());

        Row policyCountRow = sheet.createRow(rowNum++);
        policyCountRow.createCell(0).setCellValue("Total pólizas:");
        policyCountRow.createCell(1).setCellValue(report.totalPolicyCount());

        rowNum++;

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

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.##"));
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

    private CellStyle createTotalNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.##"));
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

    private CellStyle createSubtotalNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.##"));
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
