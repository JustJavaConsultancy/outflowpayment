package net.techcrunch.outflowPayment.observability;

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
@Table(name = "operational_events")
@Getter
@Setter
public class OperationalEvent {

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

    @Column(nullable = false, length = 80)
    private String serviceName;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationalEventSeverity severity;

    @Column(length = 80)
    private String referenceType;

    @Column(length = 160)
    private String reference;

    @Column(length = 160)
    private String correlationId;

    @Column(length = 160)
    private String messageId;

    @Column(length = 160)
    private String merchantId;

    @Column(length = 160)
    private String processInstanceId;

    @Column(length = 120)
    private String failureClass;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(nullable = false)
    private OffsetDateTime dateCreated = OffsetDateTime.now();
}
