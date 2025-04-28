package net.techcrunch.outflowPayment.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCodeIgnoreCase(String code);

    List<Product> findByMerchantId(String merchantId);



}
