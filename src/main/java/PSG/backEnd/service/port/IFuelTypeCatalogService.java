package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.gasStation.FuelTypeOptionDTO;

import java.util.List;

public interface IFuelTypeCatalogService {

    /** Built-in FuelType constants (in declaration order) followed by the tenant's active custom types (by label). */
    List<FuelTypeOptionDTO> listAll();

    /** Resolves a stored fuel_type key (built-in or custom) to its display label. Never throws — falls back to the raw key. */
    String resolveLabel(String key);

    /** Creates a new tenant-scoped custom fuel type from a display label. */
    FuelTypeOptionDTO createCustom(String label);
}
