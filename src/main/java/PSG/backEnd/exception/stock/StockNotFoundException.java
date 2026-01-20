package PSG.backEnd.exception.stock;

import PSG.backEnd.service.util.MessageSourceHelper;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("stockNotFound.notFound", id));
    }
}

