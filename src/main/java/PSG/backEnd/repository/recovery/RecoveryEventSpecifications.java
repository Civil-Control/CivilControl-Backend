package PSG.backEnd.repository.recovery;

import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import PSG.backEnd.model.entity.recovery.RecoverySupplierConfig;
import PSG.backEnd.model.entity.treasury.CashBoxMovement;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Criteria-API specification for {@link RecoveryEventRepository#findAll(Specification, org.springframework.data.domain.Pageable)}.
 *
 * Built with the Criteria API (not a string {@code @Query}) so each filter only adds a predicate
 * when its value is actually present — no "(:param IS NULL OR ...)" JPQL string idiom, which is
 * what was producing a 500 in production whenever any filter besides the unconditional
 * {@code projectAreaId} was set (root cause: type-inference edge case in that idiom's translation
 * for this specific combination of implicit multi-level joins). Criteria predicates are built with
 * real Java types, so this class of bug can't happen here.
 */
public final class RecoveryEventSpecifications {

    private RecoveryEventSpecifications() {}

    /**
     * Always restricted to active {@code GENERATED} events — at most one per document. REVERSED /
     * ADJUSTED_CREDIT_NOTE rows and any GENERATED event later superseded by a REVERSED are
     * deliberately excluded: this listing is meant to read as "what's currently recovered", not
     * a raw event-by-event audit log (that full history is still in the ledger and reachable
     * per-document via {@code listEventsForDocument}).
     */
    public static Specification<RecoveryEvent> forSector(
            Long projectAreaId,
            LocalDate fromDate,
            LocalDate toDate,
            Long cashBoxId,
            Long supplierId) {
        return (root, query, cb) -> {
            Join<RecoveryEvent, RecoverySupplierConfig> supplierConfig = root.join("supplierConfig");
            Join<RecoveryEvent, CashBoxMovement> movement = root.join("cashBoxMovement");

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(supplierConfig.get("projectArea").get("id"), projectAreaId));
            predicates.add(cb.equal(root.get("eventType"), RecoveryEventType.GENERATED));

            Subquery<Long> reversal = query.subquery(Long.class);
            Root<RecoveryEvent> r = reversal.from(RecoveryEvent.class);
            reversal.select(r.get("id"));
            reversal.where(
                    cb.equal(r.get("reversesEvent"), root),
                    cb.equal(r.get("eventType"), RecoveryEventType.REVERSED));
            predicates.add(cb.not(cb.exists(reversal)));

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(movement.get("movementDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(movement.get("movementDate"), toDate));
            }
            if (cashBoxId != null) {
                predicates.add(cb.equal(root.get("snapshotCashBox").get("id"), cashBoxId));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(supplierConfig.get("supplier").get("id"), supplierId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
