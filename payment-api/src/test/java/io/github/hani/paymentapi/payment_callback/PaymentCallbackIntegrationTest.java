
package io.github.hani.paymentapi.payment_callback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentCallbackIntegrationTest {

    private static final String PAYMENT_ID = "pay_demo_toss_001";

    private static final String TOSS_CALLBACK_JSON = """
        {
            "paymentKey": "toss_key_test_1001",
            "orderId": "pay_demo_toss_001",
            "amount": 129900
        }
        """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
    }

    @Test
    void tossCallbackShouldCompletePaymentAndCreateEvents() throws Exception {
        // 1. 결제 콜백 요청
        mockMvc.perform(post("/v1/payment-callbacks/toss/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TOSS_CALLBACK_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("completed"))
            .andExpect(jsonPath("$.payment_id").value(PAYMENT_ID))
            .andExpect(jsonPath("$.order_status").value("PAID"));

        // JPA 변경 사항을 DB에 반영
        entityManager.flush();

        // 2. Payment 상태 확인
        String paymentStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM payments WHERE public_id = ?",
            String.class,
            PAYMENT_ID
        );

        assertEquals("COMPLETED", paymentStatus);

        // 3. Order 상태 확인
        String orderStatus = jdbcTemplate.queryForObject(
            """
            SELECT o.status
            FROM orders o
            JOIN payments p ON p.order_id = o.id
            WHERE p.public_id = ?
            """,
            String.class,
            PAYMENT_ID
        );

        assertEquals("PAID", orderStatus);

        // 4. PaymentEvent 생성 확인
        Integer eventCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM payment_events pe
            JOIN payments p ON pe.payment_id = p.id
            WHERE p.public_id = ?
            """,
            Integer.class,
            PAYMENT_ID
        );

        assertEquals(1, eventCount);

        // 5. OutboxMessage 생성 확인
        Integer outboxCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM outbox_messages
            WHERE deduplication_key = ?
            """,
            Integer.class,
            "payment-completed:" + PAYMENT_ID
        );

        assertEquals(1, outboxCount);
    }

    @Test
    void duplicateCallbackShouldReturnAlreadyCompleted() throws Exception {
        // 1. 최초 콜백
        mockMvc.perform(post("/v1/payment-callbacks/toss/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TOSS_CALLBACK_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("completed"));

        // 2. 동일 콜백 재전송
        mockMvc.perform(post("/v1/payment-callbacks/toss/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TOSS_CALLBACK_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("already_completed"));

        entityManager.flush();

        // 3. PaymentEvent 중복 생성 방지 확인
        Integer eventCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM payment_events pe
            JOIN payments p ON pe.payment_id = p.id
            WHERE p.public_id = ?
            """,
            Integer.class,
            PAYMENT_ID
        );

        assertEquals(1, eventCount);

        // 4. OutboxMessage 중복 생성 방지 확인
        Integer outboxCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM outbox_messages
            WHERE deduplication_key = ?
            """,
            Integer.class,
            "payment-completed:" + PAYMENT_ID
        );

        assertEquals(1, outboxCount);
    }
}
