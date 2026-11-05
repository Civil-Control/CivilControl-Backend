package PSG.backEnd.service.export;

import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.model.dto.report.sales.*;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
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
 * Excel exporter for the Sales Report.
 * Three sheets: Resumen por Área, Resumen por Cliente, Detalle de Ventas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SalesReportExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<SalesDocumentType> DOCUMENT_TYPE_ORDER = List.of(
            SalesDocumentType.FACTURA_A, SalesDocumentType.FACTURA_B, SalesDocumentType.FACTURA_C,
            SalesDocumentType.NOTA_DEBITO_A, SalesDocumentType.NOTA_DEBITO_B, SalesDocumentType.NOTA_DEBITO_C,
            SalesDocumentType.NOTA_CREDITO_A, SalesDocumentType.NOTA_CREDITO_B, SalesDocumentType.NOTA_CREDITO_C);

    private static final Map<SalesDocumentType, String> DOC_TYPE_LABELS;
    static {
        Map<SalesDocumentType, String> m = new LinkedHashMap<>();
        m.put(SalesDocumentType.FACTURA_A, "Factura A");
        m.put(SalesDocumentType.FACTURA_B, "Factura B");
        m.put(SalesDocumentType.FACTURA_C, "Factura C");
        m.put(SalesDocumentType.NOTA_DEBITO_A, "Nota Débito A");
        m.put(SalesDocumentType.NOTA_DEBITO_B, "Nota Débito B");
        m.put(SalesDocumentType.NOTA_DEBITO_C, "Nota Débito C");
        m.put(SalesDocumentType.NOTA_CREDITO_A, "Nota Crédito A");
        m.put(SalesDocumentType.NOTA_CREDITO_B, "Nota Crédito B");
        m.put(SalesDocumentType.NOTA_CREDITO_C, "Nota Crédito C");
        DOC_TYPE_LABELS = m;
    }

    private static final Map<CertificationStatus, String> CERT_STATUS_LABELS;
    static {
        Map<CertificationStatus, String> m = new LinkedHashMap<>();
        m.put(CertificationStatus.PRESENTADO, "Presentado");
        m.put(CertificationStatus.APROBADO, "Aprobado");
        m.put(CertificationStatus.FACTURADO, "Facturado");
        m.put(CertificationStatus.COBRADO, "Cobrado");
        CERT_STATUS_LABELS = m;
    }

    private final MessageSourceHelper messageSourceHelper;

    public byte[] export(SalesReportDTO report) {
        log.info("Exporting sales report to Excel: {} areas, {} rows",
                report.areaGroups().size(), report.totalCount());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtotalStyle = createSubtotalStyle(workbook);
            CellStyle subtotalLabelStyle = createSubtotalLabelStyle(workbook);
            CellStyle certOnlyStyle = createCertOnlyStyle(workbook);
            CellStyle certOnlyCurrencyStyle = createCertOnlyCurrencyStyle(workbook);

            List<SalesDocumentType> activeTypes = DOCUMENT_TYPE_ORDER.stream()
                    .filter(dt -> report.totalsByDocumentType().containsKey(dt))
                    .toList();

            createAreaSummarySheet(workbook, report, activeTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, titleStyle);
            createClientSummarySheet(workbook, report, activeTypes,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle,
                    titleStyle, subtotalStyle, subtotalLabelStyle);
            createDetailSheet(workbook, report,
                    headerStyle, currencyStyle, totalStyle, totalLabelStyle, dateStyle,
                    titleStyle, subtotalStyle, subtotalLabelStyle, certOnlyStyle, certOnlyCurrencyStyle);

            workbook.write(baos);

            log.info("Sales Excel report generated successfully");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating sales Excel report", e);
            throw new ReportGenerationException(
                    messageSourceHelper.getMessage("report.generation.excel.error"), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 1: Resumen por Área
    // ═══════════════════════════════════════════════════════════════════════

    private void createAreaSummarySheet(Workbook workbook, SalesReportDTO report,
                                         List<SalesDocumentType> activeTypes,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle totalStyle, CellStyle totalLabelStyle,
                                         CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Área");
        int rowNum = 0;

        rowNum = addSheetHeader(sheet, "REPORTE DE VENTAS - RESUMEN POR ÁREA", report, titleStyle,
                3 + activeTypes.size() + 1);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Área", headerStyle);
        setCellWithStyle(headerRow, col++, "Cant. Filas", headerStyle);
        for (SalesDocumentType dt : activeTypes) {
            setCellWithStyle(headerRow, col++, "Total " + DOC_TYPE_LABELS.get(dt), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Cert. sin Factura", headerStyle);
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(area.projectAreaName());
            row.createCell(c++).setCellValue(area.rowCount());
            for (SalesDocumentType dt : activeTypes) {
                Cell cell = row.createCell(c++);
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(currencyStyle);
            }
            Cell certCell = row.createCell(c++);
            certCell.setCellValue(nz(area.subtotalCertifiedOnly()).doubleValue());
            certCell.setCellStyle(currencyStyle);

            Cell totalCell = row.createCell(c);
            totalCell.setCellValue(area.subtotalAmount().doubleValue());
            totalCell.setCellStyle(currencyStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell totalLabel = totalRow.createCell(tc++);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(totalLabelStyle);
        Cell totalCountCell = totalRow.createCell(tc++);
        totalCountCell.setCellValue(report.totalCount());
        totalCountCell.setCellStyle(totalStyle);
        for (SalesDocumentType dt : activeTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell certTotal = totalRow.createCell(tc++);
        certTotal.setCellValue(nz(report.totalCertifiedOnly()).doubleValue());
        certTotal.setCellStyle(totalStyle);

        Cell grandTotalCell = totalRow.createCell(tc);
        grandTotalCell.setCellValue(report.totalAmount().doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        for (int i = 0; i <= tc; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 2: Resumen por Cliente
    // ═══════════════════════════════════════════════════════════════════════

    private void createClientSummarySheet(Workbook workbook, SalesReportDTO report,
                                           List<SalesDocumentType> activeTypes,
                                           CellStyle headerStyle, CellStyle currencyStyle,
                                           CellStyle totalStyle, CellStyle totalLabelStyle,
                                           CellStyle titleStyle, CellStyle subtotalStyle,
                                           CellStyle subtotalLabelStyle) {
        Sheet sheet = workbook.createSheet("Resumen por Cliente");
        int rowNum = 0;

        int totalCols = 3 + activeTypes.size() + 1; // Cliente, CUIT, [types...], CertOnly, Total
        rowNum = addSheetHeader(sheet, "REPORTE DE VENTAS - RESUMEN POR CLIENTE", report, titleStyle, totalCols);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Cliente (Razón Social)", headerStyle);
        setCellWithStyle(headerRow, col++, "CUIT", headerStyle);
        for (SalesDocumentType dt : activeTypes) {
            setCellWithStyle(headerRow, col++, "Total " + DOC_TYPE_LABELS.get(dt), headerStyle);
        }
        setCellWithStyle(headerRow, col++, "Cert. sin Factura", headerStyle);
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        CellStyle areaHeaderStyle = workbook.createCellStyle();
        Font areaFont = workbook.createFont();
        areaFont.setBold(true);
        areaHeaderStyle.setFont(areaFont);
        areaHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        areaHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        areaHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            int areaHeaderRowNum = rowNum;
            Row areaRow = sheet.createRow(rowNum++);
            Cell areaCell = areaRow.createCell(0);
            areaCell.setCellValue(area.projectAreaName());
            areaCell.setCellStyle(areaHeaderStyle);
            for (int i = 1; i < totalCols; i++) {
                areaRow.createCell(i).setCellStyle(areaHeaderStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(areaHeaderRowNum, areaHeaderRowNum, 0, totalCols - 1));

            for (SalesReportClientGroupDTO client : area.clientGroups()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(
                        client.clientBusinessName() != null ? client.clientBusinessName() : "(Sin razón social)");
                row.createCell(c++).setCellValue(client.clientCuit() != null ? client.clientCuit() : "-");
                for (SalesDocumentType dt : activeTypes) {
                    Cell cell = row.createCell(c++);
                    BigDecimal val = client.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                    cell.setCellValue(val.doubleValue());
                    cell.setCellStyle(currencyStyle);
                }
                Cell cOnly = row.createCell(c++);
                cOnly.setCellValue(nz(client.subtotalCertifiedOnly()).doubleValue());
                cOnly.setCellStyle(currencyStyle);

                Cell totalCell = row.createCell(c);
                totalCell.setCellValue(client.totalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Area subtotal row
            Row sub = sheet.createRow(rowNum++);
            int sc = 0;
            Cell subLabel = sub.createCell(sc++);
            subLabel.setCellValue("Subtotal " + area.projectAreaName());
            subLabel.setCellStyle(subtotalLabelStyle);
            sub.createCell(sc++).setCellStyle(subtotalLabelStyle);
            for (SalesDocumentType dt : activeTypes) {
                Cell cell = sub.createCell(sc++);
                BigDecimal val = area.subtotalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
                cell.setCellValue(val.doubleValue());
                cell.setCellStyle(subtotalStyle);
            }
            Cell subCert = sub.createCell(sc++);
            subCert.setCellValue(nz(area.subtotalCertifiedOnly()).doubleValue());
            subCert.setCellStyle(subtotalStyle);

            Cell subTotal = sub.createCell(sc);
            subTotal.setCellValue(area.subtotalAmount().doubleValue());
            subTotal.setCellStyle(subtotalStyle);
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        int tc = 0;
        Cell tl = totalRow.createCell(tc++);
        tl.setCellValue("TOTAL GENERAL");
        tl.setCellStyle(totalLabelStyle);
        totalRow.createCell(tc++).setCellStyle(totalLabelStyle);
        for (SalesDocumentType dt : activeTypes) {
            Cell cell = totalRow.createCell(tc++);
            BigDecimal val = report.totalsByDocumentType().getOrDefault(dt, BigDecimal.ZERO);
            cell.setCellValue(val.doubleValue());
            cell.setCellStyle(totalStyle);
        }
        Cell ct = totalRow.createCell(tc++);
        ct.setCellValue(nz(report.totalCertifiedOnly()).doubleValue());
        ct.setCellStyle(totalStyle);

        Cell gt = totalRow.createCell(tc);
        gt.setCellValue(report.totalAmount().doubleValue());
        gt.setCellStyle(totalStyle);

        for (int i = 0; i <= tc; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHEET 3: Detalle de Ventas
    // ═══════════════════════════════════════════════════════════════════════

    private void createDetailSheet(Workbook workbook, SalesReportDTO report,
                                    CellStyle headerStyle, CellStyle currencyStyle,
                                    CellStyle totalStyle, CellStyle totalLabelStyle,
                                    CellStyle dateStyle, CellStyle titleStyle,
                                    CellStyle subtotalStyle, CellStyle subtotalLabelStyle,
                                    CellStyle certOnlyStyle, CellStyle certOnlyCurrencyStyle) {
        Sheet sheet = workbook.createSheet("Detalle de Ventas");
        int rowNum = 0;

        int colCount = 12;
        rowNum = addSheetHeader(sheet, "REPORTE DE VENTAS - DETALLE", report, titleStyle, colCount);
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        int col = 0;
        setCellWithStyle(headerRow, col++, "Tipo Fila", headerStyle);
        setCellWithStyle(headerRow, col++, "Fecha", headerStyle);
        setCellWithStyle(headerRow, col++, "Cliente / Contrato", headerStyle);
        setCellWithStyle(headerRow, col++, "Comprobante / Certif.", headerStyle);
        setCellWithStyle(headerRow, col++, "Tipo Doc / Estado", headerStyle);
        setCellWithStyle(headerRow, col++, "Sector / Tarea", headerStyle);
        setCellWithStyle(headerRow, col++, "Pagado/Cobrado", headerStyle);
        setCellWithStyle(headerRow, col++, "Neto", headerStyle);
        setCellWithStyle(headerRow, col++, "IVA", headerStyle);
        setCellWithStyle(headerRow, col++, "Otros Trib.", headerStyle);
        setCellWithStyle(headerRow, col++, "Cert. Vinculadas", headerStyle);
        setCellWithStyle(headerRow, col++, "Total", headerStyle);

        for (SalesReportAreaGroupDTO area : report.areaGroups()) {
            // Area band
            Row areaRow = sheet.createRow(rowNum);
            CellStyle areaStyle = workbook.createCellStyle();
            Font af = workbook.createFont();
            af.setBold(true);
            af.setFontHeightInPoints((short) 12);
            areaStyle.setFont(af);
            areaStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            areaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Cell ac = areaRow.createCell(0);
            ac.setCellValue("ÁREA: " + area.projectAreaName());
            ac.setCellStyle(areaStyle);
            for (int i = 1; i < colCount; i++) areaRow.createCell(i).setCellStyle(areaStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colCount - 1));
            rowNum++;

            for (SalesReportClientGroupDTO client : area.clientGroups()) {
                // Client band
                Row clientRow = sheet.createRow(rowNum);
                CellStyle clientStyle = workbook.createCellStyle();
                Font cf = workbook.createFont();
                cf.setBold(true);
                clientStyle.setFont(cf);
                clientStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                clientStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Cell cc = clientRow.createCell(0);
                String label = "Cliente: " + (client.clientBusinessName() != null ? client.clientBusinessName() : "(Sin razón social)")
                        + (client.clientCuit() != null ? " — CUIT: " + client.clientCuit() : "");
                cc.setCellValue(label);
                cc.setCellStyle(clientStyle);
                for (int i = 1; i < colCount; i++) clientRow.createCell(i).setCellStyle(clientStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colCount - 1));
                rowNum++;

                for (SalesReportRowDTO r : client.rows()) {
                    Row row = sheet.createRow(rowNum++);
                    boolean cert = r.kind() == SalesReportRowKind.CERTIFICATION_ONLY;
                    CellStyle txt = cert ? certOnlyStyle : null;
                    CellStyle cur = cert ? certOnlyCurrencyStyle : currencyStyle;

                    int c2 = 0;
                    setMaybe(row, c2++, cert ? "Certif. sin factura" : "Factura", txt);
                    setMaybe(row, c2++, r.date() != null ? r.date().format(DATE_FORMATTER) : "", txt);
                    if (cert) {
                        setMaybe(row, c2++, r.contractNumber() != null
                                ? "Contrato: " + r.contractNumber()
                                : "(sin contrato)", txt);
                        setMaybe(row, c2++, "Certif. #" + (r.certificationNumber() != null ? r.certificationNumber() : "-"), txt);
                        setMaybe(row, c2++, r.certificationStatus() != null
                                ? CERT_STATUS_LABELS.getOrDefault(r.certificationStatus(), r.certificationStatus().name())
                                : "-", txt);
                    } else {
                        setMaybe(row, c2++, "", txt);
                        String num = (r.branchCode() != null ? r.branchCode() : "")
                                + (r.documentNumber() != null ? "-" + r.documentNumber() : "");
                        setMaybe(row, c2++, num.isBlank() ? "-" : num, txt);
                        setMaybe(row, c2++, r.documentType() != null
                                ? DOC_TYPE_LABELS.getOrDefault(r.documentType(), r.documentType().name())
                                : "-", txt);
                    }
                    String areaTask = area.projectAreaName()
                            + (r.projectAreaTaskName() != null ? " / " + r.projectAreaTaskName() : "");
                    setMaybe(row, c2++, areaTask, txt);
                    setMaybe(row, c2++, r.paid() != null && r.paid() ? "Sí" : "No", txt);

                    Cell netCell = row.createCell(c2++);
                    netCell.setCellValue(nz(r.netTotal()).doubleValue());
                    netCell.setCellStyle(cur);
                    Cell ivaCell = row.createCell(c2++);
                    ivaCell.setCellValue(nz(r.ivaTotal()).doubleValue());
                    ivaCell.setCellStyle(cur);
                    Cell otCell = row.createCell(c2++);
                    otCell.setCellValue(nz(r.otherTaxes()).doubleValue());
                    otCell.setCellStyle(cur);

                    String linkedCerts = "";
                    if (!cert && r.linkedCertifications() != null && !r.linkedCertifications().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (SalesReportCertificationLinkDTO lc : r.linkedCertifications()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append("#").append(lc.certificationNumber());
                            if (lc.contractNumber() != null) sb.append(" (").append(lc.contractNumber()).append(")");
                        }
                        linkedCerts = sb.toString();
                    }
                    setMaybe(row, c2++, linkedCerts, txt);

                    Cell totCell = row.createCell(c2);
                    totCell.setCellValue(nz(r.amount()).doubleValue());
                    totCell.setCellStyle(cur);
                }
            }
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell tl = totalRow.createCell(0);
        tl.setCellValue("TOTAL GENERAL");
        tl.setCellStyle(totalLabelStyle);
        for (int i = 1; i < colCount - 1; i++) {
            totalRow.createCell(i).setCellStyle(totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, colCount - 2));
        Cell gtc = totalRow.createCell(colCount - 1);
        gtc.setCellValue(report.totalAmount().doubleValue());
        gtc.setCellStyle(totalStyle);

        for (int i = 0; i < colCount; i++) sheet.autoSizeColumn(i);
    }

    private void setMaybe(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private int addSheetHeader(Sheet sheet, String title, SalesReportDTO report,
                                CellStyle titleCellStyle, int mergeColCount) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleCellStyle);
        if (mergeColCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeColCount - 1));
        }
        Row companyRow = sheet.createRow(rowNum++);
        companyRow.createCell(0).setCellValue("ESEA S.A.");
        rowNum++;
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Período:");
        periodRow.createCell(1).setCellValue(report.periodDescription());
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Fecha generación:");
        dateRow.createCell(1).setCellValue(report.generatedAt().format(DATETIME_FORMATTER));
        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Total registros:");
        countRow.createCell(1).setCellValue(report.totalCount());
        rowNum++;
        return rowNum;
    }

    private void setCellWithStyle(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createTotalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createSubtotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSubtotalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createCertOnlyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font f = workbook.createFont();
        f.setItalic(true);
        style.setFont(f);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCertOnlyCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font f = workbook.createFont();
        f.setItalic(true);
        style.setFont(f);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
