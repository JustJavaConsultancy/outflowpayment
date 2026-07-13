package net.techcrunch.outflowPayment.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationAuditEventRepository extends JpaRepository<ReconciliationAuditEvent, Long> {
    List<ReconciliationAuditEvent> findByReconciliationItemIdOrderByDateCreatedAsc(Long reconciliationItemId);
}
