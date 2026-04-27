package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.CheckPaymentDTO;
import PSG.backEnd.model.dto.payment.CheckPaymentResponseDTO;
import PSG.backEnd.model.entity.payment.CheckPayment;
import PSG.backEnd.model.enums.payment.CheckStatus;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper(componentModel = "spring", uses = {PaymentDetailsMapper.class, TreasuryRefMapper.class})
public interface CheckPaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "bankAccount", source = "bankAccountId")
    @Mapping(target = "checkbook", source = "checkbookId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "settledDate", ignore = true)
    @Mapping(target = "statusComment", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "statusChangedByUserId", ignore = true)
    CheckPayment toEntityOnCreate(CheckPaymentDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "bankAccount", source = "bankAccountId")
    @Mapping(target = "checkbook", source = "checkbookId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "settledDate", ignore = true)
    @Mapping(target = "statusComment", ignore = true)
    @Mapping(target = "statusChangedAt", ignore = true)
    @Mapping(target = "statusChangedByUserId", ignore = true)
    void updateEntityFromDto(CheckPaymentDTO dto, @MappingTarget CheckPayment entity);

    @Mapping(target = "paymentDetails", source = "paymentDetails")
    @Mapping(target = "type", constant = "cheque")
    @Mapping(target = "bankAccountId", source = "bankAccount.id")
    @Mapping(target = "bankAccountName", source = "bankAccount.name")
    @Mapping(target = "bankName", source = "bankAccount.bankName")
    @Mapping(target = "checkbookId", source = "checkbook.id")
    @Mapping(target = "checkbookName", source = "checkbook.name")
    @Mapping(target = "checkbookNumber", source = "checkbook.checkbookNumber")
    @Mapping(target = "status", expression = "java(deriveEffectiveStatus(entity))")
    @Mapping(target = "persistedStatus", source = "status")
    @Mapping(target = "settledDate", source = "settledDate")
    @Mapping(target = "statusComment", source = "statusComment")
    @Mapping(target = "statusChangedAt", source = "statusChangedAt")
    @Mapping(target = "statusChangedByUserId", source = "statusChangedByUserId")
    CheckPaymentResponseDTO toResponse(CheckPayment entity);

    /**
     * Derives the effective (UI-facing) status: a PENDIENTE check whose due date is past
     * is reported as VENCIDO without ever persisting that value.
     */
    default CheckStatus deriveEffectiveStatus(CheckPayment entity) {
        if (entity == null) return null;
        CheckStatus persisted = entity.getStatus();
        if (persisted == CheckStatus.PENDIENTE
                && entity.getDueDate() != null
                && entity.getDueDate().isBefore(LocalDate.now())) {
            return CheckStatus.VENCIDO;
        }
        return persisted;
    }
}