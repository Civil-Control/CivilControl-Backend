package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.client.ClientNotFoundException;
import PSG.backEnd.exception.client.SalesDocumentNotFoundException;
import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentFilterDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailDTO;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.mapper.SalesDocumentMapper;
import PSG.backEnd.repository.CertificationRepository;
import PSG.backEnd.repository.ClientRepository;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.service.port.ISalesDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesDocumentService implements ISalesDocumentService {

    private final SalesDocumentRepository salesDocumentRepository;
    private final CertificationRepository certificationRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final SalesDocumentMapper salesDocumentMapper;

    @Override
    @Transactional
    public SalesDocumentResponseDTO createSalesDocument(SalesDocumentDTO dto) {
        SalesDocument document = salesDocumentMapper.toEntity(dto);

        if (dto.clientId() != null) {
            Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                    .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
            document.setClient(client);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            document.setProjectArea(pa);
        } else {
            document.setProjectArea(null);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            document.setProjectAreaTask(task);
        } else {
            document.setProjectAreaTask(null);
        }

        List<SalesItemDetail> items = buildItemDetails(dto.items(), document);
        document.setItems(items);
        document.setPaid(Boolean.TRUE.equals(dto.paid()));
        document.setDeleted(false);

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesDocumentResponseDTO> getAllSalesDocuments(SalesDocumentFilterDTO filterDTO, Pageable pageable) {
        return salesDocumentRepository.findAllWithFilters(
                filterDTO.clientId(), filterDTO.documentType(), filterDTO.documentNumber(),
                filterDTO.dateFrom(), filterDTO.dateTo(),
                filterDTO.minTotal(), filterDTO.maxTotal(),
                filterDTO.paid(), filterDTO.projectAreaId(), filterDTO.search(),
                pageable
        ).map(salesDocumentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDocumentResponseDTO getSalesDocumentById(Long id) {
        return salesDocumentRepository.findByIdAndDeletedFalse(id)
                .map(salesDocumentMapper::toResponseDto)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
    }

    @Override
    @Transactional
    public SalesDocumentResponseDTO updateSalesDocument(Long id, SalesDocumentDTO dto) {
        SalesDocument existing = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));

        salesDocumentMapper.partialUpdate(dto, existing);

        if (dto.clientId() != null) {
            Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                    .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
            existing.setClient(client);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            existing.setProjectArea(pa);
        } else {
            existing.setProjectArea(null);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            existing.setProjectAreaTask(task);
        } else {
            existing.setProjectAreaTask(null);
        }

        if (dto.items() != null) {
            existing.getItems().clear();
            List<SalesItemDetail> items = buildItemDetails(dto.items(), existing);
            existing.getItems().addAll(items);
        }

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(existing));
    }

    @Override
    @Transactional
    public SalesDocumentResponseDTO markAsPaid(Long id, boolean paid) {
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        document.setPaid(paid);

        // Propagate paid status to linked certifications
        List<Certification> linkedCertifications = certificationRepository.findBySalesDocumentIdAndDeletedFalse(id);
        for (Certification cert : linkedCertifications) {
            if (paid) {
                cert.setStatus(CertificationStatus.COBRADO);
            } else if (cert.getStatus() == CertificationStatus.COBRADO) {
                cert.setStatus(CertificationStatus.FACTURADO);
            }
        }
        certificationRepository.saveAll(linkedCertifications);

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public void deleteSalesDocument(Long id) {
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        document.setDeleted(true);
        salesDocumentRepository.save(document);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<SalesItemDetail> buildItemDetails(List<SalesItemDetailDTO> dtos, SalesDocument document) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        List<SalesItemDetail> result = new ArrayList<>();
        for (SalesItemDetailDTO detailDto : dtos) {
            Item item = itemRepository.findById(detailDto.itemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + detailDto.itemId()));

            // Validate the item is of type VENTA
            if (!item.getItemTypes().contains(PSG.backEnd.model.enums.ItemType.VENTA)) {
                throw new IllegalArgumentException(
                        "El item \"" + item.getName() + "\" no está habilitado para ventas.");
            }

            SalesItemDetail detail = salesDocumentMapper.toItemDetailEntity(detailDto);
            detail.setItem(item);
            detail.setSalesDocument(document);
            result.add(detail);
        }
        return result;
    }
}
