package io.github.wahhh.bacp.config;

import io.github.wahhh.bacp.common.constant.MQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares BACP exchanges, queues, and DLX bindings.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Main custody exchange (direct).
     *
     * @return exchange bean
     */
    @Bean
    public DirectExchange bacpTxExchange() {
        return new DirectExchange(MQConstants.EXCHANGE_TX, true, false);
    }

    /**
     * Alert exchange.
     *
     * @return exchange bean
     */
    @Bean
    public DirectExchange bacpAlertExchange() {
        return new DirectExchange(MQConstants.EXCHANGE_ALERT, true, false);
    }

    /**
     * Dead-letter exchange.
     *
     * @return exchange bean
     */
    @Bean
    public DirectExchange bacpDlxExchange() {
        return new DirectExchange(MQConstants.EXCHANGE_DLX, true, false);
    }

    /**
     * Deposit queue with DLX.
     *
     * @return queue bean
     */
    @Bean
    public Queue depositQueue() {
        return QueueBuilder.durable(MQConstants.QUEUE_DEPOSIT)
                .withArgument("x-dead-letter-exchange", MQConstants.EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", MQConstants.QUEUE_DLQ)
                .build();
    }

    /**
     * Withdraw queue with DLX.
     *
     * @return queue bean
     */
    @Bean
    public Queue withdrawQueue() {
        return QueueBuilder.durable(MQConstants.QUEUE_WITHDRAW)
                .withArgument("x-dead-letter-exchange", MQConstants.EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", MQConstants.QUEUE_DLQ)
                .build();
    }

    /**
     * Alert queue with DLX.
     *
     * @return queue bean
     */
    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(MQConstants.QUEUE_ALERT)
                .withArgument("x-dead-letter-exchange", MQConstants.EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", MQConstants.QUEUE_DLQ)
                .build();
    }

    /**
     * Dead-letter queue.
     *
     * @return queue bean
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(MQConstants.QUEUE_DLQ).build();
    }

    /**
     * Binds deposit queue.
     *
     * @param depositQueue queue
     * @param txExchange exchange
     * @return binding
     */
    @Bean
    public Binding depositBinding(Queue depositQueue, DirectExchange bacpTxExchange) {
        return BindingBuilder.bind(depositQueue).to(bacpTxExchange).with(MQConstants.ROUTING_DEPOSIT);
    }

    /**
     * Binds withdraw queue.
     *
     * @param withdrawQueue queue
     * @param txExchange    exchange
     * @return binding
     */
    @Bean
    public Binding withdrawBinding(Queue withdrawQueue, DirectExchange bacpTxExchange) {
        return BindingBuilder.bind(withdrawQueue).to(bacpTxExchange).with(MQConstants.ROUTING_WITHDRAW);
    }

    /**
     * Binds alert queue.
     *
     * @param alertQueue    queue
     * @param alertExchange exchange
     * @return binding
     */
    @Bean
    public Binding alertBinding(Queue alertQueue, DirectExchange bacpAlertExchange) {
        return BindingBuilder.bind(alertQueue).to(bacpAlertExchange).with(MQConstants.ROUTING_ALERT);
    }

    /**
     * Binds DLQ to DLX.
     *
     * @param dlqQueue queue
     * @param dlx      exchange
     * @return binding
     */
    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange bacpDlxExchange) {
        return BindingBuilder.bind(dlqQueue).to(bacpDlxExchange).with(MQConstants.QUEUE_DLQ);
    }
}
