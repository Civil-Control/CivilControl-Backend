package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.TransferPaymentDTO;
import PSG.backEnd.model.dto.payment.TransferPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.TransferPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class, TreasuryRefMapper.class})
public interface TransferPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "bankAccount", source = "bankAccountId")
    TransferPayment toEntityOnCreate(TransferPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "bankAccount", source = "bankAccountId")
    void updateEntityFromDto(TransferPaymentDTO dto, @MappingTarget TransferPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "transferencia")
    @Mapping(target = "bankAccountId", source = "bankAccount.id")
    @Mapping(target = "bankAccountName", source = "bankAccount.name")
    @Mapping(target = "bankName", source = "bankAccount.bankName")
    TransferPaymentResponseDTO toResponse(TransferPayment entity);
}