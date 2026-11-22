package io.github.hani.paymentapi.payment.domain;

import io.github.hani.paymentapi.order.domain.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 40)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "external_transaction_id", length = 80)
    private String externalTransactionId;

    @Column(name = "cancellation_reason", length = 40)
    private String cancellationReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static Payment create(
        Order order,
        PaymentProvider provider,
        BigDecimal amount,
        String currency
    ) {
        Payment payment = new Payment();

        payment.order = order;
        payment.provider = provider;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.PENDING;

        return payment;
    }

    public void complete(String externalTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 결제만 완료할 수 있습니다.");
        }

        this.status = PaymentStatus.COMPLETED;
        this.externalTransactionId = externalTransactionId;
        this.completedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}