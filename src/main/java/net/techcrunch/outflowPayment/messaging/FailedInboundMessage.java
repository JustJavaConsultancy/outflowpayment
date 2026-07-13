package net.techcrunch.outflowPayment.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "failed_inbound_messages")
@Getter
@Setter
public class FailedInboundMessage {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false, length = 120)
    private String queueName;

    @Column(nullable = false, length = 120)
    private String exchangeName;

    @Column(nullable = false, length = 120)
    private String routingKey;

    @Column(length = 160)
    private String reference;

    @Column(length = 160)
    private String correlationId;

    @Column(length = 160)
    private String messageId;

    @Column(length = 120)
    private String failureClass;

    @Column(length = 1000)
    private String failureReason;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FailedMessageStatus status = FailedMessageStatus.FAILED;

    @Column(nullable = false)
    private OffsetDateTime dateCreated = OffsetDateTime.now();

    private OffsetDateTime replayedAt;
}
