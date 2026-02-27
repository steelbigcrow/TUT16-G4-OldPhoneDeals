package com.oldphonedeals.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

  public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
  public static final String EMAIL_SEND_QUEUE = "email.send.queue";
  public static final String EMAIL_SEND_ROUTING_KEY = "email.send";
  public static final String EMAIL_DLQ_QUEUE = "email.send.dlq.queue";
  public static final String EMAIL_DLQ_ROUTING_KEY = "email.send.dlq";

  public static final String ORDER_EXCHANGE = "order.exchange";
  public static final String ORDER_POST_PROCESS_QUEUE = "order.post.process.queue";
  public static final String ORDER_POST_PROCESS_ROUTING_KEY = "order.post.process";
  public static final String ORDER_POST_PROCESS_DLQ_QUEUE = "order.post.process.dlq.queue";
  public static final String ORDER_POST_PROCESS_DLQ_ROUTING_KEY = "order.post.process.dlq";

  @Bean
  public TopicExchange notificationExchange() {
    return new TopicExchange(NOTIFICATION_EXCHANGE);
  }

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE);
  }

  @Bean
  public Queue emailSendQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", NOTIFICATION_EXCHANGE);
    args.put("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY);
    return new Queue(EMAIL_SEND_QUEUE, true, false, false, args);
  }

  @Bean
  public Queue emailSendDlqQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-message-ttl", 604800000);
    return new Queue(EMAIL_DLQ_QUEUE, true, false, false, args);
  }

  @Bean
  public Queue orderPostProcessQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", ORDER_EXCHANGE);
    args.put("x-dead-letter-routing-key", ORDER_POST_PROCESS_DLQ_ROUTING_KEY);
    return new Queue(ORDER_POST_PROCESS_QUEUE, true, false, false, args);
  }

  @Bean
  public Queue orderPostProcessDlqQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-message-ttl", 604800000);
    return new Queue(ORDER_POST_PROCESS_DLQ_QUEUE, true, false, false, args);
  }

  @Bean
  public Binding emailSendBinding() {
    return BindingBuilder.bind(emailSendQueue()).to(notificationExchange()).with(EMAIL_SEND_ROUTING_KEY);
  }

  @Bean
  public Binding emailSendDlqBinding() {
    return BindingBuilder.bind(emailSendDlqQueue()).to(notificationExchange()).with(EMAIL_DLQ_ROUTING_KEY);
  }

  @Bean
  public Binding orderPostProcessBinding() {
    return BindingBuilder.bind(orderPostProcessQueue()).to(orderExchange()).with(ORDER_POST_PROCESS_ROUTING_KEY);
  }

  @Bean
  public Binding orderPostProcessDlqBinding() {
    return BindingBuilder.bind(orderPostProcessDlqQueue()).to(orderExchange()).with(ORDER_POST_PROCESS_DLQ_ROUTING_KEY);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    rabbitTemplate.setMandatory(true);
    return rabbitTemplate;
  }

  @Bean(name = "emailListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory emailListenerContainerFactory(
    ConnectionFactory connectionFactory,
    MessageConverter messageConverter
  ) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(
      RetryInterceptorBuilder.stateless()
        .maxAttempts(3)
        .backOffOptions(5000, 3.0, 45000)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build()
    );
    return factory;
  }

  @Bean(name = "orderPostProcessListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory orderPostProcessListenerContainerFactory(
    ConnectionFactory connectionFactory,
    MessageConverter messageConverter
  ) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);
    factory.setAdviceChain(
      RetryInterceptorBuilder.stateless()
        .maxAttempts(3)
        .backOffOptions(2000, 3.0, 18000)
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build()
    );
    return factory;
  }
}
