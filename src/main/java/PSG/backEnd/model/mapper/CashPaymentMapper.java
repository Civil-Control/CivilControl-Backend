package PSG.backEnd.model.mapper;


import PSG.backEnd.model.dto.payment.CashPaymentDTO;
import PSG.backEnd.model.dto.payment.CashPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.CashPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class})
public interface CashPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    CashPayment toEntityOnCreate(CashPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    void updateEntityFromDto(CashPaymentDTO dto, @MappingTarget CashPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "CASH")
    CashPaymentResponseDTO toResponse(CashPayment entity);
}