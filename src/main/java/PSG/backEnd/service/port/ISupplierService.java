package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.SupplierDTO;
import PSG.backEnd.model.dto.SupplierFilterDTO;
import PSG.backEnd.model.dto.SupplierResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISupplierService {
    SupplierResponseDTO createSupplier(SupplierDTO supplierDTO);
    SupplierResponseDTO getSupplierById(Long id);
    SupplierResponseDTO updateSupplier(Long id, SupplierDTO supplierDTO);
    void deleteSupplier(Long id);
    Page<SupplierResponseDTO> getAllSuppliers(SupplierFilterDTO filterDTO, Pageable pageable);
}
