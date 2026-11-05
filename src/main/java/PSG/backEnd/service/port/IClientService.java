package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.client.ClientDTO;
import PSG.backEnd.model.dto.client.ClientFilterDTO;
import PSG.backEnd.model.dto.client.ClientResponseDTO;
import PSG.backEnd.model.dto.client.ClientStatsDTO;
import PSG.backEnd.model.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IClientService {

    ClientResponseDTO createClient(ClientDTO dto);

    Page<ClientResponseDTO> getAllClients(ClientFilterDTO filterDTO, Pageable pageable);

    ClientResponseDTO getClientById(Long id);

    ClientStatsDTO getClientStats(Long id, LocalDate fromDate, LocalDate toDate);

    ClientResponseDTO updateClient(Long id, ClientDTO dto);

    void deleteClient(Long id);

    Client getEntityById(Long id);
}
