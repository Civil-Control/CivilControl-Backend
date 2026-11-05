package PSG.backEnd.repository;

import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Criteria-API specification for {@link CertificationRepository}. Replaces the previous
 * {@code "(:param IS NULL OR ...)"} JPQL string query, which threw a 500
 * (InvalidDataAccessResourceUsageException / implicit type casts) once the {@code hasInvoice}
 * boolean-vs-TRUE/FALSE comparisons were combined with the {@code search} clause — same class of
 * Postgres parameter-type-inference issue already fixed this way for RecoveryEvent.
 */
public final class CertificationSpecifications {

    private CertificationSpecifications() {}

    public static Specification<Certification> forFilters(
            Long workContractId,
            Long clientId,
            CertificationStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean hasInvoice,
            Long salesDocumentId,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (workContractId != null) {
                predicates.add(cb.equal(root.get("contract").get("id"), workContractId));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("contract").get("client").get("id"), clientId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("certificationDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("certificationDate"), dateTo));
            }
            if (hasInvoice != null) {
                predicates.add(hasInvoice
                        ? cb.isNotNull(root.get("salesDocument"))
                        : cb.isNull(root.get("salesDocument")));
            }
            if (salesDocumentId != null) {
                predicates.add(cb.equal(root.get("salesDocument").get("id"), salesDocumentId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("contract").get("contractNumber")), pattern),
                        cb.like(cb.lower(root.get("contract").get("client").get("businessName")), pattern),
                        cb.like(root.get("certificationNumber").as(String.class), "%" + search.trim() + "%")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
