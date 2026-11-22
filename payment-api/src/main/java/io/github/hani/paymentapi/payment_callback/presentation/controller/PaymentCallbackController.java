package io.github.hani.paymentapi.payment_callback.presentation.controller;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment_callback.application.mapper.PaymentCallbackMapper;
import io.github.hani.paymentapi.payment_callback.application.service.PaymentCallbackService;
import io.github.hani.paymentapi.payment_callback.presentation.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payment-callbacks")
@RequiredArgsConstructor
@Validated
public class PaymentCallbackController {

    private final PaymentCallbackMapper mapper;
    private final PaymentCallbackService service;

    @PostMapping(value = "/toss/return", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PaymentCallbackResponse toss(
        @Valid @RequestBody TossCallbackRequest request
    ) {
        return service.complete(
            mapper.toCommand(request, PaymentProvider.TOSS)
        );
    }

    @PostMapping(value = "/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PaymentCallbackResponse stripe(
        @Valid @RequestBody StripeWebhookRequest request
    ) {
        return service.complete(
            mapper.toCommand(request, PaymentProvider.STRIPE)
        );
    }

    @PostMapping(value = "/alipay/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public PaymentCallbackResponse alipay(
        @RequestParam("order_id") @NotBlank String orderId,
        @RequestParam("trade_no") @NotBlank String tradeNo,
        @RequestParam("total_amount") @NotBlank String totalAmount
    ) {
        AlipayNotifyRequest request = new AlipayNotifyRequest(
            orderId,
            tradeNo,
            totalAmount
        );

        return service.complete(
            mapper.toCommand(request, PaymentProvider.ALIPAY)
        );
    }
}