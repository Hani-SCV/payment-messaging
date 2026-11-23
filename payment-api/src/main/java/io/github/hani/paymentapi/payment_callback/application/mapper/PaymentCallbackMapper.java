package io.github.hani.paymentapi.payment_callback.application.mapper;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.presentation.dto.AlipayNotifyRequest;
import io.github.hani.paymentapi.payment_callback.presentation.dto.StripeWebhookRequest;
import io.github.hani.paymentapi.payment_callback.presentation.dto.TossCallbackRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCallbackMapper {

    @Mapping(source = "request.orderId", target = "paymentId")
    @Mapping(source = "request.paymentKey", target = "externalTransactionId")
    @Mapping(source = "request.amount", target = "rawAmount")
    @Mapping(source = "provider", target = "provider")
    @Mapping(target = "rawCurrency", ignore = true)
    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    PaymentCompletedCommand toCommand(
        TossCallbackRequest request,
        PaymentProvider provider
    );

    @Mapping(source = "request.data.object.clientReferenceId", target = "paymentId")
    @Mapping(source = "request.data.object.id", target = "externalTransactionId")
    @Mapping(source = "request.data.object.amountTotal", target = "rawAmount")
    @Mapping(source = "request.data.object.currency", target = "rawCurrency")
    @Mapping(source = "request.type", target = "eventType")
    @Mapping(source = "request.data.object.paymentStatus", target = "paymentStatus")
    @Mapping(source = "provider", target = "provider")
    PaymentCompletedCommand toCommand(
        StripeWebhookRequest request,
        PaymentProvider provider
    );

    @Mapping(source = "request.orderId", target = "paymentId")
    @Mapping(source = "request.tradeNo", target = "externalTransactionId")
    @Mapping(source = "request.totalAmount", target = "rawAmount")
    @Mapping(source = "provider", target = "provider")
    @Mapping(target = "rawCurrency", ignore = true)
    @Mapping(target = "eventType", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    PaymentCompletedCommand toCommand(
        AlipayNotifyRequest request,
        PaymentProvider provider
    );
}