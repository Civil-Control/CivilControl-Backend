package PSG.backEnd.model.validation;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.enums.vehicle.VehicleType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TruckEquipmentValidator implements ConstraintValidator<ValidTruckEquipment, VehicleDTO> {

    @Override
    public boolean isValid(VehicleDTO vehicleDTO, ConstraintValidatorContext context) {
        if (vehicleDTO == null) {
            return true;
        }

        VehicleType vehicleType = vehicleDTO.vehicleType();
        String truckEquipment = vehicleDTO.truckEquipment();

        if (vehicleType == VehicleType.CAMION) {
            if (truckEquipment == null || truckEquipment.trim().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Truck equipment is required for vehicles of type CAMION."
                ).addPropertyNode("truckEquipment").addConstraintViolation();
                return false;
            }
        } else {
            if (truckEquipment != null && !truckEquipment.trim().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Truck equipment can only be specified for vehicles of type CAMION."
                ).addPropertyNode("truckEquipment").addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}

