package PSG.backEnd.model.dto.payment;

import java.time.LocalDate;

public record CheckPaymentResponseDTO(
        Long id,
        PaymentDetailsResponseDTO paymentDetails,
        String type,
        LocalDate dueDate,
        String checkNumber,
        Long bankAccountId,
        String bankAccountName,
        String bankName,
        Long checkbookId,
        String checkbookName,
        String checkbookNumber
) implements PaymentResponseDTO {
}
