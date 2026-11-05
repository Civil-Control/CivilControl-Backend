package PSG.backEnd.model.mapper;

import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.entity.treasury.Checkbook;
import org.mapstruct.Mapper;

/**
 * Lightweight shim that converts foreign-key IDs into managed-reference entities for MapStruct.
 * Avoids triggering full DB loads on each mapping; downstream services rely on JPA managed
 * persistence to attach the FK by id.
 */
@Mapper(componentModel = "spring")
public interface TreasuryRefMapper {

    default BankAccount toBankAccountRef(Long id) {
        if (id == null) return null;
        BankAccount ref = new BankAccount();
        ref.setId(id);
        return ref;
    }

    default Long toBankAccountId(BankAccount ref) {
        return ref == null ? null : ref.getId();
    }

    default Checkbook toCheckbookRef(Long id) {
        if (id == null) return null;
        Checkbook ref = new Checkbook();
        ref.setId(id);
        return ref;
    }

    default Long toCheckbookId(Checkbook ref) {
        return ref == null ? null : ref.getId();
    }

    default CashBox toCashBoxRef(Long id) {
        if (id == null) return null;
        CashBox ref = new CashBox();
        ref.setId(id);
        return ref;
    }

    default Long toCashBoxId(CashBox ref) {
        return ref == null ? null : ref.getId();
    }
}
