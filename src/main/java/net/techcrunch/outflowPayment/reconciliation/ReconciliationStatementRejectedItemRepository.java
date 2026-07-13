package net.techcrunch.outflowPayment.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationStatementRejectedItemRepository extends JpaRepository<ReconciliationStatementRejectedItem, Long> {
    List<ReconciliationStatementRejectedItem> findByImportBatch_IdOrderByRowNumberAsc(Long importId);
}
