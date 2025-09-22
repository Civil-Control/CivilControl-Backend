package PSG.backEnd.exception.gasStation;

public class DuplicateTicketException extends RuntimeException {
    public DuplicateTicketException(String ticketNumber, String branchCode) {
        super("Ticket number '" + ticketNumber + "' already exists for branch '" + branchCode + "'");
    }
}
