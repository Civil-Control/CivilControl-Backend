package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.laborIncident.LaborIncidentDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentFilterDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ILaborIncidentService {
    LaborIncidentResponseDTO createLaborIncident(LaborIncidentDTO dto);
    LaborIncidentResponseDTO getLaborIncidentById(Long id);
    LaborIncidentResponseDTO updateLaborIncident(Long id, LaborIncidentDTO dto);
    void deleteLaborIncident(Long id);
    Page<LaborIncidentResponseDTO> getAllLaborIncidents(LaborIncidentFilterDTO filterDTO, Pageable pageable);
}
