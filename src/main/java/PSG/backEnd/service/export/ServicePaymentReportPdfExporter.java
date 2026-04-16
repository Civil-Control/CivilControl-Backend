package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.servicePayment.*;
import PSG.backEnd.model.enums.ServiceType;
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
public class ServicePaymentReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private static final List<ServiceType> SERVICE_TYPE_ORDER = List.of(
            ServiceType.LUZ, ServiceType.AGUA, ServiceType.GAS, ServiceType.INTERNET,
            ServiceType.TELEFONIA, ServiceType.MUNICIPALES, ServiceType.PROVINCIALES,
            ServiceType.NACIONALES, ServiceType.OTRO);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(ServicePaymentReportDTO report) {
        log.info("Exporting service payment report to PDF: {} areas, {} total payments",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            List<ServiceType> activeServiceTypes = SERVICE_TYPE_ORDER.stream()
                    .filter(st -> report.totalsByServiceType().containsKey(st.name()))
                    .toList();

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE PAGO DE SERVICIOS", "Resumen por Área", report);
            addAreaSummaryTable(document, report, activeServiceTypes);

            // Section 2: Building summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE PAGO DE SERVICIOS", "Resumen por Edificio", report);
            addBuildingSummaryTable(document, report, activeServiceTypes);

            // Section 3: Payment detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE PAGO DE SERVICIOS", "Detalle de Pagos", report);
            addPaymentDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Service payment PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating service payment PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void addAreaSummaryTable(Document document, ServicePaymentReportDTO report,
                                      List<ServiceType> activeServiceTypes) {
        int colCount = 3 + activeServiceTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 3;   // Área
        colWidths[1] = 1;   // Cant
        for (int i = 0; i < activeServiceTypes.size(); i++) {
            colWidths[2 + i] = 1.5f;
        }
        colWidths[colCount - 1] = 1.5f; // Total

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Cant. Pagos"));
        for (ServiceType st : activeServiceTypes) {
            table.addHeaderCell(headerCell("Total " + st.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.paymentCount())));
            for (ServiceType st : activeServiceTypes) {
                BigDecimal val = area.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
                table.addCell(cellAmount(val));
            }
            table.addCell(cellAmount(area.subtotalAmount()));
        }

        // Grand total row
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalCount())));
        for (ServiceType st : activeServiceTypes) {
            BigDecimal val = report.totalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
            table.addCell(totalCellAmount(val));
        }
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Edificio
    // ═══════════════════════════════════════════════════════════════════════

    private void addBuildingSummaryTable(Document document, ServicePaymentReportDTO report,
                                          List<ServiceType> activeServiceTypes) {
        int colCount = 2 + activeServiceTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 3;
        for (int i = 0; i < activeServiceTypes.size(); i++) {
            colWidths[1 + i] = 1.5f;
        }
        colWidths[colCount - 1] = 1.5f;

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Edificio"));
        for (ServiceType st : activeServiceTypes) {
            table.addHeaderCell(headerCell("Total " + st.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total"));

        DeviceRgb areaHeaderColor = new DeviceRgb(189, 215, 238);
        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
            // Area header row
            table.addCell(new Cell(1, colCount)
                    .add(new Paragraph(area.projectAreaName()).setBold().setFontSize(9))
                    .setBackgroundColor(areaHeaderColor).setPadding(4));

            for (ServicePaymentReportBuildingGroupDTO building : area.buildingGroups()) {
                table.addCell(cell(building.buildingName()));
                for (ServiceType st : activeServiceTypes) {
                    BigDecimal val = building.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
                    table.addCell(cellAmount(val));
                }
                table.addCell(cellAmount(building.totalAmount()));
            }

            // Area subtotal row
            table.addCell(new Cell()
                    .add(new Paragraph("Subtotal " + area.projectAreaName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            for (ServiceType st : activeServiceTypes) {
                BigDecimal val = area.subtotalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
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

        // Grand total
        table.addCell(totalCell("TOTAL GENERAL"));
        for (ServiceType st : activeServiceTypes) {
            BigDecimal val = report.totalsByServiceType().getOrDefault(st.name(), BigDecimal.ZERO);
            table.addCell(totalCellAmount(val));
        }
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Pagos
    // ═══════════════════════════════════════════════════════════════════════

    private void addPaymentDetailTable(Document document, ServicePaymentReportDTO report) {
        Table table = new Table(new float[]{2f, 2f, 2f, 1.2f, 1.2f, 1f, 1f, 1f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Edificio"));
        table.addHeaderCell(headerCell("Proveedor"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Tipo Servicio"));
        table.addHeaderCell(headerCell("Año/Período"));
        table.addHeaderCell(headerCell("Referencia"));
        table.addHeaderCell(headerCell("Método Pago"));
        table.addHeaderCell(headerCell("Monto"));

        for (ServicePaymentReportAreaGroupDTO area : report.areaGroups()) {
            for (ServicePaymentReportBuildingGroupDTO building : area.buildingGroups()) {
                for (ServicePaymentReportItemDTO payment : building.payments()) {
                    String areaDisplay = area.projectAreaName();
                    if (payment.projectAreaTaskName() != null) {
                        areaDisplay += " - " + payment.projectAreaTaskName();
                    }
                    table.addCell(new Cell().add(new Paragraph(areaDisplay).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(building.buildingName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(payment.supplierName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.paymentDate().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            getServiceTypeDisplayName(payment.serviceType())).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            formatYearPeriod(payment.year(), payment.period())).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.referenceNumber() != null ? payment.referenceNumber() : "-")
                            .setFontSize(8)).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            payment.paymentMethod() != null ? payment.paymentMethod().getDisplayName() : "-")
                            .setFontSize(8)).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(formatAmount(payment.amount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                }

                // Building subtotal
                table.addCell(new Cell(1, 8).add(new Paragraph(
                        "Subtotal " + building.buildingName())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(building.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
            }

            // Area subtotal
            table.addCell(new Cell(1, 8).add(new Paragraph(
                    "Subtotal " + area.projectAreaName() + " (" + area.paymentCount() + " pagos)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 8).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " pagos)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   ServicePaymentReportDTO report) {
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

    private void addGenerationFooter(Document document, ServicePaymentReportDTO report) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "Reporte generado el " +
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - Sistema de Gestión ESEA S.A.")
                .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
    }

    private String getServiceTypeDisplayName(String serviceType) {
        try {
            return ServiceType.valueOf(serviceType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return serviceType;
        }
    }

    private String formatYearPeriod(Integer year, Integer period) {
        if (year != null && period != null) {
            return String.format("%02d/%d", period, year);
        } else if (year != null) {
            return String.valueOf(year);
        }
        return "-";
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

    private String formatAmount(BigDecimal amount) {
        return String.format("$ %,.2f", amount);
    }
}
