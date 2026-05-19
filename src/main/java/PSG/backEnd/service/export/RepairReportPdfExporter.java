package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.repair.*;
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
public class RepairReportPdfExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SUBTOTAL_COLOR = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(RepairReportDTO report) {
        log.info("Exporting repair report to PDF: {} areas, {} total repairs",
                report.areaGroups().size(), report.totalCount());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Section 1: Area summary
            addSectionHeader(document, "REPORTE DE REPARACIONES", "Resumen por Área", report);
            addAreaSummaryTable(document, report);

            // Section 2: Vehicle summary
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE REPARACIONES", "Resumen por Vehículo", report);
            addVehicleSummaryTable(document, report);

            // Section 3: Repair detail
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addSectionHeader(document, "REPORTE DE REPARACIONES", "Detalle de Reparaciones", report);
            addRepairDetailTable(document, report);

            addGenerationFooter(document, report);

            document.close();

            log.info("Repair PDF report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating repair PDF report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.pdf.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void addAreaSummaryTable(Document document, RepairReportDTO report) {
        Table table = new Table(new float[]{3f, 1f, 1.5f, 1.5f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Reparaciones"));
        table.addHeaderCell(headerCell("Materiales ($)"));
        table.addHeaderCell(headerCell("M.O. ($)"));
        table.addHeaderCell(headerCell("Total ($)"));

        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(cell(area.projectAreaName()));
            table.addCell(cellCenter(String.valueOf(area.repairCount())));
            table.addCell(cellAmount(area.materialSubtotal()));
            table.addCell(cellAmount(area.laborSubtotal()));
            table.addCell(cellAmount(area.subtotalAmount()));
        }

        // Grand total row
        table.addCell(totalCell("TOTAL GENERAL"));
        table.addCell(totalCellCenter(String.valueOf(report.totalCount())));
        table.addCell(totalCellAmount(report.totalMaterialCost()));
        table.addCell(totalCellAmount(report.totalLaborCost()));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: Resumen por Vehículo
    // ═══════════════════════════════════════════════════════════════════════

    private void addVehicleSummaryTable(Document document, RepairReportDTO report) {
        Table table = new Table(new float[]{2f, 2f, 1f, 1.5f, 1.5f, 1.5f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Vehículo"));
        table.addHeaderCell(headerCell("Marca/Modelo"));
        table.addHeaderCell(headerCell("Cant. Rep."));
        table.addHeaderCell(headerCell("Materiales ($)"));
        table.addHeaderCell(headerCell("M.O. ($)"));
        table.addHeaderCell(headerCell("Total ($)"));

        DeviceRgb areaHeaderColor = new DeviceRgb(189, 215, 238);
        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            table.addCell(new Cell(1, 6)
                    .add(new Paragraph(area.projectAreaName()).setBold().setFontSize(9))
                    .setBackgroundColor(areaHeaderColor).setPadding(4));

            for (RepairReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                table.addCell(cell(vehicle.vehicleLicensePlate()));
                String desc = "";
                if (vehicle.vehicleBrand() != null) desc += vehicle.vehicleBrand();
                if (vehicle.vehicleModel() != null) desc += (desc.isEmpty() ? "" : " ") + vehicle.vehicleModel();
                table.addCell(cell(desc));
                table.addCell(cellCenter(String.valueOf(vehicle.repairCount())));
                table.addCell(cellAmount(vehicle.materialSubtotal()));
                table.addCell(cellAmount(vehicle.laborSubtotal()));
                table.addCell(cellAmount(vehicle.totalAmount()));
            }

            // Area subtotal
            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("Subtotal " + area.projectAreaName())
                            .setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(area.materialSubtotal())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(area.laborSubtotal())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell()
                    .add(new Paragraph(formatAmount(area.subtotalAmount())).setFontSize(9).setBold().setItalic())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 3).add(new Paragraph("TOTAL GENERAL").setBold())
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(5));
        table.addCell(totalCellAmount(report.totalMaterialCost()));
        table.addCell(totalCellAmount(report.totalLaborCost()));
        table.addCell(totalCellAmount(report.totalAmount()));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: Detalle de Reparaciones
    // ═══════════════════════════════════════════════════════════════════════

    private void addRepairDetailTable(Document document, RepairReportDTO report) {
        Table table = new Table(new float[]{2f, 1.5f, 1f, 2.5f, 0.8f, 1.5f, 1.2f, 1.2f, 1.2f});
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("Área"));
        table.addHeaderCell(headerCell("Vehículo"));
        table.addHeaderCell(headerCell("Fecha"));
        table.addHeaderCell(headerCell("Descripción"));
        table.addHeaderCell(headerCell("Km"));
        table.addHeaderCell(headerCell("Proveedor"));
        table.addHeaderCell(headerCell("Materiales ($)"));
        table.addHeaderCell(headerCell("M.O. ($)"));
        table.addHeaderCell(headerCell("Total ($)"));

        for (RepairReportAreaGroupDTO area : report.areaGroups()) {
            for (RepairReportVehicleGroupDTO vehicle : area.vehicleGroups()) {
                for (RepairReportItemDTO repair : vehicle.repairs()) {
                    table.addCell(new Cell().add(new Paragraph(area.projectAreaName()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(vehicle.vehicleLicensePlate()).setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            repair.date().format(DATE_FORMATTER)).setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            repair.description() != null ? repair.description() : "").setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            repair.mileage() != null ? String.valueOf(repair.mileage()) : "—").setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(
                            repair.supplierName() != null ? repair.supplierName() : "Interno").setFontSize(8)));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(repair.materialCost())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(repair.laborCost())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(
                            formatAmount(repair.totalWithIva())).setFontSize(8))
                            .setTextAlignment(TextAlignment.RIGHT));
                }

                // Vehicle subtotal
                table.addCell(new Cell(1, 6).add(new Paragraph(
                        "Subtotal " + vehicle.vehicleLicensePlate())
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(vehicle.materialSubtotal()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(vehicle.laborSubtotal()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
                table.addCell(new Cell().add(new Paragraph(formatAmount(vehicle.totalAmount()))
                        .setFontSize(8).setBold().setItalic())
                        .setBackgroundColor(SUBTOTAL_COLOR)
                        .setTextAlignment(TextAlignment.RIGHT).setPadding(3));
            }

            // Area subtotal
            table.addCell(new Cell(1, 6).add(new Paragraph(
                    "Subtotal " + area.projectAreaName() + " (" + area.repairCount() + " reparaciones)")
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.materialSubtotal()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.laborSubtotal()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
            table.addCell(new Cell().add(new Paragraph(formatAmount(area.subtotalAmount()))
                    .setFontSize(9).setBold())
                    .setBackgroundColor(SUBTOTAL_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(4));
        }

        // Grand total
        table.addCell(new Cell(1, 6).add(new Paragraph(
                "TOTAL GENERAL (" + report.totalCount() + " reparaciones)").setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalMaterialCost())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalLaborCost())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatAmount(report.totalAmount())).setBold().setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(6));

        document.add(table);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void addSectionHeader(Document document, String title, String subtitle,
                                   RepairReportDTO report) {
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

    private void addGenerationFooter(Document document, RepairReportDTO report) {
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
