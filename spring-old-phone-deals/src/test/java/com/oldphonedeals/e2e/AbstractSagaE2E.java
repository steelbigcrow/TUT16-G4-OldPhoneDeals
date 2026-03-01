package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.enums.StepStatus;
import com.oldphonedeals.producer.CompensationMessageProducer;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.ProcessedMessageRepository;
import com.oldphonedeals.repository.SagaLogRepository;
import com.oldphonedeals.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(
    classes = AbstractSagaE2E.SagaE2eTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AbstractSagaE2E.E2eMailTestConfiguration.class)
abstract class AbstractSagaE2E {

    protected static final String DEFAULT_PASSWORD = "Password123!";

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(
        DockerImageName.parse("mongo:7.0.5")
    );

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.12-management-alpine")
    );

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("app.e2e.enabled", () -> "true");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected CompensationMessageProducer compensationMessageProducer;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PhoneRepository phoneRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected SagaLogRepository sagaLogRepository;

    @Autowired
    protected ProcessedMessageRepository processedMessageRepository;

    @BeforeEach
    void resetE2eState() {
        sagaLogRepository.deleteAll();
        processedMessageRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        phoneRepository.deleteAll();
        userRepository.deleteAll();

        purgeQueue(RabbitMQConfig.EMAIL_SEND_QUEUE);
        purgeQueue(RabbitMQConfig.EMAIL_DLQ_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_COMPENSATION_QUEUE);
        purgeQueue(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE);
    }

    protected void publishCompensation(OrderCompensationMessage message) {
        compensationMessageProducer.publish(message);
    }

    protected OrderCompensationMessage.Item compensationItem(String phoneId, int quantity) {
        return OrderCompensationMessage.Item.builder()
            .phoneId(phoneId)
            .quantity(quantity)
            .build();
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
            .totalAmount(items.stream().mapToInt(OrderCompensationMessage.Item::getQuantity).sum() * 100.0)
            .reason(reason)
            .timestamp(Instant.now())
            .build();
    }

    protected User seedVerifiedUser(String label) {
        String unique = label + "-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
            .firstName(label)
            .lastName("E2E")
            .email(unique + "@example.com")
            .password(passwordEncoder.encode(DEFAULT_PASSWORD))
            .role("USER")
            .isAdmin(false)
            .isDisabled(false)
            .isBan(false)
            .isVerified(true)
            .createdAt(now)
            .updatedAt(now)
            .build());
    }

    protected Phone seedPhone(User seller, String title, int stock, int salesCount, double price) {
        LocalDateTime now = LocalDateTime.now();
        return phoneRepository.save(Phone.builder()
            .title(title + " " + UUID.randomUUID())
            .brand(PhoneBrand.APPLE)
            .image("https://example.com/e2e-phone.jpg")
            .stock(stock)
            .price(price)
            .salesCount(salesCount)
            .seller(seller)
            .isDisabled(false)
            .createdAt(now)
            .updatedAt(now)
            .build());
    }

    protected Order seedCompensatingOrder(
        User buyer,
        List<OrderCompensationMessage.Item> items,
        double totalAmount
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<Order.OrderItem> orderItems = items.stream()
            .map(item -> Order.OrderItem.builder()
                .phoneId(item.getPhoneId())
                .title(resolvePhoneTitle(item.getPhoneId()))
                .quantity(item.getQuantity())
                .price(100.0)
                .build())
            .collect(Collectors.toList());

        return orderRepository.save(Order.builder()
            .userId(buyer.getId())
            .items(orderItems)
            .totalAmount(totalAmount)
            .address(Order.Address.builder()
                .street("1 E2E Street")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build())
            .checkoutStatus(OrderCheckoutStatus.COMPLETED)
            .postProcessStatus(OrderPostProcessStatus.COMPENSATING)
            .createdAt(now)
            .build());
    }

    protected int queueMessageCount(String queueName) {
        return rabbitTemplate.execute(channel ->
            channel.queueDeclarePassive(queueName).getMessageCount()
        );
    }

    protected boolean hasStep(SagaLog log, String stepName, StepStatus status) {
        if (log == null || log.getSteps() == null) {
            return false;
        }
        return log.getSteps().stream().anyMatch(step ->
            stepName.equals(step.getStepName()) && status == step.getStatus()
        );
    }

    private String resolvePhoneTitle(String phoneId) {
        return phoneRepository.findById(phoneId)
            .map(Phone::getTitle)
            .orElse("Unknown phone");
    }

    private void purgeQueue(String queueName) {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(queueName);
            return null;
        });
    }

    @SpringBootConfiguration
    @AutoConfigurationPackage(basePackages = "com.oldphonedeals")
    @EnableAutoConfiguration
    @EnableMongoAuditing
    @ComponentScan(
        basePackages = "com.oldphonedeals",
        excludeFilters = {
            @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.oldphonedeals\\.integration\\..*"
            ),
            @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = TestConfiguration.class
            ),
            @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = SpringBootConfiguration.class
            )
        }
    )
    static class SagaE2eTestApplication {
    }

    @TestConfiguration
    static class E2eMailTestConfiguration {

        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return new JavaMailSender() {
                @Override
                public MimeMessage createMimeMessage() {
                    return new MimeMessage((Session) null);
                }

                @Override
                public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
                    try {
                        return new MimeMessage((Session) null, contentStream);
                    } catch (Exception ex) {
                        throw new MailParseException("Failed to parse mime message", ex);
                    }
                }

                @Override
                public void send(MimeMessage mimeMessage) {
                }

                @Override
                public void send(MimeMessage... mimeMessages) {
                }

                @Override
                public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
                    try {
                        MimeMessage mimeMessage = createMimeMessage();
                        mimeMessagePreparator.prepare(mimeMessage);
                    } catch (Exception ex) {
                        throw new MailPreparationException("Failed to prepare mime message", ex);
                    }
                }

                @Override
                public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
                    for (MimeMessagePreparator preparator : mimeMessagePreparators) {
                        send(preparator);
                    }
                }

                @Override
                public void send(SimpleMailMessage simpleMessage) {
                }

                @Override
                public void send(SimpleMailMessage... simpleMessages) {
                }
            };
        }
    }
}
