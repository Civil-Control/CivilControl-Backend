package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.issuedPayment.*;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.payment.CheckStatus;
import PSG.backEnd.model.enums.report.IssuedPaymentReportGroupBy;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel exporter for the Issued Payments Report (Feature 16).
 *
 * <p>Generates a workbook with up to 4 sheets:
 * <ol>
 *   <li>Resumen por &lt;Layer-1&gt; — totals per primary group</li>
 *   <li>Resumen por &lt;Layer-2&gt; — same data pivoted</li>
 *   <li>Detalle de Pagos — flat row-per-payment with subtotal bands</li>
 *   <li>Resumen de Cheques — KPI block (only when checks are present)</li>
 * </ol>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IssuedPaymentReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Stable order in summary tables. */
    private static final List<PaymentMethod> METHOD_ORDER =
            List.of(PaymentMethod.CASH, PaymentMethod.TRANSFER, PaymentMethod.CHECK);

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(IssuedPaymentReportDTO report) {
        log.info("Exporting Issued Payments report to Excel: groupBy={}, primaryGroups={}, totalCount={}",
                report.groupBy(), report.primaryGroups().size(), report.totalCount());

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle title    = titleStyle(wb);
            CellStyle header   = headerStyle(wb);
            CellStyle currency = currencyStyle(wb);
            CellStyle subtotal = subtotalStyle(wb);
            CellStyle subAmt   = subtotalCurrencyStyle(wb);
            CellStyle total    = totalStyle(wb);
            CellStyle totalAmt = totalCurrencyStyle(wb);
            CellStyle date     = dateStyle(wb);

            IssuedPaymentReportGroupBy gb = report.groupBy() == null
                    ? IssuedPaymentReportGroupBy.METHOD : report.groupBy();

            String layer1Label = gb == IssuedPaymentReportGroupBy.METHOD ? "Método" : "Proveedor";
            String layer2Label = gb == IssuedPaymentReportGroupBy.METHOD ? "Proveedor" : "Método";

            buildLayer1Sheet(wb, report, layer1Label, title, header, currency, total, totalAmt);
            buildLayer2Sheet(wb, report, layer1Label, layer2Label,
                    title, header, currency, subtotal, subAmt, total, totalAmt);
            buildDetailSheet(wb, report, gb, title, header, currency, subtotal, subAmt, total, totalAmt, date);
            buildCheckSummarySheet(wb, report, title, header, currency, total, totalAmt);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Issued Payments Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ─────────────── Sheet 1: per primary group ───────────────

    private void buildLayer1Sheet(Workbook wb, IssuedPaymentReportDTO r, String l1,
                                   CellStyle title, CellStyle header, CellStyle currency,
                                   CellStyle total, CellStyle totalAmt) {
        Sheet s = wb.createSheet("Resumen por " + l1);

        addTitleRow(s, "Resumen por " + l1 + " — " + r.reportName(), 4, title);
        addPeriodRow(s, r, 4);
        int row = 3;

        Row h = s.createRow(row++);
        cell(h, 0, l1, header);
        cell(h, 1, "Cant. Pagos", header);
        cell(h, 2, "Total $", header);

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            Row dr = s.createRow(row++);
            String label = g.groupSubLabel() != null && !g.groupSubLabel().isBlank()
                    ? g.groupLabel() + "  (" + g.groupSubLabel() + ")"
                    : g.groupLabel();
            cell(dr, 0, label, null);
            numCell(dr, 1, g.paymentCount(), null);
            amtCell(dr, 2, g.subtotalAmount(), currency);
        }

        Row tr = s.createRow(row);
        cell(tr, 0, "TOTAL", total);
        numCell(tr, 1, r.totalCount(), total);
        amtCell(tr, 2, r.totalAmount(), totalAmt);

        for (int i = 0; i < 3; i++) s.autoSizeColumn(i);
    }

    // ─────────────── Sheet 2: per secondary group ───────────────

    private void buildLayer2Sheet(Workbook wb, IssuedPaymentReportDTO r, String l1, String l2,
                                   CellStyle title, CellStyle header, CellStyle currency,
                                   CellStyle subtotal, CellStyle subAmt,
                                   CellStyle total, CellStyle totalAmt) {
        Sheet s = wb.createSheet("Resumen por " + l2);
        addTitleRow(s, "Resumen por " + l2 + " — " + r.reportName(), 5, title);
        addPeriodRow(s, r, 5);
        int row = 3;

        Row h = s.createRow(row++);
        cell(h, 0, l1, header);
        cell(h, 1, l2, header);
        cell(h, 2, "Subtítulo", header);
        cell(h, 3, "Cant.", header);
        cell(h, 4, "Total $", header);

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            // primary subtotal band
            Row pr = s.createRow(row++);
            cell(pr, 0, g.groupLabel(), subtotal);
            cell(pr, 1, "", subtotal);
            cell(pr, 2, g.groupSubLabel() != null ? g.groupSubLabel() : "", subtotal);
            numCell(pr, 3, g.paymentCount(), subtotal);
            amtCell(pr, 4, g.subtotalAmount(), subAmt);

            for (IssuedPaymentReportSecondaryGroupDTO sg : g.secondaryGroups()) {
                Row dr = s.createRow(row++);
                cell(dr, 0, "", null);
                cell(dr, 1, sg.groupLabel(), null);
                cell(dr, 2, sg.groupSubLabel() != null ? sg.groupSubLabel() : "", null);
                numCell(dr, 3, sg.paymentCount(), null);
                amtCell(dr, 4, sg.subtotalAmount(), currency);
            }
        }

        Row tr = s.createRow(row);
        cell(tr, 0, "TOTAL", total);
        cell(tr, 1, "", total);
        cell(tr, 2, "", total);
        numCell(tr, 3, r.totalCount(), total);
        amtCell(tr, 4, r.totalAmount(), totalAmt);

        for (int i = 0; i < 5; i++) s.autoSizeColumn(i);
    }

    // ─────────────── Sheet 3: flat detail ───────────────

    private void buildDetailSheet(Workbook wb, IssuedPaymentReportDTO r, IssuedPaymentReportGroupBy gb,
                                   CellStyle title, CellStyle header, CellStyle currency,
                                   CellStyle subtotal, CellStyle subAmt,
                                   CellStyle total, CellStyle totalAmt, CellStyle date) {
        Sheet s = wb.createSheet("Detalle de Pagos");
        addTitleRow(s, "Detalle de Pagos — " + r.reportName(), 14, title);
        addPeriodRow(s, r, 14);
        int row = 3;

        String[] headers = {
                "Fecha", "Método", "Proveedor", "CUIT", "Monto $",
                "Cuenta / Caja", "Cheque N°", "Chequera", "Banco", "Vencimiento",
                "Estado", "Fecha Cobro", "Comentario", "Cant. Comp."
        };
        Row h = s.createRow(row++);
        for (int i = 0; i < headers.length; i++) cell(h, i, headers[i], header);

        for (IssuedPaymentReportPrimaryGroupDTO g : r.primaryGroups()) {
            Row pr = s.createRow(row++);
            cell(pr, 0, g.groupLabel() + (g.groupSubLabel() != null ? " (" + g.groupSubLabel() + ")" : ""),
                    subtotal);
            for (int i = 1; i < 4; i++) cell(pr, i, "", subtotal);
            amtCell(pr, 4, g.subtotalAmount(), subAmt);
            for (int i = 5; i < headers.length; i++) cell(pr, i, "", subtotal);

            for (IssuedPaymentReportSecondaryGroupDTO sg : g.secondaryGroups()) {
                Row sr = s.createRow(row++);
                cell(sr, 0, "  " + sg.groupLabel(), subtotal);
                for (int i = 1; i < 4; i++) cell(sr, i, "", subtotal);
                amtCell(sr, 4, sg.subtotalAmount(), subAmt);
                for (int i = 5; i < headers.length; i++) cell(sr, i, "", subtotal);

                for (IssuedPaymentReportItemDTO p : sg.payments()) {
                    Row dr = s.createRow(row++);
                    cell(dr, 0, p.paymentDate() != null ? p.paymentDate().format(DATE_FORMATTER) : "", null);
                    cell(dr, 1, methodLabel(p.method()), null);
                    cell(dr, 2, p.supplierTradeName() != null && !p.supplierTradeName().isBlank()
                            ? p.supplierTradeName() : p.supplierLegalName(), null);
                    cell(dr, 3, p.supplierCuit() != null ? p.supplierCuit() : "", null);
                    amtCell(dr, 4, p.amount(), currency);
                    cell(dr, 5, treasuryRef(p), null);
                    cell(dr, 6, p.checkNumber() != null ? p.checkNumber() : "", null);
                    cell(dr, 7, p.checkbookName() != null ? p.checkbookName() : "", null);
                    cell(dr, 8, p.bankName() != null ? p.bankName() : "", null);
                    cell(dr, 9, p.checkDueDate() != null ? p.checkDueDate().format(DATE_FORMATTER) : "", null);
                    cell(dr, 10, p.checkStatus() != null ? checkStatusLabel(p.checkStatus()) : "", null);
                    cell(dr, 11, p.checkSettledDate() != null ? p.checkSettledDate().format(DATE_FORMATTER) : "", null);
                    cell(dr, 12, p.comment() != null ? p.comment() : "", null);
                    numCell(dr, 13, p.linkedDocumentCount(), null);
                }
            }
        }

        Row tr = s.createRow(row);
        cell(tr, 0, "TOTAL GENERAL", total);
        for (int i = 1; i < 4; i++) cell(tr, i, "", total);
        amtCell(tr, 4, r.totalAmount(), totalAmt);
        for (int i = 5; i < headers.length; i++) cell(tr, i, "", total);

        for (int i = 0; i < headers.length; i++) s.autoSizeColumn(i);
    }

    // ─────────────── Sheet 4: check KPIs ───────────────

    private void buildCheckSummarySheet(Workbook wb, IssuedPaymentReportDTO r,
                                         CellStyle title, CellStyle header, CellStyle currency,
                                         CellStyle total, CellStyle totalAmt) {
        Sheet s = wb.createSheet("Resumen de Cheques");
        addTitleRow(s, "Resumen de Cheques", 3, title);
        addPeriodRow(s, r, 3);

        CheckSummaryDTO cs = r.checkSummary();
        if (cs == null) {
            Row r0 = s.createRow(3);
            cell(r0, 0, "No hay cheques en el período seleccionado.", null);
            return;
        }

        int row = 3;
        Row h = s.createRow(row++);
        cell(h, 0, "Estado", header);
        cell(h, 1, "Cantidad", header);
        cell(h, 2, "Monto $", header);

        Map<String, Object[]> rows = new LinkedHashMap<>();
        rows.put("Pendiente",  new Object[]{cs.pendingCount(),   cs.pendingAmount()});
        rows.put("Vencido",    new Object[]{cs.overdueCount(),   cs.overdueAmount()});
        rows.put("Cobrado",    new Object[]{cs.settledCount(),   cs.settledAmount()});
        rows.put("Rechazado",  new Object[]{cs.rejectedCount(),  cs.rejectedAmount()});
        rows.put("Cancelado",  new Object[]{cs.cancelledCount(), cs.cancelledAmount()});

        for (Map.Entry<String, Object[]> e : rows.entrySet()) {
            Row dr = s.createRow(row++);
            cell(dr, 0, e.getKey(), null);
            numCell(dr, 1, (Integer) e.getValue()[0], null);
            amtCell(dr, 2, (BigDecimal) e.getValue()[1], currency);
        }

        Row tr = s.createRow(row);
        cell(tr, 0, "TOTAL", total);
        numCell(tr, 1, cs.totalChecks(), total);
        amtCell(tr, 2, cs.totalChecksAmount(), totalAmt);

        for (int i = 0; i < 3; i++) s.autoSizeColumn(i);
    }

    // ─────────────── helpers ───────────────

    private static String methodLabel(PaymentMethod m) {
        if (m == null) return "";
        return switch (m) {
            case CASH     -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case CHECK    -> "Cheque";
        };
    }

    private static String checkStatusLabel(CheckStatus s) {
        return switch (s) {
            case PENDIENTE -> "Pendiente";
            case VENCIDO   -> "Vencido";
            case COBRADO   -> "Cobrado";
            case RECHAZADO -> "Rechazado";
            case CANCELADO -> "Cancelado";
        };
    }

    private static String treasuryRef(IssuedPaymentReportItemDTO p) {
        if (p.bankAccountName() != null) return p.bankAccountName();
        if (p.cashBoxName() != null) return p.cashBoxName();
        return "";
    }

    private void addTitleRow(Sheet s, String text, int span, CellStyle title) {
        Row r0 = s.createRow(0);
        Cell c = r0.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(title);
        if (span > 1) s.addMergedRegion(new CellRangeAddress(0, 0, 0, span - 1));
    }

    private void addPeriodRow(Sheet s, IssuedPaymentReportDTO r, int span) {
        Row r1 = s.createRow(1);
        Cell c = r1.createCell(0);
        c.setCellValue("Período: " + r.periodDescription());
        if (span > 1) s.addMergedRegion(new CellRangeAddress(1, 1, 0, span - 1));
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private static void numCell(Row row, int col, int value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private static void amtCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value.doubleValue() : 0.0);
        if (style != null) c.setCellStyle(style);
    }

    // ─────────────── styles ───────────────

    private static CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }
    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }
    private static CellStyle currencyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("\"$\"#,##0.00"));
        return s;
    }
    private static CellStyle subtotalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
    private static CellStyle subtotalCurrencyStyle(Workbook wb) {
        CellStyle s = subtotalStyle(wb);
        s.setDataFormat(wb.createDataFormat().getFormat("\"$\"#,##0.00"));
        return s;
    }
    private static CellStyle totalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
    private static CellStyle totalCurrencyStyle(Workbook wb) {
        CellStyle s = totalStyle(wb);
        s.setDataFormat(wb.createDataFormat().getFormat("\"$\"#,##0.00"));
        return s;
    }
    private static CellStyle dateStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));
        return s;
    }
}
