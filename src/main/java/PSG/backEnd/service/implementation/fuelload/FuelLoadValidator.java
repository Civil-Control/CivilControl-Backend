package PSG.backEnd.service.implementation.fuelload;

import PSG.backEnd.exception.gasStation.DuplicateTicketException;
import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotFoundException;
import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.repository.FuelLoadRepository;
import PSG.backEnd.repository.GasStationRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Class responsible for validating FuelLoads.
 */
@Component
@RequiredArgsConstructor
public class FuelLoadValidator {

    private final FuelLoadRepository fuelLoadRepository;
    private final VehicleRepository vehicleRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final GasStationRepository gasStationRepository;

    /**
     * Validates a FuelLoad for creation.
     */
    public void validateForCreation(FuelLoadDTO fuelLoadDTO) {
        validateTicketUniqueness(fuelLoadDTO.ticketNumber(), fuelLoadDTO.branchCode());
        validateVehicleExistsIfPresent(fuelLoadDTO.vehicleId());
        validateGasStationExists(fuelLoadDTO.gasStationId());
        validateProjectAreaExistsIfPresent(fuelLoadDTO.projectAreaId());
    }

    /**
     * Validates a FuelLoad for update.
     */
    public void validateForUpdate(FuelLoadDTO fuelLoadDTO, FuelLoad existingFuelLoad) {
        validateTicketUniquenessForUpdate(fuelLoadDTO, existingFuelLoad);
        validateVehicleExistsIfPresent(fuelLoadDTO.vehicleId());
        validateGasStationExistsIfPresent(fuelLoadDTO.gasStationId());
        validateProjectAreaExistsIfPresent(fuelLoadDTO.projectAreaId());
    }

    private void validateTicketUniqueness(String ticketNumber, String branchCode) {
        if (fuelLoadRepository.existsByTicketNumberAndBranchCode(ticketNumber, branchCode)) {
            throw new DuplicateTicketException(ticketNumber, branchCode);
        }
    }

    private void validateTicketUniquenessForUpdate(FuelLoadDTO fuelLoadDTO, FuelLoad existingFuelLoad) {
        if (isTicketChanged(fuelLoadDTO, existingFuelLoad)) {
            String newTicketNumber = fuelLoadDTO.ticketNumber() != null ?
                fuelLoadDTO.ticketNumber() : existingFuelLoad.getTicketNumber();
            String newBranchCode = fuelLoadDTO.branchCode() != null ?
                fuelLoadDTO.branchCode() : existingFuelLoad.getBranchCode();

            if (fuelLoadRepository.existsByTicketNumberAndBranchCode(newTicketNumber, newBranchCode)) {
                throw new DuplicateTicketException(newTicketNumber, newBranchCode);
            }
        }
    }

    private boolean isTicketChanged(FuelLoadDTO fuelLoadDTO, FuelLoad existingFuelLoad) {
        return (fuelLoadDTO.ticketNumber() != null &&
                !fuelLoadDTO.ticketNumber().equals(existingFuelLoad.getTicketNumber())) ||
               (fuelLoadDTO.branchCode() != null &&
                !fuelLoadDTO.branchCode().equals(existingFuelLoad.getBranchCode()));
    }

    private void validateVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsByIdAndDeletedFalse(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
    }

    private void validateVehicleExistsIfPresent(Long vehicleId) {
        if (vehicleId != null && !vehicleRepository.existsByIdAndDeletedFalse(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
    }

    private void validateGasStationExists(Long gasStationId) {
        if (!gasStationRepository.existsByIdAndDeletedFalse(gasStationId)) {
            throw new GasStationNotFoundException(gasStationId);
        }
    }

    private void validateGasStationExistsIfPresent(Long gasStationId) {
        if (gasStationId != null && !gasStationRepository.existsByIdAndDeletedFalse(gasStationId)) {
            throw new GasStationNotFoundException(gasStationId);
        }
    }

    private void validateProjectAreaExistsIfPresent(Long projectAreaId) {
        if (projectAreaId != null && !projectAreaRepository.existsByIdAndDeletedFalse(projectAreaId)) {
            throw new ProjectAreaNotFoundException(projectAreaId);
        }
    }
}
