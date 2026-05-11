package PSG.backEnd.exception.purchaseOrder;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class PurchaseOrderNotFoundException extends NotFoundException {
    public PurchaseOrderNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("purchaseOrder.notFound", id));
    }
}
