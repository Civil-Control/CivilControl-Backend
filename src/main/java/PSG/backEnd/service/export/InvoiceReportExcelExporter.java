package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.invoice.*;
import PSG.backEnd.model.enums.documents.DocumentType;
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
public class InvoiceReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<DocumentType> DOCUMENT_TYPE_ORDER = List.of(
            DocumentType.BILL_A, DocumentType.BILL_B, DocumentType.BILL_C,
            DocumentType.DEBIT_NOTE_A, DocumentType.DEBIT_NOTE_B, DocumentType.DEBIT_NOTE_C,
            DocumentType.CREDIT_NOTE_A, DocumentType.CREDIT_NOTE_B, DocumentType.CREDIT_NOTE_C,
            DocumentType.OTHER_DOCUMENT);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(InvoiceReportDTO report) {
        log.info("Exporting invoice report to Excel: {} areas, {} total documents",
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

            List<DocumentType> activeTypes = DOCUMENT_TYPE_ORDER.stream()
                    .filter(dt -> report.totalsByDocumentType().containsKey(dt))
                    .toList();

            createAreaSummarySheet(workbook, report, activeTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleCellStyle);
            createSupplierSummarySheet(workbook, report, activeTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);
            createDocumentDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleCellStyle, subtotalStyle, subtotalLabelStyle);

            workbook.write(baos);

            log.info("Invoice Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, InvoiceReportDTO report,
                                         List<DocumentType> activeTypes,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleCellStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        rowNum = addSheetHeader(sheet, "REPORTE DE FACTURACIÓN - RESUMEN POR ÁREA", report, titleCellStyle,
                3 + activeTypes.size());
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Área", headerStyle);
        setCellWithStyle(headerRow, col++, "Cant. Docs", headerStyle);
        for (DocumentType dt : activeTypes) {
            setCellWithStyle(headerRow, col++, "Total " + dt.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(area.projectAreaName());
            row.createCell(c++).setCellValue(area.documentCount());
            for (DocumentType dt : activeTypes) {
                Cell cell = row.createCell(c++);
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(currencyStyle);
            }
            Cell totalCell = row.createCell(c);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
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
        for (DocumentType dt : activeTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
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
    // SHEET 2: Resumen por Proveedor
    // ═══════════════════════════════════════════════════════════════════════

    private void createSupplierSummarySheet(Workbook workbook, InvoiceReportDTO report,
                                             List<DocumentType> activeTypes,
                                             CellStyle headerStyle, CellStyle currencyStyle,
                                             CellStyle totalStyle, CellStyle totalLabelStyle,
                                             CellStyle titleCellStyle, CellStyle subtotalStyle,
                                             CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Proveedor");
        int rowNum = 0;

        int totalCols = 3 + activeTypes.size(); // Proveedor, CUIT, [types...], Total
        rowNum = addSheetHeader(sheet, "REPORTE DE FACTURACIÓN - RESUMEN POR PROVEEDOR", report, titleCellStyle,
                totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Proveedor (Razón Social)", headerStyle);
        setCellWithStyle(headerRow, col++, "CUIT", headerStyle);
        for (DocumentType dt : activeTypes) {
            setCellWithStyle(headerRow, col++, "Total " + dt.getDisplayName(), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            int areaHeaderRowNum = rowNum;
            Row areaRow = sheet.createRow(rowNum++);
            Cell areaCell = areaRow.createCell(0);
            areaCell.setCellValue(area.projectAreaName());
            areaCell.setCellStyle(areaHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                areaRow.createCell(i).setCellStyle(areaHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaHeaderRowNum, areaHeaderRowNum, 0, totalCols - 1));

            for (InvoiceReportSupplierGroupDTO supplier : area.supplierGroups()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(supplier.supplierLegalName());
                row.createCell(c++).setCellValue(supplier.supplierCuit() != null ? supplier.supplierCuit() : "-");
                for (DocumentType dt : activeTypes) {
                    Cell cell = row.createCell(c++);
                    BigDecimal val = supplier.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                    cell.setCellValue(val.doubleValue());
                    cell.setCellStyle(currencyStyle);
                }
                Cell totalCell = row.createCell(c);
                totalCell.setCellValue(supplier.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            Row subtotalRow = sheet.createRow(rowNum++);
            Cell subtotalLabel = subtotalRow.createCell(0);
            subtotalLabel.setCellValue("Subtotal " + area.projectAreaName());
            subtotalLabel.setCellStyle(subtotalLabelStyle);
            subtotalRow.createCell(1).setCellStyle(subtotalLabelStyle);
            int sc = 2;
            for (DocumentType dt : activeTypes) {
                Cell cell = subtotalRow.createCell(sc++);
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(subtotalStyle);
            }
            Cell subtotalAmount = subtotalRow.createCell(sc);
            subtotalAmount.setCellValue(area.subtotalAmount().doubleValue());
            subtotalAmount.setCellStyle(subtotalStyle);

            rowNum++;
        }

        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        totalRow.createCell(tc++).setCellStyle(totalLabelStyle);
        for (DocumentType dt : activeTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
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
    // SHEET 3: Detalle de Comprobantes
    // ═══════════════════════════════════════════════════════════════════════

    private void createDocumentDetailSheet(Workbook workbook, InvoiceReportDTO report,
                                            CellStyle headerStyle, CellStyle currencyStyle,
                                            CellStyle totalStyle, CellStyle totalLabelStyle,
                                            CellStyle dateStyle,
                                            CellStyle titleCellStyle, CellStyle subtotalStyle,
                                            CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Comprobantes");
        int rowNum = 0;

        rowNum = addSheetHeader(sheet, "REPORTE DE FACTURACIÓN - DETALLE DE COMPROBANTES", report, titleCellStyle, 12);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        setCellWithStyle(headerRow, 0, "Área", headerStyle);
        setCellWithStyle(headerRow, 1, "Proveedor", headerStyle);
        setCellWithStyle(headerRow, 2, "Fecha", headerStyle);
        setCellWithStyle(headerRow, 3, "Tipo Doc.", headerStyle);
        setCellWithStyle(headerRow, 4, "PV-Nro", headerStyle);
        setCellWithStyle(headerRow, 5, "Neto", headerStyle);
        setCellWithStyle(headerRow, 6, "IVA", headerStyle);
        setCellWithStyle(headerRow, 7, "IVA Exento", headerStyle);
        setCellWithStyle(headerRow, 8, "Otros Trib.", headerStyle);
        setCellWithStyle(headerRow, 9, "Perc. IIBB", headerStyle);
        setCellWithStyle(headerRow, 10, "Total", headerStyle);
        setCellWithStyle(headerRow, 11, "Pagado", headerStyle);

        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            for (InvoiceReportSupplierGroupDTO supplier : area.supplierGroups()) {
                for (InvoiceReportDocumentDTO doc : supplier.documents()) {
                    Row row = sheet.createRow(rowNum++);
                    String areaDisplay = area.projectAreaName();
                    if (doc.projectAreaTaskName() != null) {
                        areaDisplay += " - " + doc.projectAreaTaskName();
                    }
                    row.createCell(0).setCellValue(areaDisplay);
                    row.createCell(1).setCellValue(supplier.supplierLegalName());

                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(doc.date().format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateStyle);

                    row.createCell(3).setCellValue(doc.documentType().getDisplayName());
                    row.createCell(4).setCellValue(doc.branchCode() + "-" + doc.documentNumber());

                    Cell netCell = row.createCell(5);
                    netCell.setCellValue(doc.netTotal() != null ? doc.netTotal().doubleValue() : 0);
                    netCell.setCellStyle(currencyStyle);

                    Cell ivaCell = row.createCell(6);
                    ivaCell.setCellValue(doc.ivaTotal() != null ? doc.ivaTotal().doubleValue() : 0);
                    ivaCell.setCellStyle(currencyStyle);

                    Cell ivaExemptCell = row.createCell(7);
                    ivaExemptCell.setCellValue(doc.ivaExemptTotal() != null ? doc.ivaExemptTotal().doubleValue() : 0);
                    ivaExemptCell.setCellStyle(currencyStyle);

                    Cell otherTaxesCell = row.createCell(8);
                    otherTaxesCell.setCellValue(doc.otherTaxes() != null ? doc.otherTaxes().doubleValue() : 0);
                    otherTaxesCell.setCellStyle(currencyStyle);

                    Cell iibbCell = row.createCell(9);
                    iibbCell.setCellValue(doc.iibbPerception() != null ? doc.iibbPerception().doubleValue() : 0);
                    iibbCell.setCellStyle(currencyStyle);

                    Cell amountCell = row.createCell(10);
                    amountCell.setCellValue(doc.totalAmount().doubleValue());
                    amountCell.setCellStyle(currencyStyle);

                    row.createCell(11).setCellValue(Boolean.TRUE.equals(doc.paid()) ? "Sí" : "No");
                }

                int supplierSubtotalRowNum = rowNum;
                Row supplierSubtotalRow = sheet.createRow(rowNum++);
                Cell supplierLabel = supplierSubtotalRow.createCell(0);
                supplierLabel.setCellValue("Subtotal " + supplier.supplierLegalName());
                supplierLabel.setCellStyle(subtotalLabelStyle);
                for (int i = 1; i <= 9; i++) {
                    supplierSubtotalRow.createCell(i).setCellStyle(subtotalLabelStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(supplierSubtotalRowNum, supplierSubtotalRowNum, 0, 9));
                Cell supplierTotal = supplierSubtotalRow.createCell(10);
                supplierTotal.setCellValue(supplier.totalAmount().doubleValue());
                supplierTotal.setCellStyle(subtotalStyle);
                supplierSubtotalRow.createCell(11).setCellStyle(subtotalLabelStyle);
            }

            int areaSubtotalRowNum = rowNum;
            Row areaSubtotalRow = sheet.createRow(rowNum++);
            Cell areaLabel = areaSubtotalRow.createCell(0);
            areaLabel.setCellValue("Subtotal " + area.projectAreaName() + " (" + area.documentCount() + " docs)");
            areaLabel.setCellStyle(subtotalLabelStyle);
            for (int i = 1; i <= 9; i++) {
                areaSubtotalRow.createCell(i).setCellStyle(subtotalLabelStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaSubtotalRowNum, areaSubtotalRowNum, 0, 9));
            Cell areaTotal = areaSubtotalRow.createCell(10);
            areaTotal.setCellValue(area.subtotalAmount().doubleValue());
            areaTotal.setCellStyle(subtotalStyle);
            areaSubtotalRow.createCell(11).setCellStyle(subtotalLabelStyle);

            rowNum++;
        }

        int grandTotalRowNum = rowNum;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL (" + report.totalCount() + " docs)");
        totalLabel.setCellStyle(totalLabelStyle);
        for (int i = 1; i <= 9; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(grandTotalRowNum, grandTotalRowNum, 0, 9));
        Cell grandTotal = totalRow.createCell(10);
        grandTotal.setCellValue(report.totalAmount().doubleValue());
        grandTotal.setCellStyle(totalStyle);
        totalRow.createCell(11).setCellStyle(totalLabelStyle);

        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, InvoiceReportDTO report,
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
