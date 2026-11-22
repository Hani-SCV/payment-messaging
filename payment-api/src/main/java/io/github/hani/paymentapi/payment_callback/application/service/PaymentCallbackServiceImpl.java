package io.github.hani.paymentapi.payment_callback.application.service;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.application.mapper.PaymentCallbackMapper;
import io.github.hani.paymentapi.payment_callback.presentation.dto.PaymentCallbackResponse;
import io.github.hani.paymentapi.payment_callback.presentation.dto.TossCallbackRequest;
import io.github.hani.paymentapi.payment_callback.presentation.dto.StripeWebhookRequest;
import io.github.hani.paymentapi.payment_callback.presentation.dto.AlipayNotifyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private final PaymentCallbackMapper paymentCallbackMapper;

    public PaymentCompletedCommand convert(TossCallbackRequest request) {
        PaymentCompletedCommand command =
            paymentCallbackMapper.toCommand(request, PaymentProvider.TOSS);

        return normalize(command, "KRW", 0);
    }

    public PaymentCompletedCommand convert(StripeWebhookRequest request) {
        if (!"checkout.session.completed".equals(request.type())) {
            throw new IllegalArgumentException("지원하지 않는 Stripe 이벤트입니다.");
        }

        var payment = request.data().object();

        if (!"paid".equals(payment.paymentStatus())) {
            throw new IllegalArgumentException("결제가 완료되지 않은 이벤트입니다.");
        }

        String currency = payment.currency().toUpperCase();
        int fractionDigits = Currency.getInstance(currency).getDefaultFractionDigits();

        return normalize(
            paymentCallbackMapper.toCommand(request, PaymentProvider.STRIPE),
            currency,
            fractionDigits
        );
    }

    public PaymentCompletedCommand convert(AlipayNotifyRequest request) {
        return normalize(
            paymentCallbackMapper.toCommand(request, PaymentProvider.ALIPAY),
            "CNY",
            2
        );
    }

    private PaymentCompletedCommand normalize(
        PaymentCompletedCommand command,
        String currency,
        int fractionDigits
    ) {
        BigDecimal amount = new BigDecimal(command.amount())
            .movePointLeft(fractionDigits);

        return new PaymentCompletedCommand(
            command.paymentId(),
            command.provider(),
            command.externalTransactionId(),
            amount.toPlainString(),
            currency
        );
    }

    @Override
    public PaymentCallbackResponse complete(PaymentCompletedCommand command) {
        throw new UnsupportedOperationException("결제 완료 처리는 다음 단계에서 구현합니다.");
    }
}