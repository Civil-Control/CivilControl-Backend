package PSG.backEnd.service.implementation.treasury;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.treasury.BankAccountDTO;
import PSG.backEnd.model.dto.treasury.BankAccountMovementDTO;
import PSG.backEnd.model.dto.treasury.BankAccountMovementResponseDTO;
import PSG.backEnd.model.dto.treasury.BankAccountResponseDTO;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.BankAccountMovement;
import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.treasury.BankAccountMovementType;
import PSG.backEnd.repository.treasury.BankAccountMovementRepository;
import PSG.backEnd.repository.treasury.BankAccountRepository;
import PSG.backEnd.service.port.IBankAccountService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BankAccountService implements IBankAccountService {

    private static final Set<BankAccountMovementType> MANUAL_TYPES = EnumSet.of(
            BankAccountMovementType.INCREMENTO_MANUAL,
            BankAccountMovementType.DECREMENTO_MANUAL,
            BankAccountMovementType.AJUSTE
    );

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountMovementRepository movementRepository;
    private final TreasuryAssembler assembler;
    private final MessageSourceHelper messages;

    @Override
    @Transactional
    public BankAccountResponseDTO create(BankAccountDTO dto) {
        if (bankAccountRepository.existsByAccountNumberAndBankNameIgnoreCaseAndDeletedFalse(
                dto.accountNumber(), dto.bankName())) {
            throw new IllegalArgumentException(messages.getMessage("treasury.bankAccount.duplicate", dto.accountNumber(), dto.bankName()));
        }
        BigDecimal initial = dto.initialBalance() != null ? dto.initialBalance() : BigDecimal.ZERO;
        BankAccount acc = BankAccount.builder()
                .name(dto.name())
                .bankName(dto.bankName())
                .accountType(dto.accountType())
                .accountNumber(dto.accountNumber())
                .cbu(dto.cbu())
                .alias(dto.alias())
                .currency(dto.currency() == null ? Currency.ARS : dto.currency())
                .balance(BigDecimal.ZERO)
                .active(dto.active() == null ? Boolean.TRUE : dto.active())
                .deleted(false)
                .build();
        acc = bankAccountRepository.save(acc);

        if (initial.compareTo(BigDecimal.ZERO) != 0) {
            BankAccountMovementType type = initial.signum() > 0
                    ? BankAccountMovementType.INCREMENTO_MANUAL
                    : BankAccountMovementType.DECREMENTO_MANUAL;
            applyMovement(acc, type, initial.abs(), LocalDate.now(),
                    messages.getMessageOrDefault("treasury.bankAccount.initialBalance", "Saldo inicial"),
                    null, null);
        }
        return assembler.toResponse(acc);
    }

    @Override
    @Transactional
    public BankAccountResponseDTO update(Long id, BankAccountDTO dto) {
        BankAccount acc = getEntityById(id);
        if (dto.name() != null) acc.setName(dto.name());
        if (dto.bankName() != null) acc.setBankName(dto.bankName());
        if (dto.accountType() != null) acc.setAccountType(dto.accountType());
        if (dto.accountNumber() != null) acc.setAccountNumber(dto.accountNumber());
        acc.setCbu(dto.cbu());
        acc.setAlias(dto.alias());
        if (dto.currency() != null) acc.setCurrency(dto.currency());
        if (dto.active() != null) acc.setActive(dto.active());

        // Re-check unique account_number + bank_name
        if (bankAccountRepository.existsByAccountNumberAndBankNameIgnoreCaseAndIdNotAndDeletedFalse(
                acc.getAccountNumber(), acc.getBankName(), id)) {
            throw new IllegalArgumentException(messages.getMessage("treasury.bankAccount.duplicate", acc.getAccountNumber(), acc.getBankName()));
        }
        bankAccountRepository.save(acc);
        return assembler.toResponse(acc);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BankAccount acc = getEntityById(id);
        acc.setDeleted(true);
        acc.setActive(false);
        bankAccountRepository.save(acc);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountResponseDTO getById(Long id) {
        return assembler.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankAccountResponseDTO> findAll(Boolean active, String search, Pageable pageable) {
        return bankAccountRepository.findAllWithFilters(active, normalize(search), pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional
    public BankAccountMovementResponseDTO registerManualMovement(BankAccountMovementDTO dto) {
        BankAccount acc = getEntityById(dto.bankAccountId());
        if (Boolean.FALSE.equals(acc.getActive())) {
            throw new IllegalStateException(messages.getMessage("treasury.bankAccount.inactive", acc.getName()));
        }
        if (!MANUAL_TYPES.contains(dto.type())) {
            throw new IllegalArgumentException(messages.getMessage("treasury.bankAccount.movementType.notManual", dto.type()));
        }
        BigDecimal amount = dto.amount();
        if (amount == null) {
            throw new IllegalArgumentException(messages.getMessage("treasury.movement.amount.required"));
        }
        if (dto.type() == BankAccountMovementType.AJUSTE) {
            if (dto.comment() == null || dto.comment().isBlank()) {
                throw new IllegalArgumentException(messages.getMessage("treasury.movement.comment.requiredForAdjust"));
            }
            BigDecimal delta = amount.subtract(acc.getBalance());
            if (delta.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(messages.getMessage("treasury.movement.adjust.noChange"));
            }
            BankAccountMovement m = applyAdjustment(acc, delta.abs(), delta, dto.movementDate(), dto.comment(), null, null);
            return assembler.toResponse(m);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(messages.getMessage("treasury.movement.amount.positive"));
        }
        BankAccountMovement m = applyMovement(acc, dto.type(), amount, dto.movementDate(), dto.comment(), null, null);
        return assembler.toResponse(m);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankAccountMovementResponseDTO> findMovements(Long bankAccountId, Pageable pageable) {
        getEntityById(bankAccountId);
        return movementRepository.findByBankAccountIdOrderByMovementDateDescIdDesc(bankAccountId, pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccount getEntityById(Long id) {
        return bankAccountRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messages.getMessage("treasury.bankAccount.notFound", id)));
    }

    /** Public so {@code PaymentService} (and similar callers) can register payment-driven movements. */
    public BankAccountMovement applyMovement(BankAccount acc,
                                             BankAccountMovementType type,
                                             BigDecimal positiveAmount,
                                             LocalDate movementDate,
                                             String comment,
                                             PSG.backEnd.model.entity.payment.CheckPayment checkPayment,
                                             PSG.backEnd.model.entity.payment.TransferPayment transferPayment) {
        BigDecimal signed = type.applySign(positiveAmount);
        BigDecimal newBalance = acc.getBalance().add(signed);
        BankAccountMovement m = BankAccountMovement.builder()
                .bankAccount(acc)
                .type(type)
                .amount(positiveAmount)
                .signedAmount(signed)
                .balanceAfter(newBalance)
                .movementDate(movementDate != null ? movementDate : LocalDate.now())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .createdByUserId(currentUserId())
                .checkPayment(checkPayment)
                .transferPayment(transferPayment)
                .build();
        m = movementRepository.save(m);
        acc.setBalance(newBalance);
        bankAccountRepository.save(acc);
        return m;
    }

    private BankAccountMovement applyAdjustment(BankAccount acc,
                                                BigDecimal absoluteDelta,
                                                BigDecimal signedDelta,
                                                LocalDate movementDate,
                                                String comment,
                                                PSG.backEnd.model.entity.payment.CheckPayment checkPayment,
                                                PSG.backEnd.model.entity.payment.TransferPayment transferPayment) {
        BigDecimal newBalance = acc.getBalance().add(signedDelta);
        BankAccountMovement m = BankAccountMovement.builder()
                .bankAccount(acc)
                .type(BankAccountMovementType.AJUSTE)
                .amount(absoluteDelta)
                .signedAmount(signedDelta)
                .balanceAfter(newBalance)
                .movementDate(movementDate != null ? movementDate : LocalDate.now())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .createdByUserId(currentUserId())
                .checkPayment(checkPayment)
                .transferPayment(transferPayment)
                .build();
        m = movementRepository.save(m);
        acc.setBalance(newBalance);
        bankAccountRepository.save(acc);
        return m;
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) return null;
        return u.getId();
    }
}
