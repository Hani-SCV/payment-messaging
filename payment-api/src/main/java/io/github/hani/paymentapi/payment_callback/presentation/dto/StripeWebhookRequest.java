package io.github.hani.paymentapi.payment_callback.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeWebhookRequest(
    @NotBlank String id,
    @NotBlank String type,
    @NotNull @Valid Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        @NotNull @Valid ObjectData object
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectData(
        @NotBlank String id,

        @JsonProperty("client_reference_id")
        @NotBlank String clientReferenceId,

        @JsonProperty("amount_total")
        @NotNull @PositiveOrZero Long amountTotal,

        @NotBlank String currency,

        @JsonProperty("payment_status")
        @NotBlank String paymentStatus
    ) {}
}