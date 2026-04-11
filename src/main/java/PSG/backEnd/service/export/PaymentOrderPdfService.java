package PSG.backEnd.service.export;

import PSG.backEnd.model.entity.Address;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.Tenant;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.entity.payment.TransferPayment;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a Payment Order PDF that replicates the format of the company's
 * official "Orden de Pago" document: black-and-white, thin borders, no colors.
 */
@Component
@Slf4j
public class PaymentOrderPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SolidBorder THIN = new SolidBorder(0.5f);
    private static final float SZ_XS = 7.5f;
    private static final float SZ_SM = 8.5f;
    private static final float SZ_MD = 9f;
    private static final float SZ_LG = 13f;
    private static final float SZ_XL = 22f;
    private static final float CELL_PAD = 3f;
    private static final float BOLD_SPACING = 0.4f;
    private static final float TITLE_SPACING = 0.6f;

    // ────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────

    public byte[] generate(PaymentDetails payment, Tenant tenant) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);
            doc.setMargins(30, 30, 30, 30);

            addHeader(doc, payment, tenant);
            addBeneficiary(doc, payment.getSupplier());
            addConcept(doc, payment.getComment());
            addBody(doc, payment);
            addTotals(doc, payment);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating payment order PDF for payment ID {}", payment.getId(), e);
            throw new RuntimeException("Error generating payment order PDF", e);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Header  (company name + X + "Orden de pago" + issuer data)
    // Matches the reference: one outer bordered box split in 3 columns
    // ────────────────────────────────────────────────────────────────

    private void addHeader(Document doc, PaymentDetails payment, Tenant tenant) {
        // Outer table: [left = company+issuer] [center = X] [right = title+meta]
        Table outer = new Table(new float[]{5, 1, 5});
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(THIN);

        // ── LEFT CELL ──
        Cell left = new Cell().setBorder(THIN).setPadding(6);
        String companyName = tenant.getLegalName() != null ? tenant.getLegalName() : tenant.getName();
        left.add(new Paragraph(companyName).setBold().setFontSize(SZ_LG)
                .setCharacterSpacing(TITLE_SPACING)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
        left.add(labelValue("Razón Social:  ", companyName));
        left.add(labelValue("Condición IVA:",
                tenant.getIvaCondition() != null ? tenant.getIvaCondition().getDisplayName() : "-"));
        left.add(labelValue("Domicilio:     ", formatAddress(tenant.getAddress())));
        outer.addCell(left);

        // ── CENTER CELL (X) ──
        Cell center = new Cell().setBorder(THIN)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(4);
        center.add(new Paragraph("X").setBold().setFontSize(SZ_XL)
                .setCharacterSpacing(TITLE_SPACING)
                .setTextAlignment(TextAlignment.CENTER));
        outer.addCell(center);

        // ── RIGHT CELL ──
        Cell right = new Cell().setBorder(THIN).setPadding(6);
        right.add(new Paragraph("Orden de pago").setBold().setFontSize(SZ_LG)
                .setCharacterSpacing(TITLE_SPACING).setMarginBottom(4));

        // Two-column table for metadata so values align to the right
        Table meta = new Table(new float[]{3, 4});
        meta.setWidth(UnitValue.createPercentValue(100));
        meta.setBorder(Border.NO_BORDER);

        meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                .add(new Paragraph("Fecha emisión:").setBold().setFontSize(SZ_SM).setCharacterSpacing(BOLD_SPACING)));
        meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                .add(new Paragraph(payment.getPaymentDate().format(DATE_FMT)).setFontSize(SZ_SM)
                        .setTextAlignment(TextAlignment.RIGHT)));

        meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                .add(new Paragraph("Numero:").setBold().setFontSize(SZ_SM).setCharacterSpacing(BOLD_SPACING)));
        meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                .add(new Paragraph(formatNumber(payment.getId())).setFontSize(SZ_SM)
                        .setTextAlignment(TextAlignment.RIGHT)));

        if (tenant.getCuit() != null) {
            meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                    .add(new Paragraph("C.U.I.T:").setBold().setFontSize(SZ_SM).setCharacterSpacing(BOLD_SPACING)));
            meta.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(1)
                    .add(new Paragraph(tenant.getCuit()).setFontSize(SZ_SM)
                            .setTextAlignment(TextAlignment.RIGHT)));
        }

        right.add(meta);
        outer.addCell(right);

        doc.add(outer);
        doc.add(spacer());
    }

    // ────────────────────────────────────────────────────────────────
    // Beneficiary – 4-column grid: label | value | label | value
    // ────────────────────────────────────────────────────────────────

    private void addBeneficiary(Document doc, Supplier s) {
        Table t = new Table(new float[]{1.5f, 4, 1.8f, 3});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(THIN);

        String name = s.getTradeName() != null ? s.getTradeName() : s.getLegalName();
        addKvCell(t, "Nombre:", name);
        addKvCell(t, "C.U.I.T:", s.getCuit() != null ? s.getCuit() : "-");

        addKvCell(t, "Domicilio:", formatAddress(s.getAddress()));
        addKvCell(t, "Condición de IVA:",
                s.getIvaCondition() != null ? s.getIvaCondition().getDisplayName() : "-");

        doc.add(t);
        doc.add(spacer());
    }

    // ────────────────────────────────────────────────────────────────
    // Concept
    // ────────────────────────────────────────────────────────────────

    private void addConcept(Document doc, String comment) {
        Table t = new Table(new float[]{1.5f, 10});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(THIN);

        addKvCell(t, "Concepto:", comment != null && !comment.isBlank() ? comment : "");

        doc.add(t);
        doc.add(spacer());
    }

    // ────────────────────────────────────────────────────────────────
    // Body – side-by-side: documents table | payment method table
    // Both share the same outer border
    // ────────────────────────────────────────────────────────────────

    private void addBody(Document doc, PaymentDetails payment) {
        Table wrapper = new Table(new float[]{1, 1});
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.setBorder(THIN);

        // Left: Comprobantes Imputados
        Cell leftCell = new Cell().setBorder(THIN).setPadding(0);
        leftCell.add(buildDocumentsTable(payment.getPaidDocuments()));
        wrapper.addCell(leftCell);

        // Right: Payment method
        Cell rightCell = new Cell().setBorder(THIN).setPadding(0);
        rightCell.add(buildPaymentMethodTable(payment));
        wrapper.addCell(rightCell);

        doc.add(wrapper);
    }

    private Table buildDocumentsTable(List<TransactionalDocument> documents) {
        // Columns: date | reference + amount original | amount paid
        Table t = new Table(new float[]{2, 4, 2.5f, 2.5f});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        // Title row
        Cell title = noBorderCell(1, 4)
                .add(new Paragraph("Comprobantes Imputados").setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.CENTER))
                .setPaddingBottom(2);
        t.addCell(title);

        if (documents != null && !documents.isEmpty()) {
            for (TransactionalDocument d : documents) {
                t.addCell(dataCell(d.getDate().format(DATE_FMT), TextAlignment.LEFT));

                String ref = shortDocType(d.getDocumentType().getDisplayName())
                        + " " + d.getBranchCode() + "-" + d.getDocumentNumber();
                t.addCell(dataCell(ref, TextAlignment.LEFT));

                t.addCell(dataCell(fmt(d.getTotal()), TextAlignment.RIGHT));
                t.addCell(dataCell(fmt(d.getTotal()), TextAlignment.RIGHT));
            }
        }

        // Fill remaining space so the table doesn't collapse
        Cell filler = noBorderCell(1, 4).setMinHeight(30);
        t.addCell(filler);

        return t;
    }

    private Table buildPaymentMethodTable(PaymentDetails payment) {
        if (payment.getCheckPayment() != null) {
            return buildCheckTable(payment.getCheckPayment(), payment.getAmount());
        } else if (payment.getTransferPayment() != null) {
            return buildTransferTable(payment.getTransferPayment(), payment.getAmount());
        } else {
            return buildCashTable(payment.getAmount());
        }
    }

    private Table buildCheckTable(CheckPayment check, BigDecimal amount) {
        Table t = new Table(new float[]{2, 3, 3});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        t.addCell(noBorderCell(1, 3)
                .add(new Paragraph("Cheques").setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.CENTER))
                .setPaddingBottom(2));

        String dueDate = check.getDueDate() != null ? check.getDueDate().format(DATE_FMT) : "-";
        t.addCell(dataCell(dueDate, TextAlignment.LEFT));
        t.addCell(dataCell(check.getCheckNumber() != null ? check.getCheckNumber() : "-", TextAlignment.CENTER));
        t.addCell(dataCell(fmt(amount), TextAlignment.RIGHT));

        t.addCell(noBorderCell(1, 3).setMinHeight(30));
        return t;
    }

    private Table buildTransferTable(TransferPayment transfer, BigDecimal amount) {
        Table t = new Table(new float[]{3, 3, 3});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        t.addCell(noBorderCell(1, 3)
                .add(new Paragraph("Transferencia Bancaria").setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.CENTER))
                .setPaddingBottom(2));

        t.addCell(dataCell(transfer.getTransactionNumber() != null ? transfer.getTransactionNumber() : "-", TextAlignment.LEFT));
        t.addCell(dataCell(transfer.getBankName() != null ? transfer.getBankName() : "-", TextAlignment.CENTER));
        t.addCell(dataCell(fmt(amount), TextAlignment.RIGHT));

        t.addCell(noBorderCell(1, 3).setMinHeight(30));
        return t;
    }

    private Table buildCashTable(BigDecimal amount) {
        Table t = new Table(new float[]{5, 3});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        t.addCell(noBorderCell(1, 2)
                .add(new Paragraph("Efectivo").setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.CENTER))
                .setPaddingBottom(2));

        t.addCell(dataCell("Pago en efectivo", TextAlignment.LEFT));
        t.addCell(dataCell(fmt(amount), TextAlignment.RIGHT));

        t.addCell(noBorderCell(1, 2).setMinHeight(30));
        return t;
    }

    // ────────────────────────────────────────────────────────────────
    // Totals – matches the reference two-area layout
    // Left area:  No Imputado / Total Imputado / Retenciones
    // Right area: Total Neto / Total Bruto / Total Retenciones / Total Neto
    // ────────────────────────────────────────────────────────────────

    private void addTotals(Document doc, PaymentDetails payment) {
        BigDecimal totalDocs = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        if (payment.getPaidDocuments() != null) {
            for (TransactionalDocument d : payment.getPaidDocuments()) {
                totalDocs = totalDocs.add(d.getTotal());
                totalNet = totalNet.add(d.getNetTotal());
            }
        }
        BigDecimal totalPayment = payment.getAmount();
        BigDecimal notAllocated = totalPayment.subtract(totalDocs).max(BigDecimal.ZERO);

        doc.add(spacer());

        // Outer wrapper to keep left + right totals in one bordered section
        Table wrapper = new Table(new float[]{1, 1});
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.setBorder(THIN);

        // ── LEFT totals ──
        Cell leftCell = new Cell().setBorder(THIN).setPadding(4);
        Table leftT = new Table(new float[]{3, 2, 3, 2});
        leftT.setWidth(UnitValue.createPercentValue(100));
        leftT.setBorder(Border.NO_BORDER);

        leftT.addCell(totalLabel("No Imputado:"));
        leftT.addCell(totalValue(fmt(notAllocated)));
        leftT.addCell(totalLabel("Total Imputado:"));
        leftT.addCell(totalValue(fmt(totalDocs)));

        // Retenciones row (placeholder — system has no withholdings yet)
        leftT.addCell(noBorderCell(1, 4)
                .add(new Paragraph("Retenciones").setBold().setFontSize(SZ_XS)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.CENTER))
                .setPaddingTop(4));

        leftCell.add(leftT);
        wrapper.addCell(leftCell);

        // ── RIGHT totals ──
        Cell rightCell = new Cell().setBorder(THIN).setPadding(4);
        Table rightT = new Table(new float[]{4, 3});
        rightT.setWidth(UnitValue.createPercentValue(100));
        rightT.setBorder(Border.NO_BORDER);

        rightT.addCell(totalLabel("Total Neto:"));
        rightT.addCell(totalValue(fmt(totalNet)));

        rightT.addCell(totalLabel("Total Bruto:"));
        rightT.addCell(totalValue(fmt(totalPayment)));

        rightT.addCell(totalLabel("Total Retenciones:"));
        rightT.addCell(totalValue(fmt(BigDecimal.ZERO)));

        rightT.addCell(totalLabel("Total Neto:"));
        rightT.addCell(totalValue(fmt(totalPayment)));

        rightCell.add(rightT);
        wrapper.addCell(rightCell);

        doc.add(wrapper);
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private Paragraph labelValue(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label).setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING))
                .add(new com.itextpdf.layout.element.Text("  " + (value != null ? value : "-")).setFontSize(SZ_SM))
                .setMarginBottom(1);
    }

    private void addKvCell(Table t, String label, String value) {
        t.addCell(new Cell().setBorder(THIN).setPadding(CELL_PAD)
                .add(new Paragraph(label).setBold().setFontSize(SZ_SM).setCharacterSpacing(BOLD_SPACING)));
        t.addCell(new Cell().setBorder(THIN).setPadding(CELL_PAD)
                .add(new Paragraph(value != null ? value : "-").setFontSize(SZ_SM)));
    }

    private Cell dataCell(String text, TextAlignment align) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                .add(new Paragraph(text).setFontSize(SZ_XS).setTextAlignment(align));
    }

    private Cell noBorderCell(int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan).setBorder(Border.NO_BORDER).setPadding(2);
    }

    private Cell totalLabel(String text) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                .add(new Paragraph(text).setBold().setFontSize(SZ_SM)
                        .setCharacterSpacing(BOLD_SPACING).setTextAlignment(TextAlignment.RIGHT));
    }

    private Cell totalValue(String text) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                .add(new Paragraph(text).setFontSize(SZ_SM)
                        .setTextAlignment(TextAlignment.RIGHT));
    }

    private Paragraph spacer() {
        return new Paragraph("\n").setFontSize(3);
    }

    private String formatAddress(Address address) {
        if (address == null) return "-";
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null) sb.append(address.getStreet());
        if (address.getNumber() != null) sb.append(" ").append(address.getNumber());
        if (address.getCity() != null) sb.append(" - ").append(address.getCity());
        if (address.getState() != null) sb.append(", ").append(address.getState());
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private String shortDocType(String displayName) {
        if (displayName == null) return "";
        return displayName
                .replace("Factura A", "FAC-A")
                .replace("Factura B", "FAC-B")
                .replace("Factura C", "FAC-C")
                .replace("Nota de débito A", "ND-A")
                .replace("Nota de débito B", "ND-B")
                .replace("Nota de débito C", "ND-C")
                .replace("Nota de crédito A", "NC-A")
                .replace("Nota de crédito B", "NC-B")
                .replace("Nota de crédito C", "NC-C");
    }

    private String formatNumber(Long id) {
        return String.format("0004-%06d", id);
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
