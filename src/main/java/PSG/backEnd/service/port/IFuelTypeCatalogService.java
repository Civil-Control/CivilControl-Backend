package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.gasStation.FuelTypeOptionDTO;

import java.util.List;

public interface IFuelTypeCatalogService {

    /**
     * Built-in FuelType constants (in declaration order) followed by the tenant's custom types (by label).
     * Deleted custom types are excluded unless {@code includeDeleted} is true.
     */
    List<FuelTypeOptionDTO> listAll(boolean includeDeleted);

    /** Resolves a stored fuel_type key (built-in or custom, deleted or not) to its display label. Never throws — falls back to the raw key. */
    String resolveLabel(String key);

    /** Creates a new tenant-scoped custom fuel type from a display label. */
    FuelTypeOptionDTO createCustom(String label);

    /** Soft-deletes a tenant-scoped custom fuel type so it no longer appears in selectable lists. */
    void deleteCustom(String key);
}
