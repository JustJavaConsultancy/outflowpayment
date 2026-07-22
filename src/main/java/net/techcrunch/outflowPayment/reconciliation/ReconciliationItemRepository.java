package net.techcrunch.outflowPayment.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, Long> {
    List<ReconciliationItem> findByRun_IdOrderByIdAsc(Long runId);

    List<ReconciliationItem> findByResolutionStatusOrderByIdDesc(ReconciliationItemStatus resolutionStatus);

    List<ReconciliationItem> findByResolutionStatusInOrderByIdDesc(List<ReconciliationItemStatus> resolutionStatuses);
}
