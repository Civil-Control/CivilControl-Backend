package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.service.util.MessageSourceHelper;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PDF Report Exporter using iText7 library.
 * Generates professional PDF reports with tables, headers, and summaries.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PdfReportExporter implements IReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185); // Blue
    private static final DeviceRgb SUMMARY_COLOR = new DeviceRgb(243, 156, 18); // Orange

    private final MessageSourceHelper messageSourceHelper;

    @Override
    public byte[] export(MoneyOutflowReportDTO report) {
        log.info("Exporting report to PDF with {} items", report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(com.itextpdf.kernel.geom.PageSize.A4.rotate());
            Document document = new Document(pdfDoc);

            // Add title
            addTitle(document, report);

            // Add metadata
            addMetadata(document, report);

            // Add summary by category
            addSummary(document, report);

            // Add items table
            addItemsTable(document, report);

            // Add footer with totals
            addFooter(document, report);

            document.close();

            log.info("PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new ReportGenerationException(messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    /**
     * Adds the title to the document.
     */
    private void addTitle(Document document, MoneyOutflowReportDTO report) {
        Paragraph title = new Paragraph("REPORTE DE SALIDAS DE DINERO")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("ESEA S.A.")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);
    }

    /**
     * Adds metadata section with report information.
     */
    private void addMetadata(Document document, MoneyOutflowReportDTO report) {
        Table metadataTable = new Table(2);
        metadataTable.setWidth(UnitValue.createPercentValue(100));

        addMetadataRow(metadataTable, "Período:", report.periodDescription());
        addMetadataRow(metadataTable, "Fecha de generación:",
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        addMetadataRow(metadataTable, "Total de registros:", report.totalCount().toString());

        // Add project area if filtered
        if (report.projectAreaName() != null) {
            addMetadataRow(metadataTable, "Sector:", report.projectAreaName());
        }

        if (report.filters().categories() != null && !report.filters().categories().isEmpty()) {
            String categoriesStr = report.filters().categories().stream()
                    .map(MoneyOutflowCategory::getDisplayName)
                    .collect(Collectors.joining(", "));
            addMetadataRow(metadataTable, "Categorías:", categoriesStr);
        }

        document.add(metadataTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds a metadata row to the table.
     */
    private void addMetadataRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold())
                .setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value))
                .setBorder(null));
    }

    /**
     * Adds summary by category section.
     */
    private void addSummary(Document document, MoneyOutflowReportDTO report) {
        if (report.summaryByCategory().isEmpty()) {
            return;
        }

        Paragraph summaryTitle = new Paragraph("Resumen por Categoría")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(summaryTitle);

        Table summaryTable = new Table(new float[]{3, 2});
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        // Header
        summaryTable.addHeaderCell(createHeaderCell("Categoría"));
        summaryTable.addHeaderCell(createHeaderCell("Total"));

        // Rows
        for (Map.Entry<MoneyOutflowCategory, BigDecimal> entry : report.summaryByCategory().entrySet()) {
            summaryTable.addCell(new Cell().add(new Paragraph(entry.getKey().getDisplayName())));
            summaryTable.addCell(new Cell().add(new Paragraph(formatAmount(entry.getValue())))
                    .setTextAlignment(TextAlignment.RIGHT));
        }

        document.add(summaryTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds the main items table.
     */
    private void addItemsTable(Document document, MoneyOutflowReportDTO report) {
        Paragraph itemsTitle = new Paragraph("Detalle de Salidas")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);
        document.add(itemsTitle);

        // Create table with 8 columns
        Table table = new Table(new float[]{1.2f, 1f, 1f, 2.5f, 1.5f, 1f, 1f, 1.2f});
        table.setWidth(UnitValue.createPercentValue(100));

        // Headers
        table.addHeaderCell(createHeaderCell("Fecha"));
        table.addHeaderCell(createHeaderCell("Categoría"));
        table.addHeaderCell(createHeaderCell("Área"));
        table.addHeaderCell(createHeaderCell("Descripción"));
        table.addHeaderCell(createHeaderCell("Beneficiario"));
        table.addHeaderCell(createHeaderCell("Método"));
        table.addHeaderCell(createHeaderCell("Referencia"));
        table.addHeaderCell(createHeaderCell("Monto"));

        // Rows
        for (ReportItemDTO item : report.items()) {
            table.addCell(new Cell().add(new Paragraph(item.date().format(DATE_FORMATTER)))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(item.category().getDisplayName()))
                    .setFontSize(7));
            table.addCell(new Cell().add(new Paragraph(item.projectAreaName() != null ? item.projectAreaName() : ""))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(item.description() != null ? item.description() : ""))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(item.beneficiary() != null ? item.beneficiary() : ""))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(item.paymentMethod() != null ? item.paymentMethod() : ""))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(item.reference() != null ? item.reference() : ""))
                    .setFontSize(8));
            table.addCell(new Cell().add(new Paragraph(formatAmount(item.amount())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(8));
        }

        document.add(table);
    }

    /**
     * Adds footer with total amount.
     */
    private void addFooter(Document document, MoneyOutflowReportDTO report) {
        document.add(new Paragraph("\n"));

        DeviceRgb adjustedColor = new DeviceRgb(39, 174, 96); // Green

        Table footerTable = new Table(new float[]{3, 2});
        footerTable.setWidth(UnitValue.createPercentValue(100));

        Cell labelCell = new Cell().add(new Paragraph("TOTAL GENERAL").setBold().setFontSize(12))
                .setBackgroundColor(SUMMARY_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(10);

        Cell valueCell = new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(12))
                .setBackgroundColor(SUMMARY_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(10);

        footerTable.addCell(labelCell);
        footerTable.addCell(valueCell);

        // Show adjusted total when there are duplicated amounts
        if (report.duplicatedAmount() != null && report.duplicatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            Cell dupLabelCell = new Cell().add(new Paragraph("MONTO VINCULADO (incluido en facturas)").setFontSize(10))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(5);
            Cell dupValueCell = new Cell().add(new Paragraph("- " + formatAmount(report.duplicatedAmount())).setFontSize(10))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(5);
            footerTable.addCell(dupLabelCell);
            footerTable.addCell(dupValueCell);

            BigDecimal adjustedTotal = report.totalAmount().subtract(report.duplicatedAmount());
            Cell adjLabelCell = new Cell().add(new Paragraph("TOTAL AJUSTADO").setBold().setFontSize(12))
                    .setBackgroundColor(adjustedColor)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(10);
            Cell adjValueCell = new Cell().add(new Paragraph(formatAmount(adjustedTotal)).setBold().setFontSize(12))
                    .setBackgroundColor(adjustedColor)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setPadding(10);
            footerTable.addCell(adjLabelCell);
            footerTable.addCell(adjValueCell);
        }

        document.add(footerTable);

        // Add generation info
        Paragraph generatedInfo = new Paragraph(
                "Reporte generado el " + report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - Sistema de Gestión ESEA S.A.")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(generatedInfo);
    }

    /**
     * Creates a styled header cell.
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    /**
     * Formats a BigDecimal amount as currency.
     */
    private String formatAmount(BigDecimal amount) {
        return String.format("$ %.2f", amount);
    }

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.PDF;
    }

    @Override
    public String getContentType() {
        return ReportFormat.PDF.getContentType();
    }

    @Override
    public String getFileExtension() {
        return ReportFormat.PDF.getFileExtension();
    }
}

