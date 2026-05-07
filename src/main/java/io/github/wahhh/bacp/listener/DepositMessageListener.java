package io.github.wahhh.bacp.listener;

import io.github.wahhh.bacp.common.constant.MQConstants;
import io.github.wahhh.bacp.common.util.JsonUtil;
import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.service.DepositFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter forwarding indexer payloads into {@link DepositFacade}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositMessageListener {

    private final DepositFacade depositFacade;

    /**
     * Applies deposit updates from queue consumers.
     *
     * @param json JSON payload
     */
    @RabbitListener(queues = MQConstants.QUEUE_DEPOSIT)
    public void onMessage(String json) {
        try {
            DepositNotifyRequest body = JsonUtil.fromJson(json, DepositNotifyRequest.class);
            depositFacade.handleDeposit(null, body);
        } catch (Exception ex) {
            log.warn("Deposit MQ handling failed: {}", ex.getMessage());
            throw ex;
        }
    }
}
