package io.openleap.starter.core.messaging.dispatcher;

import io.openleap.starter.core.repository.entity.OlOutboxEvent;

// TODO (itasesk): Consider use of OutboxEvent in the API
public interface OutboxDispatcher {

    /**
     * Dispatches the event to the external system.
     * @return DispatchResult if successfully acknowledged, DispatchResult with reason otherwise.
     */
    DispatchResult dispatch(OlOutboxEvent event) throws Exception;

}
