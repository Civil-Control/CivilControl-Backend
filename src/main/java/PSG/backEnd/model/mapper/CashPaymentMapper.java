package PSG.backEnd.model.mapper;


import PSG.backEnd.model.dto.payment.CashPaymentDTO;
import PSG.backEnd.model.dto.payment.CashPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.CashPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class, TreasuryRefMapper.class})
public interface CashPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "cashBox", source = "cashBoxId")
    CashPayment toEntityOnCreate(CashPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "cashBox", source = "cashBoxId")
    void updateEntityFromDto(CashPaymentDTO dto, @MappingTarget CashPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "efectivo")
    @Mapping(target = "cashBoxId", source = "cashBox.id")
    @Mapping(target = "cashBoxName", source = "cashBox.name")
    CashPaymentResponseDTO toResponse(CashPayment entity);
}