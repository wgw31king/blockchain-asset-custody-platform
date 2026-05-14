package io.github.wahhh.bacp.service.trade;

import io.github.wahhh.bacp.common.enums.OrderStatus;
import io.github.wahhh.bacp.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTransitionsTest {

    @Test
    void pendingToProcessingAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.PROCESSING));
    }

    @Test
    void pendingToCancelledAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
    }

    @Test
    void pendingToRejectedAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.REJECTED));
    }

    @Test
    void pendingToFilledPassiveMakerAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.FILLED));
    }

    @Test
    void pendingToPartiallyFilledPassiveMakerAllowed() {
        assertDoesNotThrow(() ->
                OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED));
    }

    @Test
    void processingToPendingRestingAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PROCESSING, OrderStatus.PENDING));
    }

    @Test
    void processingToFilledAllowed() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PROCESSING, OrderStatus.FILLED));
    }

    @Test
    void processingToPartiallyFilledAllowed() {
        assertDoesNotThrow(() ->
                OrderStatusTransitions.assertCanTransition(OrderStatus.PROCESSING, OrderStatus.PARTIALLY_FILLED));
    }

    @Test
    void partiallyFilledToProcessingAllowed() {
        assertDoesNotThrow(() ->
                OrderStatusTransitions.assertCanTransition(OrderStatus.PARTIALLY_FILLED, OrderStatus.PROCESSING));
    }

    @Test
    void partiallyFilledToFilledAllowed() {
        assertDoesNotThrow(() ->
                OrderStatusTransitions.assertCanTransition(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED));
    }

    @Test
    void filledToAnythingRejected() {
        assertThrows(BizException.class,
                () -> OrderStatusTransitions.assertCanTransition(OrderStatus.FILLED, OrderStatus.PENDING));
    }

    @Test
    void cancelledToAnythingRejected() {
        assertThrows(BizException.class,
                () -> OrderStatusTransitions.assertCanTransition(OrderStatus.CANCELLED, OrderStatus.PENDING));
    }

    @Test
    void sameStatusNoOp() {
        assertDoesNotThrow(() -> OrderStatusTransitions.assertCanTransition(OrderStatus.PENDING, OrderStatus.PENDING));
    }

    @Test
    void parseValid() {
        assertDoesNotThrow(() -> OrderStatusTransitions.parse("PENDING"));
    }

    @Test
    void parseInvalidThrows() {
        assertThrows(BizException.class, () -> OrderStatusTransitions.parse("OPEN"));
    }
}
