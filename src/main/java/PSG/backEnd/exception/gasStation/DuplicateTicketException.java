package PSG.backEnd.exception.gasStation;

import PSG.backEnd.service.util.MessageSourceHelper;

public class DuplicateTicketException extends RuntimeException {
    public DuplicateTicketException(String ticketNumber, String branchCode) {
        super(MessageSourceHelper.getMessageStatic("fuelLoad.ticket.duplicate", ticketNumber, branchCode));
    }
}
