package io.github.hani.paymentapi.payment.infrastructure;

import io.github.hani.paymentapi.order.domain.Order;
import io.github.hani.paymentapi.payment.domain.Payment;
import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockingByPublicId(String publicId);

    boolean existsByOrderAndStatusAndIdNot(Order order, PaymentStatus status, Long id);

    boolean existsByProviderAndExternalTransactionIdAndIdNot(
        PaymentProvider provider,
        String externalTransactionId,
        Long id
    );
}