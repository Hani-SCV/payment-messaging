package io.github.hani.paymentapi.payment_event.infrastructure;

import io.github.hani.paymentapi.payment_event.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
}