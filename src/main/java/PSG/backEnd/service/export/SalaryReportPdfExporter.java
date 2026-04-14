package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.salary.*;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
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

/**
 * Dedicated PDF exporter for salary reports.
 * Generates a document with 3 sections (page-broken):
 * 1. Resumen por Área
 * 2. Resumen por Empleado
 * 3. Detalle de Pagos
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SalaryReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private static final List<SalaryFrecuency> FREQUENCY_ORDER = List.of(
            SalaryFrecuency.MENSUAL, SalaryFrecuency.QUINCENAL, SalaryFrecuency.SEMANAL);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SalaryReportDTO report) {
        log.info("Exporting salary report to PDF: {} areas, {} total payments",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            List<SalaryFrecuency> activeFrequencies = FREQUENCY_ORDER.stream()
                    .filter(f -> report.totalsByFrequency().containsKey(f))
                    .toList();

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE SALARIOS", "Resumen por Área", report);
            addAreaSummaryTable(document, report, activeFrequencies);

            // Section 2: Employee summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE SALARIOS", "Resumen por Empleado", report);
            addEmployeeSummaryTable(document, report, activeFrequencies);

            // Section 3: Payment detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE SALARIOS", "Detalle de Pagos", report);
            addPaymentDetailTable(document, report);

            // Footer
            addGenerationFooter(document, report);

            document.close();

            log.info("Salary PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating salary PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void addAreaSummaryTable(Document document, SalaryReportDTO report,
                                      List<SalaryFrecuency> activeFrequencies) {
        int colCount = 3 + activeFrequencies.size(); // Área, Cant, [freqs...], Total
        float[] colWidths = new float[colCount];
        colWidths[0] = 3; // Área
        colWidths[1] = 1; // Cant
        for (int i = 0; i < activeFrequencies.size(); i++) {
            colWidths[2 + i] = 1.5f; // freq
        }
        colWidths[colCount - 1] = 1.5f; // Total

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        // Headers
        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Cant. Pagos"));
        for (SalaryFrecuency freq : activeFrequencies) {
            table.addHeaderCell(headerCell("Total " + freq.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        // Data rows
        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.paymentCount())));
            for (SalaryFrecuency freq : activeFrequencies) {
                BigDecimal val = area.subtotalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
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
        for (SalaryFrecuency freq : activeFrequencies) {
            BigDecimal val = report.totalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
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
    // SECTION 2: Resumen por Empleado
    // ═══════════════════════════════════════════════════════════════════════

    private void addEmployeeSummaryTable(Document document, SalaryReportDTO report,
                                          List<SalaryFrecuency> activeFrequencies) {
        int colCount = 4 + activeFrequencies.size(); // Área, Empleado, Cant, [freqs...], Total
        float[] colWidths = new float[colCount];
        colWidths[0] = 2;   // Área
        colWidths[1] = 3;   // Empleado
        colWidths[2] = 0.8f; // Cant
        for (int i = 0; i < activeFrequencies.size(); i++) {
            colWidths[3 + i] = 1.5f;
        }
        colWidths[colCount - 1] = 1.5f; // Total

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        // Headers
        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Empleado"));
        table.addHeaderCell(headerCell("Cant."));
        for (SalaryFrecuency freq : activeFrequencies) {
            table.addHeaderCell(headerCell("Total " + freq.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        // Data rows
        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            for (SalaryReportEmployeeGroupDTO emp : area.employeeGroups()) {
                table.addCell(cell(area.projectAreaName()));
                table.addCell(cell(emp.employeeLastName() + ", " + emp.employeeName()));
                table.addCell(cellCenter(String.valueOf(emp.paymentCount())));
                for (SalaryFrecuency freq : activeFrequencies) {
                    BigDecimal val = emp.subtotalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
                    table.addCell(cellAmount(val));
                }
                table.addCell(cellAmount(emp.totalAmount()));
            }
        }

        // Grand total row
        Cell totalLabel = new Cell(1, 2).add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(5);
        table.addCell(totalLabel);
        table.addCell(new Cell().add(new Paragraph(String.valueOf(report.totalCount())).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setPadding(5));
        for (SalaryFrecuency freq : activeFrequencies) {
            BigDecimal val = report.totalsByFrequency().getOrDefault(freq, BigDecimal.ZERO);
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
    // SECTION 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void addPaymentDetailTable(Document document, SalaryReportDTO report) {
        Table table = new Table(new float[]{2f, 2.5f, 1.2f, 1.2f, 1.2f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        // Headers
        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Apellido y Nombre"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Frecuencia"));
        table.addHeaderCell(headerCell("Método Pago"));
        table.addHeaderCell(headerCell("Monto"));

        for (SalaryReportAreaGroupDTO area : report.areaGroups()) {
            for (SalaryReportEmployeeGroupDTO emp : area.employeeGroups()) {
                for (SalaryReportPaymentDTO payment : emp.payments()) {
                    table.addCell(new Cell().add(new Paragraph(area.projectAreaName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            emp.employeeLastName() + ", " + emp.employeeName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.paymentDate().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.salaryFrequency().getDisplayName()).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.paymentMethod() != null ? payment.paymentMethod().getDisplayName() : "-")
                            .setFontSize(8)).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(formatAmount(payment.amount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                }

                // Employee subtotal
                table.addCell(new Cell(1, 5).add(new Paragraph(
                        "Subtotal " + emp.employeeLastName() + ", " + emp.employeeName())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(emp.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
            }

            // Area subtotal
            table.addCell(new Cell(1, 5).add(new Paragraph(
                    "Subtotal " + area.projectAreaName() + " (" + area.paymentCount() + " pagos)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        Cell totalLabel = new Cell(1, 5).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " pagos)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6);
        table.addCell(totalLabel);
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   SalaryReportDTO report) {
        document.add(new Paragraph(title)
                .setFontSize(18).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        document.add(new Paragraph("ESEA S.A.")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        document.add(new Paragraph(subtitle)
                .setFontSize(14).setBold()
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

        // Metadata
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

    private void addGenerationFooter(Document document, SalaryReportDTO report) {
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
