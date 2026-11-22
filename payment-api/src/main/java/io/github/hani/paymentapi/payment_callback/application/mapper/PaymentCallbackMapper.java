package io.github.hani.paymentapi.payment_callback.application.mapper;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.presentation.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCallbackMapper {

    @Mapping(source = "request.orderId", target = "paymentId")
    @Mapping(source = "request.paymentKey", target = "externalTransactionId")
    @Mapping(source = "request.amount", target = "amount")
    @Mapping(source = "provider", target = "provider")
    @Mapping(target = "currency", ignore = true)
    PaymentCompletedCommand toCommand(
        TossCallbackRequest request,
        PaymentProvider provider
    );

    @Mapping(source = "request.data.object.clientReferenceId", target = "paymentId")
    @Mapping(source = "request.data.object.id", target = "externalTransactionId")
    @Mapping(source = "request.data.object.amountTotal", target = "amount")
    @Mapping(source = "request.data.object.currency", target = "currency")
    @Mapping(source = "provider", target = "provider")
    PaymentCompletedCommand toCommand(
        StripeWebhookRequest request,
        PaymentProvider provider
    );

    @Mapping(source = "request.orderId", target = "paymentId")
    @Mapping(source = "request.tradeNo", target = "externalTransactionId")
    @Mapping(source = "request.totalAmount", target = "amount")
    @Mapping(source = "provider", target = "provider")
    @Mapping(target = "currency", ignore = true)
    PaymentCompletedCommand toCommand(
        AlipayNotifyRequest request,
        PaymentProvider provider
    );
}