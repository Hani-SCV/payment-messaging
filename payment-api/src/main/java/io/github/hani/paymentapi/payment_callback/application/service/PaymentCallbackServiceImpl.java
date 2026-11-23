
package io.github.hani.paymentapi.payment_callback.application.service;

import io.github.hani.paymentapi.order.domain.Order;
import io.github.hani.paymentapi.order.domain.OrderStatus;
import io.github.hani.paymentapi.order.infrastructure.OrderRepository;
import io.github.hani.paymentapi.outbox.domain.OutboxMessage;
import io.github.hani.paymentapi.outbox.infrastructure.OutboxMessageRepository;
import io.github.hani.paymentapi.payment.domain.Payment;
import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment.domain.PaymentStatus;
import io.github.hani.paymentapi.payment.infrastructure.PaymentRepository;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.application.exception.PaymentCallbackException;
import io.github.hani.paymentapi.payment_callback.presentation.dto.PaymentCallbackResponse;
import io.github.hani.paymentapi.payment_event.domain.PaymentEvent;
import io.github.hani.paymentapi.payment_event.infrastructure.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.Currency;

@Service
@RequiredArgsConstructor
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final String EVENT_TYPE = "PAYMENT_COMPLETED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentCallbackResponse complete(PaymentCompletedCommand command) {

        // Payment 조회: 연결된 Order ID 확인
        Payment initialPayment = paymentRepository.findByPublicId(command.paymentId())
            .orElseThrow(() -> new PaymentCallbackException(
                "PAYMENT_NOT_FOUND",
                "결제 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // Order 비관적 락
        Order order = orderRepository.findWithLockingById(
                initialPayment.getOrder().getId()
            )
            .orElseThrow(() -> new PaymentCallbackException(
                "ORDER_NOT_FOUND",
                "주문 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // Payment 비관적 락
        Payment payment = paymentRepository.findWithLockingByPublicId(command.paymentId())
            .orElseThrow(() -> new PaymentCallbackException(
                "PAYMENT_NOT_FOUND",
                "결제 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // PG 및 금액 검증
        BigDecimal amount = normalizeAmount(command);
        String currency = normalizeCurrency(command);

        if (payment.getProvider() != command.provider()) {
            throw new PaymentCallbackException(
                "PROVIDER_MISMATCH",
                "결제 수단이 일치하지 않습니다.",
                HttpStatus.CONFLICT
            );
        }

        if (payment.getAmount().compareTo(amount) != 0
            || !payment.getCurrency().equalsIgnoreCase(currency)) {
            throw new PaymentCallbackException(
                "AMOUNT_MISMATCH",
                "결제 금액 또는 통화가 일치하지 않습니다.",
                HttpStatus.CONFLICT
            );
        }

        // 이미 완료된 결제인지 확인
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            if (payment.getExternalTransactionId() != null
                && payment.getExternalTransactionId().equals(command.externalTransactionId())) {
                return new PaymentCallbackResponse(
                    "already_completed",
                    payment.getPublicId(),
                    order.getStatus().name()
                );
            }

            throw new PaymentCallbackException(
                "PAYMENT_ALREADY_COMPLETED",
                "이미 다른 거래로 완료된 결제입니다.",
                HttpStatus.CONFLICT
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentCallbackException(
                "INVALID_PAYMENT_STATUS",
                "결제 대기 상태가 아닙니다.",
                HttpStatus.CONFLICT
            );
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new PaymentCallbackException(
                "INVALID_ORDER_STATUS",
                "결제 대기 상태의 주문이 아닙니다.",
                HttpStatus.CONFLICT
            );
        }

        // 동일 주문의 다른 결제 완료 여부 확인
        boolean anotherPaymentCompleted =
            paymentRepository.existsByOrderAndStatusAndIdNot(
                order,
                PaymentStatus.COMPLETED,
                payment.getId()
            );

        if (anotherPaymentCompleted) {
            throw new PaymentCallbackException(
                "ORDER_ALREADY_PAID",
                "이미 완료된 결제가 존재합니다.",
                HttpStatus.CONFLICT
            );
        }

        // 결제 및 주문 상태 변경
        payment.complete(command.externalTransactionId());
        order.markPaid();

        // 이벤트 payload 생성
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", EVENT_TYPE);
        payload.put("paymentId", payment.getPublicId());
        payload.put("orderId", order.getPublicId());
        payload.put("provider", payment.getProvider().name());
        payload.put("externalTransactionId", payment.getExternalTransactionId());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());

        // PaymentEvent 저장
        paymentEventRepository.save(
            PaymentEvent.create(payment, EVENT_TYPE, payload)
        );

        // OutboxMessage 저장
        outboxMessageRepository.save(
            OutboxMessage.create(
                "payment-completed:" + payment.getPublicId(),
                EVENT_TYPE,
                "PAYMENT",
                payment.getPublicId(),
                payload
            )
        );

        return new PaymentCallbackResponse(
            "completed",
            payment.getPublicId(),
            order.getStatus().name()
        );
    }

    private BigDecimal normalizeAmount(PaymentCompletedCommand command) {

        BigDecimal amount;

        try {
            amount = new BigDecimal(command.rawAmount());

            if (command.provider() == PaymentProvider.STRIPE) {
                String currency = normalizeCurrency(command);
                int fractionDigits = Currency.getInstance(currency).getDefaultFractionDigits();

                if (fractionDigits < 0) {
                    throw new PaymentCallbackException(
                        "UNSUPPORTED_CURRENCY",
                        "지원하지 않는 통화입니다.",
                        HttpStatus.BAD_REQUEST
                    );
                }

                amount = amount.movePointLeft(fractionDigits);
            }

            return amount.setScale(2);
        } catch (NumberFormatException exception) {
            throw new PaymentCallbackException(
                "INVALID_AMOUNT",
                "결제 금액 형식이 올바르지 않습니다.",
                HttpStatus.BAD_REQUEST
            );
        } catch (IllegalArgumentException exception) {
            throw new PaymentCallbackException(
                "UNSUPPORTED_CURRENCY",
                "지원하지 않는 통화입니다.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private String normalizeCurrency(PaymentCompletedCommand command) {

        return switch (command.provider()) {
            case TOSS -> "KRW";
            case ALIPAY -> "CNY";
            case STRIPE -> {
                if (command.rawCurrency() == null || command.rawCurrency().isBlank()) {
                    throw new PaymentCallbackException(
                        "INVALID_CURRENCY",
                        "통화 정보가 누락되었습니다.",
                        HttpStatus.BAD_REQUEST
                    );
                }
                yield command.rawCurrency().toUpperCase();
            }
        };
    }
}
