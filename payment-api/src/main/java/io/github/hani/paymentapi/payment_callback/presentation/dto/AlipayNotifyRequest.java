package io.github.hani.paymentapi.payment_callback.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record AlipayNotifyRequest(
    @NotBlank String orderId,
    @NotBlank String tradeNo,
    @NotBlank String totalAmount
) {}