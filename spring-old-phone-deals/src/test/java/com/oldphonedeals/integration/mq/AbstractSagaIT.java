package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.consumer.SagaCompensationOrchestrator;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.StepStatus;
import com.oldphonedeals.producer.CompensationMessageProducer;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.SagaLogRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.impl.SagaCompensationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@SpringBootTest(classes = AbstractSagaIT.SagaMqTestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractSagaIT extends AbstractRabbitMqIT {

    @Autowired
    protected CompensationMessageProducer compensationMessageProducer;

    @MockBean
    protected SagaLogRepository sagaLogRepository;

    @MockBean
    protected PhoneStockRepository phoneStockRepository;

    @MockBean
    protected OrderRepository orderRepository;

    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected EmailMessageProducer emailMessageProducer;

    @BeforeEach
    void purgeCompensationQueues() {
        purgeQueue(RabbitMQConfig.ORDER_COMPENSATION_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE);
    }

    protected OrderCompensationMessage buildCompensationMessage(String sagaId, String orderId, String userId) {
        return buildCompensationMessage(
            sagaId,
            orderId,
            userId,
            List.of(OrderCompensationMessage.Item.builder().phoneId("phone-1").quantity(1).build()),
            "post-process failed"
        );
    }

    protected OrderCompensationMessage buildCompensationMessage(
        String sagaId,
        String orderId,
        String userId,
        List<OrderCompensationMessage.Item> items,
        String reason
    ) {
        return OrderCompensationMessage.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .userId(userId)
            .items(items)
            .totalAmount(199.0)
            .reason(reason)
            .timestamp(Instant.now())
            .build();
    }

    protected Order buildOrder(String orderId) {
        return Order.builder()
            .id(orderId)
            .postProcessStatus(OrderPostProcessStatus.COMPENSATING)
            .checkoutStatus(OrderCheckoutStatus.COMPLETED)
            .build();
    }

    protected User buildUser(String userId) {
        return User.builder()
            .id(userId)
            .firstName("Integration")
            .lastName("Buyer")
            .email("integration+" + UUID.randomUUID() + "@example.com")
            .build();
    }

    protected boolean hasStep(SagaLog log, String stepName, StepStatus status) {
        if (log == null || log.getSteps() == null) {
            return false;
        }
        return log.getSteps().stream().anyMatch(step ->
            stepName.equals(step.getStepName()) && status == step.getStatus()
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    @Import({
        RabbitMQConfig.class,
        CompensationMessageProducer.class,
        SagaCompensationOrchestrator.class,
        SagaCompensationServiceImpl.class
    })
    static class SagaMqTestApplication {
    }
}
