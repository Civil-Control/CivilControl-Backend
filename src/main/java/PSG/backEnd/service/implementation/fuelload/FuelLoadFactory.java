package PSG.backEnd.service.implementation.fuelload;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.mapper.FuelLoadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory responsible for creating and configuring FuelLoad entities.
 */
@Component
@RequiredArgsConstructor
public class FuelLoadFactory {

    private final FuelLoadMapper fuelLoadMapper;
    private final FuelLoadValidator validator;
    private final FuelLoadCalculator calculator;

    /**
     * Creates a new fully configured and validated FuelLoad.
     */
    public FuelLoad createFuelLoad(FuelLoadDTO fuelLoadDTO) {
        // Validate input data
        validator.validateForCreation(fuelLoadDTO);

        // Create base entity
        FuelLoad fuelLoad = fuelLoadMapper.toEntity(fuelLoadDTO);

        // Calculate prices and totals
        calculator.calculatePriceAndTotal(fuelLoad, fuelLoadDTO);

        return fuelLoad;
    }

    /**
     * Updates an existing FuelLoad with new data.
     */
    public void updateFuelLoad(FuelLoad existingFuelLoad, FuelLoadDTO fuelLoadDTO) {
        // Validate changes
        validator.validateForUpdate(fuelLoadDTO, existingFuelLoad);

        // Apply basic changes
        fuelLoadMapper.partialUpdate(fuelLoadDTO, existingFuelLoad);

        // Recalculate if necessary
        calculator.recalculateIfNeeded(existingFuelLoad, fuelLoadDTO);
    }
}
