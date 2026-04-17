package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.fuelLoad.*;
import PSG.backEnd.model.enums.vehicle.FuelType;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class FuelLoadReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private static final List<FuelType> FUEL_TYPE_ORDER = List.of(
            FuelType.INFINIA, FuelType.SUPER, FuelType.INFINIA_DIESEL,
            FuelType.DIESEL_500, FuelType.GNC, FuelType.DISTILLED_WATER, FuelType.OIL);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(FuelLoadReportDTO report) {
        log.info("Exporting fuel load report to PDF: {} areas, {} total loads",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            List<FuelType> activeFuelTypes = FUEL_TYPE_ORDER.stream()
                    .filter(ft -> report.totalsByFuelType().containsKey(ft.name()))
                    .toList();

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE COMBUSTIBLE", "Resumen por Área", report);
            addAreaSummaryTable(document, report, activeFuelTypes);

            // Section 2: Vehicle summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE COMBUSTIBLE", "Resumen por Vehículo", report);
            addVehicleSummaryTable(document, report, activeFuelTypes);

            // Section 3: Load detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE COMBUSTIBLE", "Detalle de Cargas", report);
            addLoadDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Fuel load PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating fuel load PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void addAreaSummaryTable(Document document, FuelLoadReportDTO report,
                                      List<FuelType> activeFuelTypes) {
        int colCount = 4 + activeFuelTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 3;   // Área
        colWidths[1] = 1;   // Cant
        for (int i = 0; i < activeFuelTypes.size(); i++) {
            colWidths[2 + i] = 1.5f;
        }
        colWidths[colCount - 2] = 1.5f; // Total $
        colWidths[colCount - 1] = 1.2f; // Total L

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Cargas"));
        for (FuelType ft : activeFuelTypes) {
            table.addHeaderCell(headerCell(ft.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total ($)"));
        table.addHeaderCell(headerCell("Total (L)"));

        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.loadCount())));
            for (FuelType ft : activeFuelTypes) {
                BigDecimal val = area.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO);
                table.addCell(cellAmount(val));
            }
            table.addCell(cellAmount(area.subtotalAmount()));
            table.addCell(cellNumber(area.subtotalLiters()));
        }

        // Grand total row
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalCount())));
        for (FuelType ft : activeFuelTypes) {
            BigDecimal val = report.totalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO);
            table.addCell(totalCellAmount(val));
        }
        table.addCell(totalCellAmount(report.totalAmount()));
        table.addCell(totalCellNumber(report.totalLiters()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Vehículo
    // ═══════════════════════════════════════════════════════════════════════

    private void addVehicleSummaryTable(Document document, FuelLoadReportDTO report,
                                         List<FuelType> activeFuelTypes) {
        int colCount = 4 + activeFuelTypes.size();
        float[] colWidths = new float[colCount];
        colWidths[0] = 2;
        colWidths[1] = 2;
        for (int i = 0; i < activeFuelTypes.size(); i++) {
            colWidths[2 + i] = 1.5f;
        }
        colWidths[colCount - 2] = 1.5f;
        colWidths[colCount - 1] = 1.2f;

        Table table = new Table(colWidths);
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Vehículo"));
        table.addHeaderCell(headerCell("Descripción"));
        for (FuelType ft : activeFuelTypes) {
            table.addHeaderCell(headerCell(ft.getDisplayName()));
        }
        table.addHeaderCell(headerCell("Total ($)"));
        table.addHeaderCell(headerCell("Total (L)"));

        DeviceRgb areaHeaderColor = new DeviceRgb(189, 215, 238);
        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(new Cell(1, colCount)
                    .add(new Paragraph(area.projectAreaName()).setBold().setFontSize(9))
                    .setBackgroundColor(areaHeaderColor).setPadding(4));

            for (FuelLoadReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                table.addCell(cell(vehicle.vehicleName()));
                table.addCell(cell(vehicle.vehicleDescription() != null ? vehicle.vehicleDescription() : ""));
                for (FuelType ft : activeFuelTypes) {
                    BigDecimal val = vehicle.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO);
                    table.addCell(cellAmount(val));
                }
                table.addCell(cellAmount(vehicle.totalAmount()));
                table.addCell(cellNumber(vehicle.totalLiters()));
            }

            // Area subtotal
            table.addCell(new Cell(1, 2)
                    .add(new Paragraph("Subtotal " + area.projectAreaName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            for (FuelType ft : activeFuelTypes) {
                BigDecimal val = area.subtotalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO);
                table.addCell(new Cell()
                        .add(new Paragraph(formatAmount(val)).setFontSize(9).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            }
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(area.subtotalAmount())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatNumber(area.subtotalLiters())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 2).add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(5));
        for (FuelType ft : activeFuelTypes) {
            BigDecimal val = report.totalsByFuelType().getOrDefault(ft.name(), BigDecimal.ZERO);
            table.addCell(totalCellAmount(val));
        }
        table.addCell(totalCellAmount(report.totalAmount()));
        table.addCell(totalCellNumber(report.totalLiters()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Cargas
    // ═══════════════════════════════════════════════════════════════════════

    private void addLoadDetailTable(Document document, FuelLoadReportDTO report) {
        Table table = new Table(new float[]{2f, 1.5f, 1.5f, 1.2f, 1.2f, 0.9f, 1f, 1f, 1f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Vehículo"));
        table.addHeaderCell(headerCell("Estación"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Tipo Comb."));
        table.addHeaderCell(headerCell("Sucursal"));
        table.addHeaderCell(headerCell("Ticket"));
        table.addHeaderCell(headerCell("Litros"));
        table.addHeaderCell(headerCell("Precio/L"));
        table.addHeaderCell(headerCell("Total"));

        for (FuelLoadReportAreaGroupDTO area : report.areaGroups()) {
            for (FuelLoadReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                for (FuelLoadReportItemDTO load : vehicle.loads()) {
                    String areaDisplay = area.projectAreaName();
                    if (load.projectAreaTaskName() != null) {
                        areaDisplay += " - " + load.projectAreaTaskName();
                    }
                    table.addCell(new Cell().add(new Paragraph(areaDisplay).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            load.vehicleLicensePlate() != null ? load.vehicleLicensePlate() : "Bidón").setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(load.gasStationName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            load.date().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            getFuelTypeDisplayName(load.fuelType())).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(load.branchCode()).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(load.ticketNumber()).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            formatNumber(load.liters())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(load.pricePerLiter())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(load.totalAmount())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                }

                // Vehicle subtotal
                table.addCell(new Cell(1, 7).add(new Paragraph(
                        "Subtotal " + vehicle.vehicleName())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatNumber(vehicle.totalLiters()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph("").setFontSize(8))
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(vehicle.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
            }

            // Area subtotal
            table.addCell(new Cell(1, 7).add(new Paragraph(
                    "Subtotal " + area.projectAreaName() + " (" + area.loadCount() + " cargas)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatNumber(area.subtotalLiters()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9))
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 7).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " cargas)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatNumber(report.totalLiters())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   FuelLoadReportDTO report) {
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

    private void addGenerationFooter(Document document, FuelLoadReportDTO report) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(
                "Reporte generado el " +
                report.generatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - Sistema de Gestión ESEA S.A.")
                .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
    }

    private String getFuelTypeDisplayName(String fuelType) {
        try {
            return FuelType.valueOf(fuelType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return fuelType;
        }
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

    private Cell cellNumber(BigDecimal number) {
        return new Cell().add(new Paragraph(formatNumber(number)).setFontSize(9))
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

    private Cell totalCellNumber(BigDecimal number) {
        return new Cell().add(new Paragraph(formatNumber(number)).setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(5);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("$ %,.2f", amount);
    }

    private String formatNumber(BigDecimal number) {
        return String.format("%,.2f", number);
    }
}
