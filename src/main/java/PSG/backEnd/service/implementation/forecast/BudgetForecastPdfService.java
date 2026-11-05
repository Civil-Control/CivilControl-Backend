package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemType;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Genera el reporte PDF de una previsión usando el mismo layout matricial que la tabla
 * del frontend y el export Excel:
 * <pre>
 *  Tipo │ Identificador │ &lt;día 1&gt; │ … │ &lt;día N&gt; │ Monto │ Cantidad │ Proveedor │ Estado
 * </pre>
 * Si el período es muy largo se pagina en bloques de {@link #DAYS_PER_CHUNK} días por
 * página, repitiendo siempre las columnas ancla (Tipo / Identificador / Monto / Estado…)
 * para conservar el contexto.
 */
@Component
@Slf4j
public class BudgetForecastPdfService {

    private static final DeviceRgb HEADER_COLOR     = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb DAY_HEADER_COLOR = new DeviceRgb(189, 195, 199);
    private static final DeviceRgb TOTAL_COLOR     = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb APPLIED_COLOR   = new DeviceRgb(46, 204, 113);
    private static final DeviceRgb SKIPPED_COLOR   = new DeviceRgb(189, 195, 199);
    private static final DeviceRgb DAY_CELL_COLOR  = new DeviceRgb(245, 247, 250);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private static final Locale ES_AR = new Locale("es", "AR");

    /** Días por bloque al paginar. Calibrado para que entre cómodamente en A4 horizontal. */
    private static final int DAYS_PER_CHUNK = 14;

    public byte[] export(BudgetForecast forecast) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document doc = new Document(pdfDoc);
            doc.setMargins(24, 24, 24, 24);

            // ── Cabecera del reporte ──
            doc.add(new Paragraph("PREVISIÓN DE GASTOS")
                    .setBold().setFontSize(15).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(nullSafe(forecast.getName()))
                    .setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                    .useAllAvailableWidth();
            info.addCell(label("Período")).addCell(value(formatPeriod(forecast)));
            info.addCell(label("Estado")).addCell(value(forecast.getStatus().name()));
            info.addCell(label("Total estimado")).addCell(value(money(forecast.getTotalAmount())));
            info.addCell(label("Total aplicado")).addCell(value(money(forecast.getAppliedAmount())));
            if (forecast.getDescription() != null && !forecast.getDescription().isBlank()) {
                info.addCell(label("Descripción"));
                Cell descCell = new Cell(1, 3)
                        .add(new Paragraph(forecast.getDescription()).setFontSize(9));
                info.addCell(descCell);
            }
            doc.add(info);
            doc.add(new Paragraph(" ").setFontSize(4));

            // ── Items ordenados igual que el frontend ──
            List<BudgetForecastItem> items = forecast.getItems().stream()
                    .sorted(Comparator.comparing(i -> i.getRowOrder() == null ? 0 : i.getRowOrder()))
                    .toList();

            List<LocalDate> allDays = buildDateRange(forecast);
            List<List<LocalDate>> chunks = chunk(allDays, DAYS_PER_CHUNK);

            BigDecimal totalEstimated = BigDecimal.ZERO;
            BigDecimal totalApplied = BigDecimal.ZERO;
            for (BudgetForecastItem it : items) {
                if (it.getExpectedAmount() != null) totalEstimated = totalEstimated.add(it.getExpectedAmount());
                if (it.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO
                        && it.getExpectedAmount() != null) {
                    totalApplied = totalApplied.add(it.getExpectedAmount());
                }
            }

            // ── Una tabla por bloque de días, reutilizando columnas ancla ──
            for (int chunkIdx = 0; chunkIdx < chunks.size(); chunkIdx++) {
                List<LocalDate> chunkDays = chunks.get(chunkIdx);
                boolean isLastChunk = chunkIdx == chunks.size() - 1;
                Table table = buildChunkTable(items, chunkDays, isLastChunk, totalEstimated, totalApplied);
                if (chunkIdx > 0) doc.add(new Paragraph(" ").setFontSize(2));
                doc.add(table);
            }

            doc.add(new Paragraph("Generado el " + LocalDate.now().format(DATE_FMT))
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(8));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de previsión", e);
            throw new ReportGenerationException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ─────────── Tabla de un bloque de días ───────────

    private Table buildChunkTable(List<BudgetForecastItem> items,
                                  List<LocalDate> days,
                                  boolean isLastChunk,
                                  BigDecimal totalEstimated,
                                  BigDecimal totalApplied) {
        // Anchos: Tipo (12), Identif. (16), días (n × 7), Monto (12), Cantidad (8),
        // Proveedor (12), Estado (10). En el último bloque agregamos Monto/Estado.
        int dayCount = days.size();
        float[] widths = new float[2 + dayCount + 4];
        widths[0] = 12f;
        widths[1] = 16f;
        for (int i = 0; i < dayCount; i++) widths[2 + i] = 7f;
        widths[2 + dayCount]     = 12f; // Monto
        widths[2 + dayCount + 1] = 8f;  // Cantidad
        widths[2 + dayCount + 2] = 12f; // Proveedor
        widths[2 + dayCount + 3] = 10f; // Estado

        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();

        // ── Encabezado ──
        table.addHeaderCell(headerCell("Tipo"));
        table.addHeaderCell(headerCell("Identificador"));
        for (LocalDate d : days) table.addHeaderCell(dayHeaderCell(d));
        table.addHeaderCell(headerCell("Monto"));
        table.addHeaderCell(headerCell("Cantidad"));
        table.addHeaderCell(headerCell("Proveedor"));
        table.addHeaderCell(headerCell("Estado"));

        // ── Filas ──
        for (BudgetForecastItem it : items) {
            table.addCell(cell(it.getItemType().name()));
            table.addCell(cell(buildIdentifier(it)));
            for (LocalDate d : days) {
                Cell c = new Cell()
                        .setBackgroundColor(DAY_CELL_COLOR)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setTextAlignment(TextAlignment.CENTER);
                if (it.getExpectedDate() != null && it.getExpectedDate().equals(d)) {
                    c.add(new Paragraph(nullSafe(it.getDescription())).setFontSize(7).setBold());
                }
                table.addCell(c);
            }
            table.addCell(cell(money(it.getExpectedAmount())).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(cell(it.getStockQuantity() != null
                    ? stripTrailingZeros(it.getStockQuantity()) : ""));
            table.addCell(cell(it.getSupplier() != null ? nullSafe(it.getSupplier().getCuit()) : ""));
            table.addCell(statusCell(it.getApplicationStatus()));
        }

        // ── Totales sólo en el último bloque ──
        if (isLastChunk) {
            int spanLeft = 2 + dayCount; // Tipo + Identificador + días
            Cell totalLabel = totalCell("TOTAL ESTIMADO", 1, spanLeft)
                    .setTextAlignment(TextAlignment.RIGHT);
            table.addCell(totalLabel);
            table.addCell(totalCell(money(totalEstimated)).setTextAlignment(TextAlignment.RIGHT));
            Cell appliedLabel = totalCell("Aplicado: " + money(totalApplied), 1, 3)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(appliedLabel);
        }

        return table;
    }

    // ─────────── Helpers ───────────

    private List<LocalDate> buildDateRange(BudgetForecast forecast) {
        List<LocalDate> out = new ArrayList<>();
        if (forecast.getPeriodFrom() == null || forecast.getPeriodTo() == null) return out;
        LocalDate cur = forecast.getPeriodFrom();
        LocalDate end = forecast.getPeriodTo();
        for (int i = 0; i < 366 && !cur.isAfter(end); i++) {
            out.add(cur);
            cur = cur.plusDays(1);
        }
        return out;
    }

    private List<List<LocalDate>> chunk(List<LocalDate> all, int size) {
        List<List<LocalDate>> out = new ArrayList<>();
        if (all.isEmpty()) {
            out.add(List.of());
            return out;
        }
        for (int i = 0; i < all.size(); i += size) {
            out.add(all.subList(i, Math.min(i + size, all.size())));
        }
        return out;
    }

    /** Identificador canónico por tipo (espejo del frontend / Excel). */
    private String buildIdentifier(BudgetForecastItem item) {
        BudgetForecastItemType type = item.getItemType();
        return switch (type) {
            case SALARIO -> item.getEmployee() != null
                    ? joinNonBlank(item.getEmployee().getName(), item.getEmployee().getLastName()) : "";
            case SERVICIO, PATENTE -> item.getServiceAssignment() != null
                    ? "#" + item.getServiceAssignment().getId() : "";
            case REPARACION -> item.getVehicle() != null ? nullSafe(item.getVehicle().getLicensePlate()) : "";
            case COMPRA_STOCK -> item.getStock() != null ? nullSafe(item.getStock().getName()) : "";
            case OTRO -> "—";
        };
    }

    private String joinNonBlank(String a, String b) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isBlank()) sb.append(a.trim());
        if (b != null && !b.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(b.trim());
        }
        return sb.toString();
    }

    private Cell label(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(new DeviceRgb(236, 240, 241));
    }

    private Cell value(String text) {
        return new Cell().add(new Paragraph(text == null ? "" : text).setFontSize(9));
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE).setFontSize(8))
                .setBackgroundColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell dayHeaderCell(LocalDate d) {
        String weekday = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES_AR).replace(".", "");
        if (!weekday.isEmpty()) weekday = Character.toUpperCase(weekday.charAt(0)) + weekday.substring(1);
        return new Cell()
                .add(new Paragraph(weekday).setBold().setFontSize(7))
                .add(new Paragraph(d.format(SHORT_FMT)).setFontSize(7))
                .setBackgroundColor(DAY_HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell cell(String text) {
        return new Cell()
                .add(new Paragraph(text == null ? "" : text).setFontSize(8))
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell statusCell(BudgetForecastItemApplicationStatus s) {
        Cell c = cell(s == null ? "—" : s.name());
        if (s == BudgetForecastItemApplicationStatus.APLICADO) c.setBackgroundColor(APPLIED_COLOR);
        else if (s == BudgetForecastItemApplicationStatus.OMITIDO) c.setBackgroundColor(SKIPPED_COLOR);
        return c;
    }

    private Cell totalCell(String text) {
        return totalCell(text, 1, 1);
    }

    /** iText 7 sólo permite definir colspan/rowspan en el constructor. */
    private Cell totalCell(String text, int rowspan, int colspan) {
        return new Cell(rowspan, colspan)
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE).setFontSize(9))
                .setBackgroundColor(TOTAL_COLOR);
    }

    private String formatPeriod(BudgetForecast f) {
        return (f.getPeriodFrom() != null ? f.getPeriodFrom().format(DATE_FMT) : "?")
                + " — "
                + (f.getPeriodTo() != null ? f.getPeriodTo().format(DATE_FMT) : "?");
    }

    private String money(BigDecimal v) { return v == null ? "—" : MONEY.format(v); }

    private String stripTrailingZeros(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private String nullSafe(String s) { return s == null ? "" : s; }
}
