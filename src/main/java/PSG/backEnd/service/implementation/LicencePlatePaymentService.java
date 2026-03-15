package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.vehicle.LicencePlatePaymentAlreadyExistsException;
import PSG.backEnd.exception.vehicle.LicencePlatePaymentNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentBatchDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentFilterDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import PSG.backEnd.model.entity.vehicle.LicencePlatePayment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.mapper.LicencePlatePaymentMapper;
import PSG.backEnd.repository.LicencePlatePaymentRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.ILicencePlatePaymentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicencePlatePaymentService implements ILicencePlatePaymentService {

    private final LicencePlatePaymentRepository licencePlatePaymentRepository;
    private final LicencePlatePaymentMapper licencePlatePaymentMapper;
    private final VehicleRepository vehicleRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public LicencePlatePaymentResponseDTO createLicencePlatePayment(LicencePlatePaymentDTO licencePlatePaymentDTO) {
        validateVehicleExists(licencePlatePaymentDTO.vehicleId());
        validateUniquePayment(licencePlatePaymentDTO);

        LicencePlatePayment licencePlatePayment = licencePlatePaymentMapper.toEntity(licencePlatePaymentDTO);
        LicencePlatePayment savedPayment = licencePlatePaymentRepository.save(licencePlatePayment);

        Vehicle vehicle = vehicleRepository.findById(licencePlatePaymentDTO.vehicleId())
                .orElseThrow(() -> new VehicleNotValidException(licencePlatePaymentDTO.vehicleId()));

        return licencePlatePaymentMapper.toResponseDto(savedPayment, vehicle);
    }

    @Override
    @Transactional
    public List<LicencePlatePaymentResponseDTO> createBatchLicencePlatePayments(LicencePlatePaymentBatchDTO batchDTO) {
        return batchDTO.payments().stream()
                .map(this::createLicencePlatePayment)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LicencePlatePaymentResponseDTO> getAllLicencePlatePayments(LicencePlatePaymentFilterDTO filterDTO, Pageable pageable) {
        Page<LicencePlatePayment> payments = licencePlatePaymentRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.minAmount(),
                filterDTO.maxAmount(),
                filterDTO.year(),
                filterDTO.period(),
                filterDTO.jurisdictionType(),
                pageable
        );

        return payments.map(payment -> {
            Vehicle vehicle = vehicleRepository.findById(payment.getVehicleId())
                    .orElse(null);
            return licencePlatePaymentMapper.toResponseDto(payment, vehicle);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LicencePlatePaymentResponseDTO getLicencePlatePaymentById(Long id) {
        LicencePlatePayment payment = licencePlatePaymentRepository.findById(id)
                .orElseThrow(() -> new LicencePlatePaymentNotFoundException(id));

        Vehicle vehicle = vehicleRepository.findById(payment.getVehicleId())
                .orElseThrow(() -> new VehicleNotValidException(payment.getVehicleId()));

        return licencePlatePaymentMapper.toResponseDto(payment, vehicle);
    }

    @Override
    @Transactional
    public LicencePlatePaymentResponseDTO updateLicencePlatePayment(Long id, LicencePlatePaymentDTO licencePlatePaymentDTO) {
        LicencePlatePayment existingPayment = licencePlatePaymentRepository.findById(id)
                .orElseThrow(() -> new LicencePlatePaymentNotFoundException(id));

        if (licencePlatePaymentDTO.vehicleId() != null) {
            validateVehicleExists(licencePlatePaymentDTO.vehicleId());

            // Validar que no exista otro pago para el mismo vehículo, año y período
            if (!existingPayment.getVehicleId().equals(licencePlatePaymentDTO.vehicleId()) ||
                !existingPayment.getYear().equals(licencePlatePaymentDTO.year()) ||
                !existingPayment.getPeriod().equals(licencePlatePaymentDTO.period())) {
                validateUniquePayment(licencePlatePaymentDTO);
            }
        }

        licencePlatePaymentMapper.partialUpdate(licencePlatePaymentDTO, existingPayment);
        LicencePlatePayment updatedPayment = licencePlatePaymentRepository.save(existingPayment);

        Vehicle vehicle = vehicleRepository.findById(updatedPayment.getVehicleId())
                .orElseThrow(() -> new VehicleNotValidException(updatedPayment.getVehicleId()));

        return licencePlatePaymentMapper.toResponseDto(updatedPayment, vehicle);
    }

    @Override
    @Transactional
    public void deleteLicencePlatePayment(Long id) {
        LicencePlatePayment payment = licencePlatePaymentRepository.findById(id)
                .orElseThrow(() -> new LicencePlatePaymentNotFoundException(id));

        licencePlatePaymentRepository.delete(payment);
    }

    private void validateVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotValidException(vehicleId);
        }
    }

    private void validateUniquePayment(LicencePlatePaymentDTO licencePlatePaymentDTO) {
        Optional<LicencePlatePayment> existingPayment = licencePlatePaymentRepository
                .findByVehicleIdAndYearAndPeriod(
                        licencePlatePaymentDTO.vehicleId(),
                        licencePlatePaymentDTO.year(),
                        licencePlatePaymentDTO.period()
                );

        if (existingPayment.isPresent()) {
            throw new LicencePlatePaymentAlreadyExistsException(
                    messageSourceHelper.getMessage("licencePlatePayment.duplicate",
                            licencePlatePaymentDTO.vehicleId(),
                            licencePlatePaymentDTO.year(),
                            licencePlatePaymentDTO.period())
            );
        }
    }
}

