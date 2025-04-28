package net.techcrunch.outflowPayment.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByInvoiceId(Long invoiceId);

    List<Order> findByMerchantIdOrderByDateCreatedAsc(String merchantId);
}