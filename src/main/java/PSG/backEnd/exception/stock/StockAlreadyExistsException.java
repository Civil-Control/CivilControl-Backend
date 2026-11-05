package PSG.backEnd.exception.stock;

public class StockAlreadyExistsException extends RuntimeException {
    public StockAlreadyExistsException(String message) {
        super(message);
    }
}

