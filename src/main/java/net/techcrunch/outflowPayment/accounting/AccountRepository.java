package net.techcrunch.outflowPayment.accounting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByCode(String code);

    Optional<Account> findByCodeAndOwnerId(String code, String ownerId);

    Optional<Account> findByTypeAndOwnerId(String type, String ownerId);


}