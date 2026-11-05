package PSG.backEnd.service.export;

import PSG.backEnd.model.entity.Tenant;
import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a Repair Order (Orden de Reparación) PDF meant to be printed on
 * A4 and handed to the workshop: large bold type, generous spacing, a
 * checklist for the items to repair, and a signature area. Priority is
 * legibility over density — the opposite trade-off from the Payment Order.
 */
@Component
@Slf4j
public class RepairOrderPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final SolidBorder THIN = new SolidBorder(0.75f);
    private static final SolidBorder THICK = new SolidBorder(1.25f);

    private static final float SZ_COMPANY = 12f;
    private static final float SZ_TITLE = 26f;
    private static final float SZ_PLATE = 32f;
    private static final float SZ_SECTION = 13f;
    private static final float SZ_BODY = 13.5f;
    private static final float SZ_ITEM = 15f;
    private static final float SZ_META = 11.5f;
    private static final float LETTER_SPACING = 0.5f;

    // ────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────

    public byte[] generate(RepairOrder order, Tenant tenant) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);
            doc.setMargins(28, 28, 28, 28);

            addHeader(doc, order, tenant);
            addVehicleBox(doc, order);
            addItems(doc, order.getItems());
            addDescription(doc, order.getDescription());
            addFooter(doc, order);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating repair order PDF for order ID {}", order.getId(), e);
            throw new RuntimeException("Error generating repair order PDF", e);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Header — company name (small) + big "ORDEN DE REPARACIÓN" title,
    // order number / date / status on the right.
    // ────────────────────────────────────────────────────────────────

    private void addHeader(Document doc, RepairOrder order, Tenant tenant) {
        Table outer = new Table(new float[]{6, 5});
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(THICK);

        String companyName = tenant.getLegalName() != null ? tenant.getLegalName() : tenant.getName();
        Cell left = new Cell().setBorder(THICK).setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        left.add(new Paragraph(companyName).setBold().setFontSize(SZ_COMPANY)
                .setCharacterSpacing(LETTER_SPACING).setMarginBottom(4));
        left.add(new Paragraph("ORDEN DE REPARACIÓN").setBold().setFontSize(SZ_TITLE)
                .setCharacterSpacing(LETTER_SPACING).setMultipliedLeading(1));
        outer.addCell(left);

        Cell right = new Cell().setBorder(THICK).setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(metaRow("N° de orden:", formatOrderNumber(order.getId())));
        right.add(metaRow("Fecha:", order.getDate().format(DATE_FMT)));
        right.add(metaRow("Estado:", statusLabel(order.getStatus())));
        outer.addCell(right);

        doc.add(outer);
        doc.add(spacer(6));
    }

    private Paragraph metaRow(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label + "  ").setBold().setFontSize(SZ_BODY))
                .add(new com.itextpdf.layout.element.Text(value).setBold().setFontSize(SZ_BODY))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(3);
    }

    // ────────────────────────────────────────────────────────────────
    // Vehicle — the plate is the single most important thing on the page
    // for whoever picks this order up, so it is set in the largest type.
    // ────────────────────────────────────────────────────────────────

    private void addVehicleBox(Document doc, RepairOrder order) {
        Vehicle v = order.getVehicle();

        Table t = new Table(new float[]{4, 7});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(THICK);

        Cell plateCell = new Cell().setBorder(THIN).setPadding(10)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        plateCell.add(new Paragraph("PATENTE").setBold().setFontSize(SZ_META)
                .setCharacterSpacing(LETTER_SPACING).setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        plateCell.add(new Paragraph(v != null && v.getLicensePlate() != null ? v.getLicensePlate() : "-")
                .setBold().setFontSize(SZ_PLATE).setCharacterSpacing(1f)
                .setTextAlignment(TextAlignment.CENTER).setMultipliedLeading(1));
        t.addCell(plateCell);

        Cell infoCell = new Cell().setBorder(THIN).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE);
        String brandModel = v != null ? joinNonBlank(v.getBrand(), v.getModel()) : "-";
        infoCell.add(labelValue("Vehículo:", brandModel.isEmpty() ? "-" : brandModel));
        infoCell.add(labelValue("Año:", v != null && v.getYear() != null ? String.valueOf(v.getYear()) : "-"));
        infoCell.add(labelValue("Reportado por:", order.getReportedBy() != null && !order.getReportedBy().isBlank()
                ? order.getReportedBy() : "-"));
        t.addCell(infoCell);

        doc.add(t);
        doc.add(spacer(8));
    }

    // ────────────────────────────────────────────────────────────────
    // Items — a checklist so the mechanic can tick each task off by hand.
    // ────────────────────────────────────────────────────────────────

    private void addItems(Document doc, List<String> items) {
        doc.add(sectionTitle("TRABAJOS A REALIZAR"));

        Table t = new Table(new float[]{1, 14});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(THICK);

        if (items != null && !items.isEmpty()) {
            for (String item : items) {
                t.addCell(checkboxCell());
                t.addCell(new Cell().setBorder(THIN).setPadding(8)
                        .add(new Paragraph(item).setBold().setFontSize(SZ_ITEM)));
            }
        } else {
            t.addCell(checkboxCell());
            t.addCell(new Cell().setBorder(THIN).setPadding(8)
                    .add(new Paragraph("-").setFontSize(SZ_ITEM)));
        }

        doc.add(t);
        doc.add(spacer(8));
    }

    private Cell checkboxCell() {
        return new Cell().setBorder(THIN).setPadding(4)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(" ").setFontSize(SZ_ITEM));
    }

    // ────────────────────────────────────────────────────────────────
    // Description / observations — free text, left blank if not provided.
    // ────────────────────────────────────────────────────────────────

    private void addDescription(Document doc, String description) {
        doc.add(sectionTitle("DESCRIPCIÓN / OBSERVACIONES"));

        Cell box = new Cell().setBorder(THICK).setPadding(10).setMinHeight(70);
        box.add(new Paragraph(description != null && !description.isBlank() ? description : "-")
                .setFontSize(SZ_BODY));
        doc.add(box);
        doc.add(spacer(10));
    }

    // ────────────────────────────────────────────────────────────────
    // Footer — signature lines for the mechanic and for whoever picks up
    // the vehicle, so the printed sheet doubles as a work receipt.
    // ────────────────────────────────────────────────────────────────

    private void addFooter(Document doc, RepairOrder order) {
        Table t = new Table(new float[]{1, 1});
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        t.addCell(signatureCell("Firma y aclaración del mecánico"));
        t.addCell(signatureCell("Firma de conformidad"));

        doc.add(spacer(24));
        doc.add(t);
    }

    private Cell signatureCell(String label) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        c.add(new Paragraph("_______________________________").setFontSize(SZ_BODY).setMarginBottom(2));
        c.add(new Paragraph(label).setBold().setFontSize(SZ_META).setCharacterSpacing(LETTER_SPACING));
        return c;
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text) {
        return new Paragraph(text).setBold().setFontSize(SZ_SECTION)
                .setCharacterSpacing(LETTER_SPACING).setMarginBottom(4).setMarginTop(0);
    }

    private Paragraph labelValue(String label, String value) {
        return new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label + " ").setBold().setFontSize(SZ_BODY))
                .add(new com.itextpdf.layout.element.Text(value).setFontSize(SZ_BODY))
                .setMarginBottom(4);
    }

    private Paragraph spacer(float height) {
        return new Paragraph(" ").setFontSize(1).setMultipliedLeading(1).setMarginBottom(height);
    }

    private String joinNonBlank(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isBlank()) sb.append(a);
        if (b != null && !b.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(b);
        }
        return sb.toString();
    }

    private String statusLabel(RepairOrderStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En Proceso";
            case COMPLETADA -> "Completada";
        };
    }

    private String formatOrderNumber(Long id) {
        return String.format("%06d", id);
    }
}
