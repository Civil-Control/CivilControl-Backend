package PSG.backEnd.exception.salaryPayment;

public class DuplicateSalaryPaymentException extends RuntimeException {
    public DuplicateSalaryPaymentException(String message) {
        super(message);
    }
}

