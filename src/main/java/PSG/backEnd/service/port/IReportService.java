package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
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
     * Collects all money outflow items based on the provided filters.
     * This is an internal method that consolidates data from all sources.
     *
     * @param filters The filter criteria to apply
     * @return List of report items matching the filter criteria
     */
    List<ReportItemDTO> collectMoneyOutflows(ReportFilterDTO filters);
}

