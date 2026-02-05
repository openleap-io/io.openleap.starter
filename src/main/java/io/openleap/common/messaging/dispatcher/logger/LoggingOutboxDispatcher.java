package io.openleap.common.messaging.dispatcher.logger;

import io.openleap.common.messaging.dispatcher.DispatchResult;
import io.openleap.common.messaging.dispatcher.OutboxDispatcher;
import io.openleap.common.messaging.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingOutboxDispatcher implements OutboxDispatcher {

    @Override
    public DispatchResult dispatch(OutboxEvent event) {
        log.info("[Mock-Dispatch] Sending {} to {}", event.getId(), event.getRoutingKey());
        return DispatchResult.ok();
    }
}
