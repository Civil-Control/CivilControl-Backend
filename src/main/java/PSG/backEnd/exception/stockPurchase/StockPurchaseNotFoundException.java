package PSG.backEnd.exception.stockPurchase;

import PSG.backEnd.service.util.MessageSourceHelper;

public class StockPurchaseNotFoundException extends RuntimeException {
    public StockPurchaseNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("stockPurchaseNotFound.notFound", id));
    }
}
