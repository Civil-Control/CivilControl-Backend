package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.invoice.*;
import PSG.backEnd.model.enums.documents.DocumentType;
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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class InvoiceReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private static final List<DocumentType> DOCUMENT_TYPE_ORDER = List.of(
            DocumentType.BILL_A, DocumentType.BILL_B, DocumentType.BILL_C,
            DocumentType.DEBIT_NOTE_A, DocumentType.DEBIT_NOTE_B, DocumentType.DEBIT_NOTE_C,
            DocumentType.CREDIT_NOTE_A, DocumentType.CREDIT_NOTE_B, DocumentType.CREDIT_NOTE_C,
            DocumentType.OTHER_DOCUMENT);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(InvoiceReportDTO report) {
        log.info("Exporting invoice report to PDF: {} areas, {} total documents",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            List<DocumentType> activeTypes = DOCUMENT_TYPE_ORDER.stream()
                    .filter(dt -> report.totalsByDocumentType().containsKey(dt))
                    .toList();

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE FACTURACIÓN", "Resumen por Área", report);
            addAreaSummaryTable(document, report, activeTypes);

            // Section 2: Supplier summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE FACTURACIÓN", "Resumen por Proveedor", report);
            addSupplierSummaryTable(document, report, activeTypes);

            // Section 3: Document detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE FACTURACIÓN", "Detalle de Comprobantes", report);
            addDocumentDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Invoice PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void addAreaSummaryTable(Document document, InvoiceReportDTO report,
                                      List<DocumentType> activeTypes) {
        int colCount = 3 + activeTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 3;
        colWidths[1] = 1;
        for (int i = 0; i < activeTypes.size(); i++) {
            colWidths[2 + i] = 1.5f;
        }
        colWidths[colCount - 1] = 1.5f;

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Cant. Docs"));
        for (DocumentType dt : activeTypes) {
            table.addHeaderCell(headerCell("Total " + dt.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.documentCount())));
            for (DocumentType dt : activeTypes) {
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                table.addCell(cellAmount(val));
            }
            table.addCell(cellAmount(area.subtotalAmount()));
        }

        // Grand total row
        Cell totalLabel = new Cell().add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(5);
        table.addCell(totalLabel);
        table.addCell(new Cell().add(new Paragraph(String.valueOf(report.totalCount())).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(5));
        for (DocumentType dt : activeTypes) {
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
            table.addCell(new Cell().add(new Paragraph(formatAmount(val)).setBold())
                    .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(5));
        }
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(5));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Proveedor
    // ═══════════════════════════════════════════════════════════════════════

    private void addSupplierSummaryTable(Document document, InvoiceReportDTO report,
                                          List<DocumentType> activeTypes) {
        int colCount = 3 + activeTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 3;
        colWidths[1] = 1.5f;
        for (int i = 0; i < activeTypes.size(); i++) {
            colWidths[2 + i] = 1.5f;
        }
        colWidths[colCount - 1] = 1.5f;

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Proveedor (Razón Social)"));
        table.addHeaderCell(headerCell("CUIT"));
        for (DocumentType dt : activeTypes) {
            table.addHeaderCell(headerCell("Total " + dt.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        DeviceRgb areaHeaderColor = new DeviceRgb(189, 215, 238);
        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(new Cell(1, colCount)
                    .add(new Paragraph(area.projectAreaName()).setBold().setFontSize(9))
                    .setBackgroundColor(areaHeaderColor).setPadding(4));

            for (InvoiceReportSupplierGroupDTO supplier : area.supplierGroups()) {
                table.addCell(cell(supplier.supplierLegalName()));
                table.addCell(cell(supplier.supplierCuit() != null ? supplier.supplierCuit() : "-"));
                for (DocumentType dt : activeTypes) {
                    BigDecimal val = supplier.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                    table.addCell(cellAmount(val));
                }
                table.addCell(cellAmount(supplier.totalAmount()));
            }

            table.addCell(new Cell()
                    .add(new Paragraph("Subtotal " + area.projectAreaName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph("").setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            for (DocumentType dt : activeTypes) {
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                table.addCell(new Cell()
                        .add(new Paragraph(formatAmount(val)).setFontSize(9).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            }
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(area.subtotalAmount())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total row
        table.addCell(new Cell()
                .add(new Paragraph("TOTAL GENERAL").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));
        for (DocumentType dt : activeTypes) {
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
            table.addCell(new Cell().add(new Paragraph(formatAmount(val)).setBold().setFontSize(10))
                    .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        }
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Comprobantes
    // ═══════════════════════════════════════════════════════════════════════

    private void addDocumentDetailTable(Document document, InvoiceReportDTO report) {
        Table table = new Table(new float[]{1.8f, 2f, 1f, 1.2f, 1.2f, 1f, 1f, 1f, 1f, 1f, 1.2f, 0.8f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Proveedor"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Tipo Doc."));
        table.addHeaderCell(headerCell("PV-Nro"));
        table.addHeaderCell(headerCell("Neto"));
        table.addHeaderCell(headerCell("IVA"));
        table.addHeaderCell(headerCell("IVA Exento"));
        table.addHeaderCell(headerCell("Otros Trib."));
        table.addHeaderCell(headerCell("Perc. IIBB"));
        table.addHeaderCell(headerCell("Total"));
        table.addHeaderCell(headerCell("Pagado"));

        for (InvoiceReportAreaGroupDTO area : report.areaGroups()) {
            for (InvoiceReportSupplierGroupDTO supplier : area.supplierGroups()) {
                for (InvoiceReportDocumentDTO doc : supplier.documents()) {
                    String areaDisplay = area.projectAreaName();
                    if (doc.projectAreaTaskName() != null) {
                        areaDisplay += " - " + doc.projectAreaTaskName();
                    }
                    table.addCell(new Cell().add(new Paragraph(areaDisplay).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(supplier.supplierLegalName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            doc.date().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            doc.documentType().getDisplayName()).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            doc.branchCode() + "-" + doc.documentNumber()).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(doc.netTotal() != null ? doc.netTotal() : BigDecimal.ZERO)).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(doc.ivaTotal() != null ? doc.ivaTotal() : BigDecimal.ZERO)).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(doc.ivaExemptTotal() != null ? doc.ivaExemptTotal() : BigDecimal.ZERO)).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(doc.otherTaxes() != null ? doc.otherTaxes() : BigDecimal.ZERO)).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(doc.iibbPerception() != null ? doc.iibbPerception() : BigDecimal.ZERO)).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(formatAmount(doc.totalAmount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            Boolean.TRUE.equals(doc.paid()) ? "Sí" : "No").setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                }

                // Supplier subtotal
                table.addCell(new Cell(1, 10).add(new Paragraph(
                        "Subtotal " + supplier.supplierLegalName())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(supplier.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(""))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
            }

            // Area subtotal
            table.addCell(new Cell(1, 10).add(new Paragraph(
                    "Subtotal " + area.projectAreaName() + " (" + area.documentCount() + " docs)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(""))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
        }

        // Grand total
        Cell totalLabel = new Cell(1, 10).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " docs)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6);
        table.addCell(totalLabel);
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(""))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   InvoiceReportDTO report) {
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

    private void addGenerationFooter(Document document, InvoiceReportDTO report) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "Reporte generado el " +
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - Sistema de Gestión ESEA S.A.")
                .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
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

    private String formatAmount(BigDecimal amount) {
        return String.format("$ %,.2f", amount);
    }
}
