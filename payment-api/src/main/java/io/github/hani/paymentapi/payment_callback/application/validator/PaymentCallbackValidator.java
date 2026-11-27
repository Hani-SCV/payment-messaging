
package io.github.hani.paymentapi.payment_callback.application.validator;

import io.github.hani.paymentapi.payment.domain.Payment;
import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment.domain.PaymentStatus;
import io.github.hani.paymentapi.payment.infrastructure.PaymentRepository;
import io.github.hani.paymentapi.order.domain.Order;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.application.exception.PaymentCallbackException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCallbackValidator {

    private static final Set<String> SUPPORTED_CURRENCIES =
        Set.of("KRW", "JPY", "USD", "CNY");

    private final PaymentRepository paymentRepository;

    public void validateCommand(PaymentCompletedCommand command) {

        if (command.provider() == null) {
            throw new PaymentCallbackException(
                "INVALID_PROVIDER",
                "결제 수단 정보가 누락되었습니다.",
                HttpStatus.BAD_REQUEST
            );
        }

        if (command.paymentId() == null || command.paymentId().isBlank()) {
            throw new PaymentCallbackException(
                "INVALID_PAYMENT_ID",
                "결제 ID가 누락되었습니다.",
                HttpStatus.BAD_REQUEST
            );
        }

        if (command.externalTransactionId() == null
            || command.externalTransactionId().isBlank()) {
            throw new PaymentCallbackException(
                "INVALID_EXTERNAL_TRANSACTION_ID",
                "PG 거래 ID가 누락되었습니다.",
                HttpStatus.BAD_REQUEST
            );
        }

        if (command.provider() == PaymentProvider.STRIPE) {

            if (!"checkout.session.completed".equals(command.eventType())) {
                throw new PaymentCallbackException(
                    "UNSUPPORTED_EVENT",
                    "지원하지 않는 Stripe 이벤트입니다.",
                    HttpStatus.BAD_REQUEST
                );
            }

            if (!"paid".equals(command.paymentStatus())) {
                throw new PaymentCallbackException(
                    "PAYMENT_NOT_PAID",
                    "결제가 완료된 상태가 아닙니다.",
                    HttpStatus.CONFLICT
                );
            }
        }
    }

    public BigDecimal normalizeAmount(PaymentCompletedCommand command) {

        BigDecimal amount;

        try {
            amount = new BigDecimal(command.rawAmount());

            if (command.provider() == PaymentProvider.STRIPE) {
                String currency = normalizeCurrency(command);
                int fractionDigits =
                    Currency.getInstance(currency).getDefaultFractionDigits();

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
        } catch (ArithmeticException exception) {
            throw new PaymentCallbackException(
                "INVALID_AMOUNT",
                "결제 금액의 소수점 자릿수가 올바르지 않습니다.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    public String normalizeCurrency(PaymentCompletedCommand command) {

        String currency = switch (command.provider()) {
            case TOSS -> "KRW";
            case ALIPAY -> "CNY";
            case STRIPE -> {
                if (command.rawCurrency() == null
                    || command.rawCurrency().isBlank()) {
                    throw new PaymentCallbackException(
                        "INVALID_CURRENCY",
                        "통화 정보가 누락되었습니다.",
                        HttpStatus.BAD_REQUEST
                    );
                }

                yield command.rawCurrency().toUpperCase(Locale.ROOT);
            }
        };

        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new PaymentCallbackException(
                "UNSUPPORTED_CURRENCY",
                "지원하지 않는 통화입니다.",
                HttpStatus.BAD_REQUEST
            );
        }

        return currency;
    }

    public void validatePayment(Payment payment, PaymentCompletedCommand command) {

        if (payment.getProvider() != command.provider()) {
            throw new PaymentCallbackException(
                "PROVIDER_MISMATCH",
                "결제 수단이 일치하지 않습니다.",
                HttpStatus.CONFLICT
            );
        }

        BigDecimal amount = normalizeAmount(command);
        String currency = normalizeCurrency(command);

        if (payment.getAmount().compareTo(amount) != 0
            || !payment.getCurrency().equalsIgnoreCase(currency)) {
            throw new PaymentCallbackException(
                "AMOUNT_MISMATCH",
                "결제 금액 또는 통화가 일치하지 않습니다.",
                HttpStatus.CONFLICT
            );
        }
    }

    public void validatePaymentStatus(Payment payment, Order order) {

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentCallbackException(
                "INVALID_PAYMENT_STATUS",
                "결제 대기 상태가 아닙니다.",
                HttpStatus.CONFLICT
            );
        }

        if (order.getStatus() != io.github.hani.paymentapi.order.domain.OrderStatus.PAYMENT_PENDING) {
            throw new PaymentCallbackException(
                "INVALID_ORDER_STATUS",
                "결제 대기 상태의 주문이 아닙니다.",
                HttpStatus.CONFLICT
            );
        }
    }

    public void validateAnotherCompletedPayment(Order order, Payment payment) {

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
    }

    public void validateExternalTransactionId(PaymentCompletedCommand command, Payment payment) {

        boolean externalTransactionExists =
            paymentRepository.existsByProviderAndExternalTransactionIdAndIdNot(
                command.provider(),
                command.externalTransactionId(),
                payment.getId()
            );

        if (externalTransactionExists) {
            throw new PaymentCallbackException(
                "DUPLICATE_EXTERNAL_TRANSACTION",
                "이미 다른 결제에서 사용된 PG 거래 ID입니다.",
                HttpStatus.CONFLICT
            );
        }
    }
}
