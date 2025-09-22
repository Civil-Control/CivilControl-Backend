package PSG.backEnd.service.implementation.fuelload;

import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.repository.GasStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Class responsible for price and total calculations for FuelLoads.
 */
@Component
@RequiredArgsConstructor
public class FuelLoadCalculator {

    private final GasStationRepository gasStationRepository;

    /**
     * Calculates the price per liter and total for a new FuelLoad.
     */
    public void calculatePriceAndTotal(FuelLoad fuelLoad, FuelLoadDTO fuelLoadDTO) {
        BigDecimal pricePerLiter = getPricePerLiter(fuelLoadDTO.gasStationId(), fuelLoadDTO.fuelType());
        fuelLoad.setPricePerLiter(pricePerLiter);
        fuelLoad.setTotalAmount(calculateTotal(pricePerLiter, BigDecimal.valueOf(fuelLoadDTO.liters())));
    }

    /**
     * Recalculates the price per liter and total if necessary during an update.
     */
    public boolean recalculateIfNeeded(FuelLoad fuelLoad, FuelLoadDTO fuelLoadDTO) {
        boolean needsRecalculation = false;
        BigDecimal newPricePerLiter = fuelLoad.getPricePerLiter();

        // If gas station or fuel type changed, get new price
        if (fuelLoadDTO.gasStationId() != null || fuelLoadDTO.fuelType() != null) {
            Long gasStationId = fuelLoadDTO.gasStationId() != null ?
                fuelLoadDTO.gasStationId() : fuelLoad.getGasStation().getId();
            FuelType fuelType = fuelLoadDTO.fuelType() != null ?
                fuelLoadDTO.fuelType() : fuelLoad.getFuelType();

            newPricePerLiter = getPricePerLiter(gasStationId, fuelType);
            fuelLoad.setPricePerLiter(newPricePerLiter);
            needsRecalculation = true;
        }

        // If liters changed, we also need to recalculate
        if (fuelLoadDTO.liters() != null) {
            needsRecalculation = true;
        }

        // Recalculate total if necessary
        if (needsRecalculation) {
            BigDecimal newLiters = fuelLoad.getLiters();
            fuelLoad.setTotalAmount(calculateTotal(newPricePerLiter, newLiters));
        }

        return needsRecalculation;
    }

    /**
     * Gets the price per liter for a fuel type at a specific gas station.
     */
    private BigDecimal getPricePerLiter(Long gasStationId, FuelType fuelType) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(gasStationId)
                .orElseThrow(() -> new GasStationNotFoundException(gasStationId));

        return gasStation.getPrices().stream()
                .filter(price -> price.getFuelType() == fuelType)
                .map(PSG.backEnd.model.entity.gasStation.GasStationPrice::getPrice)
                .findFirst()
                .orElseThrow(() -> new PSG.backEnd.exception.gasStation.FuelTypeNotAvailableException(
                        fuelType, gasStationId));
    }

    /**
     * Calculates the total by multiplying price per liter by liters.
     */
    private BigDecimal calculateTotal(BigDecimal pricePerLiter, BigDecimal liters) {
        return pricePerLiter.multiply(liters);
    }
}
