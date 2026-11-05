package PSG.backEnd.repository;

import PSG.backEnd.model.entity.ItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemDetailRepository extends JpaRepository<ItemDetail, Long> {

    List<ItemDetail> findByDocumentId(Long documentId);

    List<ItemDetail> findByItemId(Long itemId);

    @Query("SELECT d FROM ItemDetail d JOIN FETCH d.document doc " +
           "WHERE d.item.id = :itemId AND doc.deleted = false ORDER BY doc.date DESC, doc.id DESC")
    List<ItemDetail> findActiveByItemId(@Param("itemId") Long itemId);
}
