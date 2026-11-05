package PSG.backEnd.exception.salaryPayment;

public class SalaryPaymentNotValidException extends RuntimeException {
    public SalaryPaymentNotValidException(String message) {
        super(message);
    }
}

