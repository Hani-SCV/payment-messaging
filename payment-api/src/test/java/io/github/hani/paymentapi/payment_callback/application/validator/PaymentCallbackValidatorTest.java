
package io.github.hani.paymentapi.payment_callback.application.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.hani.paymentapi.payment.domain.PaymentProvider;
import io.github.hani.paymentapi.payment.infrastructure.PaymentRepository;
import io.github.hani.paymentapi.payment_callback.application.command.PaymentCompletedCommand;
import io.github.hani.paymentapi.payment_callback.application.exception.PaymentCallbackException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackValidatorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentCallbackValidator validator;

    @Test
    void tossAmountShouldBeNormalized() {
        PaymentCompletedCommand command = new PaymentCompletedCommand(
            "pay_demo_toss_001",
            PaymentProvider.TOSS,
            "toss_key_demo_1001",
            "129900",
            null,
            null,
            null
        );

        BigDecimal result = validator.normalizeAmount(command);

        assertEquals(new BigDecimal("129900.00"), result);
    }

    @Test
    void stripeAmountShouldBeConvertedFromMinorUnit() {
        PaymentCompletedCommand command = new PaymentCompletedCommand(
            "pay_demo_stripe_001",
            PaymentProvider.STRIPE,
            "cs_demo_1001",
            "7700",
            "usd",
            "checkout.session.completed",
            "paid"
        );

        BigDecimal result = validator.normalizeAmount(command);

        assertEquals(new BigDecimal("77.00"), result);
    }

    @Test
    void stripeCurrencyShouldBeUppercase() {
        PaymentCompletedCommand command = new PaymentCompletedCommand(
            "pay_demo_stripe_001",
            PaymentProvider.STRIPE,
            "cs_demo_1001",
            "7700",
            "usd",
            "checkout.session.completed",
            "paid"
        );

        assertEquals("USD", validator.normalizeCurrency(command));
    }

    @Test
    void unsupportedStripeEventShouldThrowException() {
        PaymentCompletedCommand command = new PaymentCompletedCommand(
            "pay_demo_stripe_001",
            PaymentProvider.STRIPE,
            "cs_demo_1001",
            "7700",
            "usd",
            "checkout.session.expired",
            "paid"
        );

        assertThrows(
            PaymentCallbackException.class,
            () -> validator.validateCommand(command)
        );
    }

    @Test
    void unpaidStripePaymentShouldThrowException() {
        PaymentCompletedCommand command = new PaymentCompletedCommand(
            "pay_demo_stripe_001",
            PaymentProvider.STRIPE,
            "cs_demo_1001",
            "7700",
            "usd",
            "checkout.session.completed",
            "unpaid"
        );

        assertThrows(
            PaymentCallbackException.class,
            () -> validator.validateCommand(command)
        );
    }
}
