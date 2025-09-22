package PSG.backEnd.service.implementation.fuelload;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadErrorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Class responsible for batch processing of FuelLoads.
 */
@Component
@RequiredArgsConstructor
public class FuelLoadBatchProcessor {

    /**
     * Processes a list of FuelLoads using a processing function.
     * Applies the Strategy pattern allowing different types of processing.
     */
    public FuelLoadBatchResponseDTO processBatch(
            FuelLoadBatchDTO fuelLoadBatchDTO,
            Function<FuelLoadDTO, FuelLoadResponseDTO> processor) {

        List<FuelLoadResponseDTO> successfulLoads = new ArrayList<>();
        List<FuelLoadErrorDTO> failedLoads = new ArrayList<>();

        for (int i = 0; i < fuelLoadBatchDTO.fuelLoads().size(); i++) {
            FuelLoadDTO fuelLoadDTO = fuelLoadBatchDTO.fuelLoads().get(i);

            try {
                FuelLoadResponseDTO result = processor.apply(fuelLoadDTO);
                successfulLoads.add(result);
            } catch (Exception ex) {
                FuelLoadErrorDTO error = new FuelLoadErrorDTO(i, fuelLoadDTO, ex.getMessage());
                failedLoads.add(error);
            }
        }

        return new FuelLoadBatchResponseDTO(
                successfulLoads,
                failedLoads,
                fuelLoadBatchDTO.fuelLoads().size(),
                successfulLoads.size(),
                failedLoads.size()
        );
    }
}
