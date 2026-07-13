package net.techcrunch.outflowPayment.accounting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingPostingRecordRepository extends JpaRepository<AccountingPostingRecord, Long> {

    Optional<AccountingPostingRecord> findByOperationAndPostingKey(String operation, String postingKey);
}
