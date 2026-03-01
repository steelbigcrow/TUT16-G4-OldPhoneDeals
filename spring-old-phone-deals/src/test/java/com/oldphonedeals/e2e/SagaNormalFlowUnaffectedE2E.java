package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
class SagaNormalFlowUnaffectedE2E extends AbstractSagaE2E {

    @Test
    void shouldKeepNormalCheckoutPostProcessFlowUnaffected() {
        User buyer = seedVerifiedUser("buyer-normal");
        User seller = seedVerifiedUser("seller-normal");
        Phone phone = seedPhone(seller, "Saga Normal Phone", 8, 2, 499.0);

        int purchaseQuantity = 2;
        int stockBeforeCheckout = phone.getStock();
        int salesBeforeCheckout = phone.getSalesCount();

        String token = loginAndGetToken(buyer.getEmail(), DEFAULT_PASSWORD);
        addItemToCart(token, phone.getId(), purchaseQuantity);
        String orderId = checkout(token);

        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                Order order = orderRepository.findById(orderId).orElse(null);
                assertNotNull(order);
                assertEquals(OrderCheckoutStatus.COMPLETED, order.getCheckoutStatus());
                assertEquals(OrderPostProcessStatus.SUCCESS, order.getPostProcessStatus());

                Phone updatedPhone = phoneRepository.findById(phone.getId()).orElse(null);
                assertNotNull(updatedPhone);
                assertEquals(stockBeforeCheckout - purchaseQuantity, updatedPhone.getStock());
                assertEquals(salesBeforeCheckout + purchaseQuantity, updatedPhone.getSalesCount());

                assertEquals(0, sagaLogRepository.count());
            });

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }

    private String loginAndGetToken(String email, String password) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", requestBody, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(Boolean.TRUE, body.get("success"));

        Map<String, Object> data = castToMap(body.get("data"));
        String token = (String) data.get("token");
        assertNotNull(token);
        assertTrue(!token.isBlank());
        return token;
    }

    private void addItemToCart(String token, String phoneId, int quantity) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("phoneId", phoneId);
        requestBody.put("quantity", quantity);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, authorizedHeaders(token));
        ResponseEntity<Map> response = restTemplate.exchange("/api/cart", HttpMethod.POST, requestEntity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(Boolean.TRUE, body.get("success"));
    }

    private String checkout(String token) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("street", "1 E2E Street");
        address.put("city", "Sydney");
        address.put("state", "NSW");
        address.put("zip", "2000");
        address.put("country", "Australia");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("address", address);

        HttpHeaders headers = authorizedHeaders(token);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange("/api/orders/checkout", HttpMethod.POST, requestEntity, Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(Boolean.TRUE, body.get("success"));

        Map<String, Object> data = castToMap(body.get("data"));
        String orderId = (String) data.get("id");
        assertNotNull(orderId);
        assertTrue(!orderId.isBlank());
        return orderId;
    }

    private HttpHeaders authorizedHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object value) {
        return (Map<String, Object>) value;
    }
}
