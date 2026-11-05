package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.sales.*;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import PSG.backEnd.service.util.MessageSourceHelper;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF exporter for the Sales Report (3 sections).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SalesReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb CERT_BG = new DeviceRgb(252, 248, 227);

    private static final List<SalesDocumentType> DOCUMENT_TYPE_ORDER = List.of(
            SalesDocumentType.FACTURA_A, SalesDocumentType.FACTURA_B, SalesDocumentType.FACTURA_C,
            SalesDocumentType.NOTA_DEBITO_A, SalesDocumentType.NOTA_DEBITO_B, SalesDocumentType.NOTA_DEBITO_C,
            SalesDocumentType.NOTA_CREDITO_A, SalesDocumentType.NOTA_CREDITO_B, SalesDocumentType.NOTA_CREDITO_C);

    private static final Map<SalesDocumentType, String> DOC_TYPE_LABELS;
    static {
        Map<SalesDocumentType, String> m = new LinkedHashMap<>();
        m.put(SalesDocumentType.FACTURA_A, "Factura A");
        m.put(SalesDocumentType.FACTURA_B, "Factura B");
        m.put(SalesDocumentType.FACTURA_C, "Factura C");
        m.put(SalesDocumentType.NOTA_DEBITO_A, "N.D. A");
        m.put(SalesDocumentType.NOTA_DEBITO_B, "N.D. B");
        m.put(SalesDocumentType.NOTA_DEBITO_C, "N.D. C");
        m.put(SalesDocumentType.NOTA_CREDITO_A, "N.C. A");
        m.put(SalesDocumentType.NOTA_CREDITO_B, "N.C. B");
        m.put(SalesDocumentType.NOTA_CREDITO_C, "N.C. C");
        DOC_TYPE_LABELS = m;
    }

    private static final Map<CertificationStatus, String> CERT_STATUS_LABELS;
    static {
        Map<CertificationStatus, String> m = new LinkedHashMap<>();
        m.put(CertificationStatus.PRESENTADO, "Presentado");
        m.put(CertificationStatus.APROBADO, "Aprobado");
        m.put(CertificationStatus.FACTURADO, "Facturado");
        m.put(CertificationStatus.COBRADO, "Cobrado");
        CERT_STATUS_LABELS = m;
    }

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SalesReportDTO report) {
        log.info("Exporting sales report to PDF: {} areas, {} rows",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);

            List<SalesDocumentType> activeTypes = DOCUMENT_TYPE_ORDER.stream()
                    .filter(dt -> report.totalsByDocumentType().containsKey(dt))
                    .toList();

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE VENTAS", "Resumen por Área", report);
            addAreaSummaryTable(document, report, activeTypes);

            // Section 2: Client summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE VENTAS", "Resumen por Cliente", report);
            addClientSummaryTable(document, report, activeTypes);

            // Section 3: Detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE VENTAS", "Detalle de Ventas", report);
            addDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Sales PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating sales PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    private void addAreaSummaryTable(Document document, SalesReportDTO report, List<SalesDocumentType> activeTypes) {
        int colCount = 3 + activeTypes.size() + 1; // Área, Cant, [types], CertOnly, Total
        float[] widths = new float[colCount];
        widths[0] = 3f;
        for (int i = 1; i < colCount; i++) widths[i] = 2f;
        Table table = new Table(UnitValue.createPercentArray(widths));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Cant."));
        for (SalesDocumentType dt : activeTypes) {
            table.addHeaderCell(headerCell(DOC_TYPE_LABELS.get(dt)));
        }
        table.addHeaderCell(headerCell("Cert. s/Fact."));
        table.addHeaderCell(headerCell("Total"));

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.rowCount())));
            for (SalesDocumentType dt : activeTypes) {
                table.addCell(cellAmount(area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO)));
            }
            table.addCell(cellAmount(nz(area.subtotalCertifiedOnly())));
            table.addCell(cellAmount(area.subtotalAmount()));
        }

        // Totals
        table.addCell(totalCell("TOTAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalCount())));
        for (SalesDocumentType dt : activeTypes) {
            table.addCell(totalCellAmount(report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO)));
        }
        table.addCell(totalCellAmount(nz(report.totalCertifiedOnly())));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    private void addClientSummaryTable(Document document, SalesReportDTO report, List<SalesDocumentType> activeTypes) {
        int colCount = 3 + activeTypes.size() + 1;
        float[] widths = new float[colCount];
        widths[0] = 4f;
        widths[1] = 2.5f;
        for (int i = 2; i < colCount; i++) widths[i] = 2f;
        Table table = new Table(UnitValue.createPercentArray(widths));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Cliente"));
        table.addHeaderCell(headerCell("CUIT"));
        for (SalesDocumentType dt : activeTypes) {
            table.addHeaderCell(headerCell(DOC_TYPE_LABELS.get(dt)));
        }
        table.addHeaderCell(headerCell("Cert. s/Fact."));
        table.addHeaderCell(headerCell("Total"));

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            // Area band row spanning all
            Cell areaBand = new Cell(1, colCount)
                    .add(new Paragraph("Área: " + area.projectAreaName()).setBold().setFontSize(10))
                    .setBackgroundColor(new DeviceRgb(220, 232, 244));
            table.addCell(areaBand);

            for (SalesReportClientGroupDTO client : area.clientGroups()) {
                table.addCell(cell(client.clientBusinessName() != null ? client.clientBusinessName() : "(Sin razón social)"));
                table.addCell(cellCenter(client.clientCuit() != null ? client.clientCuit() : "-"));
                for (SalesDocumentType dt : activeTypes) {
                    table.addCell(cellAmount(client.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO)));
                }
                table.addCell(cellAmount(nz(client.subtotalCertifiedOnly())));
                table.addCell(cellAmount(client.totalAmount()));
            }

            // Area subtotal
            table.addCell(subtotalCell("Subtotal " + area.projectAreaName()));
            table.addCell(subtotalCellCenter(String.valueOf(area.rowCount())));
            for (SalesDocumentType dt : activeTypes) {
                table.addCell(subtotalCellAmount(area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO)));
            }
            table.addCell(subtotalCellAmount(nz(area.subtotalCertifiedOnly())));
            table.addCell(subtotalCellAmount(area.subtotalAmount()));
        }

        // Total
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(""));
        for (SalesDocumentType dt : activeTypes) {
            table.addCell(totalCellAmount(report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO)));
        }
        table.addCell(totalCellAmount(nz(report.totalCertifiedOnly())));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    private void addDetailTable(Document document, SalesReportDTO report) {
        // 9 columns: Tipo, Fecha, Cliente/Contrato, Comprobante/Cert, Tipo/Estado, Sector/Tarea, Pagado, Cert. Vinc., Total
        float[] widths = new float[]{1.4f, 1.5f, 3f, 2.2f, 2.2f, 2.5f, 1.2f, 2.2f, 2f};
        Table table = new Table(UnitValue.createPercentArray(widths));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Tipo"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Cliente / Contrato"));
        table.addHeaderCell(headerCell("Comprobante / Cert."));
        table.addHeaderCell(headerCell("Tipo / Estado"));
        table.addHeaderCell(headerCell("Sector / Tarea"));
        table.addHeaderCell(headerCell("Pag."));
        table.addHeaderCell(headerCell("Cert. Vinc."));
        table.addHeaderCell(headerCell("Total"));

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            Cell areaBand = new Cell(1, 9)
                    .add(new Paragraph("ÁREA: " + area.projectAreaName()).setBold().setFontSize(11))
                    .setBackgroundColor(new DeviceRgb(220, 232, 244))
                    .setPadding(4);
            table.addCell(areaBand);

            for (SalesReportClientGroupDTO client : area.clientGroups()) {
                String cl = "Cliente: " + (client.clientBusinessName() != null ? client.clientBusinessName() : "(Sin razón social)")
                        + (client.clientCuit() != null ? " — CUIT: " + client.clientCuit() : "");
                Cell clientBand = new Cell(1, 9)
                        .add(new Paragraph(cl).setBold().setFontSize(9))
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setPadding(3);
                table.addCell(clientBand);

                for (SalesReportRowDTO r : client.rows()) {
                    boolean cert = r.kind() == SalesReportRowKind.CERTIFICATION_ONLY;
                    DeviceRgb bg = cert ? CERT_BG : null;

                    table.addCell(maybeBg(cellCenter(cert ? "Cert. s/Fact." : "Factura"), bg));
                    table.addCell(maybeBg(cellCenter(r.date() != null ? r.date().format(DATE_FORMATTER) : ""), bg));

                    if (cert) {
                        String cliCol = r.contractNumber() != null
                                ? "Contrato: " + r.contractNumber()
                                : "(sin contrato)";
                        if (r.contractDescription() != null && !r.contractDescription().isBlank()) {
                            cliCol += " — " + r.contractDescription();
                        }
                        table.addCell(maybeBg(cell(cliCol), bg));
                        table.addCell(maybeBg(cellCenter("Certif. #" + (r.certificationNumber() != null ? r.certificationNumber() : "-")), bg));
                        table.addCell(maybeBg(cellCenter(r.certificationStatus() != null
                                ? CERT_STATUS_LABELS.getOrDefault(r.certificationStatus(), r.certificationStatus().name())
                                : "-"), bg));
                    } else {
                        table.addCell(maybeBg(cell(client.clientBusinessName() != null ? client.clientBusinessName() : "-"), bg));
                        String num = (r.branchCode() != null ? r.branchCode() : "")
                                + (r.documentNumber() != null ? "-" + r.documentNumber() : "");
                        table.addCell(maybeBg(cellCenter(num.isBlank() ? "-" : num), bg));
                        table.addCell(maybeBg(cellCenter(r.documentType() != null
                                ? DOC_TYPE_LABELS.getOrDefault(r.documentType(), r.documentType().name())
                                : "-"), bg));
                    }

                    String areaTask = area.projectAreaName()
                            + (r.projectAreaTaskName() != null ? " / " + r.projectAreaTaskName() : "");
                    table.addCell(maybeBg(cell(areaTask), bg));
                    table.addCell(maybeBg(cellCenter(r.paid() != null && r.paid() ? "Sí" : "No"), bg));

                    String linkedCerts = "";
                    if (!cert && r.linkedCertifications() != null && !r.linkedCertifications().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (SalesReportCertificationLinkDTO lc : r.linkedCertifications()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("#").append(lc.certificationNumber());
                            if (lc.contractNumber() != null) sb.append(" (").append(lc.contractNumber()).append(")");
                        }
                        linkedCerts = sb.toString();
                    }
                    table.addCell(maybeBg(cell(linkedCerts), bg));
                    table.addCell(maybeBg(cellAmount(nz(r.amount())), bg));
                }
            }
        }

        Cell totalLabel = new Cell(1, 8)
                .add(new Paragraph("TOTAL GENERAL").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(4);
        table.addCell(totalLabel);
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    private Cell maybeBg(Cell c, DeviceRgb bg) {
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ─── Helpers ───
    private void addSectionHeader(Document document, String title, String subtitle, SalesReportDTO report) {
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

    private void addGenerationFooter(Document document, SalesReportDTO report) {
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
        return new Cell().add(new Paragraph(text != null ? text : "").setFontSize(8)).setPadding(3);
    }

    private Cell cellCenter(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "").setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER).setPadding(3);
    }

    private Cell cellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT).setPadding(3);
    }

    private Cell subtotalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3);
    }

    private Cell subtotalCellCenter(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR).setTextAlignment(TextAlignment.CENTER).setPadding(3);
    }

    private Cell subtotalCellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR).setTextAlignment(TextAlignment.RIGHT).setPadding(3);
    }

    private Cell totalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(4);
    }

    private Cell totalCellCenter(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(4);
    }

    private Cell totalCellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(4);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("$ %,.2f", amount != null ? amount : BigDecimal.ZERO);
    }
}
