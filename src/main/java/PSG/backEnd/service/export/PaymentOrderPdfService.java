package PSG.backEnd.service.export;

import PSG.backEnd.model.entity.Address;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.Tenant;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.entity.payment.TransferPayment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PaymentOrderPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_BG = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final float FONT_SIZE_SMALL = 8f;
    private static final float FONT_SIZE_NORMAL = 9f;
    private static final float FONT_SIZE_LABEL = 10f;
    private static final float FONT_SIZE_TITLE = 14f;

    public byte[] generate(PaymentDetails payment, Tenant tenant) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(30, 30, 30, 30);

            addHeader(document, payment, tenant);
            addIssuerSection(document, tenant);
            addBeneficiarySection(document, payment.getSupplier());
            addConceptSection(document, payment.getComment());
            addBodySection(document, payment);
            addTotalsSection(document, payment);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating payment order PDF for payment ID {}", payment.getId(), e);
            throw new RuntimeException("Error generating payment order PDF", e);
        }
    }

    private void addHeader(Document document, PaymentDetails payment, Tenant tenant) {
        Table headerTable = new Table(new float[]{3, 1, 3});
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setBorder(new SolidBorder(1));

        // Left: Company name
        String companyName = tenant.getLegalName() != null ? tenant.getLegalName() : tenant.getName();
        Cell companyCell = new Cell(1, 1)
                .add(new Paragraph(companyName).setBold().setFontSize(12))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(10);
        headerTable.addCell(companyCell);

        // Center: "X" document type marker
        Cell xCell = new Cell(1, 1)
                .add(new Paragraph("X").setBold().setFontSize(28))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(5);
        headerTable.addCell(xCell);

        // Right: Document info
        Cell infoCell = new Cell(1, 1)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(8);
        infoCell.add(new Paragraph("ORDEN DE PAGO").setBold().setFontSize(FONT_SIZE_TITLE)
                .setTextAlignment(TextAlignment.CENTER));
        infoCell.add(new Paragraph("Fecha emisión: " + payment.getPaymentDate().format(DATE_FORMATTER))
                .setFontSize(FONT_SIZE_NORMAL));
        infoCell.add(new Paragraph("Número: " + formatPaymentNumber(payment.getId()))
                .setFontSize(FONT_SIZE_NORMAL));
        if (tenant.getCuit() != null) {
            infoCell.add(new Paragraph("C.U.I.T: " + tenant.getCuit())
                    .setFontSize(FONT_SIZE_NORMAL));
        }
        headerTable.addCell(infoCell);

        document.add(headerTable);
        document.add(new Paragraph("\n").setFontSize(4));
    }

    private void addIssuerSection(Document document, Tenant tenant) {
        Table table = new Table(new float[]{1.5f, 4});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 2)
                .add(new Paragraph("EMISOR").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        String legalName = tenant.getLegalName() != null ? tenant.getLegalName() : tenant.getName();
        addLabelValueRow(table, "Razón Social:", legalName);

        String ivaCondition = tenant.getIvaCondition() != null
                ? tenant.getIvaCondition().getDisplayName() : "-";
        addLabelValueRow(table, "Condición IVA:", ivaCondition);

        addLabelValueRow(table, "Domicilio:", formatAddress(tenant.getAddress()));

        document.add(table);
        document.add(new Paragraph("\n").setFontSize(4));
    }

    private void addBeneficiarySection(Document document, Supplier supplier) {
        Table table = new Table(new float[]{1.5f, 4});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 2)
                .add(new Paragraph("BENEFICIARIO").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        String name = supplier.getTradeName() != null ? supplier.getTradeName() : supplier.getLegalName();
        addLabelValueRow(table, "Nombre:", name);
        addLabelValueRow(table, "C.U.I.T:", supplier.getCuit() != null ? supplier.getCuit() : "-");
        addLabelValueRow(table, "Domicilio:", formatAddress(supplier.getAddress()));

        String ivaCondition = supplier.getIvaCondition() != null
                ? supplier.getIvaCondition().getDisplayName() : "-";
        addLabelValueRow(table, "Condición de IVA:", ivaCondition);

        document.add(table);
        document.add(new Paragraph("\n").setFontSize(4));
    }

    private void addConceptSection(Document document, String comment) {
        Table table = new Table(new float[]{1.5f, 4});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        addLabelValueRow(table, "Concepto:", comment != null ? comment : "");

        document.add(table);
        document.add(new Paragraph("\n").setFontSize(4));
    }

    private void addBodySection(Document document, PaymentDetails payment) {
        // Two side-by-side tables: Comprobantes Imputados | Forma de Pago
        Table bodyTable = new Table(new float[]{1, 1});
        bodyTable.setWidth(UnitValue.createPercentValue(100));

        // Left column: Comprobantes Imputados
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(5);
        leftCell.add(buildDocumentsTable(payment.getPaidDocuments()));
        bodyTable.addCell(leftCell);

        // Right column: Payment method details
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(5);
        rightCell.add(buildPaymentMethodTable(payment));
        bodyTable.addCell(rightCell);

        document.add(bodyTable);
        document.add(new Paragraph("\n").setFontSize(4));
    }

    private Table buildDocumentsTable(List<TransactionalDocument> documents) {
        Table table = new Table(new float[]{2, 3, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 3)
                .add(new Paragraph("Comprobantes Imputados").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        // Headers
        table.addHeaderCell(createTableHeaderCell("Fecha"));
        table.addHeaderCell(createTableHeaderCell("Comprobante"));
        table.addHeaderCell(createTableHeaderCell("Importe"));

        if (documents != null && !documents.isEmpty()) {
            for (TransactionalDocument doc : documents) {
                table.addCell(createDataCell(doc.getDate().format(DATE_FORMATTER), TextAlignment.CENTER));

                String docRef = doc.getDocumentType().getDisplayName() + " "
                        + doc.getBranchCode() + "-" + doc.getDocumentNumber();
                table.addCell(createDataCell(docRef, TextAlignment.LEFT));

                table.addCell(createDataCell(formatAmount(doc.getTotal()), TextAlignment.RIGHT));
            }
        } else {
            Cell emptyCell = new Cell(1, 3)
                    .add(new Paragraph("Sin comprobantes vinculados").setFontSize(FONT_SIZE_SMALL)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(new SolidBorder(0.5f))
                    .setPadding(8);
            table.addCell(emptyCell);
        }

        return table;
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
        Table table = new Table(new float[]{2, 2, 2, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 4)
                .add(new Paragraph("Cheques").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        table.addHeaderCell(createTableHeaderCell("Vencimiento"));
        table.addHeaderCell(createTableHeaderCell("Nro. Cheque"));
        table.addHeaderCell(createTableHeaderCell("Banco"));
        table.addHeaderCell(createTableHeaderCell("Importe"));

        String dueDate = check.getDueDate() != null ? check.getDueDate().format(DATE_FORMATTER) : "-";
        table.addCell(createDataCell(dueDate, TextAlignment.CENTER));
        table.addCell(createDataCell(check.getCheckNumber() != null ? check.getCheckNumber() : "-", TextAlignment.CENTER));
        table.addCell(createDataCell(check.getBankName() != null ? check.getBankName() : "-", TextAlignment.LEFT));
        table.addCell(createDataCell(formatAmount(amount), TextAlignment.RIGHT));

        return table;
    }

    private Table buildTransferTable(TransferPayment transfer, BigDecimal amount) {
        Table table = new Table(new float[]{2, 2, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 3)
                .add(new Paragraph("Transferencia Bancaria").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        table.addHeaderCell(createTableHeaderCell("Nro. Transacción"));
        table.addHeaderCell(createTableHeaderCell("Banco"));
        table.addHeaderCell(createTableHeaderCell("Importe"));

        table.addCell(createDataCell(
                transfer.getTransactionNumber() != null ? transfer.getTransactionNumber() : "-",
                TextAlignment.CENTER));
        table.addCell(createDataCell(
                transfer.getBankName() != null ? transfer.getBankName() : "-",
                TextAlignment.LEFT));
        table.addCell(createDataCell(formatAmount(amount), TextAlignment.RIGHT));

        return table;
    }

    private Table buildCashTable(BigDecimal amount) {
        Table table = new Table(new float[]{3, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        Cell titleCell = new Cell(1, 2)
                .add(new Paragraph("Efectivo").setBold().setFontSize(FONT_SIZE_LABEL))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(titleCell);

        table.addHeaderCell(createTableHeaderCell("Descripción"));
        table.addHeaderCell(createTableHeaderCell("Importe"));

        table.addCell(createDataCell("Pago en efectivo", TextAlignment.LEFT));
        table.addCell(createDataCell(formatAmount(amount), TextAlignment.RIGHT));

        return table;
    }

    private void addTotalsSection(Document document, PaymentDetails payment) {
        BigDecimal totalDocuments = BigDecimal.ZERO;
        if (payment.getPaidDocuments() != null) {
            totalDocuments = payment.getPaidDocuments().stream()
                    .map(TransactionalDocument::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalPayment = payment.getAmount();
        BigDecimal notAllocated = totalPayment.subtract(totalDocuments).max(BigDecimal.ZERO);
        BigDecimal totalAllocated = totalDocuments.min(totalPayment);

        // Net totals from documents
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        if (payment.getPaidDocuments() != null) {
            totalNet = payment.getPaidDocuments().stream()
                    .map(TransactionalDocument::getNetTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalIva = payment.getPaidDocuments().stream()
                    .map(TransactionalDocument::getIvaTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Table table = new Table(new float[]{3, 2});
        table.setWidth(UnitValue.createPercentValue(100));
        table.setBorder(new SolidBorder(0.5f));

        addTotalRow(table, "No Imputado:", formatAmount(notAllocated), false);
        addTotalRow(table, "Total Imputado:", formatAmount(totalAllocated), false);
        addTotalRow(table, "Total Neto:", formatAmount(totalNet), false);
        addTotalRow(table, "Total IVA:", formatAmount(totalIva), false);
        addTotalRow(table, "Total Bruto:", formatAmount(totalPayment), true);

        document.add(table);
    }

    // --- Helper methods ---

    private void addLabelValueRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold().setFontSize(FONT_SIZE_NORMAL))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        Cell valueCell = new Cell()
                .add(new Paragraph(value != null ? value : "-").setFontSize(FONT_SIZE_NORMAL))
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Cell createTableHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(FONT_SIZE_SMALL))
                .setBackgroundColor(HEADER_BG)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(4);
    }

    private Cell createDataCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(FONT_SIZE_SMALL))
                .setTextAlignment(alignment)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(3);
    }

    private void addTotalRow(Table table, String label, String value, boolean highlight) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold().setFontSize(FONT_SIZE_NORMAL))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(5);
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setBold().setFontSize(FONT_SIZE_NORMAL))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(0.5f))
                .setPadding(5);

        if (highlight) {
            labelCell.setBackgroundColor(HEADER_BG).setFontColor(ColorConstants.WHITE);
            valueCell.setBackgroundColor(HEADER_BG).setFontColor(ColorConstants.WHITE);
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatAddress(Address address) {
        if (address == null) return "-";
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null) sb.append(address.getStreet());
        if (address.getNumber() != null) sb.append(" ").append(address.getNumber());
        if (address.getCity() != null) sb.append(", ").append(address.getCity());
        if (address.getState() != null) sb.append(", ").append(address.getState());
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private String formatPaymentNumber(Long id) {
        return String.format("0001-%08d", id);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "$ 0.00";
        return String.format("$ %,.2f", amount);
    }
}
