package io.github.wahhh.bacp.custody.spi;

import io.github.wahhh.bacp.dto.request.DepositNotifyRequest;
import io.github.wahhh.bacp.service.DepositFacade;
import org.springframework.stereotype.Component;

/**
 * Routes indexer-shaped deposit events into {@link DepositFacade}.
 */
@Component
public class LotteryIndexerEventConsumer implements BlockchainEventConsumer {

    private final DepositFacade depositFacade;

    /**
     * @param depositFacade custody deposit pipeline
     */
    public LotteryIndexerEventConsumer(DepositFacade depositFacade) {
        this.depositFacade = depositFacade;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeposit(DepositNotifyRequest req) {
        depositFacade.handleDeposit(null, req);
    }
}
