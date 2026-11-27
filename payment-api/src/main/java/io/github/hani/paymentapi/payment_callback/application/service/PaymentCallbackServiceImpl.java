
package io.github.hani.paymentapi.payment_callback.application.service;

import io.github.hani.paymentapi.order.domain.Order;
import io.github.hani.paymentapi.order.infrastructure.OrderRepository;
import io.github.hani.paymentapi.outbox.domain.OutboxMessage;
import io.github.hani.paymentapi.outbox.infrastructure.OutboxMessageRepository;
import io.github.hani.paymentapi.payment.domain.Payment;
import io.github.hani.paymentapi.payment.domain.PaymentStatus;
import io.github.hani.paymentapi.payment.infrastructure.PaymentRepository;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.application.exception.PaymentCallbackException;
import io.github.hani.paymentapi.payment_callback.application.validator.PaymentCallbackValidator;
import io.github.hani.paymentapi.payment_callback.presentation.dto.PaymentCallbackResponse;
import io.github.hani.paymentapi.payment_event.domain.PaymentEvent;
import io.github.hani.paymentapi.payment_event.infrastructure.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final String EVENT_TYPE = "PAYMENT_COMPLETED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final PaymentCallbackValidator validator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentCallbackResponse complete(PaymentCompletedCommand command) {

        validator.validateCommand(command);

        // Payment 조회: 연결된 Order ID 확인
        Payment initialPayment = paymentRepository.findByPublicId(command.paymentId())
            .orElseThrow(() -> new PaymentCallbackException(
                "PAYMENT_NOT_FOUND",
                "결제 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // Order 잠금
        Order order = orderRepository.findWithLockingById(
                initialPayment.getOrder().getId()
            )
            .orElseThrow(() -> new PaymentCallbackException(
                "ORDER_NOT_FOUND",
                "주문 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // Payment 잠금
        Payment payment = paymentRepository.findWithLockingByPublicId(
                command.paymentId()
            )
            .orElseThrow(() -> new PaymentCallbackException(
                "PAYMENT_NOT_FOUND",
                "결제 정보를 찾을 수 없습니다.",
                HttpStatus.NOT_FOUND
            ));

        // PG 및 금액 검증
        validator.validatePayment(payment, command);

        // 이미 완료된 동일 거래의 재전송
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            if (command.externalTransactionId().equals(
                payment.getExternalTransactionId()
            )) {
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

        // 결제 및 주문 상태 검증
        validator.validatePaymentStatus(payment, order);

        // 주문의 다른 완료 결제 여부 검증
        validator.validateAnotherCompletedPayment(order, payment);

        // PG 거래 ID 중복 검증
        validator.validateExternalTransactionId(command, payment);

        // 결제 및 주문 상태 변경
        payment.complete(command.externalTransactionId());
        order.markPaid();

        // 결제 이벤트 payload 생성
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", EVENT_TYPE);
        payload.put("paymentId", payment.getPublicId());
        payload.put("orderId", order.getPublicId());
        payload.put("provider", payment.getProvider().name());
        payload.put("externalTransactionId", payment.getExternalTransactionId());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());

        // Payment Event 저장
        paymentEventRepository.save(
            PaymentEvent.create(payment, EVENT_TYPE, payload)
        );

        // Outbox 저장
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
}
