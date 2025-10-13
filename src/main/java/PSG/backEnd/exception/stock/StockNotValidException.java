package PSG.backEnd.exception.stock;

public class StockNotValidException extends RuntimeException {
    public StockNotValidException(String message) {
        super(message);
    }
}

