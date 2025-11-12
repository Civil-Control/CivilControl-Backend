package PSG.backEnd.exception.report;

/**
 * Exception thrown when an invalid report format is requested.
 */
public class InvalidReportFormatException extends RuntimeException {

    public InvalidReportFormatException(String format) {
        super(String.format("Invalid report format: %s. Supported formats are: PDF, EXCEL", format));
    }
}

