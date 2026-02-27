package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
abstract class AbstractRabbitMqIT {

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.12-management-alpine")
    );

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
    }

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @BeforeEach
    void purgeQueues() {
        purgeQueue(RabbitMQConfig.EMAIL_SEND_QUEUE);
        purgeQueue(RabbitMQConfig.EMAIL_DLQ_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE);
    }

    protected void purgeQueue(String queueName) {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(queueName);
            return null;
        });
    }

    protected int queueMessageCount(String queueName) {
        return rabbitTemplate.execute(channel ->
            channel.queueDeclarePassive(queueName).getMessageCount()
        );
    }
}
