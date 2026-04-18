package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.policyPayment.*;
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
public class PolicyPaymentReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(PolicyPaymentReportDTO report) {
        log.info("Exporting policy payment report to PDF: {} type groups, {} total payments",
                report.typeGroups().size(), report.totalPaymentCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Section 1: Type summary
            addSectionHeader(document, "REPORTE DE PAGOS DE PÓLIZA", "Resumen por Tipo", report);
            addTypeSummaryTable(document, report);

            // Section 2: Policy summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE PAGOS DE PÓLIZA", "Resumen por Póliza", report);
            addPolicySummaryTable(document, report);

            // Section 3: Payment detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE PAGOS DE PÓLIZA", "Detalle de Pagos", report);
            addPaymentDetailTable(document, report);

            // Section 4: Vehicles (only if AUTOMOTOR policies with vehicles)
            boolean hasVehicles = report.typeGroups().stream()
                    .flatMap(tg -> tg.policyGroups().stream())
                    .anyMatch(pg -> pg.insuredVehicles() != null && !pg.insuredVehicles().isEmpty());
            if (hasVehicles) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                addSectionHeader(document, "REPORTE DE PAGOS DE PÓLIZA", "Vehículos Asegurados", report);
                addVehiclesTable(document, report);
            }

            addGenerationFooter(document, report);

            document.close();

            log.info("Policy payment PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating policy payment PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Tipo
    // ═══════════════════════════════════════════════════════════════════════

    private void addTypeSummaryTable(Document document, PolicyPaymentReportDTO report) {
        Table table = new Table(new float[]{3f, 1f, 1.5f, 1.5f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Tipo de Póliza"));
        table.addHeaderCell(headerCell("Cant. Pagos"));
        table.addHeaderCell(headerCell("Total Pagado ($)"));
        table.addHeaderCell(headerCell("Premio Esperado ($)"));
        table.addHeaderCell(headerCell("Diferencia ($)"));

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            table.addCell(cell(typeGroup.policyTypeName()));
            table.addCell(cellCenter(String.valueOf(typeGroup.paymentCount())));
            table.addCell(cellAmount(typeGroup.subtotalPaid()));
            table.addCell(cellAmount(typeGroup.subtotalExpected()));
            table.addCell(cellAmount(typeGroup.subtotalDifference()));
        }

        // Grand total
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalPaymentCount())));
        table.addCell(totalCellAmount(report.totalPaidAmount()));
        table.addCell(totalCellAmount(report.totalExpectedAmount()));
        table.addCell(totalCellAmount(report.totalDifference()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Póliza
    // ═══════════════════════════════════════════════════════════════════════

    private void addPolicySummaryTable(Document document, PolicyPaymentReportDTO report) {
        Table table = new Table(new float[]{2f, 1f, 1f, 1.2f, 0.8f, 1.2f, 1.2f, 1.2f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Póliza"));
        table.addHeaderCell(headerCell("Estado"));
        table.addHeaderCell(headerCell("Frecuencia"));
        table.addHeaderCell(headerCell("Premio Mensual ($)"));
        table.addHeaderCell(headerCell("Pagos"));
        table.addHeaderCell(headerCell("Total Pagado ($)"));
        table.addHeaderCell(headerCell("Esperado ($)"));
        table.addHeaderCell(headerCell("Diferencia ($)"));

        DeviceRgb typeHeaderColor = new DeviceRgb(189, 215, 238);
        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            table.addCell(new Cell(1, 8)
                    .add(new Paragraph(typeGroup.policyTypeName()).setBold().setFontSize(9))
                    .setBackgroundColor(typeHeaderColor).setPadding(4));

            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                table.addCell(cell(pg.policyNumber()));
                table.addCell(cell(pg.policyStatus() != null ? pg.policyStatus() : ""));
                table.addCell(cell(pg.paymentFrequency() != null ? pg.paymentFrequency() : ""));
                table.addCell(cellAmount(pg.premioMensual()));
                table.addCell(cellCenter(String.valueOf(pg.paymentCount())));
                table.addCell(cellAmount(pg.totalPaid()));
                table.addCell(cellAmount(pg.expectedAmount()));
                table.addCell(cellAmount(pg.difference()));
            }

            // Type subtotal
            table.addCell(new Cell(1, 4)
                    .add(new Paragraph("Subtotal " + typeGroup.policyTypeName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(typeGroup.paymentCount())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.CENTER).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(typeGroup.subtotalPaid())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(typeGroup.subtotalExpected())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(typeGroup.subtotalDifference())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 4).add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(5));
        table.addCell(totalCellCenter(String.valueOf(report.totalPaymentCount())));
        table.addCell(totalCellAmount(report.totalPaidAmount()));
        table.addCell(totalCellAmount(report.totalExpectedAmount()));
        table.addCell(totalCellAmount(report.totalDifference()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void addPaymentDetailTable(Document document, PolicyPaymentReportDTO report) {
        Table table = new Table(new float[]{1.5f, 1.5f, 1f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 2.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Tipo"));
        table.addHeaderCell(headerCell("Póliza"));
        table.addHeaderCell(headerCell("Fecha Pago"));
        table.addHeaderCell(headerCell("Período Desde"));
        table.addHeaderCell(headerCell("Período Hasta"));
        table.addHeaderCell(headerCell("Monto ($)"));
        table.addHeaderCell(headerCell("Premio Esp. ($)"));
        table.addHeaderCell(headerCell("Diferencia ($)"));
        table.addHeaderCell(headerCell("Notas"));

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                for (PolicyPaymentReportPaymentDTO payment : pg.payments()) {
                    table.addCell(new Cell().add(new Paragraph(typeGroup.policyTypeName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(pg.policyNumber()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.paymentDate() != null ? payment.paymentDate().format(DATE_FORMATTER) : "")
                            .setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.periodFrom() != null ? payment.periodFrom().format(DATE_FORMATTER) : "")
                            .setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.periodTo() != null ? payment.periodTo().format(DATE_FORMATTER) : "")
                            .setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(payment.amount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(payment.premioMensual())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(payment.difference())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.notes() != null ? payment.notes() : "").setFontSize(8)));
                }

                // Policy subtotal
                table.addCell(new Cell(1, 5).add(new Paragraph(
                        "Subtotal " + pg.policyNumber() + " (" + pg.paymentCount() + " pagos)")
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(pg.totalPaid()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(pg.expectedAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(pg.difference()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph("").setFontSize(8))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
            }

            // Type subtotal
            table.addCell(new Cell(1, 5).add(new Paragraph(
                    "Subtotal " + typeGroup.policyTypeName() + " (" + typeGroup.paymentCount() + " pagos)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(typeGroup.subtotalPaid()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(typeGroup.subtotalExpected()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(typeGroup.subtotalDifference()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 5).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalPaymentCount() + " pagos)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalPaidAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalExpectedAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalDifference())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: Vehículos Asegurados
    // ═══════════════════════════════════════════════════════════════════════

    private void addVehiclesTable(Document document, PolicyPaymentReportDTO report) {
        Table table = new Table(new float[]{2f, 1.5f, 2f, 1.5f, 1.5f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Póliza"));
        table.addHeaderCell(headerCell("Patente"));
        table.addHeaderCell(headerCell("Marca/Modelo"));
        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Suma Asegurada ($)"));
        table.addHeaderCell(headerCell("Premio Mensual ($)"));

        for (PolicyPaymentReportTypeGroupDTO typeGroup : report.typeGroups()) {
            for (PolicyPaymentReportPolicyGroupDTO pg : typeGroup.policyGroups()) {
                if (pg.insuredVehicles() == null || pg.insuredVehicles().isEmpty()) {
                    continue;
                }
                for (PolicyPaymentReportVehicleDTO vehicle : pg.insuredVehicles()) {
                    table.addCell(cell(pg.policyNumber()));
                    table.addCell(cell(vehicle.licensePlate() != null ? vehicle.licensePlate() : ""));

                    String brandModel = "";
                    if (vehicle.brand() != null) brandModel += vehicle.brand();
                    if (vehicle.model() != null) {
                        if (!brandModel.isEmpty()) brandModel += " ";
                        brandModel += vehicle.model();
                    }
                    table.addCell(cell(brandModel));
                    table.addCell(cell(vehicle.projectAreaName() != null ? vehicle.projectAreaName() : ""));
                    table.addCell(cellAmount(vehicle.sumInsured()));
                    table.addCell(cellAmount(vehicle.premioMensual()));
                }
            }
        }

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   PolicyPaymentReportDTO report) {
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
        addMetaRow(meta, "Total de pagos:", String.valueOf(report.totalPaymentCount()));
        addMetaRow(meta, "Total de pólizas:", String.valueOf(report.totalPolicyCount()));
        document.add(meta);
        document.add(new Paragraph("\n"));
    }

    private void addMetaRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value)).setBorder(null));
    }

    private void addGenerationFooter(Document document, PolicyPaymentReportDTO report) {
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
}
