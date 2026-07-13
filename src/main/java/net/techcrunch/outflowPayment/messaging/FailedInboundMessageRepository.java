package net.techcrunch.outflowPayment.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedInboundMessageRepository extends JpaRepository<FailedInboundMessage, Long> {
    List<FailedInboundMessage> findByStatusOrderByDateCreatedDesc(FailedMessageStatus status);
}
