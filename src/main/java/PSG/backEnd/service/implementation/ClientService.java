package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.client.ClientAlreadyExistsException;
import PSG.backEnd.exception.client.ClientNotFoundException;
import PSG.backEnd.model.dto.client.ClientDTO;
import PSG.backEnd.model.dto.client.ClientFilterDTO;
import PSG.backEnd.model.dto.client.ClientResponseDTO;
import PSG.backEnd.model.dto.client.ClientStatsDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoDTO;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.ContactInfo;
import PSG.backEnd.model.mapper.ClientMapper;
import PSG.backEnd.model.mapper.ContactInfoMapper;
import PSG.backEnd.repository.ClientRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.service.port.IClientService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final SalesDocumentRepository salesDocumentRepository;
    private final ClientMapper clientMapper;
    private final ContactInfoMapper contactInfoMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public ClientResponseDTO createClient(ClientDTO dto) {
        validateNewClient(dto);
        Optional<Client> deleted = findDeletedClientByCuit(dto);
        if (deleted.isPresent()) {
            return reactivateClient(deleted.get(), dto);
        }
        return createNewClient(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponseDTO> getAllClients(ClientFilterDTO filterDTO, Pageable pageable) {
        return clientRepository.findAllWithFilters(
                filterDTO.cuit(), filterDTO.businessName(), filterDTO.tradeName(),
                filterDTO.ivaCondition(), filterDTO.active(), filterDTO.search(),
                pageable
        ).map(clientMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO getClientById(Long id) {
        return clientRepository.findByIdAndDeletedFalse(id)
                .map(clientMapper::toResponseDto)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientStatsDTO getClientStats(Long id, LocalDate fromDate, LocalDate toDate) {
        clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        BigDecimal totalInvoiced = salesDocumentRepository.sumTotalByClientIdAndDateRange(id, fromDate, toDate);
        BigDecimal totalCollected = salesDocumentRepository.sumCollectedByClientIdAndDateRange(id, fromDate, toDate);
        BigDecimal totalPending = totalInvoiced.subtract(totalCollected);
        return new ClientStatsDTO(totalInvoiced, totalCollected, totalPending);
    }

    @Override
    @Transactional
    public ClientResponseDTO updateClient(Long id, ClientDTO dto) {
        Client existing = clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        validateUniqueFieldsForUpdate(dto, existing);
        clientMapper.partialUpdate(dto, existing);
        syncContacts(existing, dto);
        return clientMapper.toResponseDto(clientRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
        client.setDeleted(true);
        clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Client getEntityById(Long id) {
        return clientRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateNewClient(ClientDTO dto) {
        if (dto.cuit() != null && !dto.cuit().isBlank()
                && clientRepository.existsByCuitAndDeletedFalse(dto.cuit())) {
            throw new ClientAlreadyExistsException(
                    messageSourceHelper.getMessage("client.cuit.alreadyExists", dto.cuit()));
        }
    }

    private Optional<Client> findDeletedClientByCuit(ClientDTO dto) {
        if (dto.cuit() == null || dto.cuit().isBlank()) return Optional.empty();
        return clientRepository.findByCuitAndTenantIdAndDeletedFalse(dto.cuit(), null)
                .filter(c -> Boolean.TRUE.equals(c.getDeleted()));
    }

    private ClientResponseDTO reactivateClient(Client client, ClientDTO dto) {
        clientMapper.partialUpdate(dto, client);
        syncContacts(client, dto);
        client.setDeleted(false);
        client.setActive(true);
        return clientMapper.toResponseDto(clientRepository.save(client));
    }

    private ClientResponseDTO createNewClient(ClientDTO dto) {
        Client client = clientMapper.toEntity(dto);
        if (client.getActive() == null) client.setActive(true);
        client.setDeleted(false);
        syncContacts(client, dto);
        return clientMapper.toResponseDto(clientRepository.save(client));
    }

    private void validateUniqueFieldsForUpdate(ClientDTO dto, Client existing) {
        if (dto.cuit() != null && !dto.cuit().isBlank()
                && !dto.cuit().equals(existing.getCuit())
                && clientRepository.existsByCuitAndDeletedFalse(dto.cuit())) {
            throw new ClientAlreadyExistsException(
                    messageSourceHelper.getMessage("client.update.conflict.cuit", dto.cuit()));
        }
    }

    private void syncContacts(Client client, ClientDTO dto) {
        if (dto.contacts() == null) return;
        client.getContacts().clear();
        for (ContactInfoDTO ciDto : dto.contacts()) {
            ContactInfo ci = contactInfoMapper.toEntity(ciDto);
            contactInfoMapper.handleCollections(ci, ciDto);
            ci.setClient(client);
            client.getContacts().add(ci);
        }
    }
}
