package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Genera el reporte PDF de una previsión: cabecera + tabla de items con totales.
 * Sigue la convención visual del sistema (header azul, fila de total naranja).
 */
@Component
@Slf4j
public class BudgetForecastPdfService {

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb TOTAL_COLOR = new DeviceRgb(243, 156, 18);
    private static final DeviceRgb APPLIED_COLOR = new DeviceRgb(46, 204, 113);
    private static final DeviceRgb SKIPPED_COLOR = new DeviceRgb(189, 195, 199);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    public byte[] export(BudgetForecast forecast) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc);

            // Título
            doc.add(new Paragraph("PREVISIÓN DE GASTOS")
                    .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(forecast.getName())
                    .setFontSize(13).setTextAlignment(TextAlignment.CENTER));

            // Cabecera
            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                    .useAllAvailableWidth();
            info.addCell(label("Período")).addCell(value(formatPeriod(forecast)));
            info.addCell(label("Estado")).addCell(value(forecast.getStatus().name()));
            info.addCell(label("Total estimado")).addCell(value(money(forecast.getTotalAmount())));
            info.addCell(label("Total aplicado")).addCell(value(money(forecast.getAppliedAmount())));
            if (forecast.getDescription() != null && !forecast.getDescription().isBlank()) {
                info.addCell(label("Descripción"));
                info.addCell(value(forecast.getDescription()));
                info.addCell(label(""));
                info.addCell(value(""));
            }
            doc.add(info);

            doc.add(new Paragraph(" "));

            // Tabla de items
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 4, 2, 2, 2}))
                    .useAllAvailableWidth();
            String[] headers = {"Tipo", "Fecha", "Descripción", "Monto", "Estado", "Aplicado #"};
            for (String h : headers) table.addHeaderCell(headerCell(h));

            List<BudgetForecastItem> items = forecast.getItems().stream()
                    .sorted(Comparator.comparing(i -> i.getRowOrder() == null ? 0 : i.getRowOrder()))
                    .toList();

            BigDecimal totalEstimated = BigDecimal.ZERO;
            BigDecimal totalApplied = BigDecimal.ZERO;
            for (BudgetForecastItem it : items) {
                table.addCell(cell(it.getItemType().name()));
                table.addCell(cell(it.getExpectedDate() != null ? it.getExpectedDate().format(DATE_FMT) : ""));
                table.addCell(cell(it.getDescription()));
                table.addCell(cell(money(it.getExpectedAmount())).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(statusCell(it.getApplicationStatus()));
                table.addCell(cell(it.getAppliedEntityId() != null
                        ? (it.getAppliedEntityType() + "#" + it.getAppliedEntityId()) : "—"));
                if (it.getExpectedAmount() != null) totalEstimated = totalEstimated.add(it.getExpectedAmount());
                if (it.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO
                        && it.getExpectedAmount() != null) {
                    totalApplied = totalApplied.add(it.getExpectedAmount());
                }
            }

            // Fila de totales
            Cell totalLabel = totalCell("TOTAL").setTextAlignment(TextAlignment.RIGHT);
            table.addCell(totalCell("").setTextAlignment(TextAlignment.RIGHT));
            table.addCell(totalCell("").setTextAlignment(TextAlignment.RIGHT));
            table.addCell(totalLabel);
            table.addCell(totalCell(money(totalEstimated)).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(totalCell("Aplicado").setTextAlignment(TextAlignment.RIGHT));
            table.addCell(totalCell(money(totalApplied)).setTextAlignment(TextAlignment.RIGHT));

            doc.add(table);

            // Footer
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Generado el " + LocalDate.now().format(DATE_FMT))
                    .setFontSize(9).setItalic().setTextAlignment(TextAlignment.RIGHT));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de previsión", e);
            throw new ReportGenerationException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ─────────── Cell helpers ───────────

    private Cell label(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(new DeviceRgb(236, 240, 241));
    }

    private Cell value(String text) {
        return new Cell().add(new Paragraph(text == null ? "" : text).setFontSize(9));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(HEADER_COLOR).setTextAlignment(TextAlignment.CENTER);
    }

    private Cell cell(String text) {
        return new Cell().add(new Paragraph(text == null ? "" : text).setFontSize(9));
    }

    private Cell statusCell(BudgetForecastItemApplicationStatus s) {
        Cell c = cell(s == null ? "—" : s.name());
        if (s == BudgetForecastItemApplicationStatus.APLICADO) c.setBackgroundColor(APPLIED_COLOR);
        else if (s == BudgetForecastItemApplicationStatus.OMITIDO) c.setBackgroundColor(SKIPPED_COLOR);
        return c;
    }

    private Cell totalCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(TOTAL_COLOR);
    }

    private String formatPeriod(BudgetForecast f) {
        return (f.getPeriodFrom() != null ? f.getPeriodFrom().format(DATE_FMT) : "?")
                + " — "
                + (f.getPeriodTo() != null ? f.getPeriodTo().format(DATE_FMT) : "?");
    }

    private String money(BigDecimal v) {
        return v == null ? "—" : MONEY.format(v);
    }
}
