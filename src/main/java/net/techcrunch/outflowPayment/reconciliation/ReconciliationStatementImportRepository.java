package net.techcrunch.outflowPayment.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationStatementImportRepository extends JpaRepository<ReconciliationStatementImport, Long> {
    List<ReconciliationStatementImport> findAllByOrderByDateCreatedDesc();
}
