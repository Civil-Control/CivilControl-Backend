package PSG.backEnd.model.dto.payment;

import java.time.LocalDate;

public record CheckPaymentResponseDTO(
        Long id,
        PaymentDetailsResponseDTO paymentDetails,
        String type,
        LocalDate dueDate,
        String checkNumber,
        String bankName
) implements PaymentResponseDTO {
}