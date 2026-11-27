package io.github.hani.paymentapi;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class PaymentApiIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void contextLoads() {
        assertNotNull(entityManager);
    }
}