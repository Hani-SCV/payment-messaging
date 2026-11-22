package io.github.hani.paymentapi.payment_callback.application.command;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;

public record PaymentCompletedCommand(
    String paymentId,
    PaymentProvider provider,
    String externalTransactionId,
    String amount,
    String currency
) {}