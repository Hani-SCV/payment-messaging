package io.github.hani.paymentapi.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "outbox_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deduplication_key", nullable = false, length = 120)
    private String deduplicationKey;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 40)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 40)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static OutboxMessage create(
        String deduplicationKey,
        String eventType,
        String aggregateType,
        String aggregateId,
        JsonNode payload
    ) {
        OutboxMessage message = new OutboxMessage();
        message.deduplicationKey = deduplicationKey;
        message.eventType = eventType;
        message.aggregateType = aggregateType;
        message.aggregateId = aggregateId;
        message.payload = payload;
        message.status = OutboxStatus.PENDING;

        return message;
    }
}