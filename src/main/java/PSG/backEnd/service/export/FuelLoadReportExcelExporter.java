package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.fuelLoad.*;
import PSG.backEnd.model.enums.vehicle.FuelType;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class FuelLoadReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<FuelType> FUEL_TYPE_ORDER = List.of(
            FuelType.INFINIA, FuelType.SUPER, FuelType.INFINIA_DIESEL,
            FuelType.DIESEL_500, FuelType.GNC, FuelType.DISTILLED_WATER, FuelType.OIL);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(FuelLoadReportDTO report) {
        log.info("Exporting fuel load report to Excel: {} areas, {} total loads",
                report.areaGroups().size(), report.totalCount());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle titleCellStyle = createTitleStyle(workbook);
            CellStyle subtotalStyle = createSubtotalStyle(workbook);
            CellStyle subtotalLabelStyle = createSubtotalLabelStyle(workbook);

            List<FuelType> activeFuelTypes = FUEL_TYPE_ORDER.stream()
                    .filter(ft -> report.totalsByFuelType().containsKey(ft.name()))
                    .toList();

            createAreaSummarySheet(workbook, report, activeFuelTypes,
                    headerStyle, currencyStyle, numberStyle, totalStyle, totalLabelStyle, titleCellStyle);
            createVehicleSummarySheet(workbook, report, activeFuelTypes,
                    headerStyle, currencyStyle, numberStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);
            createLoadDetailSheet(workbook, report,
                    headerStyle, currencyStyle, numberStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);

            workbook.write(baos);

            log.info("Fuel load Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating fuel load Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, FuelLoadReportDTO report,
                                         List<FuelType> activeFuelTypes,
                                         CellStyle headerStyle, CellStyle currencyStyle, CellStyle numberStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleCellStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        // Each fuel type has 2 cols: amount + liters
        int totalCols = 3 + activeFuelTypes.size() * 2 + 1; // Área, Cant, [ft$, ftL]..., Total$, TotalL
        rowNum = addSheetHeader(sheet, "REPORTE DE COMBUSTIBLE - RESUMEN POR ÁREA", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Área", headerStyle);
        setCellWithStyle(headerRow, col++, "Cant. Cargas", headerStyle);
        for (FuelType ft : activeFuelTypes) {
            setCellWithStyle(headerRow, col++, ft.getDisplayName() + " ($)", headerStyle);
            setCellWithStyle(headerRow, col++, ft.getDisplayName() + " (L)", headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total ($)", headerStyle);
        setCellWithStyle(headerRow, col++, "Total (L)", headerStyle);

        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(area.projectAreaName());
            row.createCell(c++).setCellValue(area.loadCount());
            for (FuelType ft : activeFuelTypes) {
                Cell amtCell = row.createCell(c++);
                amtCell.setCellValue(area.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                amtCell.setCellStyle(currencyStyle);
                Cell litCell = row.createCell(c++);
                litCell.setCellValue(area.litersByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                litCell.setCellStyle(numberStyle);
            }
            Cell totalCell = row.createCell(c++);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
            Cell totalLitCell = row.createCell(c);
            totalLitCell.setCellValue(area.subtotalLiters().doubleValue());
            totalLitCell.setCellStyle(numberStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(tc++);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        for (FuelType ft : activeFuelTypes) {
            Cell cell = totalRow.createCell(tc++);
            cell.setCellValue(report.totalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
            cell.setCellStyle(totalStyle);
            Cell litCell = totalRow.createCell(tc++);
            litCell.setCellValue(report.litersByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
            litCell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc++);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);
        Cell grandTotalLitCell = totalRow.createCell(tc);
        grandTotalLitCell.setCellValue(report.totalLiters().doubleValue());
        grandTotalLitCell.setCellStyle(totalStyle);

        for (int i = 0; i <= tc; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Vehículo
    // ═══════════════════════════════════════════════════════════════════════

    private void createVehicleSummarySheet(Workbook workbook, FuelLoadReportDTO report,
                                            List<FuelType> activeFuelTypes,
                                            CellStyle headerStyle, CellStyle currencyStyle, CellStyle numberStyle,
                                            CellStyle totalStyle, CellStyle totalLabelStyle,
                                            CellStyle titleCellStyle, CellStyle subtotalStyle,
                                            CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Vehículo");
        int rowNum = 0;

        int totalCols = 3 + activeFuelTypes.size() * 2 + 1;
        rowNum = addSheetHeader(sheet, "REPORTE DE COMBUSTIBLE - RESUMEN POR VEHÍCULO", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Vehículo", headerStyle);
        setCellWithStyle(headerRow, col++, "Descripción", headerStyle);
        for (FuelType ft : activeFuelTypes) {
            setCellWithStyle(headerRow, col++, ft.getDisplayName() + " ($)", headerStyle);
            setCellWithStyle(headerRow, col++, ft.getDisplayName() + " (L)", headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total ($)", headerStyle);
        setCellWithStyle(headerRow, col++, "Total (L)", headerStyle);

        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            int areaHeaderRowNum = rowNum;
            Row areaRow = sheet.createRow(rowNum++);
            Cell areaCell = areaRow.createCell(0);
            areaCell.setCellValue(area.projectAreaName());
            areaCell.setCellStyle(areaHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                areaRow.createCell(i).setCellStyle(areaHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaHeaderRowNum, areaHeaderRowNum, 0, totalCols - 1));

            for (FuelLoadReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(vehicle.vehicleName());
                row.createCell(c++).setCellValue(vehicle.vehicleDescription() != null ? vehicle.vehicleDescription() : "");
                for (FuelType ft : activeFuelTypes) {
                    Cell amtCell = row.createCell(c++);
                    amtCell.setCellValue(vehicle.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                    amtCell.setCellStyle(currencyStyle);
                    Cell litCell = row.createCell(c++);
                    litCell.setCellValue(vehicle.litersByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                    litCell.setCellStyle(numberStyle);
                }
                Cell totalCell = row.createCell(c++);
                totalCell.setCellValue(vehicle.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
                Cell totalLitCell = row.createCell(c);
                totalLitCell.setCellValue(vehicle.totalLiters().doubleValue());
                totalLitCell.setCellStyle(numberStyle);
            }

            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + area.projectAreaName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            subtotalRow.createCell(1).setCellStyle(subtotalLabelStyle);
            int sc = 2;
            for (FuelType ft : activeFuelTypes) {
                Cell cell = subtotalRow.createCell(sc++);
                cell.setCellValue(area.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                cell.setCellStyle(subtotalStyle);
                Cell litCell = subtotalRow.createCell(sc++);
                litCell.setCellValue(area.litersByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
                litCell.setCellStyle(subtotalStyle);
            }
            Cell subtotalAmount = subtotalRow.createCell(sc++);
            subtotalAmount.setCellValue(area.subtotalAmount().doubleValue());
            subtotalAmount.setCellStyle(subtotalStyle);
            Cell subtotalLiters = subtotalRow.createCell(sc);
            subtotalLiters.setCellValue(area.subtotalLiters().doubleValue());
            subtotalLiters.setCellStyle(subtotalStyle);

            rowNum++;
        }

        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        totalRow.createCell(tc++).setCellStyle(totalLabelStyle);
        for (FuelType ft : activeFuelTypes) {
            Cell cell = totalRow.createCell(tc++);
            cell.setCellValue(report.totalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
            cell.setCellStyle(totalStyle);
            Cell litCell = totalRow.createCell(tc++);
            litCell.setCellValue(report.litersByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO).doubleValue());
            litCell.setCellStyle(totalStyle);
        }
        Cell grandTotalCell = totalRow.createCell(tc++);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);
        Cell grandTotalLitCell = totalRow.createCell(tc);
        grandTotalLitCell.setCellValue(report.totalLiters().doubleValue());
        grandTotalLitCell.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Cargas
    // ═══════════════════════════════════════════════════════════════════════

    private void createLoadDetailSheet(Workbook workbook, FuelLoadReportDTO report,
                                        CellStyle headerStyle, CellStyle currencyStyle, CellStyle numberStyle,
                                        CellStyle totalStyle, CellStyle totalLabelStyle,
                                        CellStyle dateStyle,
                                        CellStyle titleCellStyle, CellStyle subtotalStyle,
                                        CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Cargas");
        int rowNum = 0;

        int detailCols = 10;
        rowNum = addSheetHeader(sheet, "REPORTE DE COMBUSTIBLE - DETALLE DE CARGAS", report, titleCellStyle, detailCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Vehículo", headerStyle);
        setCellWithStyle(headerRow, 2, "Estación", headerStyle);
        setCellWithStyle(headerRow, 3, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 4, "Tipo Comb.", headerStyle);
        setCellWithStyle(headerRow, 5, "Sucursal", headerStyle);
        setCellWithStyle(headerRow, 6, "Ticket", headerStyle);
        setCellWithStyle(headerRow, 7, "Litros", headerStyle);
        setCellWithStyle(headerRow, 8, "Precio/L", headerStyle);
        setCellWithStyle(headerRow, 9, "Total", headerStyle);

        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            for (FuelLoadReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                for (FuelLoadReportItemDTO load : vehicle.loads()) {
                    Row row = sheet.createRow(rowNum++);
                    String areaDisplay = area.projectAreaName();
                    if (load.projectAreaTaskName() != null) {
                        areaDisplay += " - " + load.projectAreaTaskName();
                    }
                    row.createCell(0).setCellValue(areaDisplay);
                    row.createCell(1).setCellValue(load.vehicleLicensePlate() != null ? load.vehicleLicensePlate() : "Bidón");
                    row.createCell(2).setCellValue(load.gasStationName());

                    Cell dateCell = row.createCell(3);
                    dateCell.setCellValue(load.date().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    row.createCell(4).setCellValue(getFuelTypeDisplayName(load.fuelType()));
                    row.createCell(5).setCellValue(load.branchCode());
                    row.createCell(6).setCellValue(load.ticketNumber());

                    Cell litersCell = row.createCell(7);
                    litersCell.setCellValue(load.liters().doubleValue());
                    litersCell.setCellStyle(numberStyle);

                    Cell priceCell = row.createCell(8);
                    priceCell.setCellValue(load.pricePerLiter().doubleValue());
                    priceCell.setCellStyle(currencyStyle);

                    Cell amountCell = row.createCell(9);
                    amountCell.setCellValue((load.totalWithIva() != null ? load.totalWithIva() : load.totalAmount()).doubleValue());
                    amountCell.setCellStyle(currencyStyle);
                }

                // Vehicle subtotal row
                int vSubRowNum = rowNum;
                Row vSubRow = sheet.createRow(rowNum++);
                Cell vLabel = vSubRow.createCell(0);
                vLabel.setCellValue("Subtotal " + vehicle.vehicleName());
                vLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 6; i++) {
                    vSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(vSubRowNum, vSubRowNum, 0, 6));
                Cell vLiters = vSubRow.createCell(7);
                vLiters.setCellValue(vehicle.totalLiters().doubleValue());
                vLiters.setCellStyle(subtotalStyle);
                vSubRow.createCell(8).setCellStyle(subtotalLabelStyle);
                Cell vTotal = vSubRow.createCell(9);
                vTotal.setCellValue(vehicle.totalAmount().doubleValue());
                vTotal.setCellStyle(subtotalStyle);
            }

            // Area subtotal row
            int aSubRowNum = rowNum;
            Row aSubRow = sheet.createRow(rowNum++);
            Cell aLabel = aSubRow.createCell(0);
            aLabel.setCellValue("Subtotal " + area.projectAreaName() + " (" + area.loadCount() + " cargas)");
            aLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 6; i++) {
                aSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(aSubRowNum, aSubRowNum, 0, 6));
            Cell aLiters = aSubRow.createCell(7);
            aLiters.setCellValue(area.subtotalLiters().doubleValue());
            aLiters.setCellStyle(subtotalStyle);
            aSubRow.createCell(8).setCellStyle(subtotalLabelStyle);
            Cell aTotal = aSubRow.createCell(9);
            aTotal.setCellValue(area.subtotalAmount().doubleValue());
            aTotal.setCellStyle(subtotalStyle);

            rowNum++;
        }

        // Grand total
        int gtRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " cargas)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 6; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(gtRowNum, gtRowNum, 0, 6));
        Cell grandLiters = totalRow.createCell(7);
        grandLiters.setCellValue(report.totalLiters().doubleValue());
        grandLiters.setCellStyle(totalStyle);
        totalRow.createCell(8).setCellStyle(totalLabelStyle);
        Cell grandTotal = totalRow.createCell(9);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < detailCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, FuelLoadReportDTO report,
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

    private String getFuelTypeDisplayName(String fuelType) {
        try {
            return FuelType.valueOf(fuelType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return fuelType;
        }
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
        style.setDataFormat(format.getFormat("#,##0.00"));
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
