package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.tenant.TenantAlreadyExistsException;
import PSG.backEnd.exception.tenant.TenantDataConflictException;
import PSG.backEnd.exception.tenant.TenantNotFoundException;
import PSG.backEnd.exception.tenant.TenantNotValidException;
import PSG.backEnd.model.dto.tenant.TenantDTO;
import PSG.backEnd.model.dto.tenant.TenantFilterDTO;
import PSG.backEnd.model.dto.tenant.TenantResponseDTO;
import PSG.backEnd.model.entity.Tenant;
import PSG.backEnd.model.mapper.TenantMapper;
import PSG.backEnd.repository.TenantRepository;
import PSG.backEnd.service.port.ITenantService;
import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantService implements ITenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final MessageSourceHelper messageSourceHelper;
    private final TenantProvisioningService tenantProvisioningService;

    @Override
    @Transactional
    public TenantResponseDTO createTenant(TenantDTO tenantDTO) {
        validateNewTenant(tenantDTO);
        Optional<Tenant> deletedTenant = findDeletedTenant(tenantDTO);
        if (deletedTenant.isPresent()) {
            return reactivateTenant(deletedTenant.get(), tenantDTO);
        }
        return createNewTenant(tenantDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponseDTO> getAllTenants(TenantFilterDTO filterDTO, Pageable pageable) {
        return tenantRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.cuit(),
                filterDTO.active(),
                filterDTO.search(),
                pageable
        ).map(tenantMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponseDTO getCurrentTenant() {
        Long tenantId = TenantContext.getCurrentTenant();
        return getTenantById(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponseDTO getTenantById(Long id) {
        return tenantRepository.findByIdAndDeletedFalse(id)
                .map(tenantMapper::toResponseDto)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    @Override
    @Transactional
    public TenantResponseDTO updateTenant(Long id, TenantDTO tenantDTO) {
        Long currentTenantId = TenantContext.getCurrentTenant();
        if (!id.equals(currentTenantId)) {
            throw new TenantNotValidException(messageSourceHelper.getMessage("tenant.update.forbidden"));
        }
        Tenant existingTenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        validateTenantUpdate(id, tenantDTO);
        try {
            tenantMapper.partialUpdate(tenantDTO, existingTenant);
            Tenant updatedTenant = tenantRepository.save(existingTenant);
            return tenantMapper.toResponseDto(updatedTenant);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, tenantDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        tenant.setDeleted(true);
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return tenantRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getEntityById(Long id) {
        return tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    private void validateNewTenant(TenantDTO tenantDTO) {
        if (tenantDTO.name() == null || tenantDTO.name().trim().isEmpty()) {
            throw new TenantNotValidException(messageSourceHelper.getMessage("tenant.name.empty"));
        }
        if (tenantDTO.cuit() == null || tenantDTO.cuit().trim().isEmpty()) {
            throw new TenantNotValidException(messageSourceHelper.getMessage("tenant.cuit.empty"));
        }
        if (tenantRepository.existsByCuitAndDeletedFalse(tenantDTO.cuit())) {
            throw new TenantAlreadyExistsException(
                    messageSourceHelper.getMessage("tenant.cuit.alreadyExists", tenantDTO.cuit()));
        }
    }

    private void validateTenantUpdate(Long id, TenantDTO tenantDTO) {
        if (tenantDTO.cuit() != null && !tenantDTO.cuit().trim().isEmpty()) {
            tenantRepository.findByCuit(tenantDTO.cuit())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            if (existing.getDeleted()) {
                                throw new TenantAlreadyExistsException(
                                        messageSourceHelper.getMessage("tenant.cuit.deletedTenant", tenantDTO.cuit(), existing.getId()));
                            } else {
                                throw new TenantAlreadyExistsException(
                                        messageSourceHelper.getMessage("tenant.cuit.alreadyExistsAnother", tenantDTO.cuit()));
                            }
                        }
                    });
        }
        if (tenantDTO.name() != null && tenantDTO.name().trim().isEmpty()) {
            throw new TenantNotValidException(messageSourceHelper.getMessage("tenant.name.empty"));
        }
    }

    private Optional<Tenant> findDeletedTenant(TenantDTO tenantDTO) {
        return tenantRepository.findByCuitAndDeletedTrue(tenantDTO.cuit());
    }

    private TenantResponseDTO reactivateTenant(Tenant tenant, TenantDTO tenantDTO) {
        tenantMapper.partialUpdate(tenantDTO, tenant);
        tenant.setDeleted(false);
        tenant.setActive(tenantDTO.active() != null ? tenantDTO.active() : true);
        return tenantMapper.toResponseDto(tenantRepository.save(tenant));
    }

    private TenantResponseDTO createNewTenant(TenantDTO tenantDTO) {
        Tenant tenant = tenantMapper.toEntity(tenantDTO);
        tenant.setDeleted(false);
        Tenant savedTenant = tenantRepository.save(tenant);

        // Provision the new tenant with default system roles and owner user
        tenantProvisioningService.provisionNewTenant(
                savedTenant.getId(),
                "admin@" + savedTenant.getName().toLowerCase().replaceAll("\\s+", "") + ".com",
                "admin",
                "Admin123",
                "System",
                "Administrator"
        );

        return tenantMapper.toResponseDto(savedTenant);
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, TenantDTO tenantDTO) {
        String errorMessage = e.getMessage().toLowerCase();
        if (errorMessage.contains("cuit")) {
            throw new TenantDataConflictException(
                "Cannot update tenant: CUIT '" + tenantDTO.cuit() + "' is already in use by another tenant", e);
        }
        throw new TenantDataConflictException(
            "Cannot update tenant due to a data integrity violation. Please verify that the CUIT is not already in use", e);
    }
}
