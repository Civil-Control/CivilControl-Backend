package PSG.backEnd.service.implementation.treasury;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.treasury.CheckbookDTO;
import PSG.backEnd.model.dto.treasury.CheckbookResponseDTO;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.Checkbook;
import PSG.backEnd.repository.treasury.CheckbookRepository;
import PSG.backEnd.service.port.IBankAccountService;
import PSG.backEnd.service.port.ICheckbookService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckbookService implements ICheckbookService {

    private final CheckbookRepository checkbookRepository;
    private final IBankAccountService bankAccountService;
    private final TreasuryAssembler assembler;
    private final MessageSourceHelper messages;

    @Override
    @Transactional
    public CheckbookResponseDTO create(CheckbookDTO dto) {
        validateRange(dto.rangeFrom(), dto.rangeTo());
        BankAccount acc = bankAccountService.getEntityById(dto.bankAccountId());
        if (checkbookRepository.existsByCheckbookNumberIgnoreCaseAndBankAccountIdAndDeletedFalse(
                dto.checkbookNumber(), acc.getId())) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.duplicateNumber",
                    dto.checkbookNumber(), acc.getName()));
        }
        validateNoOverlap(acc.getId(), dto.rangeFrom(), dto.rangeTo(), null);

        Checkbook cb = Checkbook.builder()
                .name(dto.name())
                .checkbookNumber(dto.checkbookNumber())
                .bankAccount(acc)
                .checkType(dto.checkType())
                .rangeFrom(dto.rangeFrom())
                .rangeTo(dto.rangeTo())
                .active(true)
                .deleted(false)
                .build();
        cb = checkbookRepository.save(cb);
        return assembler.toResponse(cb);
    }

    @Override
    @Transactional
    public CheckbookResponseDTO update(Long id, CheckbookDTO dto) {
        Checkbook cb = getEntityById(id);
        if (dto.name() != null) cb.setName(dto.name());
        if (dto.checkType() != null) cb.setCheckType(dto.checkType());

        // Range and bankAccount changes are restricted: only allowed when no checks have been used yet.
        boolean hasUsage = !assembler.numericUsedNumbers(cb.getId(), cb.getRangeFrom(), cb.getRangeTo()).isEmpty();
        boolean rangeChanging = (dto.rangeFrom() != null && !dto.rangeFrom().equals(cb.getRangeFrom()))
                || (dto.rangeTo() != null && !dto.rangeTo().equals(cb.getRangeTo()));
        boolean accountChanging = dto.bankAccountId() != null
                && !dto.bankAccountId().equals(cb.getBankAccount().getId());

        if ((rangeChanging || accountChanging) && hasUsage) {
            throw new IllegalStateException(messages.getMessage("treasury.checkbook.rangeLocked"));
        }
        if (accountChanging) {
            cb.setBankAccount(bankAccountService.getEntityById(dto.bankAccountId()));
        }
        if (rangeChanging) {
            Long newFrom = dto.rangeFrom() != null ? dto.rangeFrom() : cb.getRangeFrom();
            Long newTo = dto.rangeTo() != null ? dto.rangeTo() : cb.getRangeTo();
            validateRange(newFrom, newTo);
            validateNoOverlap(cb.getBankAccount().getId(), newFrom, newTo, cb.getId());
            cb.setRangeFrom(newFrom);
            cb.setRangeTo(newTo);
        }
        if (dto.checkbookNumber() != null && !dto.checkbookNumber().equalsIgnoreCase(cb.getCheckbookNumber())) {
            if (checkbookRepository.existsByCheckbookNumberIgnoreCaseAndBankAccountIdAndIdNotAndDeletedFalse(
                    dto.checkbookNumber(), cb.getBankAccount().getId(), id)) {
                throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.duplicateNumber",
                        dto.checkbookNumber(), cb.getBankAccount().getName()));
            }
            cb.setCheckbookNumber(dto.checkbookNumber());
        }
        checkbookRepository.save(cb);
        return assembler.toResponse(cb);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Checkbook cb = getEntityById(id);
        cb.setDeleted(true);
        cb.setActive(false);
        checkbookRepository.save(cb);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckbookResponseDTO getById(Long id) {
        return assembler.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckbookResponseDTO> findAll(Long bankAccountId, Boolean active, String search, Pageable pageable) {
        return checkbookRepository.findAllWithFilters(bankAccountId, active,
                        (search == null || search.isBlank()) ? null : search.trim(), pageable)
                .map(assembler::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Checkbook getEntityById(Long id) {
        return checkbookRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messages.getMessage("treasury.checkbook.notFound", id)));
    }

    private void validateRange(Long from, Long to) {
        if (from == null || to == null || from <= 0 || to <= 0 || from > to) {
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.invalidRange"));
        }
    }

    private void validateNoOverlap(Long bankAccountId, Long from, Long to, Long excludeId) {
        List<Checkbook> overlap = checkbookRepository.findOverlappingRanges(bankAccountId, from, to, excludeId);
        if (!overlap.isEmpty()) {
            String numbers = overlap.stream().map(Checkbook::getCheckbookNumber).reduce((a, b) -> a + ", " + b).orElse("");
            throw new IllegalArgumentException(messages.getMessage("treasury.checkbook.rangeOverlap", numbers));
        }
    }
}
