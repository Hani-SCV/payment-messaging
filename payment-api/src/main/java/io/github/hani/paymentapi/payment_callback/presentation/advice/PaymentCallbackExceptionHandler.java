package io.github.hani.paymentapi.payment_callback.presentation.advice;

import io.github.hani.paymentapi.payment_callback.application.exception.PaymentCallbackException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentCallbackExceptionHandler {

    public record ErrorResponse(ErrorDetail error) {}

    public record ErrorDetail(
        String code,
        String message
    ) {}

    @ExceptionHandler(PaymentCallbackException.class)
    public ResponseEntity<ErrorResponse> handlePaymentCallbackException(
        PaymentCallbackException exception
    ) {
        return ResponseEntity
            .status(exception.getStatus())
            .body(new ErrorResponse(
                new ErrorDetail(exception.getCode(), exception.getMessage())
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                new ErrorDetail(
                    "VALIDATION_ERROR",
                    "요청 데이터가 올바르지 않습니다."
                )
            ));
    }
}