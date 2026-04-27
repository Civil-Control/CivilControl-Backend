package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.CheckPaymentDTO;
import PSG.backEnd.model.dto.payment.CheckPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.CheckPayment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class, TreasuryRefMapper.class})
public interface CheckPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "bankAccount", source = "bankAccountId")
    @Mapping(target = "checkbook", source = "checkbookId")
    CheckPayment toEntityOnCreate(CheckPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "bankAccount", source = "bankAccountId")
    @Mapping(target = "checkbook", source = "checkbookId")
    void updateEntityFromDto(CheckPaymentDTO dto, @MappingTarget CheckPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "cheque")
    @Mapping(target = "bankAccountId", source = "bankAccount.id")
    @Mapping(target = "bankAccountName", source = "bankAccount.name")
    @Mapping(target = "bankName", source = "bankAccount.bankName")
    @Mapping(target = "checkbookId", source = "checkbook.id")
    @Mapping(target = "checkbookName", source = "checkbook.name")
    @Mapping(target = "checkbookNumber", source = "checkbook.checkbookNumber")
    CheckPaymentResponseDTO toResponse(CheckPayment entity);
}