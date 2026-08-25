package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.CustomFuelTypeAlreadyExistsException;
import PSG.backEnd.model.dto.gasStation.FuelTypeOptionDTO;
import PSG.backEnd.model.entity.gasStation.CustomFuelType;
import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.repository.CustomFuelTypeRepository;
import PSG.backEnd.service.port.IFuelTypeCatalogService;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves and manages fuel types across the built-in {@link FuelType} enum and
 * tenant-defined custom types (see {@link CustomFuelType}). GasStationPrice.fuelType and
 * FuelLoad.fuelType are plain strings that can hold either a built-in enum constant name
 * or a custom type's key — this service is the single place that reconciles the two.
 */
@Service
@RequiredArgsConstructor
public class FuelTypeCatalogService implements IFuelTypeCatalogService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");
    private static final int MAX_KEY_LENGTH = 50;

    private final CustomFuelTypeRepository customFuelTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FuelTypeOptionDTO> listAll() {
        List<FuelTypeOptionDTO> options = new ArrayList<>();
        for (FuelType ft : FuelType.values()) {
            options.add(new FuelTypeOptionDTO(ft.name(), ft.getDisplayName(), false));
        }
        customFuelTypeRepository.findAllByTenantIdAndDeletedFalseOrderByLabel(TenantContext.getCurrentTenant())
                .forEach(c -> options.add(new FuelTypeOptionDTO(c.getKey(), c.getLabel(), true)));
        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveLabel(String key) {
        if (key == null) return null;
        FuelType builtIn = builtInOrNull(key);
        if (builtIn != null) return builtIn.getDisplayName();
        return customFuelTypeRepository.findByTenantIdAndKeyAndDeletedFalse(TenantContext.getCurrentTenant(), key)
                .map(CustomFuelType::getLabel)
                .orElse(key);
    }

    @Override
    @Transactional
    public FuelTypeOptionDTO createCustom(String label) {
        Long tenantId = TenantContext.getCurrentTenant();
        String key = normalizeToKey(label);

        if (builtInOrNull(key) != null || customFuelTypeRepository.existsByTenantIdAndKeyAndDeletedFalse(tenantId, key)) {
            throw new CustomFuelTypeAlreadyExistsException(label);
        }

        CustomFuelType saved = customFuelTypeRepository.save(CustomFuelType.builder()
                .key(key)
                .label(label.trim())
                .deleted(false)
                .build());
        return new FuelTypeOptionDTO(saved.getKey(), saved.getLabel(), true);
    }

    private FuelType builtInOrNull(String key) {
        try {
            return FuelType.valueOf(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** "aceite para motos" -> "ACEITE_PARA_MOTOS" (accents stripped, non-alphanumerics collapsed). */
    private String normalizeToKey(String label) {
        String noAccents = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String key = NON_ALNUM.matcher(noAccents.toUpperCase()).replaceAll("_");
        key = key.replaceAll("^_+|_+$", "");
        return key.length() > MAX_KEY_LENGTH ? key.substring(0, MAX_KEY_LENGTH) : key;
    }
}
