package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.dto.vehicle.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.entity.vehicle.RepairItem;
import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.enums.vehicle.RepairItemType;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RepairMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    @Mapping(target = "repairOrder", ignore = true)
    @Mapping(target = "items", ignore = true)
    Repair toEntity(RepairDTO repairDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "vehicleIdToEntity")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToEntity")
    @Mapping(target = "repairOrder", ignore = true)
    @Mapping(target = "items", ignore = true)
    void partialUpdate(RepairDTO updateDTO, @MappingTarget Repair repair);

    default RepairResponseDTO toResponseDto(Repair repair) {
        if (repair == null) return null;

        List<RepairItemResponseDTO> itemDtos = new ArrayList<>();
        BigDecimal materialSubtotal = BigDecimal.ZERO;
        BigDecimal laborSubtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;

        if (repair.getItems() != null) {
            List<RepairItem> sorted = repair.getItems().stream()
                    .sorted(Comparator.comparingInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0))
                    .toList();

            for (RepairItem item : sorted) {
                BigDecimal ivaPercentage = item.getIvaPercentage() != null
                        ? item.getIvaPercentage()
                        : new BigDecimal("21.00");
                BigDecimal quantity = item.getQuantity() != null
                        ? item.getQuantity()
                        : BigDecimal.ONE;
                BigDecimal subtotal = item.getAmount() != null
                        ? item.getAmount().multiply(quantity).setScale(2, java.math.RoundingMode.HALF_UP)
                        : null;

                boolean isCredit = item.getTransactionalDocument() != null
                        && item.getTransactionalDocument().getDocumentType() != null
                        && item.getTransactionalDocument().getDocumentType().name().startsWith("CREDIT_NOTE");

                BigDecimal ivaAmount = null;
                if (item.getTransactionalDocument() != null && subtotal != null) {
                    BigDecimal rawIva = subtotal
                            .multiply(ivaPercentage)
                            .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    ivaAmount = isCredit ? rawIva.negate() : rawIva;
                    totalIva = totalIva.add(ivaAmount);
                }

                BigDecimal signedSubtotal = (subtotal != null && isCredit) ? subtotal.negate() : subtotal;
                BigDecimal subtotalWithIva = signedSubtotal != null
                        ? signedSubtotal.add(ivaAmount != null ? ivaAmount : BigDecimal.ZERO)
                        : null;

                itemDtos.add(new RepairItemResponseDTO(
                        item.getId(),
                        item.getItemType() != null ? item.getItemType().name() : null,
                        item.getDescription(),
                        item.getAmount(),
                        quantity,
                        signedSubtotal,
                        ivaPercentage,
                        ivaAmount,
                        subtotalWithIva,
                        documentToSummaryDto(item.getTransactionalDocument()),
                        item.getSortOrder()
                ));
                if (subtotal != null) {
                    if (item.getItemType() == RepairItemType.MATERIAL) {
                        materialSubtotal = isCredit ? materialSubtotal.subtract(subtotal) : materialSubtotal.add(subtotal);
                    } else if (item.getItemType() == RepairItemType.MANO_DE_OBRA) {
                        laborSubtotal = isCredit ? laborSubtotal.subtract(subtotal) : laborSubtotal.add(subtotal);
                    }
                }
            }
        }

        BigDecimal totalCost = materialSubtotal.add(laborSubtotal);
        BigDecimal totalWithIva = totalCost.add(totalIva);

        return new RepairResponseDTO(
                repair.getId(),
                repair.getDate(),
                repair.getVehicle() != null ? repair.getVehicle().getId() : null,
                repair.getVehicle() != null ? repair.getVehicle().getLicensePlate() : null,
                repair.getDescription(),
                repair.getMileage(),
                repair.getSupplier() != null ? repair.getSupplier().getId() : null,
                repair.getSupplier() != null ? repair.getSupplier().getLegalName() : null,
                repair.getSupplier() != null ? repair.getSupplier().getTradeName() : null,
                itemDtos,
                materialSubtotal,
                laborSubtotal,
                totalCost,
                totalIva,
                totalWithIva,
                mapRepairOrderToDto(repair.getRepairOrder())
        );
    }

    @Named("vehicleIdToEntity")
    default Vehicle vehicleIdToEntity(Long vehicleId) {
        if (vehicleId == null) return null;
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        return vehicle;
    }

    @Named("supplierIdToEntity")
    default Supplier supplierIdToEntity(Long supplierId) {
        if (supplierId == null) return null;
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        return supplier;
    }

    @Named("mapRepairOrderToDto")
    default RepairOrderResponseDTO mapRepairOrderToDto(RepairOrder order) {
        if (order == null) return null;
        return new RepairOrderResponseDTO(
                order.getId(),
                order.getDate(),
                order.getVehicle() != null ? order.getVehicle().getId() : null,
                order.getVehicle() != null ? order.getVehicle().getLicensePlate() : null,
                order.getDescription(),
                order.getItems(),
                order.getReportedBy(),
                order.getStatus(),
                order.getCreatedByUser() != null
                        ? order.getCreatedByUser().getFirstName() + " " + order.getCreatedByUser().getLastName()
                        : null
        );
    }

    @Named("documentToSummaryDto")
    default TransactionalDocumentSummaryDTO documentToSummaryDto(TransactionalDocument doc) {
        if (doc == null) return null;
        return new TransactionalDocumentSummaryDTO(
                doc.getId(),
                doc.getDocumentType() != null ? doc.getDocumentType().name() : null,
                doc.getBranchCode(),
                doc.getDocumentNumber(),
                doc.getSupplier() != null ? doc.getSupplier().getLegalName() : null,
                doc.getTotal(),
                doc.getDate()
        );
    }
}

