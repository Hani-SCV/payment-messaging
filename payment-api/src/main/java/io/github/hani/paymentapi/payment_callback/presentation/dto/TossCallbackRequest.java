package io.github.hani.paymentapi.payment_callback.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TossCallbackRequest(

    @NotBlank
    String paymentKey,

    @NotBlank
    String orderId,

    @NotNull
    @Positive
    Long amount

) {
}