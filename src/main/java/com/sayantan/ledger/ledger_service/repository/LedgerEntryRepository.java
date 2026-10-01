package com.sayantan.ledger.ledger_service.repository;

import com.sayantan.ledger.ledger_service.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN le.entryType = com.sayantan.ledger.ledger_service.domain.enums.EntryType.CREDIT
                 THEN le.amountMinorUnits
                 ELSE -le.amountMinorUnits
            END
        ), 0)
        FROM LedgerEntry le
        WHERE le.account.id = :accountId
        """)
    Long computeBalance(@Param("accountId") UUID accountId);
}
