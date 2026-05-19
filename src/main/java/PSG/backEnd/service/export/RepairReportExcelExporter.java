package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.repair.*;
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
public class RepairReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(RepairReportDTO report) {
        log.info("Exporting repair report to Excel: {} areas, {} total repairs",
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

            createAreaSummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle);
            createVehicleSummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);
            createRepairDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);

            workbook.write(baos);

            log.info("Repair Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating repair Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, RepairReportDTO report,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleCellStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        int totalCols = 5; // Área, Cant, Materiales, M.O., Total
        rowNum = addSheetHeader(sheet, "REPORTE DE REPARACIONES - RESUMEN POR ÁREA", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Cant. Reparaciones", headerStyle);
        setCellWithStyle(headerRow, 2, "Materiales ($)", headerStyle);
        setCellWithStyle(headerRow, 3, "Mano de Obra ($)", headerStyle);
        setCellWithStyle(headerRow, 4, "Total ($)", headerStyle);

        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(area.projectAreaName());
            row.createCell(1).setCellValue(area.repairCount());
            Cell matCell = row.createCell(2);
            matCell.setCellValue(area.materialSubtotal().doubleValue());
            matCell.setCellStyle(currencyStyle);
            Cell labCell = row.createCell(3);
            labCell.setCellValue(area.laborSubtotal().doubleValue());
            labCell.setCellStyle(currencyStyle);
            Cell totalCell = row.createCell(4);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(1);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        Cell matTotal = totalRow.createCell(2);
        matTotal.setCellValue(report.totalMaterialCost().doubleValue());
        matTotal.setCellStyle(totalStyle);
        Cell labTotal = totalRow.createCell(3);
        labTotal.setCellValue(report.totalLaborCost().doubleValue());
        labTotal.setCellStyle(totalStyle);
        Cell grandTotal = totalRow.createCell(4);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Vehículo
    // ═══════════════════════════════════════════════════════════════════════

    private void createVehicleSummarySheet(Workbook workbook, RepairReportDTO report,
                                            CellStyle headerStyle, CellStyle currencyStyle,
                                            CellStyle totalStyle, CellStyle totalLabelStyle,
                                            CellStyle titleCellStyle, CellStyle subtotalStyle,
                                            CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Vehículo");
        int rowNum = 0;

        int totalCols = 6; // Vehículo, Marca/Modelo, Cant, Materiales, M.O., Total
        rowNum = addSheetHeader(sheet, "REPORTE DE REPARACIONES - RESUMEN POR VEHÍCULO", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Vehículo (Patente)", headerStyle);
        setCellWithStyle(headerRow, 1, "Marca/Modelo", headerStyle);
        setCellWithStyle(headerRow, 2, "Cant. Rep.", headerStyle);
        setCellWithStyle(headerRow, 3, "Materiales ($)", headerStyle);
        setCellWithStyle(headerRow, 4, "M.O. ($)", headerStyle);
        setCellWithStyle(headerRow, 5, "Total ($)", headerStyle);

        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            int areaHeaderRowNum = rowNum;
            Row areaRow = sheet.createRow(rowNum++);
            Cell areaCell = areaRow.createCell(0);
            areaCell.setCellValue(area.projectAreaName());
            areaCell.setCellStyle(areaHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                areaRow.createCell(i).setCellStyle(areaHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaHeaderRowNum, areaHeaderRowNum, 0, totalCols - 1));

            for (RepairReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vehicle.vehicleLicensePlate());
                String desc = "";
                if (vehicle.vehicleBrand() != null) desc += vehicle.vehicleBrand();
                if (vehicle.vehicleModel() != null) desc += (desc.isEmpty() ? "" : " ") + vehicle.vehicleModel();
                row.createCell(1).setCellValue(desc);
                row.createCell(2).setCellValue(vehicle.repairCount());
                Cell matCell = row.createCell(3);
                matCell.setCellValue(vehicle.materialSubtotal().doubleValue());
                matCell.setCellStyle(currencyStyle);
                Cell labCell = row.createCell(4);
                labCell.setCellValue(vehicle.laborSubtotal().doubleValue());
                labCell.setCellStyle(currencyStyle);
                Cell totalCell = row.createCell(5);
                totalCell.setCellValue(vehicle.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Area subtotal
            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + area.projectAreaName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            subtotalRow.createCell(1).setCellStyle(subtotalLabelStyle);
            Cell subCount = subtotalRow.createCell(2);
            subCount.setCellValue(area.repairCount());
            subCount.setCellStyle(subtotalStyle);
            Cell subMat = subtotalRow.createCell(3);
            subMat.setCellValue(area.materialSubtotal().doubleValue());
            subMat.setCellStyle(subtotalStyle);
            Cell subLab = subtotalRow.createCell(4);
            subLab.setCellValue(area.laborSubtotal().doubleValue());
            subLab.setCellStyle(subtotalStyle);
            Cell subTotal = subtotalRow.createCell(5);
            subTotal.setCellValue(area.subtotalAmount().doubleValue());
            subTotal.setCellStyle(subtotalStyle);

            rowNum++;
        }

        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        totalRow.createCell(1).setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(2);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        Cell matTotal = totalRow.createCell(3);
        matTotal.setCellValue(report.totalMaterialCost().doubleValue());
        matTotal.setCellStyle(totalStyle);
        Cell labTotal = totalRow.createCell(4);
        labTotal.setCellValue(report.totalLaborCost().doubleValue());
        labTotal.setCellStyle(totalStyle);
        Cell grandTotal = totalRow.createCell(5);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Reparaciones
    // ═══════════════════════════════════════════════════════════════════════

    private void createRepairDetailSheet(Workbook workbook, RepairReportDTO report,
                                          CellStyle headerStyle, CellStyle currencyStyle,
                                          CellStyle totalStyle, CellStyle totalLabelStyle,
                                          CellStyle dateStyle,
                                          CellStyle titleCellStyle, CellStyle subtotalStyle,
                                          CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Reparaciones");
        int rowNum = 0;

        int detailCols = 9; // Área, Vehículo, Fecha, Descripción, Km, Proveedor, Materiales, M.O., Total
        rowNum = addSheetHeader(sheet, "REPORTE DE REPARACIONES - DETALLE", report, titleCellStyle, detailCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Vehículo", headerStyle);
        setCellWithStyle(headerRow, 2, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 3, "Descripción", headerStyle);
        setCellWithStyle(headerRow, 4, "Km", headerStyle);
        setCellWithStyle(headerRow, 5, "Proveedor", headerStyle);
        setCellWithStyle(headerRow, 6, "Materiales ($)", headerStyle);
        setCellWithStyle(headerRow, 7, "M.O. ($)", headerStyle);
        setCellWithStyle(headerRow, 8, "Total ($)", headerStyle);

        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            for (RepairReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                for (RepairReportItemDTO repair : vehicle.repairs()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(area.projectAreaName());
                    row.createCell(1).setCellValue(vehicle.vehicleLicensePlate());

                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(repair.date().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    row.createCell(3).setCellValue(repair.description() != null ? repair.description() : "");
                    row.createCell(4).setCellValue(repair.mileage() != null ? repair.mileage() : 0);
                    row.createCell(5).setCellValue(repair.supplierName() != null ? repair.supplierName() : "Interno");

                    Cell matCell = row.createCell(6);
                    matCell.setCellValue(repair.materialCost().doubleValue());
                    matCell.setCellStyle(currencyStyle);
                    Cell labCell = row.createCell(7);
                    labCell.setCellValue(repair.laborCost().doubleValue());
                    labCell.setCellStyle(currencyStyle);
                    Cell totalCell = row.createCell(8);
                    totalCell.setCellValue(repair.totalWithIva().doubleValue());
                    totalCell.setCellStyle(currencyStyle);
                }

                // Vehicle subtotal
                int vSubRowNum = rowNum;
                Row vSubRow = sheet.createRow(rowNum++);
                Cell vLabel = vSubRow.createCell(0);
                vLabel.setCellValue("Subtotal " + vehicle.vehicleLicensePlate());
                vLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 5; i++) {
                    vSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(vSubRowNum, vSubRowNum, 0, 5));
                Cell vMat = vSubRow.createCell(6);
                vMat.setCellValue(vehicle.materialSubtotal().doubleValue());
                vMat.setCellStyle(subtotalStyle);
                Cell vLab = vSubRow.createCell(7);
                vLab.setCellValue(vehicle.laborSubtotal().doubleValue());
                vLab.setCellStyle(subtotalStyle);
                Cell vTotal = vSubRow.createCell(8);
                vTotal.setCellValue(vehicle.totalAmount().doubleValue());
                vTotal.setCellStyle(subtotalStyle);
            }

            // Area subtotal
            int aSubRowNum = rowNum;
            Row aSubRow = sheet.createRow(rowNum++);
            Cell aLabel = aSubRow.createCell(0);
            aLabel.setCellValue("Subtotal " + area.projectAreaName() + " (" + area.repairCount() + " reparaciones)");
            aLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 5; i++) {
                aSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(aSubRowNum, aSubRowNum, 0, 5));
            Cell aMat = aSubRow.createCell(6);
            aMat.setCellValue(area.materialSubtotal().doubleValue());
            aMat.setCellStyle(subtotalStyle);
            Cell aLab = aSubRow.createCell(7);
            aLab.setCellValue(area.laborSubtotal().doubleValue());
            aLab.setCellStyle(subtotalStyle);
            Cell aTotal = aSubRow.createCell(8);
            aTotal.setCellValue(area.subtotalAmount().doubleValue());
            aTotal.setCellStyle(subtotalStyle);

            rowNum++;
        }

        // Grand total
        int gtRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " reparaciones)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 5; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(gtRowNum, gtRowNum, 0, 5));
        Cell grandMat = totalRow.createCell(6);
        grandMat.setCellValue(report.totalMaterialCost().doubleValue());
        grandMat.setCellStyle(totalStyle);
        Cell grandLab = totalRow.createCell(7);
        grandLab.setCellValue(report.totalLaborCost().doubleValue());
        grandLab.setCellStyle(totalStyle);
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

    private int addSheetHeader(Sheet sheet, String title, RepairReportDTO report,
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
