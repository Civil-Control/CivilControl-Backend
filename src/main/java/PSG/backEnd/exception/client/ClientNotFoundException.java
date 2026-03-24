package PSG.backEnd.exception.client;

import PSG.backEnd.service.util.MessageSourceHelper;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("client.notFound", id));
    }
}
