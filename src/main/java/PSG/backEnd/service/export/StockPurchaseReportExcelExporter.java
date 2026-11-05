package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.stockPurchase.*;
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
public class StockPurchaseReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(StockPurchaseReportDTO report) {
        log.info("Exporting stock purchase report to Excel: {} categories, {} total purchases",
                report.categoryGroups().size(), report.totalCount());

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

            createCategorySummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle,
                    numberStyle, totalNumberStyle);
            createStockItemSummarySheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle,
                    numberStyle, totalNumberStyle, subtotalNumberStyle);
            createPurchaseDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle,
                    numberStyle, totalNumberStyle, subtotalNumberStyle);

            workbook.write(baos);

            log.info("Stock purchase Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating stock purchase Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Categoría
    // ═══════════════════════════════════════════════════════════════════════

    private void createCategorySummarySheet(Workbook workbook, StockPurchaseReportDTO report,
                                            CellStyle headerStyle, CellStyle currencyStyle,
                                            CellStyle totalStyle, CellStyle totalLabelStyle,
                                            CellStyle titleCellStyle,
                                            CellStyle numberStyle, CellStyle totalNumberStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Categoría");
        int rowNum = 0;

        int totalCols = 4; // Categoría, Cant. Compras, Cantidad, Total
        rowNum = addSheetHeader(sheet, "REPORTE DE COMPRAS DE STOCK - RESUMEN POR CATEGORÍA", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Categoría", headerStyle);
        setCellWithStyle(headerRow, 1, "Cant. Compras", headerStyle);
        setCellWithStyle(headerRow, 2, "Cantidad", headerStyle);
        setCellWithStyle(headerRow, 3, "Total ($)", headerStyle);

        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.categoryName());
            Cell countCell = row.createCell(1);
            countCell.setCellValue(category.purchaseCount());
            countCell.setCellStyle(numberStyle);
            Cell qtyCell = row.createCell(2);
            qtyCell.setCellValue(category.subtotalQuantity().doubleValue());
            qtyCell.setCellStyle(numberStyle);
            Cell totalCell = row.createCell(3);
            totalCell.setCellValue(category.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(1);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalNumberStyle);
        Cell totalQtyCell = totalRow.createCell(2);
        totalQtyCell.setCellValue(report.totalQuantity().doubleValue());
        totalQtyCell.setCellStyle(totalNumberStyle);
        Cell grandTotal = totalRow.createCell(3);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Item
    // ═══════════════════════════════════════════════════════════════════════

    private void createStockItemSummarySheet(Workbook workbook, StockPurchaseReportDTO report,
                                              CellStyle headerStyle, CellStyle currencyStyle,
                                              CellStyle totalStyle, CellStyle totalLabelStyle,
                                              CellStyle titleCellStyle, CellStyle subtotalStyle,
                                              CellStyle subtotalLabelStyle,
                                              CellStyle numberStyle, CellStyle totalNumberStyle,
                                              CellStyle subtotalNumberStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Item");
        int rowNum = 0;

        int totalCols = 4; // Item, Cant. Compras, Cantidad, Total
        rowNum = addSheetHeader(sheet, "REPORTE DE COMPRAS DE STOCK - RESUMEN POR ITEM", report, titleCellStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Item", headerStyle);
        setCellWithStyle(headerRow, 1, "Cant. Compras", headerStyle);
        setCellWithStyle(headerRow, 2, "Cantidad", headerStyle);
        setCellWithStyle(headerRow, 3, "Total ($)", headerStyle);

        CellStyle categoryHeaderStyle = workbook.createCellStyle();
        Font categoryFont = workbook.createFont();
        categoryFont.setBold(true);
        categoryHeaderStyle.setFont(categoryFont);
        categoryHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        categoryHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        categoryHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            int catHeaderRowNum = rowNum;
            Row catRow = sheet.createRow(rowNum++);
            Cell catCell = catRow.createCell(0);
            catCell.setCellValue(category.categoryName());
            catCell.setCellStyle(categoryHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                catRow.createCell(i).setCellStyle(categoryHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(catHeaderRowNum, catHeaderRowNum, 0, totalCols - 1));

            for (StockPurchaseReportStockGroupDTO stock : category.stockGroups()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stock.stockName());
                Cell countCell = row.createCell(1);
                countCell.setCellValue(stock.purchaseCount());
                countCell.setCellStyle(numberStyle);
                Cell qtyCell = row.createCell(2);
                qtyCell.setCellValue(stock.totalQuantity().doubleValue());
                qtyCell.setCellStyle(numberStyle);
                Cell totalCell = row.createCell(3);
                totalCell.setCellValue(stock.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Category subtotal
            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + category.categoryName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            Cell subCount = subtotalRow.createCell(1);
            subCount.setCellValue(category.purchaseCount());
            subCount.setCellStyle(subtotalNumberStyle);
            Cell subQty = subtotalRow.createCell(2);
            subQty.setCellValue(category.subtotalQuantity().doubleValue());
            subQty.setCellStyle(subtotalNumberStyle);
            Cell subTotal = subtotalRow.createCell(3);
            subTotal.setCellValue(category.subtotalAmount().doubleValue());
            subTotal.setCellStyle(subtotalStyle);

            rowNum++;
        }

        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(1);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalNumberStyle);
        Cell totalQtyCell = totalRow.createCell(2);
        totalQtyCell.setCellValue(report.totalQuantity().doubleValue());
        totalQtyCell.setCellStyle(totalNumberStyle);
        Cell grandTotal = totalRow.createCell(3);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Compras
    // ═══════════════════════════════════════════════════════════════════════

    private void createPurchaseDetailSheet(Workbook workbook, StockPurchaseReportDTO report,
                                            CellStyle headerStyle, CellStyle currencyStyle,
                                            CellStyle totalStyle, CellStyle totalLabelStyle,
                                            CellStyle dateStyle,
                                            CellStyle titleCellStyle, CellStyle subtotalStyle,
                                            CellStyle subtotalLabelStyle,
                                            CellStyle numberStyle, CellStyle totalNumberStyle,
                                            CellStyle subtotalNumberStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Compras");
        int rowNum = 0;

        int detailCols = 7; // Categoría, Item, Fecha, Cantidad, P. Unitario, Total, Notas
        rowNum = addSheetHeader(sheet, "REPORTE DE COMPRAS DE STOCK - DETALLE", report, titleCellStyle, detailCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Categoría", headerStyle);
        setCellWithStyle(headerRow, 1, "Item", headerStyle);
        setCellWithStyle(headerRow, 2, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 3, "Cantidad", headerStyle);
        setCellWithStyle(headerRow, 4, "P. Unitario ($)", headerStyle);
        setCellWithStyle(headerRow, 5, "Total ($)", headerStyle);
        setCellWithStyle(headerRow, 6, "Notas", headerStyle);

        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            for (StockPurchaseReportStockGroupDTO stock : category.stockGroups()) {
                for (StockPurchaseReportItemDTO purchase : stock.purchases()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(category.categoryName());
                    row.createCell(1).setCellValue(stock.stockName());

                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(purchase.date().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    Cell qtyCell = row.createCell(3);
                    qtyCell.setCellValue(purchase.quantity().doubleValue());
                    qtyCell.setCellStyle(numberStyle);

                    Cell unitPriceCell = row.createCell(4);
                    unitPriceCell.setCellValue(purchase.unitPrice() != null ? purchase.unitPrice().doubleValue() : 0);
                    unitPriceCell.setCellStyle(currencyStyle);

                    Cell totalCell = row.createCell(5);
                    totalCell.setCellValue((purchase.totalWithIva() != null ? purchase.totalWithIva() : purchase.totalAmount()).doubleValue());
                    totalCell.setCellStyle(currencyStyle);

                    row.createCell(6).setCellValue(purchase.notes() != null ? purchase.notes() : "");
                }

                // Stock item subtotal
                int sSubRowNum = rowNum;
                Row sSubRow = sheet.createRow(rowNum++);
                Cell sLabel = sSubRow.createCell(0);
                sLabel.setCellValue("Subtotal " + stock.stockName());
                sLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 2; i++) {
                    sSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(sSubRowNum, sSubRowNum, 0, 2));
                Cell sQty = sSubRow.createCell(3);
                sQty.setCellValue(stock.totalQuantity().doubleValue());
                sQty.setCellStyle(subtotalNumberStyle);
                sSubRow.createCell(4).setCellStyle(subtotalLabelStyle);
                Cell sTotal = sSubRow.createCell(5);
                sTotal.setCellValue(stock.totalAmount().doubleValue());
                sTotal.setCellStyle(subtotalStyle);
                sSubRow.createCell(6).setCellStyle(subtotalLabelStyle);
            }

            // Category subtotal
            int cSubRowNum = rowNum;
            Row cSubRow = sheet.createRow(rowNum++);
            Cell cLabel = cSubRow.createCell(0);
            cLabel.setCellValue("Subtotal " + category.categoryName() + " (" + category.purchaseCount() + " compras)");
            cLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 2; i++) {
                cSubRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(cSubRowNum, cSubRowNum, 0, 2));
            Cell cQty = cSubRow.createCell(3);
            cQty.setCellValue(category.subtotalQuantity().doubleValue());
            cQty.setCellStyle(subtotalNumberStyle);
            cSubRow.createCell(4).setCellStyle(subtotalLabelStyle);
            Cell cTotal = cSubRow.createCell(5);
            cTotal.setCellValue(category.subtotalAmount().doubleValue());
            cTotal.setCellStyle(subtotalStyle);
            cSubRow.createCell(6).setCellStyle(subtotalLabelStyle);

            rowNum++;
        }

        // Grand total
        int gtRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " compras)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 2; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(gtRowNum, gtRowNum, 0, 2));
        Cell grandQty = totalRow.createCell(3);
        grandQty.setCellValue(report.totalQuantity().doubleValue());
        grandQty.setCellStyle(totalNumberStyle);
        totalRow.createCell(4).setCellStyle(totalLabelStyle);
        Cell grandTotal = totalRow.createCell(5);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);
        totalRow.createCell(6).setCellStyle(totalLabelStyle);

        for (int i = 0; i < detailCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, StockPurchaseReportDTO report,
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
