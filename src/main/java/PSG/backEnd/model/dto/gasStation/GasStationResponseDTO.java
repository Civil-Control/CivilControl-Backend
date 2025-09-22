package PSG.backEnd.model.dto.gasStation;

import java.util.List;

public record GasStationResponseDTO(
        Long id,
        Long supplierId,
        String supplierName,
        List<String> fuelTypes, // Se calculará desde los precios
        List<GasStationPriceResponseDTO> prices,
        boolean deleted
) {}
