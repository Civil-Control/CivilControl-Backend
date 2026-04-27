package PSG.backEnd.service.implementation.treasury;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.treasury.CashBoxDTO;
import PSG.backEnd.model.dto.treasury.CashBoxMovementDTO;
import PSG.backEnd.model.dto.treasury.CashBoxMovementResponseDTO;
import PSG.backEnd.model.dto.treasury.CashBoxResponseDTO;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.entity.treasury.CashBoxMovement;
import PSG.backEnd.model.enums.treasury.CashBoxMovementType;
import PSG.backEnd.repository.treasury.CashBoxMovementRepository;
import PSG.backEnd.repository.treasury.CashBoxRepository;
import PSG.backEnd.service.port.ICashBoxService;
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

@Service
@RequiredArgsConstructor
public class CashBoxService implements ICashBoxService {

    private final CashBoxRepository cashBoxRepository;
    private final CashBoxMovementRepository movementRepository;
    private final TreasuryAssembler assembler;
    private final MessageSourceHelper messages;

    @Override
    @Transactional
    public CashBoxResponseDTO create(CashBoxDTO dto) {
        if (cashBoxRepository.existsByNameIgnoreCaseAndDeletedFalse(dto.name())) {
            throw new IllegalArgumentException(messages.getMessage("treasury.cashBox.duplicateName", dto.name()));
        }
        BigDecimal initial = dto.initialBalance() != null ? dto.initialBalance() : BigDecimal.ZERO;
        CashBox box = CashBox.builder()
                .name(dto.name())
                .description(dto.description())
                .balance(BigDecimal.ZERO)
                .active(dto.active() == null ? Boolean.TRUE : dto.active())
                .deleted(false)
                .build();
        box = cashBoxRepository.save(box);

        if (initial.compareTo(BigDecimal.ZERO) != 0) {
            CashBoxMovementType type = initial.signum() > 0
                    ? CashBoxMovementType.INCREMENTO_MANUAL
                    : CashBoxMovementType.DECREMENTO_MANUAL;
            applyMovement(box, type, initial.abs(), LocalDate.now(),
                    messages.getMessageOrDefault("treasury.cashBox.initialBalance", "Saldo inicial"), null);
        }
        return assembler.toResponse(box);
    }

    @Override
    @Transactional
    public CashBoxResponseDTO update(Long id, CashBoxDTO dto) {
        CashBox box = getEntityById(id);
        if (dto.name() != null && !dto.name().equalsIgnoreCase(box.getName())) {
            if (cashBoxRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(dto.name(), id)) {
                throw new IllegalArgumentException(messages.getMessage("treasury.cashBox.duplicateName", dto.name()));
            }
            box.setName(dto.name());
        }
        if (dto.description() != null) box.setDescription(dto.description());
        if (dto.active() != null) box.setActive(dto.active());
        cashBoxRepository.save(box);
        return assembler.toResponse(box);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CashBox box = getEntityById(id);
        box.setDeleted(true);
        box.setActive(false);
        cashBoxRepository.save(box);
    }

    @Override
    @Transactional(readOnly = true)
    public CashBoxResponseDTO getById(Long id) {
        return assembler.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CashBoxResponseDTO> findAll(Boolean active, String search, Pageable pageable) {
        return cashBoxRepository.findAllWithFilters(active, normalize(search), pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional
    public CashBoxMovementResponseDTO registerManualMovement(CashBoxMovementDTO dto) {
        CashBox box = getEntityById(dto.cashBoxId());
        if (Boolean.FALSE.equals(box.getActive())) {
            throw new IllegalStateException(messages.getMessage("treasury.cashBox.inactive", box.getName()));
        }
        CashBoxMovementType type = dto.type();
        if (type == CashBoxMovementType.PAGO_EMITIDO || type == CashBoxMovementType.TRANSFERENCIA_INTERNA) {
            throw new IllegalArgumentException(messages.getMessage("treasury.cashBox.movementType.notManual", type));
        }
        BigDecimal amount = dto.amount();
        if (amount == null) {
            throw new IllegalArgumentException(messages.getMessage("treasury.movement.amount.required"));
        }
        BigDecimal effectiveAmount;
        if (type == CashBoxMovementType.AJUSTE) {
            if (dto.comment() == null || dto.comment().isBlank()) {
                throw new IllegalArgumentException(messages.getMessage("treasury.movement.comment.requiredForAdjust"));
            }
            // amount is the TARGET balance — record the absolute delta in `amount` and the sign in `signedAmount`.
            BigDecimal delta = amount.subtract(box.getBalance());
            if (delta.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(messages.getMessage("treasury.movement.adjust.noChange"));
            }
            effectiveAmount = delta.abs();
            CashBoxMovement m = applyAdjustment(box, effectiveAmount, delta, dto.movementDate(), dto.comment(), null);
            return assembler.toResponse(m);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(messages.getMessage("treasury.movement.amount.positive"));
        }
        CashBoxMovement m = applyMovement(box, type, amount, dto.movementDate(), dto.comment(), null);
        return assembler.toResponse(m);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CashBoxMovementResponseDTO> findMovements(Long cashBoxId, Pageable pageable) {
        getEntityById(cashBoxId);
        return movementRepository.findByCashBoxIdOrderByMovementDateDescIdDesc(cashBoxId, pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CashBox getEntityById(Long id) {
        return cashBoxRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messages.getMessage("treasury.cashBox.notFound", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tenantHasActiveCashBox() {
        return cashBoxRepository.existsByDeletedFalseAndActiveTrue();
    }

    /**
     * Helper — registers a movement of a non-AJUSTE type and updates the cached balance.
     * {@code cashPaymentReverseId} is currently unused and kept null for future symmetry.
     */
    public CashBoxMovement applyMovement(CashBox box,
                                         CashBoxMovementType type,
                                         BigDecimal positiveAmount,
                                         LocalDate movementDate,
                                         String comment,
                                         PSG.backEnd.model.entity.payment.CashPayment cashPayment) {
        BigDecimal signed = type.applySign(positiveAmount);
        BigDecimal newBalance = box.getBalance().add(signed);

        CashBoxMovement m = CashBoxMovement.builder()
                .cashBox(box)
                .type(type)
                .amount(positiveAmount)
                .signedAmount(signed)
                .balanceAfter(newBalance)
                .movementDate(movementDate != null ? movementDate : LocalDate.now())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .createdByUserId(currentUserId())
                .cashPayment(cashPayment)
                .build();
        m = movementRepository.save(m);
        box.setBalance(newBalance);
        cashBoxRepository.save(box);
        return m;
    }

    private CashBoxMovement applyAdjustment(CashBox box,
                                            BigDecimal absoluteDelta,
                                            BigDecimal signedDelta,
                                            LocalDate movementDate,
                                            String comment,
                                            PSG.backEnd.model.entity.payment.CashPayment cashPayment) {
        BigDecimal newBalance = box.getBalance().add(signedDelta);
        CashBoxMovement m = CashBoxMovement.builder()
                .cashBox(box)
                .type(CashBoxMovementType.AJUSTE)
                .amount(absoluteDelta)
                .signedAmount(signedDelta)
                .balanceAfter(newBalance)
                .movementDate(movementDate != null ? movementDate : LocalDate.now())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .createdByUserId(currentUserId())
                .cashPayment(cashPayment)
                .build();
        m = movementRepository.save(m);
        box.setBalance(newBalance);
        cashBoxRepository.save(box);
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
