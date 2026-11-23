package io.github.hani.paymentapi.payment_callback.application.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentCallbackException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public PaymentCallbackException(
        String code,
        String message,
        HttpStatus status
    ) {
        super(message);
        this.code = code;
        this.status = status;
    }

}