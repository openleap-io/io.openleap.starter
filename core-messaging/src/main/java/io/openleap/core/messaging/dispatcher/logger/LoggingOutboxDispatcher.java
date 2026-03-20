package io.openleap.core.messaging.dispatcher.logger;

import io.openleap.core.messaging.dispatcher.DispatchResult;
import io.openleap.core.messaging.dispatcher.OutboxDispatcher;
import io.openleap.core.messaging.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingOutboxDispatcher implements OutboxDispatcher {

    @Override
    public DispatchResult dispatch(OutboxEvent event) {
        log.info("[Mock-Dispatch] Sending {} to {}", event.getId(), event.getRoutingKey());
        return DispatchResult.ok();
    }
}
