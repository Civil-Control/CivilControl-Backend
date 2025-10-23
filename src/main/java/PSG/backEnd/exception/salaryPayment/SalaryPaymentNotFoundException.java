package PSG.backEnd.exception.salaryPayment;

public class SalaryPaymentNotFoundException extends RuntimeException {
    public SalaryPaymentNotFoundException(Long id) {
        super("No salary payment found for ID: " + id);
    }
}

