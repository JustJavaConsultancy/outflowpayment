package net.techcrunch.outflowPayment.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    boolean existsByExternalReferenceIgnoreCase(String externalReference);

    boolean existsBySourceAccountIgnoreCase(String sourceAccount);

    List<Transaction> findByTransactionOwnerIgnoreCase(String transactionOwner);

    Transaction findTransactionById(Long id);
}
