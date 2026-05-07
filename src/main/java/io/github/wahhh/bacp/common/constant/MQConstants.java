package io.github.wahhh.bacp.common.constant;

/**
 * RabbitMQ exchange, queue, and routing key names.
 */
public final class MQConstants {

    private MQConstants() {
    }

    /** Main transaction fanout / topic exchange for custody flows. */
    public static final String EXCHANGE_TX = "bacp.tx.exchange";

    /** Alert notification exchange. */
    public static final String EXCHANGE_ALERT = "bacp.alert.exchange";

    /** Dead-letter exchange. */
    public static final String EXCHANGE_DLX = "bacp.dlx.exchange";

    /** Deposit ingestion queue. */
    public static final String QUEUE_DEPOSIT = "bacp.tx.deposit";

    /** Withdrawal processing queue. */
    public static final String QUEUE_WITHDRAW = "bacp.tx.withdraw";

    /** Risk / ops alert queue. */
    public static final String QUEUE_ALERT = "bacp.alert";

    /** Dead-letter queue. */
    public static final String QUEUE_DLQ = "bacp.dlq";

    /** Routing key for deposit messages. */
    public static final String ROUTING_DEPOSIT = "tx.deposit";

    /** Routing key for withdrawal messages. */
    public static final String ROUTING_WITHDRAW = "tx.withdraw";

    /** Routing key for alert messages. */
    public static final String ROUTING_ALERT = "alert";
}
