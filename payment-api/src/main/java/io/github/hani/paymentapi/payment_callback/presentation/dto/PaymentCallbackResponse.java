package io.github.hani.paymentapi.payment_callback.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentCallbackResponse(
    String result,

    @JsonProperty("payment_id")
    String paymentId,

    @JsonProperty("order_status")
    String orderStatus
) {}