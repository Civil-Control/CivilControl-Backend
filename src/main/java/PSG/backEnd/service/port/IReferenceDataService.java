package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.reference.EmployeeReferenceItem;
import PSG.backEnd.model.dto.reference.GasStationReferenceItem;
import PSG.backEnd.model.dto.reference.ReferenceItem;
import PSG.backEnd.model.dto.reference.VehicleReferenceItem;
import PSG.backEnd.model.enums.ItemType;

import java.util.List;

/**
 * Provides lightweight reference data for form dropdowns and search modals.
 *
 * <p>These methods return only {@code id + label} for each entity,
 * bypassing the heavier paginated/filtered list endpoints and their
 * domain-specific READ permissions.  Security is enforced at the
 * controller level: the caller must hold at least one permission
 * that <em>implies</em> the need to look up that entity type.</p>
 */
public interface IReferenceDataService {

    List<VehicleReferenceItem> getVehicleReferences();

    List<EmployeeReferenceItem> getEmployeeReferences();

    List<ReferenceItem> getSupplierReferences();

    List<ReferenceItem> getBuildingReferences();

    List<ReferenceItem> getProjectAreaReferences();

    List<GasStationReferenceItem> getGasStationReferences();

    List<ReferenceItem> getItemReferences();

    List<ReferenceItem> getItemReferencesByType(ItemType itemType);

    List<ReferenceItem> getServiceSupplierReferences();

    List<ReferenceItem> getClientReferences();

    List<ReferenceItem> getWorkContractReferences();

    List<ReferenceItem> getSalesDocumentReferences();

    List<ReferenceItem> getServiceAssignmentReferences();

    List<Long> getLinkedSupplierIds();
}
