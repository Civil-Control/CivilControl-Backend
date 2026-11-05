package PSG.backEnd.repository;

import PSG.backEnd.model.entity.vehicle.RepairItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairItemRepository extends JpaRepository<RepairItem, Long> {

    List<RepairItem> findByTransactionalDocumentId(Long transactionalDocumentId);
}
