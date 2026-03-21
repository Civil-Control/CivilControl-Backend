package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.tenant.TenantDTO;
import PSG.backEnd.model.dto.tenant.TenantFilterDTO;
import PSG.backEnd.model.dto.tenant.TenantResponseDTO;
import PSG.backEnd.model.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITenantService {
    TenantResponseDTO createTenant(TenantDTO tenantDTO);
    TenantResponseDTO getTenantById(Long id);
    TenantResponseDTO getCurrentTenant();
    TenantResponseDTO updateTenant(Long id, TenantDTO tenantDTO);
    void deleteTenant(Long id);
    Page<TenantResponseDTO> getAllTenants(TenantFilterDTO filterDTO, Pageable pageable);
    Tenant getEntityById(Long id);
    boolean existsById(Long id);
}
