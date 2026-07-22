package net.techcrunch.outflowPayment.observability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationalEventRepository extends JpaRepository<OperationalEvent, Long> {
    List<OperationalEvent> findByReferenceOrderByDateCreatedAsc(String reference);
}
