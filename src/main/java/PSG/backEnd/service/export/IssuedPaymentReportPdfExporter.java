package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.issuedPayment.*;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.payment.CheckStatus;
import PSG.backEnd.model.enums.report.IssuedPaymentReportGroupBy;
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

/**
 * PDF exporter for the Issued Payments Report (Feature 16).
 *
 * <p>Renders 3 sections (summary by Layer-1, summary by Layer-2, detail) plus a
 * check-status summary card. Check rows are tinted with the standard status colors
 * documented in F16 §B.PDF for visual parity with the frontend badges.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IssuedPaymentReportPdfExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DeviceRgb HEADER_COLOR   = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR    = new DeviceRgb(243, 156, 18);

    // Check status colors (synced with frontend constants in F16 spec)
    private static final DeviceRgb COLOR_PENDIENTE = new DeviceRgb(160, 160, 160);
    private static final DeviceRgb COLOR_VENCIDO   = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb COLOR_COBRADO   = new DeviceRgb(120, 180, 120);
    private static final DeviceRgb COLOR_RECHAZADO = new DeviceRgb(220,  80,  80);
    private static final DeviceRgb COLOR_CANCELADO = new DeviceRgb(100, 100, 100);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(IssuedPaymentReportDTO report) {
        log.info("Exporting Issued Payments report to PDF: groupBy={}, totalCount={}",
                report.groupBy(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4.rotate());

            IssuedPaymentReportGroupBy gb = report.groupBy() == null
                    ? IssuedPaymentReportGroupBy.METHOD : report.groupBy();
            String l1 = gb == IssuedPaymentReportGroupBy.METHOD ? "Método" : "Proveedor";
            String l2 = gb == IssuedPaymentReportGroupBy.METHOD ? "Proveedor" : "Método";

            addSectionHeader(doc, "REPORTE DE PAGOS EMITIDOS", "Resumen por " + l1, report);
            addLayer1Summary(doc, report, l1);

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(doc, "REPORTE DE PAGOS EMITIDOS", "Resumen por " + l2, report);
            addLayer2Summary(doc, report, l1, l2);

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(doc, "REPORTE DE PAGOS EMITIDOS", "Detalle de Pagos", report);
            addDetailTable(doc, report);

            if (report.checkSummary() != null && report.checkSummary().totalChecks() > 0) {
                doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                addSectionHeader(doc, "REPORTE DE PAGOS EMITIDOS", "Resumen de Cheques", report);
                addCheckSummary(doc, report.checkSummary());
            }

            addFooter(doc, report);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Issued Payments PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ─────────────── sections ───────────────

    private void addLayer1Summary(Document doc, IssuedPaymentReportDTO r, String l1) {
        Table t = new Table(new float[]{4, 1.2f, 2});
        t.setWidth(UnitValue.createPercentValue(100));
        t.addHeaderCell(headerCell(l1));
        t.addHeaderCell(headerCell("Cant."));
        t.addHeaderCell(headerCell("Total"));

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            String label = g.groupSubLabel() != null && !g.groupSubLabel().isBlank()
                    ? g.groupLabel() + "  (" + g.groupSubLabel() + ")"
                    : g.groupLabel();
            t.addCell(cell(label));
            t.addCell(cellCenter(String.valueOf(g.paymentCount())));
            t.addCell(cellAmount(g.subtotalAmount()));
        }
        t.addCell(totalCell("TOTAL"));
        t.addCell(totalCellCenter(String.valueOf(r.totalCount())));
        t.addCell(totalCellAmount(r.totalAmount()));
        doc.add(t);
    }

    private void addLayer2Summary(Document doc, IssuedPaymentReportDTO r, String l1, String l2) {
        Table t = new Table(new float[]{3, 3, 1, 2});
        t.setWidth(UnitValue.createPercentValue(100));
        t.addHeaderCell(headerCell(l1));
        t.addHeaderCell(headerCell(l2));
        t.addHeaderCell(headerCell("Cant."));
        t.addHeaderCell(headerCell("Total"));

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            t.addCell(subtotalCell(g.groupLabel()));
            t.addCell(subtotalCell(g.groupSubLabel() != null ? g.groupSubLabel() : ""));
            t.addCell(subtotalCellCenter(String.valueOf(g.paymentCount())));
            t.addCell(subtotalCellAmount(g.subtotalAmount()));

            for (IssuedPaymentReportSecondaryGroupDTO sg : g.secondaryGroups()) {
                t.addCell(cell(""));
                t.addCell(cell(sg.groupLabel()));
                t.addCell(cellCenter(String.valueOf(sg.paymentCount())));
                t.addCell(cellAmount(sg.subtotalAmount()));
            }
        }
        t.addCell(totalCell("TOTAL"));
        t.addCell(totalCell(""));
        t.addCell(totalCellCenter(String.valueOf(r.totalCount())));
        t.addCell(totalCellAmount(r.totalAmount()));
        doc.add(t);
    }

    private void addDetailTable(Document doc, IssuedPaymentReportDTO r) {
        Table t = new Table(new float[]{1.4f, 1.2f, 3, 1.6f, 2, 1.5f, 1.4f, 1.4f, 1.4f});
        t.setWidth(UnitValue.createPercentValue(100));
        t.addHeaderCell(headerCell("Fecha"));
        t.addHeaderCell(headerCell("Método"));
        t.addHeaderCell(headerCell("Proveedor"));
        t.addHeaderCell(headerCell("Monto"));
        t.addHeaderCell(headerCell("Referencia"));
        t.addHeaderCell(headerCell("Cuenta / Caja"));
        t.addHeaderCell(headerCell("Vencimiento"));
        t.addHeaderCell(headerCell("Estado"));
        t.addHeaderCell(headerCell("Cobrado el"));

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            Cell pHeader = new Cell(1, 9).add(new Paragraph(g.groupLabel()
                    + (g.groupSubLabel() != null ? " (" + g.groupSubLabel() + ")" : "")
                    + "  —  Subtotal: " + formatAmount(g.subtotalAmount())).setBold().setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4);
            t.addCell(pHeader);

            for (IssuedPaymentReportSecondaryGroupDTO sg : g.secondaryGroups()) {
                Cell sHeader = new Cell(1, 9).add(new Paragraph("    " + sg.groupLabel()
                        + "  —  Subtotal: " + formatAmount(sg.subtotalAmount())).setBold().setFontSize(9))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4);
                t.addCell(sHeader);

                for (IssuedPaymentReportItemDTO p : sg.payments()) {
                    DeviceRgb tint = p.method() == PaymentMethod.CHECK
                            ? statusTint(p.checkStatus()) : null;

                    t.addCell(tinted(cellCenter(p.paymentDate() != null ? p.paymentDate().format(DATE) : ""), tint));
                    t.addCell(tinted(cellCenter(methodLabel(p.method())), tint));
                    t.addCell(tinted(cell(supplierLabel(p)), tint));
                    t.addCell(tinted(cellAmount(p.amount()), tint));
                    t.addCell(tinted(cell(buildReference(p)), tint));
                    t.addCell(tinted(cell(treasuryRef(p)), tint));
                    t.addCell(tinted(cellCenter(p.checkDueDate() != null ? p.checkDueDate().format(DATE) : ""), tint));
                    t.addCell(tinted(cellCenter(p.checkStatus() != null ? checkStatusLabel(p.checkStatus()) : ""), tint));
                    t.addCell(tinted(cellCenter(p.checkSettledDate() != null ? p.checkSettledDate().format(DATE) : ""), tint));
                }
            }
        }

        t.addCell(totalCell("TOTAL GENERAL"));
        t.addCell(totalCell(""));
        t.addCell(totalCell(""));
        t.addCell(totalCellAmount(r.totalAmount()));
        t.addCell(totalCell(""));
        t.addCell(totalCell(""));
        t.addCell(totalCell(""));
        t.addCell(totalCellCenter(String.valueOf(r.totalCount())));
        t.addCell(totalCell(""));
        doc.add(t);
    }

    private void addCheckSummary(Document doc, CheckSummaryDTO cs) {
        Table t = new Table(new float[]{2, 1, 2});
        t.setWidth(UnitValue.createPercentValue(60));
        t.addHeaderCell(headerCell("Estado"));
        t.addHeaderCell(headerCell("Cant."));
        t.addHeaderCell(headerCell("Monto"));
        addSummaryRow(t, "Pendiente", cs.pendingCount(),  cs.pendingAmount(),  COLOR_PENDIENTE);
        addSummaryRow(t, "Vencido",   cs.overdueCount(),  cs.overdueAmount(),  COLOR_VENCIDO);
        addSummaryRow(t, "Cobrado",   cs.settledCount(),  cs.settledAmount(),  COLOR_COBRADO);
        addSummaryRow(t, "Rechazado", cs.rejectedCount(), cs.rejectedAmount(), COLOR_RECHAZADO);
        addSummaryRow(t, "Cancelado", cs.cancelledCount(),cs.cancelledAmount(),COLOR_CANCELADO);
        t.addCell(totalCell("TOTAL"));
        t.addCell(totalCellCenter(String.valueOf(cs.totalChecks())));
        t.addCell(totalCellAmount(cs.totalChecksAmount()));
        doc.add(t);
    }

    private void addSummaryRow(Table t, String label, int count, BigDecimal amount, DeviceRgb tint) {
        t.addCell(tinted(cell(label), tint));
        t.addCell(tinted(cellCenter(String.valueOf(count)), tint));
        t.addCell(tinted(cellAmount(amount), tint));
    }

    // ─────────────── helpers ───────────────

    private static String supplierLabel(IssuedPaymentReportItemDTO p) {
        if (p.supplierTradeName() != null && !p.supplierTradeName().isBlank()) return p.supplierTradeName();
        return p.supplierLegalName() != null ? p.supplierLegalName() : "";
    }

    private static String treasuryRef(IssuedPaymentReportItemDTO p) {
        if (p.bankAccountName() != null) return p.bankAccountName();
        if (p.cashBoxName() != null) return p.cashBoxName();
        return "";
    }

    private static String buildReference(IssuedPaymentReportItemDTO p) {
        if (p.method() == null) return "";
        return switch (p.method()) {
            case CHECK    -> p.checkNumber() != null ? "Cheque N° " + p.checkNumber() : "Cheque";
            case TRANSFER -> p.transferTransactionNumber() != null
                    ? "Transf. " + p.transferTransactionNumber() : "Transferencia";
            case CASH     -> "Efectivo";
        };
    }

    private static DeviceRgb statusTint(CheckStatus st) {
        if (st == null) return null;
        return switch (st) {
            case PENDIENTE -> tintOf(COLOR_PENDIENTE);
            case VENCIDO   -> tintOf(COLOR_VENCIDO);
            case COBRADO   -> tintOf(COLOR_COBRADO);
            case RECHAZADO -> tintOf(COLOR_RECHAZADO);
            case CANCELADO -> tintOf(COLOR_CANCELADO);
        };
    }

    /** Lightens a color by mixing with white so that text remains readable in body cells. */
    private static DeviceRgb tintOf(DeviceRgb base) {
        float[] rgb = base.getColorValue();
        int r = (int) (rgb[0] * 255 * 0.25f + 255 * 0.75f);
        int g = (int) (rgb[1] * 255 * 0.25f + 255 * 0.75f);
        int b = (int) (rgb[2] * 255 * 0.25f + 255 * 0.75f);
        return new DeviceRgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private static Cell tinted(Cell c, DeviceRgb tint) {
        return tint == null ? c : c.setBackgroundColor(tint);
    }

    private static String methodLabel(PaymentMethod m) {
        if (m == null) return "";
        return switch (m) {
            case CASH     -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case CHECK    -> "Cheque";
        };
    }

    private static String checkStatusLabel(CheckStatus s) {
        return switch (s) {
            case PENDIENTE -> "Pendiente";
            case VENCIDO   -> "Vencido";
            case COBRADO   -> "Cobrado";
            case RECHAZADO -> "Rechazado";
            case CANCELADO -> "Cancelado";
        };
    }

    private void addSectionHeader(Document doc, String title, String subtitle, IssuedPaymentReportDTO r) {
        doc.add(new Paragraph(title).setFontSize(16).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(new Paragraph(subtitle).setFontSize(12).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
        doc.add(new Paragraph("Período: " + r.periodDescription())
                .setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
    }

    private void addFooter(Document doc, IssuedPaymentReportDTO r) {
        doc.add(new Paragraph("Reporte generado el "
                + r.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFontSize(7).setTextAlignment(TextAlignment.CENTER).setMarginTop(15));
    }

    // PDF cell builders (mirror ServicePaymentReportPdfExporter)
    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(HEADER_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(4);
    }
    private Cell cell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8)).setPadding(2);
    }
    private Cell cellCenter(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER).setPadding(2);
    }
    private Cell cellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setFontSize(8))
                .setTextAlignment(TextAlignment.RIGHT).setPadding(2);
    }
    private Cell subtotalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4);
    }
    private Cell subtotalCellCenter(String text) {
        return subtotalCell(text).setTextAlignment(TextAlignment.CENTER);
    }
    private Cell subtotalCellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(4);
    }
    private Cell totalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(4);
    }
    private Cell totalCellCenter(String text) {
        return totalCell(text).setTextAlignment(TextAlignment.CENTER);
    }
    private Cell totalCellAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(4);
    }
    private static String formatAmount(BigDecimal amount) {
        if (amount == null) return "-";
        return String.format("$ %,.2f", amount);
    }
}
