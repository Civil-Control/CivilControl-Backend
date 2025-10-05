package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import PSG.backEnd.model.entity.vehicle.LicencePlatePayment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LicencePlatePaymentMapper {

    @Mapping(target = "id", ignore = true)
    LicencePlatePayment toEntity(LicencePlatePaymentDTO licencePlatePaymentDTO);

    @Mapping(target = "id", source = "licencePlatePayment.id")
    @Mapping(target = "date", source = "licencePlatePayment.date")
    @Mapping(target = "vehicleId", source = "licencePlatePayment.vehicleId")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "amount", source = "licencePlatePayment.amount")
    @Mapping(target = "year", source = "licencePlatePayment.year")
    @Mapping(target = "period", source = "licencePlatePayment.period")
    @Mapping(target = "jurisdictionType", source = "licencePlatePayment.jurisdictionType")
    LicencePlatePaymentResponseDTO toResponseDto(LicencePlatePayment licencePlatePayment, Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void partialUpdate(LicencePlatePaymentDTO updateDTO, @MappingTarget LicencePlatePayment licencePlatePayment);
}
