package PSG.backEnd.exception.stock;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long id) {
        super("No stock found for ID: " + id);
    }
}

