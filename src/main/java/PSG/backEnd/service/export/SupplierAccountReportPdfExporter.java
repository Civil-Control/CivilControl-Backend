package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountMovementDTO;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountReportDTO;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountReportStatusGroupDTO;
import PSG.backEnd.model.dto.report.supplierAccount.SupplierAccountReportSupplierGroupDTO;
import PSG.backEnd.model.enums.report.SupplierAccountMovementType;
import PSG.backEnd.model.enums.report.SupplierAccountStatus;
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
 * PDF exporter for the Supplier Current-Account Report.
 *
 * <p>Layout:
 * <ul>
 *     <li>Section 1 — Per-supplier summary table.</li>
 *     <li>Section 2 — One detail block per supplier with previous balance,
 *     chronological movements with running balance, and final balance.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SupplierAccountReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUPPLIER_GROUP_COLOR = new DeviceRgb(189, 215, 238);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb PENDING_COLOR = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb SETTLED_COLOR = new DeviceRgb(120, 180, 120);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SupplierAccountReportDTO report) {
        log.info("Exporting supplier-account report to PDF: {} suppliers", report.supplierCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);

            addSectionHeader(document, "REPORTE CTA. CTE. PROVEEDORES", "Resumen por Proveedor", report);
            addSupplierSummaryTable(document, report);

            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE CTA. CTE. PROVEEDORES", "Detalle de Movimientos", report);
            addMovementDetail(document, report);

            addGenerationFooter(document, report);
            document.close();

            log.info("Supplier-account PDF report generated successfully");
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating supplier-account PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Proveedor
    // ═══════════════════════════════════════════════════════════════════════

    private void addSupplierSummaryTable(Document document, SupplierAccountReportDTO report) {
        float[] cols = {3f, 1.4f, 1.4f, 1.4f, 1.4f, 1.4f, 1.4f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(cols));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Proveedor"));
        table.addHeaderCell(headerCell("CUIT"));
        table.addHeaderCell(headerCell("Saldo Anterior"));
        table.addHeaderCell(headerCell("Débitos"));
        table.addHeaderCell(headerCell("Pagos"));
        table.addHeaderCell(headerCell("NC Aplic."));
        table.addHeaderCell(headerCell("Saldo Final"));
        table.addHeaderCell(headerCell("Estado"));

        for (SupplierAccountReportStatusGroupDTO statusGroup : report.statusGroups()) {
            if (statusGroup.supplierGroups().isEmpty()) continue;

            // Status banner row spanning all 8 columns
            DeviceRgb statusBg = statusGroup.status() == SupplierAccountStatus.PENDIENTE
                    ? PENDING_COLOR : SETTLED_COLOR;
            String statusLabel = statusGroup.status() == SupplierAccountStatus.PENDIENTE
                    ? "PROVEEDORES PENDIENTES (" + statusGroup.supplierCount() + ")"
                    : "PROVEEDORES CANCELADOS (" + statusGroup.supplierCount() + ")";
            Cell statusBanner = new Cell(1, 8).add(new Paragraph(statusLabel)
                    .setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(statusBg).setPadding(5);
            table.addCell(statusBanner);

            for (SupplierAccountReportSupplierGroupDTO group : statusGroup.supplierGroups()) {
                table.addCell(cell(group.supplierLegalName()));
                table.addCell(cellCenter(group.supplierCuit() != null ? group.supplierCuit() : "-"));
                table.addCell(cellAmount(group.previousBalance()));
                table.addCell(cellAmount(group.totalDebited()));
                table.addCell(cellAmount(group.totalPaid()));
                table.addCell(cellAmount(group.totalCreditNotes()));
                table.addCell(cellAmount(group.finalBalance()));
                table.addCell(statusCell(group.status()));
            }

            // Status subtotal row
            Cell subLabel = new Cell(1, 2).add(new Paragraph("Subtotal "
                    + (statusGroup.status() == SupplierAccountStatus.PENDIENTE ? "Pendientes" : "Cancelados"))
                    .setBold().setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4);
            table.addCell(subLabel);
            table.addCell(subtotalAmount(statusGroup.subtotalPreviousBalance()));
            table.addCell(subtotalAmount(statusGroup.subtotalDebited()));
            table.addCell(subtotalAmount(statusGroup.subtotalPaid()));
            table.addCell(subtotalAmount(statusGroup.subtotalCreditNotes()));
            table.addCell(subtotalAmount(statusGroup.subtotalFinalBalance()));
            Cell subCount = new Cell().add(new Paragraph(statusGroup.supplierCount() + " prov.")
                    .setBold().setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setTextAlignment(TextAlignment.CENTER).setPadding(4);
            table.addCell(subCount);
        }

        // TOTAL row
        Cell totalLabel = new Cell(1, 2).add(new Paragraph("TOTAL GENERAL").setBold().setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(4);
        table.addCell(totalLabel);
        table.addCell(totalAmount(report.totalPreviousBalance()));
        table.addCell(totalAmount(report.totalDebited()));
        table.addCell(totalAmount(sumPayments(report)));
        table.addCell(totalAmount(sumCreditNotes(report)));
        table.addCell(totalAmount(report.totalPendingBalance()));
        Cell countCell = new Cell().add(new Paragraph(report.supplierCount() + " prov.").setBold().setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(4);
        table.addCell(countCell);

        document.add(table);

        Paragraph statusSummary = new Paragraph(String.format(
                "Total proveedores: %d  |  Pendientes: %d  |  Cancelados: %d",
                report.supplierCount(), report.pendingSupplierCount(), report.settledSupplierCount()))
                .setFontSize(9).setItalic().setMarginTop(8);
        document.add(statusSummary);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Detalle por Proveedor
    // ═══════════════════════════════════════════════════════════════════════

    private void addMovementDetail(Document document, SupplierAccountReportDTO report) {
        boolean first = true;
        for (SupplierAccountReportStatusGroupDTO statusGroup : report.statusGroups()) {
            if (statusGroup.supplierGroups().isEmpty()) continue;

            // Status section banner
            DeviceRgb statusBg0 = statusGroup.status() == SupplierAccountStatus.PENDIENTE
                    ? PENDING_COLOR : SETTLED_COLOR;
            String statusBannerLabel = statusGroup.status() == SupplierAccountStatus.PENDIENTE
                    ? "── PROVEEDORES PENDIENTES (" + statusGroup.supplierCount() + ") ──"
                    : "── PROVEEDORES CANCELADOS (" + statusGroup.supplierCount() + ") ──";
            if (!first) document.add(new Paragraph("\n"));
            Table statusBanner = new Table(UnitValue.createPercentArray(new float[]{1f}));
            statusBanner.setWidth(UnitValue.createPercentValue(100));
            statusBanner.addCell(new Cell().add(new Paragraph(statusBannerLabel)
                    .setBold().setFontSize(12).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(statusBg0).setPadding(6).setTextAlignment(TextAlignment.CENTER));
            document.add(statusBanner);
            first = false;

            for (SupplierAccountReportSupplierGroupDTO group : statusGroup.supplierGroups()) {
                document.add(new Paragraph("\n"));

            // Supplier banner
            Table banner = new Table(UnitValue.createPercentArray(new float[]{1f}));
            banner.setWidth(UnitValue.createPercentValue(100));
            String label = group.supplierLegalName()
                    + (group.supplierCuit() != null ? "   |   CUIT: " + group.supplierCuit() : "");
            Cell bannerCell = new Cell().add(new Paragraph(label).setBold().setFontSize(11))
                    .setBackgroundColor(SUPPLIER_GROUP_COLOR).setPadding(6);
            banner.addCell(bannerCell);
            document.add(banner);

            // Movements table
            float[] cols = {1.2f, 1.4f, 2f, 2.5f, 1.4f, 1.4f, 1.6f, 1.2f};
            Table movements = new Table(UnitValue.createPercentArray(cols));
            movements.setWidth(UnitValue.createPercentValue(100));

            movements.addHeaderCell(headerCell("Fecha"));
            movements.addHeaderCell(headerCell("Tipo"));
            movements.addHeaderCell(headerCell("Referencia"));
            movements.addHeaderCell(headerCell("Concepto"));
            movements.addHeaderCell(headerCell("Débito"));
            movements.addHeaderCell(headerCell("Crédito"));
            movements.addHeaderCell(headerCell("Saldo"));
            movements.addHeaderCell(headerCell("M. Pago"));

            // Previous balance row
            Cell prevLabel = new Cell(1, 6).add(new Paragraph(
                    "Saldo anterior al " + report.filters().startDate().format(DATE_FORMATTER))
                    .setBold().setItalic().setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3);
            movements.addCell(prevLabel);
            movements.addCell(subtotalAmount(group.previousBalance()));
            movements.addCell(subtotalEmpty());

            for (SupplierAccountMovementDTO m : group.movements()) {
                movements.addCell(cellCenter(m.date().format(DATE_FORMATTER)));
                movements.addCell(cell(movementTypeLabel(m.type())));
                movements.addCell(cell(m.reference() != null ? m.reference() : ""));
                movements.addCell(cell(m.description() != null ? m.description() : ""));
                movements.addCell(cellAmount(m.debit()));
                movements.addCell(cellAmount(m.credit()));
                movements.addCell(cellAmount(m.accumulatedBalance()));
                movements.addCell(cellCenter(m.paymentMethod() != null ? m.paymentMethod() : "-"));
            }

            // Final balance row
            DeviceRgb statusBg = group.status() == SupplierAccountStatus.PENDIENTE
                    ? PENDING_COLOR : SETTLED_COLOR;
            Cell finalLabel = new Cell(1, 6).add(new Paragraph(
                    "Saldo final al " + report.filters().endDate().format(DATE_FORMATTER))
                    .setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(statusBg).setPadding(4);
            movements.addCell(finalLabel);
            Cell finalAmount = new Cell().add(new Paragraph(formatAmount(group.finalBalance()))
                    .setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(statusBg).setTextAlignment(TextAlignment.RIGHT).setPadding(4);
            movements.addCell(finalAmount);
            String statusText = group.status() == SupplierAccountStatus.PENDIENTE ? "PENDIENTE" : "CANCELADO";
            Cell statusTag = new Cell().add(new Paragraph(statusText)
                    .setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(statusBg).setTextAlignment(TextAlignment.CENTER).setPadding(4);
            movements.addCell(statusTag);

            document.add(movements);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String movementTypeLabel(SupplierAccountMovementType type) {
        return switch (type) {
            case INVOICE -> "Factura";
            case DEBIT_NOTE -> "Nota Débito";
            case CREDIT_NOTE -> "Nota Crédito";
            case PAYMENT -> "Pago";
        };
    }

    private BigDecimal sumPayments(SupplierAccountReportDTO report) {
        return report.statusGroups().stream()
                .flatMap(sg -> sg.supplierGroups().stream())
                .map(g -> g.totalPaid() != null ? g.totalPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCreditNotes(SupplierAccountReportDTO report) {
        return report.statusGroups().stream()
                .flatMap(sg -> sg.supplierGroups().stream())
                .map(g -> g.totalCreditNotes() != null ? g.totalCreditNotes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addSectionHeader(Document document, String title, String subtitle,
                                  SupplierAccountReportDTO report) {
        document.add(new Paragraph(title)
                .setFontSize(16).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
        document.add(new Paragraph("ESEA S.A.")
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(3));
        document.add(new Paragraph(subtitle)
                .setFontSize(13).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1f, 3f}));
        meta.setWidth(UnitValue.createPercentValue(100));
        addMetaRow(meta, "Período:", report.periodDescription());
        addMetaRow(meta, "Fecha de generación:", report.generatedAt().format(DATETIME_FORMATTER));
        addMetaRow(meta, "Proveedores:", report.supplierCount() + " (" + report.pendingSupplierCount()
                + " pendientes / " + report.settledSupplierCount() + " cancelados)");
        document.add(meta);
        document.add(new Paragraph("\n"));
    }

    private void addMetaRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(9)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(9)).setBorder(null));
    }

    private void addGenerationFooter(Document document, SupplierAccountReportDTO report) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "Reporte generado el " +
                        report.generatedAt().format(DATE_FORMATTER) +
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

    private Cell totalAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(4);
    }

    private Cell subtotalAmount(BigDecimal amount) {
        return new Cell().add(new Paragraph(formatAmount(amount)).setBold().setItalic().setFontSize(9))
                .setBackgroundColor(SUBTOTAL_COLOR).setTextAlignment(TextAlignment.RIGHT).setPadding(3);
    }

    private Cell subtotalEmpty() {
        return new Cell().add(new Paragraph(""))
                .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3);
    }

    private Cell statusCell(SupplierAccountStatus status) {
        DeviceRgb bg = status == SupplierAccountStatus.PENDIENTE ? PENDING_COLOR : SETTLED_COLOR;
        String text = status == SupplierAccountStatus.PENDIENTE ? "PEND." : "CANC.";
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(bg).setTextAlignment(TextAlignment.CENTER).setPadding(3);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return String.format("$ %,.2f", amount);
    }
}
