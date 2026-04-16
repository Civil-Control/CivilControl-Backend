package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.dto.report.invoice.InvoiceReportDTO;
import PSG.backEnd.model.dto.report.invoice.InvoiceReportFilterDTO;
import PSG.backEnd.model.dto.report.salary.SalaryReportDTO;
import PSG.backEnd.model.dto.report.salary.SalaryReportFilterDTO;
import PSG.backEnd.model.dto.report.servicePayment.ServicePaymentReportDTO;
import PSG.backEnd.model.dto.report.servicePayment.ServicePaymentReportFilterDTO;
import PSG.backEnd.model.enums.ReportFormat;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for generating money outflow reports.
 * Follows the Port-Adapter (Hexagonal) architecture pattern.
 */
public interface IReportService {

    /**
     * Generates a complete money outflow report based on the provided filters.
     * This method collects data, applies filters, and returns a structured report object.
     *
     * @param filters The filter criteria to apply to the report
     * @return A complete report DTO with items, totals, and summary statistics
     */
    MoneyOutflowReportDTO generateMoneyOutflowReport(ReportFilterDTO filters);

    /**
     * Generates a downloadable money outflow report file in the specified format.
     * Returns a ResponseEntity with the file content and appropriate headers.
     *
     * @param filters The filter criteria to apply to the report
     * @param format The desired export format (PDF or EXCEL)
     * @return ResponseEntity containing the file as byte array with proper content-type and headers
     */
    ResponseEntity<byte[]> generateMoneyOutflowReportFile(ReportFilterDTO filters, ReportFormat format);

    /**
     * Generates a preview/summary of a money outflow report based on the provided filters.
     * This method only calculates statistics and totals without fetching the detailed items list.
     * Much faster than generating the full report, ideal for previews.
     *
     * @param filters The filter criteria to apply to the report
     * @return A preview DTO with totals and summary statistics (no items list)
     */
    MoneyOutflowReportPreviewDTO generateMoneyOutflowReportPreview(ReportFilterDTO filters);

    /**
     * Collects all money outflow items based on the provided filters.
     * This is an internal method that consolidates data from all sources.
     *
     * @param filters The filter criteria to apply
     * @return List of report items matching the filter criteria
     */
    List<ReportItemDTO> collectMoneyOutflows(ReportFilterDTO filters);

    /**
     * Generates a hierarchical salary report grouped by area and employee.
     *
     * @param filters The salary-specific filter criteria
     * @return A structured salary report with area groups, employee groups, and frequency subtotals
     */
    SalaryReportDTO generateSalaryReport(SalaryReportFilterDTO filters);

    /**
     * Generates a downloadable salary report file in the specified format.
     *
     * @param filters The salary-specific filter criteria
     * @param format The desired export format (PDF or EXCEL)
     * @return ResponseEntity containing the file as byte array with proper content-type and headers
     */
    ResponseEntity<byte[]> generateSalaryReportFile(SalaryReportFilterDTO filters, ReportFormat format);

    InvoiceReportDTO generateInvoiceReport(InvoiceReportFilterDTO filters);

    ResponseEntity<byte[]> generateInvoiceReportFile(InvoiceReportFilterDTO filters, ReportFormat format);

    ServicePaymentReportDTO generateServicePaymentReport(ServicePaymentReportFilterDTO filters);

    ResponseEntity<byte[]> generateServicePaymentReportFile(ServicePaymentReportFilterDTO filters, ReportFormat format);
}

