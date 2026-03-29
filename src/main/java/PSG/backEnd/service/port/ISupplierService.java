package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.supplier.SupplierDTO;
import PSG.backEnd.model.dto.supplier.SupplierFilterDTO;
import PSG.backEnd.model.dto.supplier.SupplierResponseDTO;
import PSG.backEnd.model.dto.supplier.SupplierStatsDTO;
import PSG.backEnd.model.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ISupplierService {
    SupplierResponseDTO createSupplier(SupplierDTO supplierDTO);
    SupplierResponseDTO getSupplierById(Long id);
    SupplierStatsDTO getSupplierStats(Long id, LocalDate fromDate, LocalDate toDate);
    SupplierResponseDTO updateSupplier(Long id, SupplierDTO supplierDTO);
    void updateSupplierBalance(Long id, BigDecimal amount);
    void deleteSupplier(Long id);
    Page<SupplierResponseDTO> getAllSuppliers(SupplierFilterDTO filterDTO, Pageable pageable);
    Supplier getEntityById(Long id);

    boolean existsById(Long id);
}
