package PSG.backEnd.repository;

import PSG.backEnd.model.entity.sales.SalesItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesItemDetailRepository extends JpaRepository<SalesItemDetail, Long> {

    @Query("SELECT d FROM SalesItemDetail d JOIN FETCH d.salesDocument sd " +
           "WHERE d.item.id = :itemId AND sd.deleted = false ORDER BY sd.date DESC, sd.id DESC")
    List<SalesItemDetail> findActiveByItemId(@Param("itemId") Long itemId);
}
