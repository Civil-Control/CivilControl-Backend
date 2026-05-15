package PSG.backEnd.exception.security;

public class ChannelVerificationNotValidException extends RuntimeException {
    public ChannelVerificationNotValidException(String message) {
        super(message);
    }
}
