package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.stockPurchase.*;
import PSG.backEnd.service.util.MessageSourceHelper;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockPurchaseReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(StockPurchaseReportDTO report) {
        log.info("Exporting stock purchase report to PDF: {} categories, {} total purchases",
                report.categoryGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Section 1: Category summary
            addSectionHeader(document, "REPORTE DE COMPRAS DE STOCK", "Resumen por Categoría", report);
            addCategorySummaryTable(document, report);

            // Section 2: Stock item summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE COMPRAS DE STOCK", "Resumen por Item", report);
            addStockItemSummaryTable(document, report);

            // Section 3: Purchase detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE COMPRAS DE STOCK", "Detalle de Compras", report);
            addPurchaseDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Stock purchase PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating stock purchase PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Categoría
    // ═══════════════════════════════════════════════════════════════════════

    private void addCategorySummaryTable(Document document, StockPurchaseReportDTO report) {
        Table table = new Table(new float[]{3f, 1f, 1f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Categoría"));
        table.addHeaderCell(headerCell("Cant. Compras"));
        table.addHeaderCell(headerCell("Cantidad"));
        table.addHeaderCell(headerCell("Total ($)"));

        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            table.addCell(cell(category.categoryName()));
            table.addCell(cellCenter(String.valueOf(category.purchaseCount())));
            table.addCell(cellNumber(category.subtotalQuantity()));
            table.addCell(cellAmount(category.subtotalAmount()));
        }

        // Grand total row
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalCount())));
        table.addCell(totalCellNumber(report.totalQuantity()));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Item
    // ═══════════════════════════════════════════════════════════════════════

    private void addStockItemSummaryTable(Document document, StockPurchaseReportDTO report) {
        Table table = new Table(new float[]{3f, 1f, 1f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Item"));
        table.addHeaderCell(headerCell("Cant. Compras"));
        table.addHeaderCell(headerCell("Cantidad"));
        table.addHeaderCell(headerCell("Total ($)"));

        DeviceRgb categoryHeaderColor = new DeviceRgb(189, 215, 238);
        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            table.addCell(new Cell(1, 4)
                    .add(new Paragraph(category.categoryName()).setBold().setFontSize(9))
                    .setBackgroundColor(categoryHeaderColor).setPadding(4));

            for (StockPurchaseReportStockGroupDTO stock : category.stockGroups()) {
                table.addCell(cell(stock.stockName()));
                table.addCell(cellCenter(String.valueOf(stock.purchaseCount())));
                table.addCell(cellNumber(stock.totalQuantity()));
                table.addCell(cellAmount(stock.totalAmount()));
            }

            // Category subtotal
            table.addCell(new Cell(1, 2)
                    .add(new Paragraph("Subtotal " + category.categoryName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatNumber(category.subtotalQuantity())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(category.subtotalAmount())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 2).add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(5));
        table.addCell(totalCellNumber(report.totalQuantity()));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Compras
    // ═══════════════════════════════════════════════════════════════════════

    private void addPurchaseDetailTable(Document document, StockPurchaseReportDTO report) {
        Table table = new Table(new float[]{2f, 2f, 1f, 1f, 1.2f, 1.2f, 2.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Categoría"));
        table.addHeaderCell(headerCell("Item"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Cantidad"));
        table.addHeaderCell(headerCell("P. Unitario ($)"));
        table.addHeaderCell(headerCell("Total ($)"));
        table.addHeaderCell(headerCell("Notas"));

        for (StockPurchaseReportCategoryGroupDTO category : report.categoryGroups()) {
            for (StockPurchaseReportStockGroupDTO stock : category.stockGroups()) {
                for (StockPurchaseReportItemDTO purchase : stock.purchases()) {
                    table.addCell(new Cell().add(new Paragraph(category.categoryName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(stock.stockName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            purchase.date().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            formatNumber(purchase.quantity())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            purchase.unitPrice() != null ? formatAmount(purchase.unitPrice()) : "—").setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(purchase.totalAmount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            purchase.notes() != null ? purchase.notes() : "").setFontSize(8)));
                }

                // Stock item subtotal
                table.addCell(new Cell(1, 3).add(new Paragraph(
                        "Subtotal " + stock.stockName())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatNumber(stock.totalQuantity()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph("").setFontSize(8))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(stock.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph("").setFontSize(8))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
            }

            // Category subtotal
            table.addCell(new Cell(1, 3).add(new Paragraph(
                    "Subtotal " + category.categoryName() + " (" + category.purchaseCount() + " compras)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatNumber(category.subtotalQuantity()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(category.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 3).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " compras)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatNumber(report.totalQuantity())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   StockPurchaseReportDTO report) {
        document.add(new Paragraph(title)
                .setFontSize(18).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        document.add(new Paragraph("ESEA S.A.")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        document.add(new Paragraph(subtitle)
                .setFontSize(14).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

        Table meta = new Table(2);
        meta.setWidth(UnitValue.createPercentValue(100));
        addMetaRow(meta, "Período:", report.periodDescription());
        addMetaRow(meta, "Fecha de generación:",
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        addMetaRow(meta, "Total de registros:", String.valueOf(report.totalCount()));
        document.add(meta);
        document.add(new Paragraph("\n"));
    }

    private void addMetaRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value)).setBorder(null));
    }

    private void addGenerationFooter(Document document, StockPurchaseReportDTO report) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "Reporte generado el " +
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - Sistema de Gestión ESEA S.A.")
                .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "$ 0,00";
        return String.format("$ %,.2f", amount);
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return String.format("%,.2f", number);
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    private Cell cell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(9)).setPadding(3);
    }

    private Cell cellCenter(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(9))
                .setTextAlignment(TextAlignment.CENTER).setPadding(3);
    }

    private Cell cellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT).setPadding(3);
    }

    private Cell cellNumber(BigDecimal number) {
        return new Cell().add(new Paragraph(formatNumber(number)).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT).setPadding(3);
    }

    private Cell totalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(TOTAL_COLOR)
                .setFontColor(ColorConstants.WHITE).setPadding(5);
    }

    private Cell totalCellCenter(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(5);
    }

    private Cell totalCellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(5);
    }

    private Cell totalCellNumber(BigDecimal number) {
        return new Cell().add(new Paragraph(formatNumber(number)).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(5);
    }
}
