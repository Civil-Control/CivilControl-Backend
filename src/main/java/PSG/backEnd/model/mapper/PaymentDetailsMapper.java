package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.PaymentDetailsDTO;
import PSG.backEnd.model.dto.payment.PaymentDetailsResponseDTO;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.mapstruct.*;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PaymentDetailsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", source = "supplierId")
    @Mapping(target = "paidDocuments", source = "paidDocumentIds")
    @Mapping(target = "cashPayment", ignore = true)
    @Mapping(target = "transferPayment", ignore = true)
    @Mapping(target = "checkPayment", ignore = true)
    PaymentDetails toEntity(PaymentDetailsDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", source = "supplierId")
    @Mapping(target = "paidDocuments", source = "paidDocumentIds")
    @Mapping(target = "cashPayment", ignore = true)
    @Mapping(target = "transferPayment", ignore = true)
    @Mapping(target = "checkPayment", ignore = true)
    void updateEntityFromDto(PaymentDetailsDTO dto, @MappingTarget PaymentDetails entity);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.legalName")
    @Mapping(target = "paidDocumentIds", source = "paidDocuments")
    PaymentDetailsResponseDTO toResponse(PaymentDetails entity);

    // Custom mapping to convert Long to Supplier
    default Supplier mapSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        return supplier;
    }

    // Custom mapping to convert List<Long> to List<TransactionalDocument>
    default List<TransactionalDocument> mapPaidDocuments(List<Long> paidDocumentIds) {
        if (paidDocumentIds == null) {
            return null;
        }
        return paidDocumentIds.stream()
                .map(id -> {
                    TransactionalDocument doc = new TransactionalDocument();
                    doc.setId(id);
                    return doc;
                })
                .collect(Collectors.toList());
    }

    // Custom mapping to convert List<TransactionalDocument> to List<Long>
    default List<Long> mapPaidDocumentIds(List<TransactionalDocument> paidDocuments) {
        if (paidDocuments == null) {
            return null;
        }
        return paidDocuments.stream()
                .map(TransactionalDocument::getId)
                .collect(Collectors.toList());
    }

    @Named("extractBankName")
    default String extractBankName(PaymentDetails payment) {
        if (payment.getTransferPayment() != null) {
            return payment.getTransferPayment().getBankName();
        } else if (payment.getCheckPayment() != null) {
            return payment.getCheckPayment().getBankName();
        }
        return null;
    }

    @Named("extractDueDate")
    default java.time.LocalDate extractDueDate(PaymentDetails payment) {
        if (payment.getCheckPayment() != null) {
            return payment.getCheckPayment().getDueDate();
        }
        return null;
    }

    @Named("extractCheckNumber")
    default String extractCheckNumber(PaymentDetails payment) {
        if (payment.getCheckPayment() != null) {
            return payment.getCheckPayment().getCheckNumber();
        }
        return null;
    }

}
