package com.oldphonedeals.config;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldDeclareEmailQueuesWithDeadLetterSettings() {
        Queue mainQueue = config.emailSendQueue();
        Queue dlq = config.emailSendDlqQueue();

        assertEquals(RabbitMQConfig.EMAIL_SEND_QUEUE, mainQueue.getName());
        assertEquals(RabbitMQConfig.NOTIFICATION_EXCHANGE, mainQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.EMAIL_DLQ_ROUTING_KEY, mainQueue.getArguments().get("x-dead-letter-routing-key"));

        assertEquals(RabbitMQConfig.EMAIL_DLQ_QUEUE, dlq.getName());
        assertEquals(604800000, dlq.getArguments().get("x-message-ttl"));
    }

    @Test
    void shouldDeclareOrderQueuesWithDeadLetterSettings() {
        Queue mainQueue = config.orderPostProcessQueue();
        Queue dlq = config.orderPostProcessDlqQueue();

        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE, mainQueue.getName());
        assertEquals(RabbitMQConfig.ORDER_EXCHANGE, mainQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_ROUTING_KEY, mainQueue.getArguments().get("x-dead-letter-routing-key"));

        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE, dlq.getName());
        assertEquals(604800000, dlq.getArguments().get("x-message-ttl"));
    }

    @Test
    void shouldDeclareExpectedBindings() {
        Binding emailBinding = config.emailSendBinding();
        Binding emailDlqBinding = config.emailSendDlqBinding();
        Binding orderBinding = config.orderPostProcessBinding();
        Binding orderDlqBinding = config.orderPostProcessDlqBinding();

        assertEquals(RabbitMQConfig.EMAIL_SEND_QUEUE, emailBinding.getDestination());
        assertEquals(RabbitMQConfig.NOTIFICATION_EXCHANGE, emailBinding.getExchange());
        assertEquals(RabbitMQConfig.EMAIL_SEND_ROUTING_KEY, emailBinding.getRoutingKey());

        assertEquals(RabbitMQConfig.EMAIL_DLQ_QUEUE, emailDlqBinding.getDestination());
        assertEquals(RabbitMQConfig.NOTIFICATION_EXCHANGE, emailDlqBinding.getExchange());
        assertEquals(RabbitMQConfig.EMAIL_DLQ_ROUTING_KEY, emailDlqBinding.getRoutingKey());

        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE, orderBinding.getDestination());
        assertEquals(RabbitMQConfig.ORDER_EXCHANGE, orderBinding.getExchange());
        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_ROUTING_KEY, orderBinding.getRoutingKey());

        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE, orderDlqBinding.getDestination());
        assertEquals(RabbitMQConfig.ORDER_EXCHANGE, orderDlqBinding.getExchange());
        assertEquals(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_ROUTING_KEY, orderDlqBinding.getRoutingKey());
    }

    @Test
    void shouldConfigureRabbitTemplateWithMandatoryJsonConverter() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter messageConverter = config.messageConverter();

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory, messageConverter);

        assertNotNull(rabbitTemplate);
        assertEquals(connectionFactory, rabbitTemplate.getConnectionFactory());
        assertTrue(Boolean.TRUE.equals(rabbitTemplate.isMandatoryFor(new Message(new byte[0], new MessageProperties()))));
        assertInstanceOf(Jackson2JsonMessageConverter.class, rabbitTemplate.getMessageConverter());
    }

    @Test
    void shouldSupportJavaTimeSerializationInMessageConverter() {
        MessageConverter messageConverter = config.messageConverter();
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-1")
            .type(EmailType.GENERIC)
            .toEmail("test@example.com")
            .subject("subject")
            .htmlContent("<p>hello</p>")
            .timestamp(Instant.now())
            .build();

        Message converted = messageConverter.toMessage(message, new MessageProperties());

        assertNotNull(converted);
    }

    @Test
    void shouldConfigureEmailListenerContainerFactoryWithManualAckNoRequeueAndRetry() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter messageConverter = config.messageConverter();

        SimpleRabbitListenerContainerFactory factory = config.emailListenerContainerFactory(connectionFactory, messageConverter);

        assertEquals(AcknowledgeMode.MANUAL, ReflectionTestUtils.getField(factory, "acknowledgeMode"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(factory, "defaultRequeueRejected"));
        assertRetryConfig(factory.getAdviceChain(), 3, 5000L, 3.0, 45000L);
    }

    @Test
    void shouldConfigureOrderListenerContainerFactoryWithManualAckNoRequeueAndRetry() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter messageConverter = config.messageConverter();

        SimpleRabbitListenerContainerFactory factory = config.orderPostProcessListenerContainerFactory(connectionFactory, messageConverter);

        assertEquals(AcknowledgeMode.MANUAL, ReflectionTestUtils.getField(factory, "acknowledgeMode"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(factory, "defaultRequeueRejected"));
        assertRetryConfig(factory.getAdviceChain(), 3, 2000L, 3.0, 18000L);
    }

    private void assertRetryConfig(Advice[] adviceChain, int maxAttempts, long initialInterval, double multiplier, long maxInterval) {
        assertNotNull(adviceChain);
        assertEquals(1, adviceChain.length);
        assertInstanceOf(MethodInterceptor.class, adviceChain[0]);

        Object retryOperations = ReflectionTestUtils.getField(adviceChain[0], "retryOperations");
        RetryTemplate retryTemplate = assertInstanceOf(RetryTemplate.class, retryOperations);

        SimpleRetryPolicy retryPolicy = assertInstanceOf(
            SimpleRetryPolicy.class,
            ReflectionTestUtils.getField(retryTemplate, "retryPolicy")
        );
        assertEquals(maxAttempts, retryPolicy.getMaxAttempts());

        ExponentialBackOffPolicy backOffPolicy = assertInstanceOf(
            ExponentialBackOffPolicy.class,
            ReflectionTestUtils.getField(retryTemplate, "backOffPolicy")
        );
        assertEquals(initialInterval, backOffPolicy.getInitialInterval());
        assertEquals(multiplier, backOffPolicy.getMultiplier());
        assertEquals(maxInterval, backOffPolicy.getMaxInterval());
    }
}
