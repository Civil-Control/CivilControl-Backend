package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.CheckPaymentDTO;
import PSG.backEnd.model.dto.payment.CheckPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.CheckPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class})
public interface CheckPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    CheckPayment toEntityOnCreate(CheckPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    void updateEntityFromDto(CheckPaymentDTO dto, @MappingTarget CheckPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "cheque")
    CheckPaymentResponseDTO toResponse(CheckPayment entity);
}