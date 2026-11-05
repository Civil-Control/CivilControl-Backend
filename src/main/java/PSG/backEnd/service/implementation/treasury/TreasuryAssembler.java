package PSG.backEnd.service.implementation.treasury;

import PSG.backEnd.model.dto.treasury.*;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.treasury.*;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.repository.treasury.BankAccountMovementRepository;
import PSG.backEnd.repository.treasury.CashBoxMovementRepository;
import PSG.backEnd.repository.treasury.CheckbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Centralizes the mapping from Treasury entities to their response DTOs.
 * Encapsulates all enrichment with derived values (counts, last movement, used numbers, …).
 */
@Component
@RequiredArgsConstructor
public class TreasuryAssembler {

    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    private final CashBoxMovementRepository cashBoxMovementRepository;
    private final BankAccountMovementRepository bankAccountMovementRepository;
    private final CheckbookRepository checkbookRepository;
    private final UserRepository userRepository;

    // ── CashBox ─────────────────────────────────────────────────────────────

    public CashBoxResponseDTO toResponse(CashBox box) {
        int count = cashBoxMovementRepository.countByCashBoxId(box.getId());
        return new CashBoxResponseDTO(
                box.getId(),
                box.getName(),
                box.getDescription(),
                box.getBalance(),
                box.getBalance().compareTo(BigDecimal.ZERO) < 0,
                box.getActive(),
                count,
                cashBoxMovementRepository.findLastMovementAt(box.getId())
        );
    }

    public CashBoxMovementResponseDTO toResponse(CashBoxMovement m) {
        String userName = lookupUserName(m.getCreatedByUserId());
        Long cashPaymentId = m.getCashPayment() != null ? m.getCashPayment().getId() : null;
        return new CashBoxMovementResponseDTO(
                m.getId(),
                m.getCashBox().getId(),
                m.getCashBox().getName(),
                m.getType(),
                m.getAmount(),
                m.getSignedAmount(),
                m.getBalanceAfter(),
                m.getMovementDate(),
                m.getComment(),
                m.getCreatedAt(),
                m.getCreatedByUserId(),
                userName,
                cashPaymentId
        );
    }

    // ── BankAccount ─────────────────────────────────────────────────────────

    public BankAccountResponseDTO toResponse(BankAccount acc) {
        return new BankAccountResponseDTO(
                acc.getId(),
                acc.getName(),
                acc.getBankName(),
                acc.getAccountType(),
                acc.getAccountNumber(),
                acc.getCbu(),
                acc.getAlias(),
                acc.getCurrency(),
                acc.getBalance(),
                acc.getActive(),
                bankAccountMovementRepository.countByBankAccountId(acc.getId()),
                bankAccountMovementRepository.findLastMovementAt(acc.getId()),
                checkbookRepository.countByBankAccountIdAndActiveTrueAndDeletedFalse(acc.getId())
        );
    }

    public BankAccountMovementResponseDTO toResponse(BankAccountMovement m) {
        String userName = lookupUserName(m.getCreatedByUserId());
        Long checkPaymentId = m.getCheckPayment() != null ? m.getCheckPayment().getId() : null;
        Long transferPaymentId = m.getTransferPayment() != null ? m.getTransferPayment().getId() : null;
        return new BankAccountMovementResponseDTO(
                m.getId(),
                m.getBankAccount().getId(),
                m.getBankAccount().getName(),
                m.getType(),
                m.getAmount(),
                m.getSignedAmount(),
                m.getBalanceAfter(),
                m.getMovementDate(),
                m.getComment(),
                m.getCreatedAt(),
                m.getCreatedByUserId(),
                userName,
                checkPaymentId,
                transferPaymentId
        );
    }

    // ── Checkbook ───────────────────────────────────────────────────────────

    public CheckbookResponseDTO toResponse(Checkbook cb) {
        long total = cb.getRangeTo() - cb.getRangeFrom() + 1;
        List<Long> usedNumbers = numericUsedNumbers(cb.getId(), cb.getRangeFrom(), cb.getRangeTo());
        int used = usedNumbers.size();
        long available = total - used;
        Long next = nextAvailable(cb.getRangeFrom(), cb.getRangeTo(), usedNumbers);
        return new CheckbookResponseDTO(
                cb.getId(),
                cb.getName(),
                cb.getCheckbookNumber(),
                cb.getBankAccount().getId(),
                cb.getBankAccount().getName(),
                cb.getBankAccount().getBankName(),
                cb.getCheckType(),
                cb.getRangeFrom(),
                cb.getRangeTo(),
                (int) total,
                used,
                (int) available,
                next,
                cb.getActive()
        );
    }

    public List<Long> numericUsedNumbers(Long checkbookId, Long rangeFrom, Long rangeTo) {
        return checkbookRepository.findUsedCheckNumberStrings(checkbookId).stream()
                .filter(s -> s != null && NUMERIC.matcher(s).matches())
                .map(Long::parseLong)
                .filter(n -> n >= rangeFrom && n <= rangeTo)
                .toList();
    }

    private Long nextAvailable(Long from, Long to, List<Long> used) {
        java.util.HashSet<Long> set = new java.util.HashSet<>(used);
        for (long i = from; i <= to; i++) {
            if (!set.contains(i)) return i;
        }
        return null;
    }

    private String lookupUserName(Long userId) {
        if (userId == null) return null;
        Optional<User> u = userRepository.findById(userId);
        return u.map(x -> (x.getFirstName() == null ? "" : x.getFirstName() + " ") + (x.getLastName() == null ? "" : x.getLastName())).map(String::trim).orElse(null);
    }
}
