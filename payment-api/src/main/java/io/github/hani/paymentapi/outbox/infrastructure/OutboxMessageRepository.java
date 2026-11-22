package io.github.hani.paymentapi.outbox.infrastructure;

import io.github.hani.paymentapi.outbox.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
}