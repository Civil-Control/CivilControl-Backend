package PSG.backEnd.repository;

import PSG.backEnd.model.entity.ItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemDetailRepository extends JpaRepository<ItemDetail, Long> {

    List<ItemDetail> findByDocumentId(Long documentId);

    List<ItemDetail> findByItemId(Long itemId);
}
