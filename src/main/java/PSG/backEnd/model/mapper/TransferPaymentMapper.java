package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.TransferPaymentDTO;
import PSG.backEnd.model.dto.payment.TransferPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.TransferPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class})
public interface TransferPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    TransferPayment toEntityOnCreate(TransferPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    void updateEntityFromDto(TransferPaymentDTO dto, @MappingTarget TransferPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "transferencia")
    TransferPaymentResponseDTO toResponse(TransferPayment entity);
}