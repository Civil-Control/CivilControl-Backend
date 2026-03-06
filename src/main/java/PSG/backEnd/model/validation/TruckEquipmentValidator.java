package PSG.backEnd.model.validation;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.entity.vehicle.VehicleType;
import PSG.backEnd.repository.VehicleTypeRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class TruckEquipmentValidator implements ConstraintValidator<ValidTruckEquipment, VehicleDTO> {

    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;

    @Override
    public boolean isValid(VehicleDTO vehicleDTO, ConstraintValidatorContext context) {
        if (vehicleDTO == null) {
            return true;
        }

        Long vehicleTypeId = vehicleDTO.vehicleTypeId();
        String truckEquipment = vehicleDTO.truckEquipment();

        // Si no se especifica tipo de vehículo, no se puede validar; se permite pasar
        if (vehicleTypeId == null) {
            return true;
        }

        Optional<VehicleType> vehicleTypeOpt = vehicleTypeRepository.findById(vehicleTypeId);

        // Si el tipo no existe, se deja pasar (el servicio lanzará el error correspondiente)
        if (vehicleTypeOpt.isEmpty()) {
            return true;
        }

        boolean requiresTruckEquipment = vehicleTypeOpt.get().isRequiresTruckEquipment();

        if (requiresTruckEquipment) {
            if (truckEquipment == null || truckEquipment.trim().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "{vehicle.truckEquipment.required}"
                ).addPropertyNode("truckEquipment").addConstraintViolation();
                return false;
            }
        } else {
            if (truckEquipment != null && !truckEquipment.trim().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "{vehicle.truckEquipment.notAllowed}"
                ).addPropertyNode("truckEquipment").addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
