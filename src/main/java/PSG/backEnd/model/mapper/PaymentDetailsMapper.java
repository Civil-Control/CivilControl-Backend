package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.payment.PaymentApplicationResponseDTO;
import PSG.backEnd.model.dto.payment.PaymentDetailsDTO;
import PSG.backEnd.model.dto.payment.PaymentDetailsResponseDTO;
import PSG.backEnd.model.entity.payment.PaymentApplication;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.hibernate.Hibernate;
import org.mapstruct.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PaymentDetailsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", source = "supplierId")
    @Mapping(target = "paidDocuments", source = "paidDocumentIds")
    @Mapping(target = "applications", ignore = true)            // wired in service layer
    @Mapping(target = "onAccountAmount", ignore = true)         // wired in service layer
    @Mapping(target = "cashPayment", ignore = true)
    @Mapping(target = "transferPayment", ignore = true)
    @Mapping(target = "checkPayment", ignore = true)
    PaymentDetails toEntity(PaymentDetailsDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", source = "supplierId")
    @Mapping(target = "paidDocuments", source = "paidDocumentIds")
    @Mapping(target = "applications", ignore = true)            // wired in service layer
    @Mapping(target = "onAccountAmount", ignore = true)         // wired in service layer
    @Mapping(target = "cashPayment", ignore = true)
    @Mapping(target = "transferPayment", ignore = true)
    @Mapping(target = "checkPayment", ignore = true)
    void updateEntityFromDto(PaymentDetailsDTO dto, @MappingTarget PaymentDetails entity);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.legalName")
    @Mapping(target = "paidDocumentIds", source = "paidDocuments")
    @Mapping(target = "applications", source = "applications")
    @Mapping(target = "onAccountAmount", source = "onAccountAmount", defaultExpression = "java(java.math.BigDecimal.ZERO)")
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
    // Safe against uninitialized Hibernate proxies (lazy collections not yet loaded)
    default List<Long> mapPaidDocumentIds(List<TransactionalDocument> paidDocuments) {
        if (paidDocuments == null) {
            return null;
        }
        if (!Hibernate.isInitialized(paidDocuments)) {
            return Collections.emptyList();
        }
        return paidDocuments.stream()
                .map(TransactionalDocument::getId)
                .collect(Collectors.toList());
    }

    /**
     * Maps the bidirectional {@code applications} set into the response DTO list. Returns
     * an empty list when the collection is uninitialized to avoid {@code LazyInitializationException}
     * in serialization paths that don't fetch the collection eagerly.
     */
    default List<PaymentApplicationResponseDTO> mapApplications(java.util.Set<PaymentApplication> applications) {
        if (applications == null || !Hibernate.isInitialized(applications)) {
            return Collections.emptyList();
        }
        return applications.stream()
                .map(this::toApplicationResponse)
                .sorted((a, b) -> {
                    if (a.id() == null || b.id() == null) return 0;
                    return a.id().compareTo(b.id());
                })
                .collect(Collectors.toList());
    }

    default PaymentApplicationResponseDTO toApplicationResponse(PaymentApplication app) {
        TransactionalDocument doc = app.getDocument();
        String reference = null;
        BigDecimal docTotal = null;
        if (doc != null && Hibernate.isInitialized(doc)) {
            docTotal = doc.getTotal();
            String typeLabel = doc.getDocumentType() != null ? doc.getDocumentType().name() : "";
            String pos = doc.getBranchCode() != null ? doc.getBranchCode() : "";
            String num = doc.getDocumentNumber() != null ? doc.getDocumentNumber() : "";
            reference = (typeLabel + " " + pos + "-" + num).trim();
        }
        return new PaymentApplicationResponseDTO(
                app.getId(),
                doc != null ? doc.getId() : null,
                reference,
                docTotal,
                app.getAmountApplied()
        );
    }

    @Named("extractBankName")
    default String extractBankName(PaymentDetails payment) {
        if (payment.getTransferPayment() != null && payment.getTransferPayment().getBankAccount() != null) {
            return payment.getTransferPayment().getBankAccount().getBankName();
        } else if (payment.getCheckPayment() != null && payment.getCheckPayment().getBankAccount() != null) {
            return payment.getCheckPayment().getBankAccount().getBankName();
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
