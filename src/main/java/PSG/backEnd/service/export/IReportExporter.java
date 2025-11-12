package PSG.backEnd.service.export;

import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.enums.ReportFormat;

/**
 * Strategy interface for exporting reports in different formats.
 * Implementations should handle PDF, Excel, or any other future format.
 *
 * This follows the Strategy Design Pattern, allowing easy addition of new export formats
 * without modifying existing code (Open/Closed Principle).
 */
public interface IReportExporter {

    /**
     * Exports the report data to a byte array in the specific format.
     *
     * @param report The complete report data to export
     * @return Byte array containing the exported file
     * @throws RuntimeException if export fails
     */
    byte[] export(MoneyOutflowReportDTO report);

    /**
     * Returns the format that this exporter handles.
     *
     * @return The ReportFormat enum value
     */
    ReportFormat getFormat();

    /**
     * Returns the content type (MIME type) for the exported file.
     * Used for setting HTTP response headers.
     *
     * @return Content type string (e.g., "application/pdf")
     */
    String getContentType();

    /**
     * Returns the file extension for the exported file.
     * Used for generating the download filename.
     *
     * @return File extension with dot (e.g., ".pdf")
     */
    String getFileExtension();
}

