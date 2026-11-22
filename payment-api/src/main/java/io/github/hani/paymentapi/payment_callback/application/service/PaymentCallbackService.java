package io.github.hani.paymentapi.payment_callback.application.service;

import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.presentation.dto.PaymentCallbackResponse;

public interface PaymentCallbackService {

    PaymentCallbackResponse complete(PaymentCompletedCommand command);
}
