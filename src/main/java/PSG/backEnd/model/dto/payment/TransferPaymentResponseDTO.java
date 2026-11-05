package PSG.backEnd.model.dto.payment;

public record TransferPaymentResponseDTO(
        Long id,
        PaymentDetailsResponseDTO paymentDetails,
        String type,
        String transactionNumber,
        Long bankAccountId,
        String bankAccountName,
        String bankName
) implements PaymentResponseDTO {
}
