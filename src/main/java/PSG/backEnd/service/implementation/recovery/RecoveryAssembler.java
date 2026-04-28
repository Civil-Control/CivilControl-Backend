package PSG.backEnd.service.implementation.recovery;

import PSG.backEnd.model.dto.recovery.RecoveryEventResponseDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import PSG.backEnd.model.entity.recovery.RecoverySupplierConfig;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.repository.recovery.RecoveryEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Centralizes the mapping from Value Recovery (Feature 18) entities to their response DTOs.
 * Handles enrichment with derived values (event counters, accumulated recovered amount,
 * denormalized references) so endpoints can return self-contained payloads.
 */
@Component
@RequiredArgsConstructor
public class RecoveryAssembler {

    private final RecoveryEventRepository recoveryEventRepository;
    private final UserRepository userRepository;

    public RecoverySupplierConfigResponseDTO toResponse(RecoverySupplierConfig config) {
        if (config == null) return null;
        BigDecimal accumulated = recoveryEventRepository.sumRecoveredAmountByConfig(config.getId());
        long count = recoveryEventRepository.countBySupplierConfigId(config.getId());
        return new RecoverySupplierConfigResponseDTO(
                config.getId(),
                config.getProjectArea() != null ? config.getProjectArea().getId() : null,
                config.getProjectArea() != null ? config.getProjectArea().getName() : null,
                config.getSupplier() != null ? config.getSupplier().getId() : null,
                config.getSupplier() != null ? config.getSupplier().getLegalName() : null,
                config.getSupplier() != null ? config.getSupplier().getCuit() : null,
                config.getRecoveryPercentage(),
                config.getCashBox() != null ? config.getCashBox().getId() : null,
                config.getCashBox() != null ? config.getCashBox().getName() : null,
                config.getCashBox() != null ? config.getCashBox().getBalance() : null,
                config.getActive(),
                (int) count,
                accumulated != null ? accumulated : BigDecimal.ZERO
        );
    }

    public RecoveryEventResponseDTO toResponse(RecoveryEvent event) {
        if (event == null) return null;
        TransactionalDocument doc = event.getTransactionalDocument();
        TransactionalDocument cn = event.getCreditNoteDocument();
        return new RecoveryEventResponseDTO(
                event.getId(),
                event.getEventType(),
                doc != null ? doc.getId() : null,
                documentReference(doc),
                cn != null ? cn.getId() : null,
                documentReference(cn),
                event.getReversesEvent() != null ? event.getReversesEvent().getId() : null,
                event.getSupplierConfig() != null && event.getSupplierConfig().getSupplier() != null
                        ? event.getSupplierConfig().getSupplier().getId() : null,
                event.getSupplierConfig() != null && event.getSupplierConfig().getSupplier() != null
                        ? event.getSupplierConfig().getSupplier().getLegalName() : null,
                event.getSnapshotPercentage(),
                event.getSnapshotCashBox() != null ? event.getSnapshotCashBox().getId() : null,
                event.getSnapshotCashBox() != null ? event.getSnapshotCashBox().getName() : null,
                event.getDocumentNet(),
                event.getDocumentIva(),
                event.getRecoveredAmount(),
                event.getCashBoxMovement() != null ? event.getCashBoxMovement().getId() : null,
                event.getOccurredAt(),
                event.getTriggeredByUserId(),
                lookupUserName(event.getTriggeredByUserId())
        );
    }

    private static String documentReference(TransactionalDocument doc) {
        if (doc == null) return null;
        String type = doc.getDocumentType() != null ? doc.getDocumentType().name() : "";
        String branch = doc.getBranchCode() != null ? doc.getBranchCode() : "";
        String number = doc.getDocumentNumber() != null ? doc.getDocumentNumber() : "";
        return (type + " " + branch + "-" + number).trim();
    }

    private String lookupUserName(Long userId) {
        if (userId == null || userId == 0L) return null;
        Optional<User> user = userRepository.findById(userId);
        return user
                .map(u -> ((u.getFirstName() == null ? "" : u.getFirstName() + " ")
                        + (u.getLastName() == null ? "" : u.getLastName())).trim())
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}
