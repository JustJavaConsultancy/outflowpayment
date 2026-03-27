package net.techcrunch.outflowPayment.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByAccountNumberAndMerchantId(String accountNumber, String merchantId);
}