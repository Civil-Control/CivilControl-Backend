package PSG.backEnd.model.dto.payment;

public record CashPaymentResponseDTO(
        Long id,
        PaymentDetailsResponseDTO paymentDetails,
        String type,
        Long cashBoxId,
        String cashBoxName
) implements PaymentResponseDTO {
}
