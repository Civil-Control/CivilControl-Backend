package PSG.backEnd.exception.report;

/**
 * Exception thrown when invalid filter criteria is provided.
 */
public class InvalidReportFilterException extends RuntimeException {

    public InvalidReportFilterException(String message) {
        super(message);
    }

    public static InvalidReportFilterException invalidDateRange() {
        return new InvalidReportFilterException(
                "Invalid date range: Start date cannot be after end date"
        );
    }

    public static InvalidReportFilterException invalidAmountRange() {
        return new InvalidReportFilterException(
                "Invalid amount range: Minimum amount cannot be greater than maximum amount"
        );
    }
}

