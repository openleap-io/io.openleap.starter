package io.openleap.common.messaging.dispatcher;

import io.openleap.common.messaging.entity.OutboxEvent;

// TODO (itasesk): Consider use of OutboxEvent in the API
public interface OutboxDispatcher {

    /**
     * Dispatches the event to the external system.
     *
     * @return DispatchResult if successfully acknowledged, DispatchResult with reason otherwise.
     */
    DispatchResult dispatch(OutboxEvent event) throws Exception;

}
